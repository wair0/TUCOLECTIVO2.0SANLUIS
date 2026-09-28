package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.util.Base64
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.transpuntano.app.data.SmartMoveApi
import com.transpuntano.app.model.*
import com.transpuntano.app.ui.CyberMapView
import com.transpuntano.app.ui.MapStop
import java.io.ByteArrayInputStream
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
    private val pink = 0xFFFF00FF.toInt()
    private val bg = 0xFF02030B.toInt()
    private val panelColor = 0xFF070916.toInt()
    private val muted = 0xFF6D9BB0.toInt()
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
        rootFrame.addView(AnimatedGifBackgroundView(this), FrameLayout.LayoutParams(-1, -1))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
        }

        val headerLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF02030B.toInt())
            addView(CyberHeaderView(this@MainActivity), FrameLayout.LayoutParams(-1, -1))
        }
        val menuBtn = headerIconButton("nav_menu", "MENÚ") { toggleDrawer() }
        lateinit var searchBtn: ImageButton
        searchBtn = headerIconButton("nav_search", "BUSCAR") { openLineSearch(searchBtn) }
        val alertBtn = headerIconButton("nav_bell", "NOTIFICACIONES") { toast("NOTIFICACIONES") }
        val headerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(-1, dp(56)).apply {
                leftMargin = dp(58); rightMargin = dp(58); topMargin = dp(10)
            }
        }
        headerTitle = CyberHeaderTitleView(this, cyberpunkTypeface, cyan, pink).apply {
            translationX = 0f
            layoutParams = LinearLayout.LayoutParams(-1, dp(44))
        }
        headerContent.addView(headerTitle)
        // El título de sección se conserva solo para la lógica interna; no se muestra en el header.
        title = TextView(this).apply { text = ""; textSize = 11f; typeface = cyberpunkTypeface; setTextColor(muted) }
        headerStatus = CyberHeaderStatusView(this, cyberpunkTypeface, cyan, pink).apply {
            setStatusText("● SISTEMA LISTO")
        }
        headerLayout.addView(headerContent)
        headerLayout.addView(headerStatus, FrameLayout.LayoutParams(dp(150), dp(28)).apply {
            leftMargin = dp(58); topMargin = dp(57); gravity = Gravity.TOP
        })
        headerLayout.addView(menuBtn, FrameLayout.LayoutParams(dp(50), dp(50)).apply {
            leftMargin = dp(5); topMargin = dp(11)
        })
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(searchBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        actions.addView(alertBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        headerLayout.addView(actions, FrameLayout.LayoutParams(dp(96), dp(50)).apply {
            rightMargin = dp(4); topMargin = dp(11); gravity = Gravity.END
        })
        root.addView(headerLayout, LinearLayout.LayoutParams(-1, dp(92)))
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = CyberBottomBarBackground(resources.displayMetrics.density)
            visibility = View.VISIBLE
        }
        root.addView(navBar, LinearLayout.LayoutParams(-1, dp(72)))
        rootFrame.addView(root)

        drawerScrim = View(this).apply {
            setBackgroundColor(0x99000000.toInt()); visibility = View.GONE
            setOnClickListener { toggleDrawer() }
        }
        rootFrame.addView(drawerScrim, FrameLayout.LayoutParams(-1, -1))
        drawerPanel = FrameLayout(this).apply {
            background = CyberDrawerBackground(this@MainActivity)
            layoutParams = FrameLayout.LayoutParams(dp(320), -1).apply { gravity = Gravity.START }
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
        background = AnimatedCyberFrameDrawable(cyan, pink, resources.displayMetrics.density, dp(7).toFloat(), dp(4).toFloat())
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

    private class CyberBottomBarBackground(private val density: Float) : android.graphics.drawable.Drawable() {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = 0f
        private val handler = Handler(Looper.getMainLooper())
        private val animator = object : Runnable {
            override fun run() {
                phase = (phase + 0.20f) % 16f
                invalidateSelf()
                handler.postDelayed(this, 55L)
            }
        }
        init { handler.postDelayed(animator, 55L) }
        var selectedIndex=0; set(v){field=v;invalidateSelf()}
        override fun draw(c:Canvas){ val w=bounds.width().toFloat(); val h=bounds.height().toFloat()
            val lime=0xFF00F0FF.toInt(); val orange=0xFFFF00FF.toInt(); val blue=0xFF006CFF.toInt()
            // Fondo completamente opaco y limpio: sin triángulos ni diagonales decorativas.
            p.style=Paint.Style.FILL
            p.shader=android.graphics.LinearGradient(
                0f,0f,w,0f,
                intArrayOf(0xFF02030B.toInt(),0xFF09051A.toInt(),0xFF02030B.toInt()),
                null,android.graphics.Shader.TileMode.CLAMP
            )
            c.drawRect(0f,0f,w,h,p)
            p.shader=null

            // Marco exterior completo: cubre todo el perímetro de la barra inferior.
            CyberHeaderFrameDrawable.drawFrame(c, 1.5f, 1.5f, w - 1.5f, h - 1.5f, lime, orange, density, 9f * density, 1f, phase)
            p.style=Paint.Style.STROKE
            p.strokeWidth=1.5f
            p.color=lime
            p.alpha=190
            c.drawLine(4f,3f,w*.18f,3f,p)
            c.drawLine(w*.82f,3f,w-4f,3f,p)

            val cw=w/5f
            val x=selectedIndex.coerceIn(0,4)*cw
            p.color=orange
            p.alpha=240
            p.strokeWidth=2f
            c.drawLine(x+8f,h-5f,x+cw-8f,h-5f,p)

            p.style=Paint.Style.FILL
            p.color=lime
            p.alpha=230
            c.drawCircle(w*.5f,3.5f,2f,p)
        }
        override fun setAlpha(a:Int){};override fun setColorFilter(f:ColorFilter?){}
        @Suppress("DEPRECATION")override fun getOpacity()=android.graphics.PixelFormat.OPAQUE
    }

    private class CyberDrawerBackground(private val context: Context) : android.graphics.drawable.Drawable(){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val path=Path()
        override fun draw(c:Canvas){val w=bounds.width().toFloat();val h=bounds.height().toFloat()
            val lime=0xFF00F0FF.toInt();val orange=0xFFFF00FF.toInt();val blue=0xFF006CFF.toInt()
            // Base completamente opaca para que el menú no deje ver el contenido detrás.
            c.drawColor(0xFF02030B.toInt())
            p.style=Paint.Style.FILL;p.shader=android.graphics.LinearGradient(0f,0f,w,h,intArrayOf(0xFF02030B.toInt(),0xFF09051A.toInt(),0xFF030417.toInt()),null,android.graphics.Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
            p.color=lime;p.alpha=20
            for(i in 0..5){path.reset();path.moveTo(0f,i*h/6f);path.lineTo(w*.8f,(i+1)*h/6f);path.lineTo(w,(i+.75f)*h/6f);path.lineTo(w*.2f,(i-.25f)*h/6f);path.close();c.drawPath(path,p)}
            p.style=Paint.Style.STROKE;p.color=lime;p.alpha=230;p.strokeWidth=2f;c.drawLine(w-3f,0f,w-3f,h,p)
            p.color=orange;p.alpha=210;c.drawLine(3f,0f,3f,h,p)
            for(i in 0..13){val y=18f+i*h/14f;p.color=if(i%3==0)orange else blue;p.alpha=130;p.strokeWidth=1f;c.drawLine(18f,y,w*(.18f+(i%5)*.13f),y,p)}
            p.style=Paint.Style.FILL;p.color=lime;p.alpha=220;for(i in 0..5)c.drawCircle(w-14f,30f+i*(h-60f)/5f,2.5f,p)
            p.color=orange;for(i in 0..5)c.drawCircle(14f,30f+i*(h-60f)/5f,2.5f,p)
            p.style=Paint.Style.STROKE;p.color=blue;p.alpha=150;p.strokeWidth=1f;path.reset();path.moveTo(22f,10f);path.lineTo(w*.55f,10f);path.lineTo(w*.42f,28f);c.drawPath(path,p);path.reset();path.moveTo(22f,h-10f);path.lineTo(w*.55f,h-10f);path.lineTo(w*.42f,h-28f);c.drawPath(path,p)
        }
        override fun setAlpha(a:Int){};override fun setColorFilter(f:ColorFilter?){}
        @Suppress("DEPRECATION")override fun getOpacity()=android.graphics.PixelFormat.OPAQUE
    }

    private fun addDrawerItems() {
        drawerPanel.removeAllViews()

        // Marco exterior del panel: todo se dibuja con Canvas.
        drawerPanel.addView(CyberDrawerPanelFrameView(this, cyan, pink, resources.displayMetrics.density),
            FrameLayout.LayoutParams(-1, -1))

        val items = listOf(
            Triple("INICIO", "nav_home", 0),
            Triple("LÍNEAS", "nav_lineas", 1),
            Triple("MAPA", "nav_mapa", 2),
            Triple("FAVORITOS", "nav_favoritos", 3),
            Triple("PARADAS CERCANAS", "nav_cercanas", 4)
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(34), dp(18), dp(18))
            clipChildren = false
            clipToPadding = false
        }

        items.forEach { (label, iconName, index) ->
            val item = CyberDrawerItemView(
                this,
                label,
                iconName,
                index == currentSection,
                cyberpunkTypeface,
                cyan,
                pink,
                muted
            ).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    navigateTo(index)
                    toggleDrawer()
                }
            }
            applyCyberTap(item)
            list.addView(item, LinearLayout.LayoutParams(-1, dp(64)).apply {
                bottomMargin = dp(12)
            })
        }

        drawerPanel.addView(list, FrameLayout.LayoutParams(-1, -1))
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome(); 1 -> showLines(); 2 -> showMap(null); 3 -> showFavorites(); 4 -> showNearby()
        }
    }

    private fun updateNav(selected: Int) {
        currentSection = selected
        // El menú contextual conserva sus vistas entre navegaciones, por lo que
        // debemos reconstruirlo para que cada elemento reciba el nuevo estado
        // selected y no quede INICIO visualmente seleccionado.
        addDrawerItems()
        navBar.removeAllViews()
        (navBar.background as? CyberBottomBarBackground)?.selectedIndex = selected
        navBar.setPadding(0, dp(2), 0, dp(2))
        navBar.clipChildren = false
        navBar.clipToPadding = false

        val items = listOf(
            Triple("INICIO", "nav_home", 0),
            Triple("LÍNEAS", "nav_lineas", 1),
            Triple("MAPA", "nav_mapa", 2),
            Triple("FAVORITOS", "nav_favoritos", 3),
            Triple("PARADAS", "nav_cercanas", 4)
        )

        items.forEach { (label, iconName, index) ->
            val item = CyberNavItemView(
                this,
                label,
                iconName,
                index == selected,
                cyberpunkTypeface,
                cyan,
                pink,
                muted
            ).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    when (index) {
                        0 -> showHome()
                        1 -> showLines()
                        2 -> showMap(null)
                        3 -> showFavorites()
                        4 -> showNearby()
                    }
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
        // FUENTE CYBERPUNK: se reaplica a TODA la sección Inicio.
        // Esto incluye todos los TextView actuales y cualquier TextView hijo
        // generado dentro de las tarjetas, botones o textos de respaldo.
        applyCyberpunkTypeface(box)
        content.addView(ScrollView(this).apply {
            addView(box)
            // Cyberpunk ya fue aplicada recursivamente al contenido completo de Inicio.
        })
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

    private fun addCyberVioletFrame(container: FrameLayout) {
        val frame = AnimatedVioletCyberFrameView(this)
        frame.isClickable = false
        frame.isFocusable = false
        container.addView(frame, FrameLayout.LayoutParams(-1, -1))
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
                text = label; gravity = Gravity.CENTER; textSize = 14f; typeface = cyberpunkTypeface
                setTextColor(cyan); contentDescription = description
            }, FrameLayout.LayoutParams(-1, -1))
        }
        addCyberVioletFrame(this)
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
        addCyberVioletFrame(this)
    }

    private fun applyCyberpunkTypeface(view: View) {
        if (view is TextView) {
            view.typeface = cyberpunkTypeface
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyCyberpunkTypeface(view.getChildAt(i))
            }
        }
    }

    private fun box() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(24))
    }

    private fun card(primary: String, secondary: String, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12))
        setBackgroundColor(panelColor); setOnClickListener { action() }
        addView(TextView(this@MainActivity).apply {
            text = primary; textSize = 14f; typeface = cyberpunkTypeface; setTextColor(Color.WHITE)
        })
        addView(TextView(this@MainActivity).apply { text = secondary; textSize = 10f; typeface = cyberpunkTypeface; setTextColor(muted) })
    }

    private fun panel(primary: String, secondary: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundColor(panelColor)
        addView(TextView(this@MainActivity).apply {
            text = primary; textSize = 11f; typeface = cyberpunkTypeface; setTextColor(cyan)
        })
        addView(TextView(this@MainActivity).apply { text = secondary; textSize = 13f; typeface = cyberpunkTypeface; setTextColor(muted) })
    }

    private fun button(label: String, color: Int, action: () -> Unit) = TextView(this).apply {
        text = label; gravity = Gravity.CENTER; textSize = 13f; typeface = cyberpunkTypeface
        setTextColor(color); setBackgroundColor(panelColor); setPadding(dp(12), dp(14), dp(12), dp(14))
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }

    private fun cyberSyncButton(action: () -> Unit): View {
        val bitmap = loadAssetBitmap("sincronizar_lineas.webp")
        if (bitmap == null) {
            return FrameLayout(this).apply {
                addView(button("SINCRONIZAR LÍNEAS", cyan, action).also { applyCyberTap(it) },
                    FrameLayout.LayoutParams(-1, -1))
                addCyberVioletFrame(this)
            }
        }
        return FrameLayout(this).apply {
            addView(ImageView(this@MainActivity).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_XY
                contentDescription = "Sincronizar líneas"
                isClickable = false
                isFocusable = false
                background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            }, FrameLayout.LayoutParams(-1, -1))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            applyCyberTap(this)
            addCyberVioletFrame(this)
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
            text = line.name.uppercase(); textSize = 25f; typeface = cyberpunkTypeface; setTextColor(cyan)
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
            textSize = 11f; typeface = cyberpunkTypeface; setTextColor(cyan)
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

    private class CyberDrawerPanelFrameView(
        context: Context,
        private val cyan: Int,
        private val pink: Int,
        private val density: Float
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            // Marco exterior completo del menú desplegable.
            CyberHeaderFrameDrawable.drawFrame(
                canvas,
                2f * density,
                2f * density,
                w - 2f * density,
                h - 2f * density,
                cyan,
                pink,
                density,
                12f * density,
                1f,
                0f
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * density
            paint.color = cyan
            paint.alpha = 150
            canvas.drawLine(10f * density, 20f * density, 58f * density, 20f * density, paint)
            canvas.drawLine(10f * density, 20f * density, 10f * density, 68f * density, paint)

            paint.color = pink
            paint.alpha = 170
            canvas.drawLine(w - 10f * density, h - 20f * density, w - 58f * density, h - 20f * density, paint)
            canvas.drawLine(w - 10f * density, h - 20f * density, w - 10f * density, h - 68f * density, paint)

            paint.style = Paint.Style.FILL
            paint.color = cyan
            paint.alpha = 210
            canvas.drawCircle(w - 16f * density, 16f * density, 2f * density, paint)
            paint.color = pink
            canvas.drawCircle(16f * density, h - 16f * density, 2f * density, paint)
        }
    }

    private class CyberDrawerItemView(
        context: Context,
        private val label: String,
        iconName: String,
        private val selected: Boolean,
        private val typeface: Typeface,
        private val cyan: Int,
        private val pink: Int,
        private val muted: Int
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val scaledDensity = resources.displayMetrics.scaledDensity
        private val icon: android.graphics.drawable.Drawable? = runCatching {
            val id = resources.getIdentifier(iconName, "drawable", context.packageName)
            if (id != 0) resources.getDrawable(id, context.theme) else null
        }.getOrNull()
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = 10.5f * scaledDensity
            textAlign = Paint.Align.LEFT
        }
        private var phase = if (selected) 0f else 8f
        private val runner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                phase += if (selected) .72f else .42f
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, 60L)
            }
        }

        init {
            setWillNotDraw(false)
            contentDescription = label
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            postOnAnimation(runner)
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(runner)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            val frameL = 2f * density
            val frameT = 2f * density
            val frameR = width - 2f * density
            val frameB = height - 2f * density

            // Cada botón del menú recibe su propio marco neon Canvas.
            CyberHeaderFrameDrawable.drawFrame(
                canvas,
                frameL,
                frameT,
                frameR,
                frameB,
                cyan,
                pink,
                density,
                9f * density,
                if (selected) .98f else .58f,
                phase
            )

            val iconSize = 30f * density
            val iconLeft = 15f * density
            val iconTop = (height - iconSize) * .5f
            icon?.let {
                it.setBounds(
                    iconLeft.toInt(),
                    iconTop.toInt(),
                    (iconLeft + iconSize).toInt(),
                    (iconTop + iconSize).toInt()
                )
                it.alpha = if (selected) 255 else 145
                it.setTint(if (selected) cyan else muted)
                it.draw(canvas)
            }

            textPaint.color = if (selected) cyan else muted
            textPaint.alpha = if (selected) 255 else 215
            canvas.drawText(
                label,
                58f * density,
                height * .5f - (textPaint.ascent() + textPaint.descent()) * .5f,
                textPaint
            )

            // Indicador lateral y punto de estado, también en Canvas.
            val indicatorColor = if (selected) pink else cyan
            paintItem.color = indicatorColor
            paintItem.alpha = if (selected) 235 else 105
            paintItem.style = Paint.Style.FILL
            canvas.drawCircle(width - 15f * density, height * .5f, 2f * density, paintItem)

            paintItem.style = Paint.Style.STROKE
            paintItem.strokeWidth = 1f * density
            paintItem.alpha = if (selected) 190 else 70
            canvas.drawLine(width - 28f * density, 14f * density, width - 18f * density, 14f * density, paintItem)
            canvas.drawLine(width - 28f * density, height - 14f * density, width - 18f * density, height - 14f * density, paintItem)
        }

        private val paintItem = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    /**
     * Fondo GIF animado con decodificación nativa de Android moderno.
     *
     * Android 12+ (API 31):
     * - Recupera el GIF original desde el asset Base64.
     * - ImageDecoder lo convierte a AnimatedImageDrawable.
     * - AnimatedImageDrawable decodifica los frames en segundo plano y se repite infinitamente.
     *
     * Android 7-11:
     * - Mantiene Movie como fallback para conservar minSdk 24.
     */
    /**
     * Fondo GIF animado real.
     *
     * Se carga el GIF binario directamente desde assets y se dibuja con
     * Movie en una capa de software para evitar problemas de callbacks
     * internos y de compatibilidad con ImageDecoder.
     */
    private class AnimatedGifBackgroundView(context: Context) : View(context) {
        private var movie: android.graphics.Movie? = null
        private var startedAt = 0L
        private var loadError = false

        private val invalidator = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                invalidate()
                postOnAnimation(this)
            }
        }

        init {
            setWillNotDraw(false)
            isClickable = false
            isFocusable = false
            // Movie/GIF se renderiza de forma fiable mediante Canvas software.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            movie = loadMovie(context)
        }

        private fun loadMovie(context: Context): android.graphics.Movie? {
            return runCatching {
                context.assets.open("background_cyberpunk.gif").use { input ->
                    android.graphics.Movie.decodeStream(input)
                }.also {
                    if (it == null) loadError = true
                }
            }.getOrElse {
                loadError = true
                null
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            startedAt = SystemClock.uptimeMillis()
            postOnAnimation(invalidator)
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(invalidator)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            val gif = movie ?: return
            if (width <= 0 || height <= 0) return

            val duration = gif.duration().takeIf { it > 0 } ?: 12000
            val elapsed = ((SystemClock.uptimeMillis() - startedAt) % duration).toInt()
            gif.setTime(elapsed)

            val mw = gif.width().toFloat()
            val mh = gif.height().toFloat()
            if (mw <= 0f || mh <= 0f) return

            // Cover: ocupa todo el contenedor sin deformar el GIF.
            val scale = maxOf(width / mw, height / mh)
            val drawW = mw * scale
            val drawH = mh * scale
            val left = (width - drawW) * .5f
            val top = (height - drawH) * .5f

            canvas.save()
            canvas.translate(left, top)
            canvas.scale(scale, scale)
            gif.draw(canvas, 0f, 0f)
            canvas.restore()
        }
    }

    private class CyberBackgroundView(context: Context) : View(context) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val path=Path()
        override fun onDraw(c:Canvas){val w=width.toFloat();val h=height.toFloat();val cx=w*.5f;val cy=h*.48f
            val lime=0xFF00F0FF.toInt();val orange=0xFFFF00FF.toInt();val blue=0xFF006CFF.toInt()
            c.drawColor(0xFF010302.toInt());p.style=Paint.Style.FILL
            p.shader=android.graphics.RadialGradient(cx,cy,maxOf(w,h)*.78f,intArrayOf(0x30100035,0x18002C45,0x00000000),null,android.graphics.Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
            // New visual language: vertical light shafts + orange volumetric zones.
            for(i in 0..7){val x=w*(.08f+i*.12f);p.color=if(i%2==0)lime else orange;p.alpha=34;path.reset();path.moveTo(x,0f);path.lineTo(x+28f,0f);path.lineTo(cx+(x-cx)*.18f,h);path.lineTo(cx+(x-cx)*.18f+12f,h);path.close();c.drawPath(path,p)}
            p.style=Paint.Style.STROKE;p.strokeWidth=1f
            // Radar rings, no grid.
            p.color=lime;p.alpha=105;c.drawCircle(cx,cy,38f,p);c.drawCircle(cx,cy,74f,p);c.drawCircle(cx,cy,126f,p)
            p.color=orange;p.alpha=110;c.drawArc(cx-170f,cy-170f,cx+170f,cy+170f,18f,95f,false,p);c.drawArc(cx-120f,cy-120f,cx+120f,cy+120f,202f,78f,false,p)
            // Data beams.
            for(i in 0..11){val x=w*i/11f;p.color=if(i%2==0) blue else orange;p.alpha=42;c.drawLine(x,0f,cx+(x-cx)*.25f,cy,p);c.drawLine(x,h,cx+(x-cx)*.25f,cy,p)}
            // Floating nodes / beacons.
            p.style=Paint.Style.FILL;for(i in 1..16){val x=w*(.04f+(i*47%92)/100f);val y=h*(.06f+(i*31%88)/100f);p.color=if(i%4==0)orange else lime;p.alpha=120;c.drawCircle(x,y,1.5f+(i%3),p);p.alpha=25;c.drawCircle(x,y,10f+(i%4)*4f,p)}
            // Central radar target.
            p.style=Paint.Style.STROKE;p.color=lime;p.alpha=160;p.strokeWidth=2f;c.drawCircle(cx,cy,18f,p);p.color=orange;p.alpha=210;c.drawLine(cx-28f,cy,cx+28f,cy,p);c.drawLine(cx,cy-28f,cx,cy+28f,p)
            p.color=blue;p.alpha=110;c.drawCircle(cx,cy,5f,p)
            // Asymmetric corner brackets.
            p.color=lime;p.alpha=100;p.strokeWidth=2f;c.drawLine(10f,10f,78f,10f,p);c.drawLine(10f,10f,10f,55f,p);p.color=orange;c.drawLine(w-10f, h-10f,w-78f,h-10f,p);c.drawLine(w-10f,h-10f,w-10f,h-55f,p)
        }
    }

    /**
     * Header title rendered directly on Canvas.
     *
     * Unlike a TextView property animation, the title's pixels are rebuilt on
     * every animation frame: cyan/magenta RGB ghosts, brightness cuts and
     * short horizontal glitch slices are all part of the rendered frame.
     */
    private class CyberHeaderOuterFrameDrawable(
        private val cyan: Int,
        private val pink: Int,
        private val density: Float
    ) : android.graphics.drawable.Drawable() {
        override fun draw(canvas: Canvas) {
            val inset = 2f * density
            CyberHeaderFrameDrawable.drawFrame(
                canvas,
                bounds.left + inset,
                bounds.top + inset,
                bounds.right - inset,
                bounds.bottom - inset,
                cyan,
                pink,
                density,
                9f * density,
                1f,
                0f
            )
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    private class AnimatedVioletCyberFrameView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private var phase = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private val runner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                phase = (phase + 0.20f) % 16f
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, 55L)
            }
        }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); postOnAnimation(runner) }
        override fun onDetachedFromWindow() { removeCallbacks(runner); super.onDetachedFromWindow() }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            // Marco reforzado: el grosor y el halo aumentan para que la animación
            // sea claramente visible y cubra las esquinas negras de las tarjetas.
            // Marco extra grueso y casi al ras del borde para ocultar por completo
            // las esquinas negras que pueda conservar la imagen de la tarjeta.
            val cut = minOf(2f * density, minOf(w, h) * .035f)
            val edge = 1.5f * density
            path.reset()
            path.moveTo(cut, edge)
            path.lineTo(w - cut, edge)
            path.lineTo(w - edge, edge + cut)
            path.lineTo(w - edge, h - cut)
            path.lineTo(w - cut, h - edge)
            path.lineTo(cut, h - edge)
            path.lineTo(edge, h - cut)
            path.lineTo(edge, cut)
            path.close()
            // Marco base negro: queda fijo y oculta las esquinas negras de la imagen.
            val black = 0xFF000000.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f * density
            paint.color = black
            paint.alpha = 255
            canvas.drawPath(path, paint)

            // Animación celeste neón: halo + línea brillante que recorre el marco.
            val neonCyan = 0xFF00F0FF.toInt()
            paint.strokeWidth = 5f * density
            paint.color = neonCyan
            paint.alpha = 38
            canvas.drawPath(path, paint)

            // Segmento animado que recorre TODO el perímetro, no solamente el borde superior.
            val perimeter = PathMeasure(path, false)
            val pathLength = perimeter.length
            if (pathLength > 0f) {
                val sweep = ((phase % 16f) / 16f)
                val segmentLength = minOf(24f * density, pathLength * 0.13f)
                val start = pathLength * sweep
                val end = start + segmentLength
                val animatedPath = Path()
                if (end <= pathLength) {
                    perimeter.getSegment(start, end, animatedPath, true)
                } else {
                    perimeter.getSegment(start, pathLength, animatedPath, true)
                    perimeter.getSegment(0f, end - pathLength, animatedPath, true)
                }
                paint.strokeWidth = 2.5f * density
                paint.color = neonCyan
                paint.alpha = 255
                canvas.drawPath(animatedPath, paint)
            }
        }
    }

    private class AnimatedCyberFrameDrawable(
        private val cyan: Int,
        private val pink: Int,
        private val density: Float,
        private val cornerCut: Float,
        private val inset: Float
    ) : CyberHeaderFrameDrawable(cyan, pink, density, cornerCut, inset) {
        private var phase = 0f
        private val handler = Handler(Looper.getMainLooper())
        private val runner = object : Runnable {
            override fun run() {
                phase = (phase + 0.20f) % 16f
                invalidateSelf()
                handler.postDelayed(this, 55L)
            }
        }
        init { handler.postDelayed(runner, 55L) }

        override fun draw(canvas: Canvas) {
            drawFrame(
                canvas,
                bounds.left + inset, bounds.top + inset,
                bounds.right - inset, bounds.bottom - inset,
                cyan, pink, density, cornerCut, 1f, phase
            )
        }
    }

    private open class CyberHeaderFrameDrawable(
        private val cyan: Int,
        private val pink: Int,
        private val density: Float,
        private val cornerCut: Float,
        private val inset: Float
    ) : android.graphics.drawable.Drawable() {
        override fun draw(canvas: Canvas) {
            drawFrame(
                canvas,
                bounds.left + inset,
                bounds.top + inset,
                bounds.right - inset,
                bounds.bottom - inset,
                cyan,
                pink,
                density,
                cornerCut,
                1f,
                0f
            )
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

        companion object {
            fun drawFrame(
                canvas: Canvas,
                left: Float,
                top: Float,
                right: Float,
                bottom: Float,
                cyan: Int,
                pink: Int,
                density: Float,
                cut: Float,
                intensity: Float,
                phase: Float
            ) {
                if (right <= left || bottom <= top) return
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                val w = right - left
                val h = bottom - top
                val c = cut.coerceAtMost(minOf(w, h) * 0.28f)
                val path = Path().apply {
                    moveTo(left + c, top)
                    lineTo(right - c, top)
                    lineTo(right, top + c)
                    lineTo(right, bottom - c)
                    lineTo(right - c, bottom)
                    lineTo(left + c, bottom)
                    lineTo(left, bottom - c)
                    lineTo(left, top + c)
                    close()
                }
                glow.strokeWidth = 5f * density
                glow.color = cyan
                glow.alpha = (48 * intensity).toInt()
                canvas.drawPath(path, glow)
                glow.strokeWidth = 2.5f * density
                glow.color = pink
                glow.alpha = (36 * intensity).toInt()
                canvas.drawPath(path, glow)
                p.strokeWidth = 1.15f * density
                p.color = cyan
                p.alpha = (230 * intensity).toInt()
                canvas.drawPath(path, p)
                p.strokeWidth = 1f * density
                p.color = pink
                p.alpha = (190 * intensity).toInt()
                val split = left + w * 0.54f
                canvas.drawLine(left, bottom, split, bottom, p)
                canvas.drawLine(right, top, split, top, p)
                p.color = cyan
                p.alpha = (210 * intensity).toInt()
                val notch = minOf(11f * density, w * 0.22f)
                canvas.drawLine(left + c, top, left + c + notch, top, p)
                canvas.drawLine(right - c - notch, bottom, right - c, bottom, p)
                // Recorrido animado por TODO el perímetro del marco.
                val perimeter = PathMeasure(path, false)
                val pathLength = perimeter.length
                if (pathLength > 0f) {
                    val pulse = ((phase % 16f) / 16f)
                    val segmentLength = minOf(20f * density, pathLength * 0.10f)
                    val start = pathLength * pulse
                    val end = start + segmentLength
                    val animatedPath = Path()
                    if (end <= pathLength) {
                        perimeter.getSegment(start, end, animatedPath, true)
                    } else {
                        perimeter.getSegment(start, pathLength, animatedPath, true)
                        perimeter.getSegment(0f, end - pathLength, animatedPath, true)
                    }
                    p.color = cyan
                    p.alpha = 255
                    p.strokeWidth = 2.6f * density
                    canvas.drawPath(animatedPath, p)
                }
            }
        }
    }

    private class CyberHeaderView(context: Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val density = resources.displayMetrics.density
        private var phase = 0f
        private val runner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                phase = (phase + 0.20f) % 16f
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, 55L)
            }
        }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); postOnAnimation(runner) }
        override fun onDetachedFromWindow() { removeCallbacks(runner); super.onDetachedFromWindow() }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val cyan = 0xFF00F0FF.toInt()
            val pink = 0xFFFF00FF.toInt()
            val blue = 0xFF006CFF.toInt()

            p.style = Paint.Style.FILL
            p.shader = android.graphics.LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(0xFF02030B.toInt(), 0xFF08051A.toInt(), 0xFF01020A.toInt()),
                null, android.graphics.Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, w, h, p)
            p.shader = null

            // Marco exterior completo: cubre todo el perímetro del header.
            CyberHeaderFrameDrawable.drawFrame(
                c,
                1.5f * density,
                1.5f * density,
                w - 1.5f * density,
                h - 1.5f * density,
                cyan,
                pink,
                density,
                9f * density,
                1f,
                phase
            )

            // Detalles interiores neon que acompañan al marco exterior.
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.4f * density
            p.color = cyan
            p.alpha = 190
            c.drawLine(8f * density, 3f * density, w * .34f, 3f * density, p)
            c.drawLine(w * .66f, 3f * density, w - 8f * density, 3f * density, p)
            c.drawLine(8f * density, h - 3f * density, w * .34f, h - 3f * density, p)
            c.drawLine(w * .66f, h - 3f * density, w - 8f * density, h - 3f * density, p)

            p.color = blue
            p.alpha = 160
            c.drawLine(2f * density, 9f * density, 9f * density, 2f * density, p)
            c.drawLine(w - 2f * density, 9f * density, w - 9f * density, 2f * density, p)
            c.drawLine(2f * density, h - 9f * density, 9f * density, h - 2f * density, p)
            c.drawLine(w - 2f * density, h - 9f * density, w - 9f * density, h - 2f * density, p)
        }
    }

    private class CyberHeaderTitleView(
        context: Context,
        private val typeface: Typeface,
        private val cyan: Int,
        private val pink: Int
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val scaledDensity = resources.displayMetrics.scaledDensity
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = 15f * scaledDensity
            textAlign = Paint.Align.CENTER
        }
        private val ghost = Paint(paint)
        private var phase = 0f
        private val runner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                phase = (phase + .15f) % 16f
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, 55L)
            }
        }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); postOnAnimation(runner) }
        override fun onDetachedFromWindow() { removeCallbacks(runner); super.onDetachedFromWindow() }

        override fun onDraw(canvas: Canvas) {
            val text = "TU COLECTIVO 2.0"
            val cx = width * .5f
            val baseline = height * .55f - (paint.ascent() + paint.descent()) * .5f
            val tw = paint.measureText(text)
            val left = cx - tw * .5f - 18f * density
            val right = cx + tw * .5f + 18f * density
            val top = maxOf(2f * density, baseline + paint.ascent() - 7f * density)
            val bottom = minOf(height - 2f * density, baseline + paint.descent() + 7f * density)
            // La fase ahora recorre el perímetro completo antes de volver a cero.
            // El valor es continuo, por lo que el segmento animado no queda limitado
            // a una fracción del marco ni se reinicia prematuramente.
            CyberHeaderFrameDrawable.drawFrame(canvas, left, top, right, bottom, cyan, pink, density, 6f * density, .82f, phase)

            val frame = (phase / .15f).toInt()
            val glitch = frame % 16 in setOf(3, 4, 9, 10)
            ghost.color = if (frame % 2 == 0) cyan else pink
            ghost.alpha = if (glitch) 190 else 70
            canvas.drawText(text, cx + if (glitch) 2.5f else 0f, baseline, ghost)
            paint.color = cyan
            paint.alpha = 255
            canvas.drawText(text, cx, baseline, paint)
        }
    }

    private class CyberHeaderStatusView(
        context: Context,
        private val typeface: Typeface,
        private val cyan: Int,
        private val pink: Int
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val scaledDensity = resources.displayMetrics.scaledDensity
        private var value = "● SISTEMA LISTO"
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f * scaledDensity
            textAlign = Paint.Align.LEFT
        }
        private var phase = 0f
        private val runner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                phase += .15f
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, 60L)
            }
        }
        fun setStatusText(text: String) { value = text; invalidate() }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); postOnAnimation(runner) }
        override fun onDetachedFromWindow() { removeCallbacks(runner); super.onDetachedFromWindow() }

        override fun onDraw(canvas: Canvas) {
            val baseline = height * .68f
            val textWidth = paint.measureText(value)
            val left = 0f
            val right = textWidth + 18f * density
            val top = maxOf(1f * density, baseline + paint.ascent() - 5f * density)
            val bottom = minOf(height - 1f * density, baseline + paint.descent() + 5f * density)
            CyberHeaderFrameDrawable.drawFrame(canvas, left, top, right, bottom, cyan, pink, density, 5f * density, .72f, phase)
            paint.color = if (value.contains("ERROR")) pink else cyan
            paint.alpha = 245
            canvas.drawText(value, 9f * density, baseline, paint)
        }
    }

    private class CyberNavItemView(
        context: Context,
        private val label: String,
        iconName: String,
        private val selected: Boolean,
        private val typeface: Typeface,
        private val cyan: Int,
        private val pink: Int,
        private val muted: Int
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val scaledDensity = resources.displayMetrics.scaledDensity
        private val icon: android.graphics.drawable.Drawable? = runCatching {
            val id = resources.getIdentifier(iconName, "drawable", context.packageName)
            if (id != 0) resources.getDrawable(id, context.theme) else null
        }.getOrNull()
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = 8.5f * scaledDensity
        }
        private var phase = if (selected) 4f else 10f
        private val runner = object : Runnable {
            override fun run() {
                if (!isAttachedToWindow) return
                phase += if (selected) .7f else .38f
                postInvalidateOnAnimation()
                postOnAnimationDelayed(this, 60L)
            }
        }
        init { setWillNotDraw(false); contentDescription = label }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); postOnAnimation(runner) }
        override fun onDetachedFromWindow() { removeCallbacks(runner); super.onDetachedFromWindow() }

        override fun onDraw(canvas: Canvas) {
            val frameW = 66f * density
            val frameL = (width - frameW) * .5f
            val frameR = frameL + frameW
            CyberHeaderFrameDrawable.drawFrame(
                canvas, frameL, 3f * density, frameR, height - 3f * density,
                cyan, pink, density, 7f * density, if (selected) .95f else .62f, phase
            )
            val iconSize = 25f * density
            val iconLeft = width * .5f - iconSize * .5f
            val iconTop = 8f * density
            icon?.let {
                it.setBounds(iconLeft.toInt(), iconTop.toInt(), (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt())
                it.alpha = if (selected) 255 else 120
                it.setTint(if (selected) cyan else muted)
                it.draw(canvas)
            }
            textPaint.color = if (selected) cyan else muted
            textPaint.alpha = if (selected) 255 else 190
            canvas.drawText(label, width * .5f, height - 10f * density, textPaint)
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}

