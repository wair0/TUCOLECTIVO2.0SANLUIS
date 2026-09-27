package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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

class MainActivity : AppCompatActivity() {
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

    // WEBP del header convertido a Base64.
    private val headerWebpBase64 = """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"""

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
        val backgroundImage = ImageView(this).apply {
            val bitmap = assets.open("background.webp").use { BitmapFactory.decodeStream(it) }
            if (bitmap == null) throw IllegalStateException("No se pudo decodificar background.webp")
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Fondo cyberpunk neon"
        }
        rootFrame.addView(backgroundImage, FrameLayout.LayoutParams(-1, -1))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
        }
        val headerLayout = CyberHeaderView(this)
        val headerImage = ImageView(this).apply {
            val bytes = Base64.decode(headerWebpBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) throw IllegalStateException("No se pudo decodificar el WEBP del header")
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            isClickable = false
            isFocusable = false
        }
        headerLayout.addView(headerImage, FrameLayout.LayoutParams(-1, -1))
        val menuBtn = CyberNeonTextView(this).apply {
            text = "☰"; textSize = 24f; setTextColor(cyan); setPadding(0, 0, dp(16), 0)
            setOnClickListener { toggleDrawer() }
        }
        val headerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(-1, dp(48)).apply {
                leftMargin = dp(44); rightMargin = dp(4); topMargin = dp(7)
            }
        }
        headerContent.addView(CyberNeonTextView(this).apply {
            text = "TU COLECTIVO 2.0"
            textSize = 18f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, dp(40))
        })
        title = CyberNeonTextView(this).apply { text = ""; textSize = 11f; setTextColor(muted) }
        headerContent.addView(title)
        status = CyberNeonTextView(this).apply {
            text = "● SISTEMA LISTO"; textSize = 16f; setTextColor(0xFF55FFB0.toInt())
        }
        headerLayout.addView(headerContent)
        headerLayout.addView(status, FrameLayout.LayoutParams(-2, dp(28)).apply {
            leftMargin = dp(180)
            topMargin = dp(32)
            gravity = Gravity.TOP
        })
        menuBtn.apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
        }
        headerLayout.addView(menuBtn, FrameLayout.LayoutParams(dp(44), dp(44)).apply {
            leftMargin = dp(4)
            topMargin = dp(14)
        })
        root.addView(headerLayout, LinearLayout.LayoutParams(-1, dp(72)))
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFF080C13.toInt()); visibility = View.GONE
        }
        root.addView(navBar, LinearLayout.LayoutParams(-1, dp(64)))
        rootFrame.addView(root)
        drawerScrim = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT); visibility = View.GONE
            setOnClickListener { toggleDrawer() }
        }
        rootFrame.addView(drawerScrim, FrameLayout.LayoutParams(-1, -1))
        drawerPanel = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(dp(300), -1).apply { gravity = Gravity.START }
            visibility = View.GONE
        }
        addDrawerItems()
        rootFrame.addView(drawerPanel)
        setContentView(rootFrame)
        updateNav(0)
    }

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen
        if (drawerOpen) {
            drawerScrim.visibility = View.VISIBLE
            drawerPanel.visibility = View.VISIBLE
            ensureDrawerHotspots()
        } else {
            drawerPanel.visibility = View.GONE
            drawerScrim.visibility = View.GONE
        }
    }

    private fun addDrawerItems() {
        val drawerImage = ImageView(this).apply {
            val bitmap = assets.open("drawer_menu.webp").use { BitmapFactory.decodeStream(it) }
            if (bitmap == null) throw IllegalStateException("No se pudo decodificar drawer_menu.webp")
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "Menú principal"
            isClickable = false; isFocusable = false
        }
        drawerPanel.addView(drawerImage, FrameLayout.LayoutParams(-1, -1))
    }

    private fun ensureDrawerHotspots() {
        if (drawerHotspotsReady) return
        drawerPanel.post {
            if (drawerPanel.height <= 0) {
                drawerPanel.viewTreeObserver.addOnGlobalLayoutListener(
                    object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (drawerPanel.height > 0) {
                                drawerPanel.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                createDrawerHotspots(drawerPanel.height)
                            }
                        }
                    }
                )
                return@post
            }
            createDrawerHotspots(drawerPanel.height)
        }
    }

    private fun createDrawerHotspots(panelHeight: Int) {
        if (drawerHotspotsReady) return
        val items = listOf(
            Triple("INICIO", 0, 0.15f), Triple("LÍNEAS", 1, 0.215f),
            Triple("MAPA", 2, 0.28f), Triple("FAVORITOS", 3, 0.345f),
            Triple("PARADAS CERCANAS", 4, 0.41f)
        )
        val hotspotHeight = maxOf((panelHeight * 0.075f).toInt(), dp(48))
        items.forEach { (label, index, topRatio) ->
            val hotspot = TextView(this).apply {
                text = ""; setBackgroundColor(Color.TRANSPARENT)
                isClickable = true; isFocusable = true; contentDescription = label
                setOnClickListener { navigateTo(index); toggleDrawer() }
            }
            drawerPanel.addView(hotspot, FrameLayout.LayoutParams(-1, hotspotHeight).apply {
                topMargin = (panelHeight * topRatio).toInt()
            })
        }
        drawerHotspotsReady = true
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome(); 1 -> showLines(); 2 -> showMap(null); 3 -> showFavorites(); 4 -> showNearby()
        }
    }

    private fun updateNav(selected: Int) {
        navBar.removeAllViews()
        listOf("⌂\nINICIO", "▤\nLÍNEAS", "★\nFAVORITOS", "◎\nPARADAS CERCANAS").forEachIndexed { index, label ->
            navBar.addView(TextView(this).apply {
                text = label; gravity = Gravity.CENTER; textSize = 10f
                setTextColor(if (index == selected) cyan else muted)
                setOnClickListener {
                    when (index) { 0 -> showHome(); 1 -> showLines(); 2 -> showFavorites(); 3 -> showNearby() }
                }
            }, LinearLayout.LayoutParams(0, -1, 1f))
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
            setOnClickListener { action() }
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
        title.text = "LÍNEA " + line.code; content.removeAllViews()
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
        content.removeAllViews(); title.text = street.name
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
        content.removeAllViews(); title.text = "PARADAS"
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
        content.removeAllViews(); title.text = "ARRIBOS"
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
        title.text = "PARADAS CERCANAS"; updateNav(3); content.removeAllViews()
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
        title.text = "FAVORITOS"; updateNav(2); content.removeAllViews()
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
        title.text = "MAPA"; content.removeAllViews()
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

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
