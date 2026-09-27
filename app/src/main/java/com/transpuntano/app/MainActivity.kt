package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.util.Base64
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.transpuntano.app.data.SmartMoveApi
import com.transpuntano.app.model.*
import com.transpuntano.app.ui.CyberMapView
import com.transpuntano.app.ui.MapStop
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val cyberpunkTypeface by lazy { Typeface.createFromAsset(assets, "fonts/cyberpunk.ttf") }
    private data class FavoriteStop(
        val lineCode: Int, val lineName: String, val stopCode: Int,
        val description: String, val identifier: String, val street: String,
        val intersection: String, val latitude: Double, val longitude: Double
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
    private lateinit var drawerPanel: FrameLayout
    private lateinit var drawerScrim: View
    private var drawerOpen = false
    private var drawerHotspotsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOpen) toggleDrawer() else finish()
            }
        })
        showHome()
    }

    private fun buildShell() {
        val rootFrame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        rootFrame.addView(CyberBackgroundView(this), FrameLayout.LayoutParams(-1, -1))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
        }

        val headerLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF05070C.toInt())
            addView(CyberHeaderView(this@MainActivity), FrameLayout.LayoutParams(-1, -1))
        }
        val menuBtn = headerIconButton("nav_menu", "MENÚ") { toggleDrawer() }
        lateinit var searchBtn: ImageButton
        searchBtn = headerIconButton("nav_search", "BUSCAR") { openLineSearch(searchBtn) }
        val alertBtn = headerIconButton("nav_bell", "NOTIFICACIONES") { toast("NOTIFICACIONES") }
        val headerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(-1, dp(48)).apply {
                leftMargin = dp(44); rightMargin = dp(4); topMargin = dp(7)
            }
        }
        headerContent.addView(TextView(this).apply {
            text = "TU COLECTIVO 2.0"
            textSize = 15f
            translationX = -dp(15).toFloat()
            typeface = cyberpunkTypeface
            setTextColor(cyan)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, dp(40))
        })
        // El título de sección se conserva solo para la lógica interna; no se muestra en el header.
        title = TextView(this).apply { text = ""; textSize = 11f; setTextColor(muted) }
        status = TextView(this).apply {
            text = "● SISTEMA LISTO"; textSize = 14f; typeface = cyberpunkTypeface
            setTextColor(0xFF55FFB0.toInt())
        }
        headerLayout.addView(headerContent)
        headerLayout.addView(status, FrameLayout.LayoutParams(-2, dp(28)).apply {
            leftMargin = dp(169.0f); topMargin = dp(41.5f); gravity = Gravity.TOP
        })
        headerLayout.addView(menuBtn, FrameLayout.LayoutParams(dp(44), dp(44)).apply {
            leftMargin = dp(4); topMargin = dp(14)
        })
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(searchBtn, LinearLayout.LayoutParams(dp(40), dp(40)))
        actions.addView(alertBtn, LinearLayout.LayoutParams(dp(40), dp(40)))
        headerLayout.addView(actions, FrameLayout.LayoutParams(dp(88), dp(44)).apply {
            rightMargin = dp(4); topMargin = dp(14); gravity = Gravity.END
        })
        root.addView(headerLayout, LinearLayout.LayoutParams(-1, dp(72)))
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = CyberBottomBarBackground()
            visibility = View.VISIBLE
        }
        root.addView(navBar, LinearLayout.LayoutParams(-1, dp(64)))
        rootFrame.addView(root)

        drawerScrim = View(this).apply {
            setBackgroundColor(0x99000000.toInt()); visibility = View.GONE
            setOnClickListener { toggleDrawer() }
        }
        rootFrame.addView(drawerScrim, FrameLayout.LayoutParams(-1, -1))
        drawerPanel = FrameLayout(this).apply {
            background = CyberDrawerBackground()
            layoutParams = FrameLayout.LayoutParams(dp(300), -1).apply { gravity = Gravity.START }
            visibility = View.GONE
        }
        addDrawerItems()
        rootFrame.addView(drawerPanel)
        setContentView(rootFrame)
        updateNav(0)
    }

    private fun headerIconButton(iconName: String, description: String, action: () -> Unit) = ImageButton(this).apply {
        val id = resources.getIdentifier(iconName, "drawable", packageName)
        setImageResource(id)
        setColorFilter(cyan)
        background = cyberRippleBackground()
        contentDescription = description
        scaleType = ImageView.ScaleType.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setOnClickListener {
            cyberTouchFeedback(this)
            action()
        }
    }

    private fun cyberRippleBackground(): android.graphics.drawable.Drawable {
        val mask = android.graphics.drawable.ColorDrawable(Color.WHITE)
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x6600FFF2),
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
            mask
        )
    }

    private fun cyberTouchFeedback(view: View) {
        view.animate().cancel()
        view.animate().scaleX(0.90f).scaleY(0.90f).alpha(0.55f).setDuration(70).withEndAction {
            view.animate().scaleX(1.08f).scaleY(1.08f).alpha(1f).setDuration(90).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }.start()
        }.start()
    }

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen
        if (drawerOpen) {
            drawerScrim.visibility = View.VISIBLE
            drawerPanel.visibility = View.VISIBLE
            drawerPanel.translationX = -dp(300).toFloat()
            drawerPanel.animate().translationX(0f).setDuration(180).start()
        } else {
            drawerPanel.animate().translationX(-dp(300).toFloat()).setDuration(150).withEndAction {
                drawerPanel.visibility = View.GONE
                drawerScrim.visibility = View.GONE
            }.start()
        }
    }

    private class CyberBottomBarBackground : android.graphics.drawable.Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF2DB2.toInt()
            paint.style = Paint.Style.FILL
            paint.color = 0xFF02070D.toInt()
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = cyan
            paint.alpha = 235
            canvas.drawLine(0f, 2f, w * .16f, 2f, paint)
            canvas.drawLine(w * .84f, 2f, w, 2f, paint)
            paint.alpha = 100
            canvas.drawLine(w * .22f, 2f, w * .78f, 2f, paint)

            paint.alpha = 230
            canvas.drawLine(0f, 2f, 14f, h * .28f, paint)
            canvas.drawLine(w, 2f, w - 14f, h * .28f, paint)

            paint.color = cyan
            paint.alpha = 30
            for (x in 0..w.toInt() step 28) canvas.drawLine(x.toFloat(), h * .25f, x.toFloat(), h, paint)
            for (y in 24..h.toInt() step 18) canvas.drawLine(0f, y.toFloat(), w, y.toFloat(), paint)

            paint.color = cyan
            paint.alpha = 230
            paint.strokeWidth = 2f
            canvas.drawLine(8f, h - 2f, w - 8f, h - 2f, paint)
            paint.color = pink
            paint.alpha = 190
            paint.strokeWidth = 1f
            canvas.drawLine(w * .30f, h - 6f, w * .70f, h - 6f, paint)
            paint.color = cyan
            paint.style = Paint.Style.FILL
            paint.alpha = 190
            canvas.drawCircle(w / 2f, 5f, 1.5f, paint)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    private class CyberDrawerBackground : android.graphics.drawable.Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF2DB2.toInt()

            paint.style = Paint.Style.FILL
            paint.color = 0xFF02060B.toInt()
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = cyan
            paint.alpha = 235
            val cut = 22f
            val p = Path()
            p.moveTo(0f, 0f)
            p.lineTo(w - cut, 0f)
            p.lineTo(w, cut)
            p.lineTo(w, h - cut)
            p.lineTo(w - cut, h)
            canvas.drawPath(p, paint)

            paint.alpha = 180
            canvas.drawLine(w - 4f, cut + 8f, w - 4f, h - cut - 8f, paint)
            paint.color = pink
            paint.alpha = 170
            canvas.drawLine(7f, 72f, 7f, h - 40f, paint)

            paint.color = cyan
            paint.alpha = 32
            for (y in 20..h.toInt() step 24) canvas.drawLine(12f, y.toFloat(), w - 12f, y.toFloat(), paint)
            paint.alpha = 120
            canvas.drawLine(18f, 58f, w - 30f, 58f, paint)
            canvas.drawLine(18f, 62f, w * .58f, 62f, paint)
            paint.alpha = 240
            paint.strokeWidth = 2f
            canvas.drawLine(12f, 78f, w - 18f, 78f, paint)
            canvas.drawLine(18f, h - 14f, w - 18f, h - 14f, paint)
            paint.color = pink
            paint.strokeWidth = 1f
            canvas.drawLine(w * .28f, 82f, w * .72f, 82f, paint)

            paint.color = cyan
            paint.style = Paint.Style.FILL
            paint.alpha = 220
            canvas.drawCircle(w - 12f, 12f, 2f, paint)
            canvas.drawCircle(12f, h - 12f, 2f, paint)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    private fun addDrawerItems() {
        drawerPanel.removeAllViews()
        val border = View(this).apply { setBackgroundColor(cyan) }
        drawerPanel.addView(border, FrameLayout.LayoutParams(dp(2), -1).apply { gravity = Gravity.END })
        val items = listOf(
            Triple("INICIO", "nav_home", 0),
            Triple("LÍNEAS", "nav_lineas", 1),
            Triple("MAPA", "nav_mapa", 2),
            Triple("FAVORITOS", "nav_favoritos", 3),
            Triple("PARADAS CERCANAS", "nav_cercanas", 4)
        )
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        items.forEach { (label, iconName, index) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, dp(8), 0); isClickable = true; isFocusable = true
                background = cyberRippleBackground()
                setOnClickListener {
                    cyberTouchFeedback(this)
                    postDelayed({ navigateTo(index); toggleDrawer() }, 110)
                }
            }
            val icon = ImageView(this).apply {
                val id = resources.getIdentifier(iconName, "drawable", packageName)
                setImageResource(id); alpha = 0.95f; contentDescription = label
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(34), dp(34)).apply { rightMargin = dp(14) })
            row.addView(TextView(this@MainActivity).apply {
                text = label; textSize = 11f; typeface = cyberpunkTypeface; setTextColor(if (index == 0) cyan else muted)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            list.addView(row, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(8) })
        }
        drawerPanel.addView(list, FrameLayout.LayoutParams(-1, -1).apply { topMargin = dp(60) })
        drawerPanel.addView(View(this).apply { setBackgroundColor(pink) }, FrameLayout.LayoutParams(dp(2), dp(90)).apply {
            leftMargin = dp(8); topMargin = dp(66)
        })
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome(); 1 -> showLines(); 2 -> showMap(null); 3 -> showFavorites(); 4 -> showNearby()
        }
    }

    private fun updateNav(selected: Int) {
        navBar.removeAllViews()
        navBar.setPadding(0, dp(3), 0, 0)
        navBar.clipChildren = false
        navBar.clipToPadding = false
        val items = listOf(
            Triple("INICIO", "nav_home", 0),
            Triple("LÍNEAS", "nav_lineas", 1),
            Triple("MAPA", "nav_mapa", 2),
            Triple("FAVORITOS", "nav_favoritos", 3),
            Triple("CERCANAS", "nav_cercanas", 4)
        )
        items.forEach { (label, iconName, index) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                clipChildren = false
                clipToPadding = false
                setPadding(0, 0, 0, dp(2))
                background = cyberRippleBackground()
            }
            val iconRes = resources.getIdentifier(iconName, "drawable", packageName)
            val icon = ImageView(this).apply {
                setImageResource(iconRes)
                alpha = if (index == selected) 1f else 0.52f
                contentDescription = label
                layoutParams = LinearLayout.LayoutParams(dp(27), dp(27))
            }
            val text = TextView(this).apply {
                this.text = label
                textSize = 8.5f
                typeface = cyberpunkTypeface
                gravity = Gravity.CENTER
                setTextColor(if (index == selected) cyan else muted)
                alpha = if (index == selected) 1f else 0.72f
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(-1, dp(18))
            }
            item.addView(icon)
            item.addView(text)
            item.setOnClickListener {
                cyberTouchFeedback(item)
                when (index) {
                    0 -> showHome()
                    1 -> showLines()
                    2 -> showMap(null)
                    3 -> showFavorites()
                    4 -> showNearby()
                }
            }
            navBar.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }
    private fun showHome() {
        title.text = ""
        updateNav(0)
        content.removeAllViews()
        val box = box()
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        }
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        }
        val item1 = cardHomeImage("lineas_cyberpunk.webp", cyan) { showLines() }
        val item2 = cardHomeAsset("mapa_cyberpunk.webp", "MAPA · Explorar el mapa") { showMap(null) }
        val item3 = cardHomeAsset("paradas_cercanas_cyberpunk.webp", "PARADAS CERCANAS · Por tu ubicación") { showNearby() }
        val item4 = cardHomeAsset("favoritos_cyberpunk.webp", "FAVORITOS · Paradas guardadas") { showFavorites() }
        row1.addView(item1, LinearLayout.LayoutParams(0, dp(140), 1f).apply { rightMargin = dp(6) })
        row1.addView(item2, LinearLayout.LayoutParams(0, dp(140), 1f).apply { leftMargin = dp(6) })
        row2.addView(item3, LinearLayout.LayoutParams(0, dp(140), 1f).apply { rightMargin = dp(6) })
        row2.addView(item4, LinearLayout.LayoutParams(0, dp(140), 1f).apply { leftMargin = dp(6) })
        grid.addView(row1); grid.addView(row2); box.addView(grid)
        box.addView(cyberSyncButton { loadLines(false) }, LinearLayout.LayoutParams(-1, dp(60)).apply { topMargin = dp(20) })
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun loadAssetBitmap(assetName: String): android.graphics.Bitmap? {
        val base = assetName.removeSuffix(".webp").removeSuffix(".b64")
        // Prefer the canonical WEBP asset so a stale legacy B64 cannot override it.
        runCatching {
            assets.open("$base.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.let { return it }
        return runCatching {
            val b64 = assets.open("$base.b64").bufferedReader().use { it.readText() }.trim()
            if (b64.isNotEmpty()) {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else null
        }.getOrNull()
    }

    private fun cardHomeAsset(assetName: String, description: String = assetName, action: () -> Unit) = FrameLayout(this).apply {
        setBackgroundColor(panelColor); setOnClickListener { action() }; isClickable = true; isFocusable = true
        val bitmap = loadAssetBitmap(assetName)
        if (bitmap != null) {
            addView(ImageView(this@MainActivity).apply {
                setImageBitmap(bitmap); scaleType = ImageView.ScaleType.FIT_XY; contentDescription = description
            }, FrameLayout.LayoutParams(-1, -1))
        } else {
            val label = description.substringBefore(" · ").ifBlank { assetName }
            addView(TextView(this@MainActivity).apply {
                text = label; gravity = Gravity.CENTER; textSize = 14f; typeface = Typeface.MONOSPACE
                setTextColor(cyan); contentDescription = description
            }, FrameLayout.LayoutParams(-1, -1))
        }
    }

    private fun cardHomeImage(assetName: String, color: Int, action: () -> Unit) = FrameLayout(this).apply {
        setBackgroundColor(panelColor); setOnClickListener { action() }; isClickable = true; isFocusable = true
        val image = ImageView(this@MainActivity).apply {
            val bitmap = loadAssetBitmap(assetName)
            if (bitmap != null) setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(bg)
            contentDescription = "LÍNEAS · Recorridos y calles"
        }
        addView(image, FrameLayout.LayoutParams(-1, -1))
        addView(View(this@MainActivity).apply { setBackgroundColor(color); alpha = 0.85f },
            FrameLayout.LayoutParams(dp(3), -1).apply { gravity = Gravity.START })
    }

    private fun box() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(24))
    }

    private fun card(primary: String, secondary: String, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12))
        setBackgroundColor(panelColor); setOnClickListener { action() }
        addView(TextView(this@MainActivity).apply {
            text = primary; textSize = 14f; typeface = Typeface.MONOSPACE; setTextColor(Color.WHITE)
        })
        addView(TextView(this@MainActivity).apply { text = secondary; textSize = 10f; setTextColor(muted) })
    }

    private fun panel(primary: String, secondary: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundColor(panelColor)
        addView(TextView(this@MainActivity).apply {
            text = primary; textSize = 11f; typeface = Typeface.MONOSPACE; setTextColor(cyan)
        })
        addView(TextView(this@MainActivity).apply { text = secondary; textSize = 13f; setTextColor(muted) })
    }

    private fun button(label: String, color: Int, action: () -> Unit) = TextView(this).apply {
        text = label; gravity = Gravity.CENTER; textSize = 13f; typeface = Typeface.MONOSPACE
        setTextColor(color); setBackgroundColor(panelColor); setPadding(dp(12), dp(14), dp(12), dp(14))
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }

    private fun cyberSyncButton(action: () -> Unit): View {
        val bitmap = loadAssetBitmap("sincronizar_lineas.webp")
        if (bitmap == null) return button("SINCRONIZAR LÍNEAS", cyan, action)
        return ImageView(this).apply {
            setImageBitmap(bitmap); scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "Sincronizar líneas"; isClickable = true; isFocusable = true
            background = cyberRippleBackground()
            setOnClickListener { cyberTouchFeedback(this); action() }
        }
    }

    private fun loadLines(navigateToLines: Boolean = true) {
        status.text = "● SINCRONIZANDO..."
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { lines -> runOnUiThread {
                    status.text = "● " + lines.size + " LÍNEAS"
                    if (navigateToLines) showLines(lines)
                }}
                .onFailure { error -> runOnUiThread {
                    status.text = "● SIN CONEXIÓN"; toast(error.message ?: "Error")
                }}
        }
    }

    private fun openLineSearch(anchor: View) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF050B12.toInt())
                setStroke(dp(1), cyan)
                cornerRadius = dp(4).toFloat()
            }
        }

        val header = TextView(this).apply {
            text = "BUSCAR // LÍNEAS"
            textSize = 12f
            typeface = cyberpunkTypeface
            setTextColor(cyan)
            setPadding(0, 0, 0, dp(8))
        }

        val input = EditText(this).apply {
            hint = "LETRA O NOMBRE"
            setSingleLine(true)
            textSize = 15f
            typeface = cyberpunkTypeface
            setTextColor(cyan)
            setHintTextColor(muted)
            setPadding(dp(10), 0, dp(10), 0)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF0A111A.toInt())
                setStroke(dp(1), 0xFF176A78.toInt())
                cornerRadius = dp(3).toFloat()
            }
        }

        val search = TextView(this).apply {
            text = "BUSCAR"
            textSize = 10f
            gravity = Gravity.CENTER
            typeface = cyberpunkTypeface
            setTextColor(cyan)
            isClickable = true
            isFocusable = true
            background = cyberRippleBackground()
            setPadding(dp(12), 0, dp(12), 0)
        }

        panel.addView(header, LinearLayout.LayoutParams(-1, dp(28)))
        panel.addView(input, LinearLayout.LayoutParams(-1, dp(42)))
        panel.addView(search, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(8) })

        val popup = android.widget.PopupWindow(
            panel, dp(270), dp(132), true
        ).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isClippingEnabled = true
            elevation = dp(12).toFloat()
            inputMethodMode = android.widget.PopupWindow.INPUT_METHOD_NEEDED
            softInputMode = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        fun performSearch() {
            val rawQuery = input.text.toString().trim()
            if (rawQuery.isBlank()) {
                input.error = "Ingresá un número o nombre de línea"
                return
            }

            popup.dismiss()
            status.text = "● BUSCANDO..."
            executor.execute {
                runCatching { api.getLines() }
                    .onSuccess { lines ->
                        val normalizedQuery = rawQuery
                            .lowercase(java.util.Locale.getDefault())
                            .replace("línea", "")
                            .replace("linea", "")
                            .replace("line", "")
                            .replace("#", "")
                            .trim()
                            .replace(Regex("\\s+"), " ")

                        val matches = if (normalizedQuery.isBlank()) {
                            emptyList()
                        } else {
                            lines.filter { line ->
                                // Las líneas de TU COLECTIVO se identifican por letras.
                                // El código numérico del servicio es interno y no se usa
                                // como criterio de búsqueda.
                                val name = line.name
                                    .lowercase(java.util.Locale.getDefault())
                                    .replace("línea", "")
                                    .replace("linea", "")
                                    .trim()
                                    .replace(Regex("\\s+"), " ")

                                name == normalizedQuery || name.contains(normalizedQuery)
                            }
                        }

                        runOnUiThread {
                            showLines(matches)
                            status.text = if (matches.isEmpty()) {
                                "● SIN RESULTADOS"
                            } else {
                                "● " + matches.size + " RESULTADOS"
                            }
                        }
                    }
                    .onFailure { error ->
                        runOnUiThread {
                            status.text = "● SIN CONEXIÓN"
                            toast(error.message ?: "No se pudo realizar la búsqueda")
                        }
                    }
            }
        }

        search.setOnClickListener {
            cyberTouchFeedback(search)
            performSearch()
        }

        input.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        popup.setOnDismissListener {
            input.clearFocus()
        }

        // El menú queda anclado al botón del buscador y aparece inmediatamente debajo,
        // sin depender de la posición global del contenido/header.
        // Debajo del header, alineado con el borde derecho del icono buscador.
        popup.showAsDropDown(anchor, -dp(230), dp(18))
        input.requestFocus()
        input.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 200L)
    }

    private fun showLines(initial: List<TransitLine>? = null) {
        title.text = "LÍNEAS"; updateNav(1); content.removeAllViews()
        val box = box()
        box.addView(panel("CATÁLOGO", "Datos solicitados al servicio SmartMove."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        fun drawLines(items: List<TransitLine>) {
            list.removeAllViews()
            items.forEach { line ->
                list.addView(card(line.name.uppercase(), "LÍNEA " + line.code) { showLine(line) },
                    LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) })
            }
            if (items.isEmpty()) list.addView(panel("SIN DATOS", "No se encontraron líneas."))
        }
        if (initial != null) drawLines(initial)
        else executor.execute {
            runCatching { api.getLines() }
                .onSuccess { items -> runOnUiThread { drawLines(items); status.text = "● " + items.size + " LÍNEAS" } }
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "No se pudo consultar.")) } }
        }
        box.addView(button("ACTUALIZAR", cyan) { loadLines(true) })
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showLine(line: TransitLine) {
        title.text = "LÍNEA " + line.code; updateNav(1); content.removeAllViews()
        val box = box()
        box.addView(TextView(this).apply {
            text = line.name.uppercase(); textSize = 25f; typeface = Typeface.MONOSPACE; setTextColor(cyan)
        })
        box.addView(panel("CALLES", "Seleccioná una calle para continuar."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        executor.execute {
            runCatching { api.getStreets(line.code) }
                .onSuccess { streets -> runOnUiThread {
                    list.removeAllViews()
                    streets.forEach { street -> list.addView(card(street.name, "VER INTERSECCIONES") { showIntersections(line, street) }) }
                }}
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
        box.addView(button("MAPA DEL RECORRIDO", pink) { showMap(line) })
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showIntersections(line: TransitLine, street: TransitStreet) {
        content.removeAllViews(); title.text = street.name; updateNav(1)
        val box = box()
        box.addView(panel("INTERSECCIONES", "LÍNEA " + line.code + " · " + street.name))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        executor.execute {
            runCatching { api.getIntersections(line.code, street.code) }
                .onSuccess { intersections -> runOnUiThread {
                    list.removeAllViews()
                    intersections.forEach { intersection -> list.addView(card(intersection.name, "VER PARADAS") { showStops(line, street, intersection) }) }
                }}
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showStops(line: TransitLine, street: TransitStreet, intersection: TransitIntersection) {
        content.removeAllViews(); title.text = "PARADAS"; updateNav(1)
        val box = box()
        box.addView(panel("PARADAS", "LÍNEA " + line.code + " · " + intersection.name))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        executor.execute {
            runCatching { api.getStops(line.code, street.code, intersection.code) }
                .onSuccess { stops -> runOnUiThread {
                    list.removeAllViews()
                    stops.forEach { stop -> list.addView(card("🚏 " + stop.description, stop.street + " " + stop.intersection) { showArrivals(stop, line) }) }
                    if (stops.isEmpty()) list.addView(panel("SIN PARADAS", "No se encontraron paradas."))
                }}
                .onFailure { error -> runOnUiThread { list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showArrivals(stop: TransitStop, line: TransitLine) {
        content.removeAllViews(); title.text = "ARRIBOS"; updateNav(1)
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
                .onSuccess { arrivals -> runOnUiThread {
                    list.removeAllViews()
                    arrivals.forEach { arrival ->
                        val lineLabel = if (arrival.line.isBlank()) "LÍNEA " + line.code else arrival.line
                        val minutes = arrival.minutes?.toString() ?: "--"
                        list.addView(card(lineLabel + " · " + minutes + " MIN", arrival.destination) {})
                    }
                    if (arrivals.isEmpty()) list.addView(panel("SIN ARRIBOS", "El servicio no devolvió datos."))
                }}
                .onFailure { error -> runOnUiThread {
                    list.removeAllViews(); list.addView(panel("ERROR", error.message ?: "Sin conexión"))
                }}
        }
    }

    private fun showNearby() {
        title.text = "PARADAS CERCANAS"; updateNav(4); content.removeAllViews()
        val box = box()
        box.addView(panel("PARADAS CERCANAS", "Buscando paradas próximas a tu ubicación."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        box.addView(button("◉ ACTUALIZAR PARADAS", cyan) { loadNearbyList(list) }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        content.addView(ScrollView(this).apply { addView(box) })
        loadNearbyList(list)
    }

    private fun loadNearbyList(list: LinearLayout) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
            return
        }
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location == null) {
            status.text = "● SIN UBICACIÓN"
            list.removeAllViews()
            list.addView(panel("UBICACIÓN NO DISPONIBLE", "Activá la ubicación e intentá de nuevo."))
            return
        }
        status.text = "● BUSCANDO PARADAS"
        list.removeAllViews()
        list.addView(panel("BUSCANDO", "Consultando paradas cercanas..."))
        executor.execute {
            runCatching { api.getNearby(location.latitude, location.longitude) }
                .onSuccess { nearby -> runOnUiThread {
                    list.removeAllViews()
                    nearby.forEach { stop ->
                        list.addView(card(stop.description, stop.street + " · " + stop.intersection) {
                            toast(stop.description)
                        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                    }
                    if (nearby.isEmpty()) list.addView(panel("SIN PARADAS", "No se encontraron paradas cercanas."))
                    status.text = "● " + nearby.size + " PARADAS CERCANAS"
                }}
                .onFailure { error -> runOnUiThread {
                    list.removeAllViews()
                    list.addView(panel("ERROR", error.message ?: "Sin conexión"))
                    status.text = "● ERROR"
                }}
        }
    }

    private fun showFavorites() {
        title.text = "FAVORITOS"; updateNav(3); content.removeAllViews()
        val box = box()
        val favorites = loadFavorites()
        box.addView(panel("MIS PARADAS", if (favorites.isEmpty()) "No hay favoritos." else "Guardados localmente."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)
        favorites.forEach { fav ->
            list.addView(card(fav.description, "LÍNEA " + fav.lineCode + " · " + fav.street) {
                toast(fav.description)
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showMap(line: TransitLine?) {
        title.text = "MAPA"; updateNav(2); content.removeAllViews()
        val root = FrameLayout(this)
        val mapFrame = FrameLayout(this).apply {
            setBackgroundColor(panelColor); setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val map = CyberMapView(this)
        mapFrame.addView(map, FrameLayout.LayoutParams(-1, -1))
        root.addView(mapFrame, FrameLayout.LayoutParams(-1, -1).apply {
            leftMargin = dp(6); rightMargin = dp(6); topMargin = dp(6); bottomMargin = dp(6)
        })
        val info = TextView(this).apply {
            text = "MAPA  •  UBICACIÓN Y PARADAS CERCANAS"
            textSize = 11f; typeface = Typeface.MONOSPACE; setTextColor(cyan)
            setPadding(dp(14), dp(10), dp(14), dp(10)); setBackgroundColor(0xCC05070C.toInt())
        }
        root.addView(info, FrameLayout.LayoutParams(-1, dp(44)).apply { gravity = Gravity.TOP })
        content.addView(root)
        if (line != null) {
            executor.execute {
                runCatching { api.getRoute(line.code) }
                    .onSuccess { route -> runOnUiThread { map.setRoute(route) } }
                    .onFailure { error -> runOnUiThread { toast(error.message ?: "No se pudo cargar el recorrido") } }
            }
        }
    }

    private fun saveFavorite(stop: TransitStop, line: TransitLine) {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        val key = stop.identifier
        val value = listOf(line.code, line.name, stop.code, stop.description, stop.identifier, stop.street, stop.intersection, stop.latitude, stop.longitude).joinToString("|")
        prefs.edit().putString(key, value).apply()
    }

    private fun loadFavorites(): List<FavoriteStop> {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        return prefs.all.mapNotNull { (_, v) ->
            val p = (v as? String)?.split("|") ?: return@mapNotNull null
            if (p.size < 9) return@mapNotNull null
            runCatching {
                FavoriteStop(p[0].toInt(), p[1], p[2].toInt(), p[3], p[4], p[5], p[6], p[7].toDouble(), p[8].toDouble())
            }.getOrNull()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()
    private class CyberBackgroundView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
        private val path = Path()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.BLACK)
            val w = width.toFloat(); val h = height.toFloat()
            paint.color = 0x2200F0FF; paint.strokeWidth = 1.2f
            for (y in (dpLocal(90)..height step dpLocal(78))) {
                path.reset(); path.moveTo(0f, y.toFloat()); path.lineTo(w * .28f, y.toFloat())
                path.lineTo(w * .34f, (y - dpLocal(8)).toFloat()); path.lineTo(w * .62f, (y - dpLocal(8)).toFloat())
                path.lineTo(w * .68f, y.toFloat()); path.lineTo(w, y.toFloat()); canvas.drawPath(path, paint)
            }
            paint.color = 0x1FFF2DB2
            for (x in (dpLocal(28)..width step dpLocal(74))) {
                canvas.drawLine(x.toFloat(), h * .78f, x.toFloat() + dpLocal(18).toFloat(), h * .70f, paint)
                canvas.drawLine(x.toFloat() + dpLocal(18).toFloat(), h * .70f, x.toFloat() + dpLocal(36).toFloat(), h * .70f, paint)
            }
            paint.color = 0x1600F0FF
            canvas.drawCircle(w * .84f, h * .26f, dpLocal(70).toFloat(), paint)
            canvas.drawCircle(w * .84f, h * .26f, dpLocal(82).toFloat(), paint)
        }
        private fun dpLocal(v: Int) = (v * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private class CyberHeaderView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val cyan = 0xFF00F0FF.toInt()

            canvas.drawColor(0xFF010408.toInt())
            fill.color = 0xFF030912.toInt()
            canvas.drawRect(dpLocal(5).toFloat(), dpLocal(5).toFloat(),
                w - dpLocal(5).toFloat(), h - dpLocal(5).toFloat(), fill)

            // Top neon rails leave a deliberate central gap so they do not crowd the title.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpLocal(1).toFloat()
            paint.color = cyan
            paint.alpha = 245
            canvas.drawLine(dpLocal(5).toFloat(), dpLocal(3).toFloat(), dpLocal(72).toFloat(), dpLocal(3).toFloat(), paint)
            canvas.drawLine(w - dpLocal(72).toFloat(), dpLocal(3).toFloat(), w - dpLocal(5).toFloat(), dpLocal(3).toFloat(), paint)
            paint.alpha = 120
            canvas.drawLine(dpLocal(82).toFloat(), dpLocal(3).toFloat(), dpLocal(116).toFloat(), dpLocal(3).toFloat(), paint)
            canvas.drawLine(w - dpLocal(116).toFloat(), dpLocal(3).toFloat(), w - dpLocal(82).toFloat(), dpLocal(3).toFloat(), paint)

            // Angular side frame and HUD brackets.
            paint.alpha = 220
            path.reset()
            path.moveTo(dpLocal(5).toFloat(), dpLocal(3).toFloat())
            path.lineTo(dpLocal(5).toFloat(), h - dpLocal(8).toFloat())
            path.lineTo(dpLocal(17).toFloat(), h - dpLocal(8).toFloat())
            path.moveTo(w - dpLocal(5).toFloat(), dpLocal(3).toFloat())
            path.lineTo(w - dpLocal(5).toFloat(), h - dpLocal(8).toFloat())
            path.lineTo(w - dpLocal(17).toFloat(), h - dpLocal(8).toFloat())
            canvas.drawPath(path, paint)

            // Neon data ticks and nodes.
            paint.color = cyan
            paint.alpha = 230
            for (i in 0..4) {
                val x = dpLocal(22 + i * 7).toFloat()
                canvas.drawLine(x, dpLocal(56).toFloat(), x, dpLocal(60).toFloat(), paint)
            }
            for (i in 0..4) {
                val x = w - dpLocal(22 + i * 7).toFloat()
                canvas.drawLine(x, dpLocal(56).toFloat(), x, dpLocal(60).toFloat(), paint)
            }
            // Strong neon lower edge.
            paint.color = cyan
            paint.alpha = 255
            paint.strokeWidth = dpLocal(2).toFloat()
            canvas.drawLine(dpLocal(8).toFloat(), h - dpLocal(2).toFloat(),
                w - dpLocal(8).toFloat(), h - dpLocal(2).toFloat(), paint)
        }

        private fun dpLocal(v: Int) =
            (v * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
