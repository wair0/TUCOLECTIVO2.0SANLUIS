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
    private lateinit var headerTitle: CyberHeaderTitleView
    private lateinit var headerStatus: CyberHeaderStatusView
    private var currentSection = 0
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
                when {
                    drawerOpen -> toggleDrawer()
                    currentSection != 0 -> showHome()
                    else -> finish()
                }
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
        headerTitle = CyberHeaderTitleView(this, cyberpunkTypeface, cyan, pink).apply {
            translationX = -dp(20).toFloat()
            layoutParams = LinearLayout.LayoutParams(-1, dp(40))
        }
        headerContent.addView(headerTitle)
        // El título de sección se conserva solo para la lógica interna; no se muestra en el header.
        title = TextView(this).apply { text = ""; textSize = 11f; setTextColor(muted) }
        headerStatus = CyberHeaderStatusView(this, cyberpunkTypeface, cyan, pink).apply {
            setStatusText("● SISTEMA LISTO")
        }
        headerLayout.addView(headerContent)
        headerLayout.addView(headerStatus, FrameLayout.LayoutParams(-2, dp(28)).apply {
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
            background = CyberDrawerBackground(this@MainActivity)
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
        background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        contentDescription = description
        scaleType = ImageView.ScaleType.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setOnClickListener { action() }
        applyCyberTap(this)
    }

    private fun applyCyberTap(view: View) {
        val pulse = CyberTapDrawable(
            cyan = cyan,
            pink = pink,
            density = resources.displayMetrics.density
        ) { offset ->
            // El propio ciclo Canvas controla el desplazamiento frame a frame.
            // No usamos View.animate(), Animator ni interpoladores externos.
            view.translationY = offset
        }

        view.foreground = pulse
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    pulse.pulse(event.x, event.y)
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    pulse.resetMotion()
                }
            }
            false
        }
    }

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen
        if (drawerOpen) {
            drawerScrim.visibility = View.VISIBLE
            drawerPanel.visibility = View.VISIBLE
            drawerPanel.translationX = 0f
        } else {
            drawerPanel.translationX = 0f
            drawerPanel.visibility = View.GONE
            drawerScrim.visibility = View.GONE
        }
    }

    private class CyberBottomBarBackground : android.graphics.drawable.Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var selectedIndex: Int = 0
            set(value) { field = value; invalidateSelf() }

        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF2DB2.toInt()
            val violet = 0xFF7A5CFF.toInt()

            paint.style = Paint.Style.FILL
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, 0f, h,
                0xFF020711.toInt(), 0xFF080319.toInt(),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            // Holographic top rail with segmented energy breaks.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = cyan
            paint.alpha = 220
            canvas.drawLine(0f, 2f, w * .12f, 2f, paint)
            canvas.drawLine(w * .18f, 2f, w * .43f, 2f, paint)
            canvas.drawLine(w * .57f, 2f, w * .82f, 2f, paint)
            canvas.drawLine(w * .88f, 2f, w, 2f, paint)
            paint.color = pink
            paint.alpha = 210
            canvas.drawLine(w * .43f, 2f, w * .48f, 2f, paint)
            canvas.drawLine(w * .52f, 2f, w * .57f, 2f, paint)

            // Deep holographic perspective grid.
            paint.color = violet
            paint.alpha = 26
            paint.strokeWidth = 1f
            val cellW = w / 5f
            for (i in 0..5) canvas.drawLine(i * cellW, h * .28f, i * cellW, h, paint)
            for (i in 1..4) {
                val y = h * (0.28f + i * 0.16f)
                canvas.drawLine(0f, y, w, y, paint)
            }

            // Selected slot becomes a compact holographic cockpit panel.
            val sx = selectedIndex.coerceIn(0, 4) * cellW
            paint.style = Paint.Style.FILL
            paint.color = cyan
            paint.alpha = 18
            canvas.drawRect(sx + 5f, 8f, sx + cellW - 5f, h - 5f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = cyan
            paint.alpha = 230
            paint.strokeWidth = 1.4f
            val inset = 7f
            val top = 8f
            val bottom = h - 6f
            val notch = 8f
            val path = Path()
            path.moveTo(sx + inset + notch, top)
            path.lineTo(sx + cellW - inset - notch, top)
            path.lineTo(sx + cellW - inset, top + notch)
            path.lineTo(sx + cellW - inset, bottom - notch)
            path.lineTo(sx + cellW - inset - notch, bottom)
            path.lineTo(sx + inset + notch, bottom)
            path.lineTo(sx + inset, bottom - notch)
            path.lineTo(sx + inset, top + notch)
            path.close()
            canvas.drawPath(path, paint)

            paint.color = pink
            paint.alpha = 220
            paint.strokeWidth = 2f
            canvas.drawLine(sx + 14f, top, sx + 30f, top, paint)
            canvas.drawLine(sx + cellW - 30f, bottom, sx + cellW - 14f, bottom, paint)

            // Central command node.
            paint.style = Paint.Style.FILL
            paint.color = cyan
            paint.alpha = 210
            canvas.drawCircle(w / 2f, 3f, 1.7f, paint)
            paint.color = pink
            paint.alpha = 170
            canvas.drawCircle(w / 2f, 3f, 4f, paint)

            // Bottom reactor rail.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = cyan
            paint.alpha = 230
            canvas.drawLine(10f, h - 2f, w * .38f, h - 2f, paint)
            canvas.drawLine(w * .62f, h - 2f, w - 10f, h - 2f, paint)
            paint.color = pink
            paint.alpha = 220
            canvas.drawLine(w * .38f, h - 2f, w * .47f, h - 2f, paint)
            canvas.drawLine(w * .53f, h - 2f, w * .62f, h - 2f, paint)
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    private class CyberDrawerBackground(private val context: Context) : android.graphics.drawable.Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF2DB2.toInt()
            val violet = 0xFF7A5CFF.toInt()

            paint.style = Paint.Style.FILL
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, w, h,
                0xFF02050D.toInt(), 0xFF10031B.toInt(),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            // Futuristic angular shell; no title text is drawn here.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = cyan
            paint.alpha = 230
            val shell = Path()
            shell.moveTo(0f, 0f)
            shell.lineTo(w - 30f, 0f)
            shell.lineTo(w, 30f)
            shell.lineTo(w, h - 30f)
            shell.lineTo(w - 30f, h)
            canvas.drawPath(shell, paint)

            paint.color = pink
            paint.alpha = 200
            canvas.drawLine(4f, 0f, 4f, h, paint)
            canvas.drawLine(w - 5f, 30f, w - 5f, h - 30f, paint)

            // Circuit traces and HUD rails.
            paint.color = violet
            paint.alpha = 75
            paint.strokeWidth = 1f
            for (i in 0..7) {
                val y = 28f + i * 78f
                val trace = Path()
                trace.moveTo(18f, y)
                trace.lineTo(w * .38f, y)
                trace.lineTo(w * .45f, y + 10f)
                trace.lineTo(w - 18f, y + 10f)
                canvas.drawPath(trace, paint)
            }
            paint.color = cyan
            paint.alpha = 70
            for (y in 18..h.toInt() step 24) canvas.drawLine(12f, y.toFloat(), w - 18f, y.toFloat(), paint)

            // Side energy spine.
            paint.color = cyan
            paint.alpha = 240
            paint.strokeWidth = 2f
            canvas.drawLine(w - 10f, 46f, w - 10f, h - 46f, paint)
            paint.color = pink
            paint.alpha = 220
            paint.strokeWidth = 1f
            canvas.drawLine(12f, 18f, 12f, h - 18f, paint)

            // Corner data markers.
            paint.style = Paint.Style.FILL
            paint.color = cyan
            paint.alpha = 230
            canvas.drawCircle(w - 18f, 18f, 2f, paint)
            canvas.drawCircle(w - 18f, h - 18f, 2f, paint)
            paint.color = pink
            canvas.drawCircle(18f, 18f, 2f, paint)
            canvas.drawCircle(18f, h - 18f, 2f, paint)
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
                setOnClickListener {
                    navigateTo(index)
                    toggleDrawer()
                }
                applyCyberTap(this)
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
        drawerPanel.addView(list, FrameLayout.LayoutParams(-1, -1).apply { topMargin = dp(16) })
        drawerPanel.addView(View(this).apply { setBackgroundColor(pink) }, FrameLayout.LayoutParams(dp(2), dp(90)).apply {
            leftMargin = dp(8); topMargin = dp(22)
        })
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome(); 1 -> showLines(); 2 -> showMap(null); 3 -> showFavorites(); 4 -> showNearby()
        }
    }

    private fun updateNav(selected: Int) {
        currentSection = selected
        navBar.removeAllViews()
        (navBar.background as? CyberBottomBarBackground)?.selectedIndex = selected
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
                when (index) {
                    0 -> showHome()
                    1 -> showLines()
                    2 -> showMap(null)
                    3 -> showFavorites()
                    4 -> showNearby()
                }
            }
            applyCyberTap(item)
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
        val item1 = cardHomeImage("lineas_cyberpunk.webp") { showLines() }
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
        setBackgroundColor(panelColor)
        setOnClickListener { action() }
        applyCyberTap(this)
        isClickable = true; isFocusable = true
        val bitmap = loadAssetBitmap(assetName)
        if (bitmap != null) {
            addView(ImageView(this@MainActivity).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(bg)
                contentDescription = description
            }, FrameLayout.LayoutParams(-1, -1))
        } else {
            val label = description.substringBefore(" · ").ifBlank { assetName }
            addView(TextView(this@MainActivity).apply {
                text = label; gravity = Gravity.CENTER; textSize = 14f; typeface = Typeface.MONOSPACE
                setTextColor(cyan); contentDescription = description
            }, FrameLayout.LayoutParams(-1, -1))
        }
    }

    private fun cardHomeImage(assetName: String, action: () -> Unit) = FrameLayout(this).apply {
        setBackgroundColor(panelColor)
        setOnClickListener { action() }
        applyCyberTap(this)
        isClickable = true; isFocusable = true
        val image = ImageView(this@MainActivity).apply {
            val bitmap = loadAssetBitmap(assetName)
            if (bitmap != null) setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(bg)
            contentDescription = "LÍNEAS · Recorridos y calles"
        }
        addView(image, FrameLayout.LayoutParams(-1, -1))
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
        if (bitmap == null) {
            return button("SINCRONIZAR LÍNEAS", cyan, action).also { applyCyberTap(it) }
        }
        return ImageView(this).apply {
            setImageBitmap(bitmap); scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "Sincronizar líneas"; isClickable = true; isFocusable = true
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setOnClickListener { action() }
            applyCyberTap(this)
        }
    }

    private fun loadLines(navigateToLines: Boolean = true) {
        headerStatus.setStatusText("● SINCRONIZANDO...");
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { lines -> runOnUiThread {
                    headerStatus.setStatusText("● " + lines.size + " LÍNEAS");
                    if (navigateToLines) showLines(lines)
                }}
                .onFailure { error -> runOnUiThread {
                    headerStatus.setStatusText("● SIN CONEXIÓN"); toast(error.message ?: "Error")
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
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setPadding(dp(12), 0, dp(12), 0)
        }
        applyCyberTap(search)

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
            headerStatus.setStatusText("● BUSCANDO...");
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
                            headerStatus.setStatusText(if (matches.isEmpty()) {
                                "● SIN RESULTADOS"
                            } else {
                                "● " + matches.size + " RESULTADOS"
                            })
                        }
                    }
                    .onFailure { error ->
                        runOnUiThread {
                            headerStatus.setStatusText("● SIN CONEXIÓN");
                            toast(error.message ?: "No se pudo realizar la búsqueda")
                        }
                    }
            }
        }

        search.setOnClickListener {
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
        popup.showAsDropDown(anchor, -dp(190), dp(18))
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
                .onSuccess { items -> runOnUiThread { drawLines(items); headerStatus.setStatusText("● " + items.size + " LÍNEAS") } }
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
            headerStatus.setStatusText("● SIN UBICACIÓN");
            list.removeAllViews()
            list.addView(panel("UBICACIÓN NO DISPONIBLE", "Activá la ubicación e intentá de nuevo."))
            return
        }
        headerStatus.setStatusText("● BUSCANDO PARADAS");
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
                    headerStatus.setStatusText("● " + nearby.size + " PARADAS CERCANAS");
                }}
                .onFailure { error -> runOnUiThread {
                    list.removeAllViews()
                    list.addView(panel("ERROR", error.message ?: "Sin conexión"))
                    headerStatus.setStatusText("● ERROR");
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
    /** Canvas-only cyberpunk pulse used for tap feedback. It never scales, fades, translates or moves the target view. */
    /**
     * Canvas-only cyberpunk tap feedback.
     *
     * The drawable drives both the neon pulse and a tiny physical-looking
     * bounce through its own frame scheduler. The target View is never
     * animated with ViewPropertyAnimator/Animator.
     */
    private class CyberTapDrawable(
        private val cyan: Int,
        private val pink: Int,
        private val density: Float,
        private val onMotion: (Float) -> Unit
    ) : android.graphics.drawable.Drawable() {
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private var active = false
        private var startTime = 0L
        private var touchX = 0f
        private var touchY = 0f
        private val durationMs = 240L
        private val bounceDistance = 3.5f * density

        private val runner = object : Runnable {
            override fun run() {
                if (!active) return

                val elapsed = android.os.SystemClock.uptimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                // Rebote corto: baja, vuelve, y termina exactamente en cero.
                val motion = when {
                    progress < 0.30f -> {
                        val p = progress / 0.30f
                        bounceDistance * p
                    }
                    progress < 0.62f -> {
                        val p = (progress - 0.30f) / 0.32f
                        bounceDistance * (1f - p * 1.22f)
                    }
                    else -> {
                        val p = (progress - 0.62f) / 0.38f
                        bounceDistance * (-0.22f * (1f - p))
                    }
                }
                onMotion(motion)

                if (progress >= 1f) {
                    active = false
                    onMotion(0f)
                    invalidateSelf()
                    return
                }

                invalidateSelf()
                scheduleSelf(this, android.os.SystemClock.uptimeMillis() + 16L)
            }
        }

        fun pulse(x: Float, y: Float) {
            touchX = x
            touchY = y
            startTime = android.os.SystemClock.uptimeMillis()
            active = true
            unscheduleSelf(runner)
            onMotion(0f)
            invalidateSelf()
            scheduleSelf(runner, android.os.SystemClock.uptimeMillis() + 16L)
        }

        fun resetMotion() {
            active = false
            unscheduleSelf(runner)
            onMotion(0f)
            invalidateSelf()
        }

        override fun draw(canvas: Canvas) {
            if (!active) return

            val elapsed = android.os.SystemClock.uptimeMillis() - startTime
            val progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress) * (1f - progress)
            val maxRadius = maxOf(bounds.width(), bounds.height()).toFloat() * 0.78f
            val radius = maxRadius * eased
            val fade = 1f - progress

            ringPaint.strokeWidth = 5f * density
            ringPaint.color = cyan
            ringPaint.alpha = (110f * fade).toInt().coerceIn(0, 255)
            canvas.drawCircle(touchX, touchY, radius, ringPaint)

            ringPaint.strokeWidth = 2f * density
            ringPaint.color = pink
            ringPaint.alpha = (175f * fade).toInt().coerceIn(0, 255)
            canvas.drawCircle(touchX, touchY, radius * 0.86f, ringPaint)

            ringPaint.strokeWidth = 1f * density
            ringPaint.color = cyan
            ringPaint.alpha = (235f * fade).toInt().coerceIn(0, 255)
            canvas.drawCircle(touchX, touchY, radius * 0.62f, ringPaint)

            corePaint.color = cyan
            corePaint.alpha = (180f * fade).toInt().coerceIn(0, 255)
            canvas.drawCircle(
                touchX,
                touchY,
                maxOf(1f, 2.5f * density * fade),
                corePaint
            )

            val tick = maxOf(3f, radius * 0.13f)
            ringPaint.strokeWidth = 1.5f * density
            ringPaint.color = pink
            ringPaint.alpha = (190f * fade).toInt().coerceIn(0, 255)
            canvas.drawLine(touchX - radius - tick, touchY, touchX - radius, touchY, ringPaint)
            canvas.drawLine(touchX + radius, touchY, touchX + radius + tick, touchY, ringPaint)
            canvas.drawLine(touchX, touchY - radius - tick, touchX, touchY - radius, ringPaint)
            canvas.drawLine(touchX, touchY + radius, touchX, touchY + radius + tick, ringPaint)

            // Estelas direccionales: refuerzan visualmente el rebote sin tocar el layout.
            val streakAlpha = (95f * fade).toInt().coerceIn(0, 255)
            ringPaint.color = cyan
            ringPaint.alpha = streakAlpha
            ringPaint.strokeWidth = 1f * density
            val streak = maxOf(4f * density, radius * 0.08f)
            canvas.drawLine(touchX - radius * 0.72f, touchY + streak, touchX - radius * 0.72f - streak, touchY + streak, ringPaint)
            canvas.drawLine(touchX + radius * 0.72f, touchY - streak, touchX + radius * 0.72f + streak, touchY - streak, ringPaint)
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    private class CyberBackgroundView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF2DB2.toInt()
            val violet = 0xFF7A5CFF.toInt()

            canvas.drawColor(0xFF01030A.toInt())

            // Layered depth field: diagonal holographic planes instead of the old grid.
            paint.style = Paint.Style.FILL
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, w, h,
                0x1800F0FF, 0x00000000,
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = cyan
            paint.alpha = 34

            val step = dpLocal(56).toFloat()
            var x = -h
            while (x < w + h) {
                canvas.drawLine(x, 0f, x + h, h, paint)
                x += step
            }

            // Broken magenta orbital arcs.
            paint.color = pink
            paint.alpha = 48
            val r = minOf(w, h) * .34f
            canvas.drawArc(w * .78f - r, h * .18f - r, w * .78f + r, h * .18f + r, 210f, 105f, false, paint)
            canvas.drawArc(w * .18f - r * .65f, h * .82f - r * .65f, w * .18f + r * .65f, h * .82f + r * .65f, 25f, 120f, false, paint)

            // Floating circuit routes.
            paint.color = violet
            paint.alpha = 42
            for (i in 0..5) {
                val y = dpLocal(90 + i * 92).toFloat()
                path.reset()
                path.moveTo(0f, y)
                path.lineTo(w * .22f, y)
                path.lineTo(w * .28f, y - dpLocal(12))
                path.lineTo(w * .56f, y - dpLocal(12))
                path.lineTo(w * .62f, y)
                path.lineTo(w, y)
                canvas.drawPath(path, paint)
            }

            // Tiny HUD nodes give the background a layered, instrument-panel feel.
            paint.style = Paint.Style.FILL
            paint.color = cyan
            paint.alpha = 95
            for (i in 1..5) {
                val nx = w * (i / 6f)
                val ny = h * (0.20f + (i % 2) * .50f)
                canvas.drawCircle(nx, ny, dpLocal(1).toFloat(), paint)
            }

            // One scan horizon and two accent brackets.
            paint.style = Paint.Style.STROKE
            paint.color = cyan
            paint.alpha = 35
            canvas.drawLine(0f, h * .62f, w, h * .62f, paint)
            paint.alpha = 75
            canvas.drawLine(dpLocal(12).toFloat(), h * .62f, dpLocal(74).toFloat(), h * .62f, paint)
            paint.color = pink
            canvas.drawLine(w - dpLocal(74).toFloat(), h * .62f, w - dpLocal(12).toFloat(), h * .62f, paint)
        }

        private fun dpLocal(v: Int) = (v * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    /**
     * Header title rendered directly on Canvas.
     *
     * Unlike a TextView property animation, the title's pixels are rebuilt on
     * every animation frame: cyan/magenta RGB ghosts, brightness cuts and
     * short horizontal glitch slices are all part of the rendered frame.
     */
    private class CyberHeaderTitleView(
        context: Context,
        typeface: Typeface,
        private val cyan: Int,
        private val pink: Int
    ) : View(context) {
        private companion object {
            const val TITLE = "TU COLECTIVO 2.0"
            const val FRAME_DELAY_MS = 45L
            const val FRAME_COUNT = 48
        }

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = 15f * resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.CENTER
            color = cyan
        }
        private val cyanGhostPaint = Paint(titlePaint)
        private val pinkGhostPaint = Paint(titlePaint)
        private val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f * resources.displayMetrics.density
        }

        private var frame = 0

        private val frameRunner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                frame = (frame + 1) % FRAME_COUNT
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, FRAME_DELAY_MS)
            }
        }

        init {
            contentDescription = TITLE
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            setWillNotDraw(false)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            removeCallbacks(frameRunner)
            postOnAnimation(frameRunner)
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(frameRunner)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width * 0.5f
            val baseline = height * 0.5f - (titlePaint.ascent() + titlePaint.descent()) * 0.5f
            val textWidth = titlePaint.measureText(TITLE)
            val top = baseline + titlePaint.ascent()
            val bottom = baseline + titlePaint.descent()

            val cycle = frame % 16
            val glitch = when (cycle) {
                3, 4, 9, 10, 11 -> true
                else -> false
            }

            val brightness = when (cycle) {
                3 -> 0.34f
                4 -> 1.0f
                9 -> 0.52f
                10 -> 0.86f
                11 -> 0.30f
                else -> 1.0f
            }

            val ghostShift = when (cycle) {
                3 -> 4.5f
                4 -> -3.0f
                9 -> -4.0f
                10 -> 2.5f
                11 -> -1.5f
                else -> 0f
            }

            // Persistent low-level RGB separation keeps the title alive even
            // between the stronger glitch bursts.
            cyanGhostPaint.alpha = if (glitch) 210 else 92
            pinkGhostPaint.alpha = if (glitch) 190 else 72
            titlePaint.alpha = (255f * brightness).toInt().coerceIn(55, 255)

            canvas.drawText(TITLE, centerX + ghostShift, baseline, cyanGhostPaint)
            canvas.drawText(TITLE, centerX - ghostShift, baseline, pinkGhostPaint)
            canvas.drawText(TITLE, centerX, baseline, titlePaint)

            if (glitch) {
                val sliceTop = top + (bottom - top) * if (cycle == 9) 0.22f else 0.48f
                val sliceBottom = sliceTop + (bottom - top) * 0.20f

                canvas.save()
                canvas.clipRect(0f, sliceTop, width.toFloat(), sliceBottom)
                canvas.drawText(TITLE, centerX + 6.0f, baseline, cyanGhostPaint)
                canvas.drawText(TITLE, centerX - 5.0f, baseline, pinkGhostPaint)
                canvas.drawText(TITLE, centerX + ghostShift, baseline, titlePaint)
                canvas.restore()
            }

            // A thin scanning line traverses the title band once per cycle.
            val scanProgress = (frame % 24) / 23f
            val scanY = top + (bottom - top) * scanProgress
            scanPaint.color = cyan
            scanPaint.alpha = if (glitch) 185 else 55
            canvas.drawLine(
                centerX - textWidth * 0.56f,
                scanY,
                centerX + textWidth * 0.56f,
                scanY,
                scanPaint
            )
        }
    }

    private class CyberHeaderStatusView(
        context: Context,
        typeface: Typeface,
        private val cyan: Int,
        private val pink: Int
    ) : View(context) {
        private var value = "● SISTEMA LISTO"
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f * resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.LEFT
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        fun setStatusText(text: String) {
            value = text
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val h = height.toFloat()
            val baseline = h * .68f
            textPaint.color = if (value.contains("ERROR")) pink else 0xFF55FFB0.toInt()
            textPaint.alpha = 245
            canvas.drawText(value, 0f, baseline, textPaint)

            val width = textPaint.measureText(value)
            linePaint.color = cyan
            linePaint.alpha = 110
            linePaint.strokeWidth = d
            canvas.drawLine(0f, h - 4f, minOf(width + 10f * d, this.width.toFloat()), h - 4f, linePaint)
            linePaint.color = pink
            linePaint.alpha = 170
            canvas.drawLine(0f, h - 1f, minOf(width * .34f, this.width.toFloat()), h - 1f, linePaint)
        }
    }

    private class CyberHeaderView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF2DB2.toInt()
            val violet = 0xFF7A5CFF.toInt()

            // New cockpit-style header: layered glass, angular armor and energy rails.
            paint.style = Paint.Style.FILL
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, w, h,
                0xFF020817.toInt(), 0xFF12031A.toInt(),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(4f, 4f, w - 4f, h - 5f, 9f, 9f, paint)
            paint.shader = null

            // Inner glass plane.
            paint.color = 0xFF06101B.toInt()
            paint.alpha = 210
            canvas.drawRoundRect(9f, 9f, w - 9f, h - 10f, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            paint.color = cyan
            paint.alpha = 235
            path.reset()
            path.moveTo(4f, 20f)
            path.lineTo(4f, 6f)
            path.lineTo(w - 34f, 6f)
            path.lineTo(w - 20f, 20f)
            path.moveTo(w - 4f, h - 20f)
            path.lineTo(w - 4f, h - 6f)
            path.lineTo(34f, h - 6f)
            path.lineTo(20f, h - 20f)
            canvas.drawPath(path, paint)

            // Pink counter-rail creates the RGB split characteristic of a cyber HUD.
            paint.color = pink
            paint.alpha = 210
            paint.strokeWidth = 1f
            canvas.drawLine(16f, 7f, w * .34f, 7f, paint)
            canvas.drawLine(w * .66f, h - 7f, w - 16f, h - 7f, paint)

            // Central reactor window behind the title.
            paint.color = violet
            paint.alpha = 42
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(w * .24f, 12f, w * .76f, h - 13f, 5f, 5f, paint)

            // Data ticks, nodes and micro-bars.
            paint.style = Paint.Style.STROKE
            paint.color = cyan
            paint.alpha = 190
            for (i in 0..6) {
                val x = 18f + i * 8f
                canvas.drawLine(x, h - 17f, x, h - 12f, paint)
            }
            for (i in 0..6) {
                val x = w - 18f - i * 8f
                canvas.drawLine(x, 12f, x, 17f, paint)
            }

            paint.style = Paint.Style.FILL
            paint.color = cyan
            paint.alpha = 230
            canvas.drawCircle(17f, 17f, 2f, paint)
            canvas.drawCircle(w - 17f, h - 17f, 2f, paint)
            paint.color = pink
            paint.alpha = 220
            canvas.drawCircle(w - 17f, 17f, 2f, paint)
            canvas.drawCircle(17f, h - 17f, 2f, paint)

            // Thin scan rails.
            paint.style = Paint.Style.STROKE
            paint.color = cyan
            paint.alpha = 55
            canvas.drawLine(w * .08f, h * .48f, w * .92f, h * .48f, paint)
            paint.color = pink
            paint.alpha = 90
            canvas.drawLine(w * .38f, h - 11f, w * .62f, h - 11f, paint)
        }

        private fun dpLocal(v: Int) = (v * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
