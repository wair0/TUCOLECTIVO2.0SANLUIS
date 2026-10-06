package com.tucolectivo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.tucolectivo.app.data.SmartMoveApi
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class ArrivalNotificationService : Service() {
    companion object {
        const val PREFS = "arrival_notifications"
        const val EXTRA_FAVORITES = "favorites_json"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val ACTION_STOP = "com.tucolectivo.app.STOP_ARRIVAL_NOTIFICATIONS"
        private const val SERVICE_CHANNEL = "arrival_monitor_service"
        private const val ALERT_CHANNEL = "arrival_alerts_v2"
        private const val SERVICE_ID = 4201
        private const val LIVE_NOTIFICATION_BASE_ID = 30000
        private const val POLL_INTERVAL_MS = 10_000L
        private val THRESHOLDS = listOf(10, 5, 3, 1, 0)
    }

    private val api = SmartMoveApi()
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val liveNotificationIds = mutableSetOf<Int>()
    @Volatile private var polling = false
    @Volatile private var destroyed = false

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        intent?.getStringExtra(EXTRA_FAVORITES)?.let { json ->
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("favorites", json)
                .putString("start_time", intent.getStringExtra(EXTRA_START_TIME) ?: "18:00")
                .putString("end_time", intent.getStringExtra(EXTRA_END_TIME) ?: "19:30")
                .putBoolean("enabled", true).apply()
        }
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        schedulePoll(0L)
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val openApp = PendingIntent.getActivity(
            this, 4201, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("TU COLECTIVO · ALERTAS ACTIVAS")
            .setContentText("Vigilando los arribos guardados en Favoritos")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(SERVICE_ID, notification)
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(SERVICE_CHANNEL, "Seguimiento de arribos", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Indica que se están vigilando los arribos favoritos"
        })
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(NotificationChannel(ALERT_CHANNEL, "Alertas de colectivos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos sonoros cuando se aproxima un colectivo guardado en Favoritos"
            enableVibration(true)
            setSound(notificationSound, audioAttributes)
        })
    }

    private fun schedulePoll(delay: Long) {
        handler.removeCallbacks(pollRunnable)
        if (!destroyed) handler.postDelayed(pollRunnable, delay)
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (destroyed || polling) return
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            if (!prefs.getBoolean("enabled", false)) {
                stopSelf()
                return
            }
            val startMinute = parseTime(prefs.getString("start_time", "18:00") ?: "18:00")
            val endMinute = parseTime(prefs.getString("end_time", "19:30") ?: "19:30")
            val now = java.util.Calendar.getInstance()
            val nowMinute = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            if (startMinute == null || endMinute == null || endMinute <= startMinute) {
                prefs.edit().putBoolean("enabled", false).apply()
                stopSelf()
                return
            }
            if (nowMinute >= endMinute) {
                prefs.edit().putBoolean("enabled", false).apply()
                stopForeground(true)
                stopSelf()
                return
            }
            if (nowMinute < startMinute) {
                schedulePoll(POLL_INTERVAL_MS)
                return
            }
            polling = true
            executor.execute {
                try {
                    val favorites = JSONArray(prefs.getString("favorites", "[]") ?: "[]")
                    for (index in 0 until favorites.length()) {
                        val favorite = favorites.optJSONObject(index) ?: continue
                        val line = favorite.optInt("line", 0)
                        val stop = favorite.optJSONObject("stop") ?: continue
                        val identifier = stop.optString("identifier", stop.optString("id", ""))
                        if (line <= 0 || identifier.isBlank()) continue
                        val arrivals = runCatching { api.getArrivals(identifier, line, timeoutMs = 8_000) }.getOrDefault(emptyList())
                        for (arrival in arrivals) {
                            val minutes = arrival.minutes ?: if (arrival.status.contains("arrib", true)) 0 else continue
                            updateLiveArrivalNotification(favorite, line, identifier, arrival.vehicleId, arrival.destination, minutes)
                            evaluateArrival(favorite, line, identifier, arrival.vehicleId, arrival.destination, minutes)
                        }
                    }
                } catch (_: Exception) {
                    // Un fallo de red no desactiva el seguimiento; se reintenta en el próximo ciclo.
                } finally {
                    polling = false
                    schedulePoll(POLL_INTERVAL_MS)
                }
            }
        }
    }

    private fun parseTime(value: String): Int? {
        val match = Regex("^(?:([01]\\d|2[0-3])):([0-5]\\d)$").matchEntire(value) ?: return null
        return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
    }

    private fun updateLiveArrivalNotification(
        favorite: JSONObject,
        line: Int,
        identifier: String,
        vehicleId: String,
        destination: String,
        minutes: Int
    ) {
        val manager = getSystemService(NotificationManager::class.java)
        val favoriteKey = favorite.optString("key", "$identifier::$line")
        val notificationId = LIVE_NOTIFICATION_BASE_ID + (favoriteKey.hashCode().toLong().and(0x7fffffff).toInt() % 10000)
        liveNotificationIds.add(notificationId)
        val stopName = favorite.optJSONObject("stop")?.optString("name", "Tu parada") ?: "Tu parada"
        val publicDestination = destination.trim().ifBlank { "Destino no informado" }
        val timeLabel = if (minutes <= 0) "ARRIBANDO" else "FALTAN $minutes MIN"
        val openApp = PendingIntent.getActivity(
            this, notificationId, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("LÍNEA $line · $timeLabel")
            .setContentText("$stopName · $publicDestination")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$stopName · $publicDestination\n$timeLabel"
                )
            )
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        runCatching { manager.notify(notificationId, notification) }
    }

    private fun evaluateArrival(favorite: JSONObject, line: Int, identifier: String, vehicleId: String, destination: String, minutes: Int) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val favoriteKey = favorite.optString("key", "$identifier::$line")
        val serviceIdentity = vehicleId.takeIf { it.isNotBlank() } ?: destination.trim().lowercase().ifBlank { "servicio" }
        val signature = "${favoriteKey}_${serviceIdentity.hashCode()}"
        val lastKey = "last_$signature"
        val firedKey = "fired_$signature"
        val previous = prefs.getInt(lastKey, Int.MAX_VALUE)
        var fired = prefs.getString(firedKey, "")!!.split(',').filter { it.isNotBlank() }.toMutableSet()
        if (previous != Int.MAX_VALUE && minutes > previous + 3) fired.clear()

        val stage = when {
            minutes <= 0 && (previous > 0 || "0" !in fired) -> "0"
            minutes == 1 && previous > 1 -> "1"
            minutes in 2..3 && previous > 3 -> "3"
            minutes in 4..5 && previous > 5 -> "5"
            minutes in 6..10 && previous > 10 -> "10"
            previous == Int.MAX_VALUE -> when (minutes) {
                in 6..10 -> "10"
                in 4..5 -> "5"
                in 2..3 -> "3"
                1 -> "1"
                else -> if (minutes <= 0) "0" else null
            }
            else -> null
        }
        if (stage != null && stage !in fired) {
            val label = when (stage) {
                "10" -> "FALTAN 10 MINUTOS"
                "5" -> "FALTAN 5 MINUTOS"
                "3" -> "FALTAN 3 MINUTOS"
                "1" -> "FALTA 1 MINUTO"
                else -> "EL COLECTIVO ESTÁ ARRIBANDO"
            }
            val stopName = favorite.optJSONObject("stop")?.optString("name", "Tu parada") ?: "Tu parada"
            postArrivalNotification(signature, line, stopName, destination, label, minutes, stage)
            fired.add(stage)
            appendHistory(line, stopName, destination, label, minutes)
        }
        prefs.edit().putInt(lastKey, minutes).putString(firedKey, fired.joinToString(",")).apply()
    }

    private fun postArrivalNotification(signature: String, line: Int, stop: String, destination: String, label: String, minutes: Int, stage: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val id = 5000 + (signature.hashCode().toLong().and(0x7fffffff).toInt() % 100000) + stage.hashCode().let { kotlin.math.abs(it % 1000) }
        val title = "LÍNEA $line · $label"
        val text = listOf(stop, destination, if (minutes <= 0) "ARRIBANDO" else "$minutes min").filter { it.isNotBlank() }.joinToString(" · ")
        val openApp = PendingIntent.getActivity(
            this, id, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .build()
        runCatching { manager.notify(id, notification) }
    }

    private fun appendHistory(line: Int, stop: String, destination: String, label: String, minutes: Int) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val history = runCatching { JSONArray(prefs.getString("history", "[]")) }.getOrDefault(JSONArray())
        val next = JSONArray()
        next.put(JSONObject().put("line", line).put("stop", stop).put("destination", destination).put("message", label).put("minutes", minutes).put("timestamp", System.currentTimeMillis()))
        for (i in 0 until history.length().coerceAtMost(19)) next.put(history.optJSONObject(i) ?: JSONObject())
        prefs.edit().putString("history", next.toString()).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        getSystemService(NotificationManager::class.java)?.let { manager ->
            liveNotificationIds.forEach { runCatching { manager.cancel(it) } }
        }
        liveNotificationIds.clear()
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }
}
