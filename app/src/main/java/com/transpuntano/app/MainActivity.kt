package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.transpuntano.app.data.SmartMoveApi
import com.transpuntano.app.model.*
import com.transpuntano.app.ui.CyberMapView
import com.transpuntano.app.ui.MapStop
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private data class FavoriteStop(
        val lineCode: Int,
        val lineName: String,
        val stopCode: Int,
        val description: String,
        val identifier: String,
        val street: String,
        val intersection: String,
        val latitude: Double,
        val longitude: Double
    )
    private val api = SmartMoveApi()
    private val executor = Executors.newFixedThreadPool(3)

    private lateinit var content: FrameLayout
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var navBar: LinearLayout

    private val cyan = 0xFF00F0FF.toInt()
    private val pink = 0xFFFF2DB2.toInt()
    private val bg = 0xFF05070C.toInt()
    private val panelColor = 0xFF0B1018.toInt()
    private val muted = 0xFF8CA5B5.toInt()
    private lateinit var drawerPanel: LinearLayout
    private var drawerOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        showHome()
    }

    private fun buildShell() {
        val rootFrame = FrameLayout(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val headerLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(20), dp(16), dp(20), dp(10)); setBackgroundColor(0xFF080C13.toInt()); gravity = Gravity.CENTER_VERTICAL }
        val menuBtn = TextView(this).apply { text = "☰"; textSize = 24f; setTextColor(cyan); setPadding(0, 0, dp(16), 0); setOnClickListener { toggleDrawer() } }
        headerLayout.addView(menuBtn, LinearLayout.LayoutParams(dp(40), dp(40)))
        val headerContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        headerContent.addView(TextView(this).apply { text = "TU COLECTIVO 2.0"; textSize = 24f; typeface = Typeface.MONOSPACE; setTextColor(cyan) })
        title = TextView(this).apply { text = ""; textSize = 11f; setTextColor(muted) }
        headerContent.addView(title)
        status = TextView(this).apply { text = "● SISTEMA LISTO"; textSize = 16f; setTextColor(0xFF55FFB0.toInt()) }
        headerContent.addView(status)
        headerLayout.addView(headerContent)
        root.addView(headerLayout)
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        navBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFF080C13.toInt()); visibility = View.GONE }
        root.addView(navBar, LinearLayout.LayoutParams(-1, dp(64)))
        rootFrame.addView(root)
        drawerPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0E1420.toInt()); layoutParams = FrameLayout.LayoutParams(dp(250), -1).apply { gravity = Gravity.START }; visibility = View.GONE }
        addDrawerItems()
        rootFrame.addView(drawerPanel)
        setContentView(rootFrame)
        updateNav(0)
    }

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen
        if (drawerOpen) {
            drawerPanel.visibility = android.view.View.VISIBLE
        } else {
            drawerPanel.visibility = android.view.View.GONE
        }
    }

    private fun addDrawerItems() {
        val items = listOf(
            Triple("⌂", "INICIO", 0),
            Triple("▤", "LÍNEAS", 1),
            Triple("★", "FAVORITOS", 2),
            Triple("◎", "CERCA", 3)
        )
        items.forEach { (icon, label, index) ->
            drawerPanel.addView(TextView(this).apply {
                text = "$icon  $label"
                textSize = 16f
                typeface = Typeface.MONOSPACE
                setTextColor(cyan)
                setPadding(dp(16), dp(20), dp(16), dp(20))
                setOnClickListener {
                    navigateTo(index)
                    toggleDrawer()
                }
            })
        }
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome()
            1 -> showLines()
            2 -> showFavorites()
            3 -> showNearby()
        }
    }
    private fun updateNav(selected: Int) {
        navBar.removeAllViews()
        val items = listOf("⌂\nINICIO", "▤\nLÍNEAS", "★\nFAVORITOS", "◎\nCERCA")
        items.forEachIndexed { index, label ->
            navBar.addView(TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(if (index == selected) cyan else muted)
                setOnClickListener {
                    when (index) {
                        0 -> showHome()
                        1 -> showLines()
                        2 -> showFavorites()
                        3 -> showNearby()
                    }
                }
            }, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun showHome() {
        title.text = ""
        updateNav(0)
        content.removeAllViews()
        val box = box()
        box.addView(TextView(this).apply {
            text = "MOVETE\nSIN PERDER TIEMPO."
            textSize = 30f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(18))
        })
        box.addView(button("SINCRONIZAR LÍNEAS", cyan) { loadLines(false) })
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun loadLines(navigateToLines: Boolean = true) {
        status.text = "● SINCRONIZANDO..."
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { lines ->
                    runOnUiThread {
                        status.text = "● " + lines.size + " LÍNEAS"
                        if (navigateToLines) showLines(lines)
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        status.text = "● SIN CONEXIÓN"
                        toast(error.message ?: "Error")
                    }
                }
        }
    }

    private fun showLines(initial: List<TransitLine>? = null) {
        title.text = "LÍNEAS"
        updateNav(1)
        content.removeAllViews()
        val box = box()
        box.addView(panel("CATÁLOGO", "Datos solicitados al servicio SmartMove."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)

        fun drawLines(items: List<TransitLine>) {
            list.removeAllViews()
            items.forEach { line ->
                list.addView(
                    card(line.name.uppercase(), "LÍNEA " + line.code) { showLine(line) },
                    LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) }
                )
            }
            if (items.isEmpty()) list.addView(panel("SIN DATOS", "No se encontraron líneas."))
        }

        if (initial != null) {
            drawLines(initial)
        } else {
            executor.execute {
                runCatching { api.getLines() }
                    .onSuccess { items -> runOnUiThread { drawLines(items); status.text = "● " + items.size + " LÍNEAS" } }
                    .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "No se pudo consultar.")) } }
            }
        }
        box.addView(button("ACTUALIZAR", cyan) { loadLines(true) })
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showLine(line: TransitLine) {
        title.text = "LÍNEA " + line.code
        content.removeAllViews()
        val box = box()
        box.addView(TextView(this).apply {
            text = line.name.uppercase()
            textSize = 25f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
        })
        box.addView(panel("CALLES", "Seleccioná una calle para continuar."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        executor.execute {
            runCatching { api.getStreets(line.code) }
                .onSuccess { streets ->
                    runOnUiThread {
                        list.removeAllViews()
                        streets.forEach { street ->
                            list.addView(card(street.name, "VER INTERSECCIONES") { showIntersections(line, street) })
                        }
                    }
                }
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
        box.addView(button("MAPA DEL RECORRIDO", pink) { showMap(line) })
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showIntersections(line: TransitLine, street: TransitStreet) {
        content.removeAllViews()
        title.text = street.name
        val box = box()
        box.addView(panel("INTERSECCIONES", "LÍNEA " + line.code + " · " + street.name))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        executor.execute {
            runCatching { api.getIntersections(line.code, street.code) }
                .onSuccess { intersections ->
                    runOnUiThread {
                        list.removeAllViews()
                        intersections.forEach { intersection ->
                            list.addView(card(intersection.name, "VER PARADAS") { showStops(line, street, intersection) })
                        }
                    }
                }
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showStops(line: TransitLine, street: TransitStreet, intersection: TransitIntersection) {
        content.removeAllViews()
        title.text = "PARADAS"
        val box = box()
        box.addView(panel("PARADAS", "LÍNEA " + line.code + " · " + intersection.name))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        executor.execute {
            runCatching { api.getStops(line.code, street.code, intersection.code) }
                .onSuccess { stops ->
                    runOnUiThread {
                        list.removeAllViews()
                        stops.forEach { stop ->
                            list.addView(card("🚏 " + stop.description, stop.street + " " + stop.intersection) { showArrivals(stop, line) })
                        }
                        if (stops.isEmpty()) list.addView(panel("SIN PARADAS", "No se encontraron paradas."))
                    }
                }
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showArrivals(stop: TransitStop, line: TransitLine) {
        content.removeAllViews()
        title.text = "ARRIBOS"
        val box = box()
        box.addView(panel("🚏 " + stop.description, "LÍNEA " + line.code + " · ID " + stop.identifier))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        box.addView(button("ACTUALIZAR ARRIBOS", cyan) { loadArrivals(stop, line, list) })
        box.addView(button("☆ GUARDAR PARADA", pink) { saveFavorite(stop, line); toast("Parada guardada") })
        content.addView(ScrollView(this).apply { addView(box) })
        loadArrivals(stop, line, list)
    }

    private fun loadArrivals(stop: TransitStop, line: TransitLine, list: LinearLayout) {
        list.removeAllViews()
        list.addView(panel("LIVE", "Consultando próximos arribos..."))
        executor.execute {
            runCatching { api.getArrivals(stop.identifier, line.code) }
                .onSuccess { arrivals ->
                    runOnUiThread {
                        list.removeAllViews()
                        arrivals.forEach { arrival ->
                            val lineLabel = if (arrival.line.isBlank()) "LÍNEA " + line.code else arrival.line
                            val minutes = arrival.minutes?.toString() ?: "--"
                            list.addView(card(lineLabel + " · " + minutes + " MIN", arrival.destination) {})
                        }
                        if (arrivals.isEmpty()) list.addView(panel("SIN ARRIBOS", "El servicio no devolvió datos."))
                    }
                }
                .onFailure { error -> runOnUiThread { list.removeAllViews(); list.addView(panel("ERROR", error.message ?: "Sin conexión")) } }
        }
    }

    private fun showNearby() {
        title.text = "PARADAS CERCANAS"
        updateNav(3)
        content.removeAllViews()
        val root = FrameLayout(this)

        val mapFrame = FrameLayout(this).apply {
            setBackgroundColor(panelColor)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val map = CyberMapView(this)
        mapFrame.addView(
            map,
            FrameLayout.LayoutParams(-1, -1)
        )

        root.addView(
            mapFrame,
            FrameLayout.LayoutParams(-1, -1).apply {
                leftMargin = dp(6)
                rightMargin = dp(6)
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        )

        val info = TextView(this).apply {
            text = "RADAR LOCAL  •  UBICACIÓN ACTIVA"
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xCC05070C.toInt())
        }
        root.addView(info, FrameLayout.LayoutParams(-1, dp(44)).apply { gravity = Gravity.TOP })

        root.addView(button("◉ ACTUALIZAR CERCA", cyan) { loadNearbyOnMap(map, info) },
            FrameLayout.LayoutParams(-1, dp(52)).apply {
                gravity = Gravity.BOTTOM
                leftMargin = dp(16)
                rightMargin = dp(16)
                bottomMargin = dp(16)
            })

        map.setOnStopTap { stop ->
            toast(stop.title + if (stop.subtitle.isBlank()) "" else " · " + stop.subtitle)
        }

        content.addView(root)
        loadNearbyOnMap(map, info)
    }

    private fun loadNearbyOnMap(map: CyberMapView, info: TextView) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
            return
        }

        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: return toast("No hay ubicación disponible")

        map.setUserLocation(location.latitude, location.longitude, true)
        status.text = "● BUSCANDO PARADAS"

        executor.execute {
            runCatching { api.getNearby(location.latitude, location.longitude) }
                .onSuccess { stops ->
                    val markers = stops.take(60).map {
                        MapStop(
                            it.code,
                            "🚏 " + it.description,
                            listOf(it.street, it.intersection)
                                .filter { value -> value.isNotBlank() }
                                .joinToString(" · "),
                            it.latitude,
                            it.longitude
                        )
                    }.filter { it.latitude != 0.0 && it.longitude != 0.0 }

                    runOnUiThread {
                        map.setStops(markers, fit = false)
                        status.text = "● " + markers.size + " PARADAS CERCANAS"
                        info.text = "RADAR LOCAL  •  " + markers.size + " PARADAS"
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        status.text = "● SIN CONEXIÓN"
                        toast(error.message ?: "No se pudieron cargar las paradas")
                    }
                }
        }
    }

    private fun showFavorites() {
        title.text = "FAVORITOS"
        updateNav(2)
        content.removeAllViews()
        val box = box()
        val favorites = getSharedPreferences("favorites", 0).getStringSet("stops", emptySet()).orEmpty()
        box.addView(panel("MIS PARADAS", if (favorites.isEmpty()) "No hay favoritos." else "Guardados localmente."))
        favorites.forEach { favorite ->
            val saved = parseFavorite(favorite)
            if (saved != null) {
                box.addView(
                    card(
                        "LÍNEA ${saved.lineCode} · ${saved.description}",
                        if (saved.street.isBlank() && saved.intersection.isBlank())
                            "TOCAR PARA VER ARRIBOS"
                        else
                            "${saved.street} · ${saved.intersection}"
                    ) {
                        val stop = TransitStop(
                            saved.stopCode,
                            saved.description,
                            saved.identifier,
                            saved.latitude,
                            saved.longitude,
                            saved.street,
                            saved.intersection,
                            saved.lineCode
                        )
                        showArrivals(stop, TransitLine(saved.lineCode, saved.lineName))
                    },
                    LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) }
                )
            } else {
                box.addView(
                    card(favorite, "FAVORITO ANTIGUO · VOLVÉ A GUARDAR LA PARADA", {}),
                    LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) }
                )
            }
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun saveFavorite(stop: TransitStop, line: TransitLine) {
        val prefs = getSharedPreferences("favorites", 0)
        val values = prefs.getStringSet("stops", emptySet())?.toMutableSet() ?: mutableSetOf()

        val favorite = JSONObject().apply {
            put("version", 2)
            put("lineCode", line.code)
            put("lineName", line.name)
            put("stopCode", stop.code)
            put("description", stop.description)
            put("identifier", stop.identifier)
            put("street", stop.street)
            put("intersection", stop.intersection)
            put("latitude", stop.latitude)
            put("longitude", stop.longitude)
        }.toString()

        values.removeAll { raw ->
            parseFavorite(raw)?.let {
                it.lineCode == line.code && it.identifier == stop.identifier
            } ?: false
        }
        values.add(favorite)
        prefs.edit().putStringSet("stops", values).apply()
    }

    private fun parseFavorite(raw: String): FavoriteStop? {
        return runCatching {
            val json = JSONObject(raw)
            if (json.optInt("version", 0) < 2) return null
            FavoriteStop(
                lineCode = json.getInt("lineCode"),
                lineName = json.optString("lineName", "LÍNEA ${json.getInt("lineCode")}"),
                stopCode = json.optInt("stopCode", 0),
                description = json.optString("description", ""),
                identifier = json.optString("identifier", ""),
                street = json.optString("street", ""),
                intersection = json.optString("intersection", ""),
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0)
            )
        }.getOrNull()
    }

    private fun showMap(line: TransitLine?) {
        title.text = "MAPA"
        content.removeAllViews()
        val mapFrame = FrameLayout(this).apply {
            setBackgroundColor(panelColor)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val map = CyberMapView(this)

        mapFrame.addView(
            map,
            FrameLayout.LayoutParams(-1, -1)
        )

        content.addView(
            mapFrame,
            FrameLayout.LayoutParams(-1, -1).apply {
                leftMargin = dp(6)
                rightMargin = dp(6)
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        )
        if (line != null) {
            executor.execute {
                runCatching { api.getRoute(line.code) }
                    .onSuccess { route -> runOnUiThread { map.setRoute(route) } }
                    .onFailure { error -> runOnUiThread { toast(error.message ?: "No se pudo cargar el recorrido") } }
            }
        }
    }

    private fun box() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(24))
    }

    private fun card(primary: String, secondary: String, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setBackgroundColor(panelColor)
        setOnClickListener { action() }
        addView(TextView(this@MainActivity).apply {
            text = primary
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
        })
        addView(TextView(this@MainActivity).apply {
            text = secondary
            textSize = 10f
            setTextColor(muted)
        })
    }

    private fun panel(primary: String, secondary: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundColor(panelColor)
        addView(TextView(this@MainActivity).apply {
            text = primary
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
        })
        addView(TextView(this@MainActivity).apply {
            text = secondary
            textSize = 13f
            setTextColor(muted)
        })
    }

    private fun button(label: String, color: Int, action: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 12f
        typeface = Typeface.MONOSPACE
        setTextColor(color)
        setBackgroundColor(panelColor)
        setPadding(0, dp(14), 0, dp(14))
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}