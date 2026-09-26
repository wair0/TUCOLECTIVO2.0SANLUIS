package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.os.Build
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
    private lateinit var drawerPanel: FrameLayout
    private lateinit var drawerScrim: View
    private var drawerOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerOpen) {
                        toggleDrawer()
                    } else {
                        finish()
                    }
                }
            }
        )

        showHome()
    }

    private fun buildShell() {
        val rootFrame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val backgroundImage = ImageView(this).apply {
            val bitmap = assets.open("background.webp").use {
                BitmapFactory.decodeStream(it)
            }

            if (bitmap == null) {
                throw IllegalStateException("No se pudo decodificar background.webp")
            }

            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Fondo cyberpunk neon"
        }

        rootFrame.addView(
            backgroundImage,
            FrameLayout.LayoutParams(-1, -1)
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
        }
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

        drawerScrim = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
            setOnClickListener {
                toggleDrawer()
            }
        }

        rootFrame.addView(
            drawerScrim,
            FrameLayout.LayoutParams(-1, -1)
        )

        drawerPanel = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                dp(300),
                -1
            ).apply {
                gravity = Gravity.START
            }
            visibility = View.GONE
        }

        addDrawerItems()
        rootFrame.addView(drawerPanel)
        setContentView(rootFrame)
        updateNav(0)
    }

    private var drawerHotspotsReady = false

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen

        if (drawerOpen) {
            drawerScrim.visibility = View.VISIBLE
            drawerPanel.visibility = View.VISIBLE
            // Los hotspots solo se pueden medir cuando el panel es VISIBLE
            // (con GONE height=0 y los botones quedan sin área táctil)
            ensureDrawerHotspots()
        } else {
            drawerPanel.visibility = View.GONE
            drawerScrim.visibility = View.GONE
        }
    }

    private fun addDrawerItems() {
        val drawerImage = ImageView(this).apply {
            val bitmap = assets.open("drawer_menu.webp").use {
                BitmapFactory.decodeStream(it)
            }

            if (bitmap == null) {
                throw IllegalStateException("No se pudo decodificar drawer_menu.webp")
            }

            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "Menú principal"
            isClickable = false
            isFocusable = false
        }

        drawerPanel.addView(
            drawerImage,
            FrameLayout.LayoutParams(-1, -1)
        )
        // Hotspots se crean en ensureDrawerHotspots() al abrir el drawer
    }

    private fun ensureDrawerHotspots() {
        if (drawerHotspotsReady) return

        drawerPanel.post {
            val panelHeight = drawerPanel.height
            if (panelHeight <= 0) {
                // Aún no medido: reintentar en el próximo layout
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
            createDrawerHotspots(panelHeight)
        }
    }

    private fun createDrawerHotspots(panelHeight: Int) {
        if (drawerHotspotsReady) return

        val items = listOf(
            Triple("INICIO", 0, 0.15f),
            Triple("LÍNEAS", 1, 0.215f),
            Triple("MAPA", 2, 0.28f),
            Triple("FAVORITOS", 3, 0.345f),
            Triple("PARADAS CERCANAS", 4, 0.41f)
        )

        // Altura suficiente para cubrir cada ícono del artwork
        val hotspotHeight = maxOf((panelHeight * 0.075f).toInt(), dp(48))

        items.forEach { (label, index, topRatio) ->
            val hotspot = TextView(this).apply {
                text = ""
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                isFocusable = true
                contentDescription = label
                setOnClickListener {
                    navigateTo(index)
                    toggleDrawer()
                }
            }

            drawerPanel.addView(
                hotspot,
                FrameLayout.LayoutParams(
                    -1,
                    hotspotHeight
                ).apply {
                    topMargin = (panelHeight * topRatio).toInt()
                }
            )
        }
        drawerHotspotsReady = true
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome()
            1 -> showLines()
            2 -> showMap(null)
            3 -> showFavorites()
            4 -> showNearby()
        }
    }
    private fun updateNav(selected: Int) {
        navBar.removeAllViews()
        val items = listOf("⌂\nINICIO", "▤\nLÍNEAS", "★\nFAVORITOS", "◎\nPARADAS CERCANAS")
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

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        }

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        }

        val item1 = cardHomeImage(
            "lineas_cyberpunk.webp",
            cyan
        ) { showLines() }

        val item2 = cardHome(
            "◉",
            "MAPA",
            "Explorar el mapa",
            0xFF00E87F.toInt()
        ) { showMap(null) }

        val item3 = cardHome(
            "⇒",
            "PARADAS CERCANAS",
            "Por tu ubicación",
            cyan
        ) { showNearby() }

        val item4 = cardHome(
            "★",
            "FAVORITOS",
            "Paradas guardadas",
            pink
        ) { showFavorites() }

        row1.addView(
            item1,
            LinearLayout.LayoutParams(0, dp(140), 1f).apply {
                rightMargin = dp(6)
            }
        )

        row1.addView(
            item2,
            LinearLayout.LayoutParams(0, dp(140), 1f).apply {
                leftMargin = dp(6)
            }
        )

        row2.addView(
            item3,
            LinearLayout.LayoutParams(0, dp(140), 1f).apply {
                rightMargin = dp(6)
            }
        )

        row2.addView(
            item4,
            LinearLayout.LayoutParams(0, dp(140), 1f).apply {
                leftMargin = dp(6)
            }
        )

        grid.addView(row1)
        grid.addView(row2)

        box.addView(grid)

        box.addView(
            button("SINCRONIZAR LÍNEAS", cyan) {
                loadLines(false)
            },
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                topMargin = dp(20)
            }
        )

        content.addView(
            ScrollView(this).apply {
                addView(box)
            }
        )
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
                .onFailure { error -> runOnUiThread { list.removeAllViews(); list.addView(panel("ERROR", error.message ?: "Sin datos")) } }
        }
    }

    private fun showMap(line: TransitLine?) {
        title.text = if (line == null) "MAPA" else "MAPA · LÍNEA " + line.code
        updateNav(2)
        content.removeAllViews()

        val mapView = CyberMapView(this)
        content.addView(mapView, FrameLayout.LayoutParams(-1, -1))

        executor.execute {
            runCatching {
                if (line != null) {
                    api.getStopsForLine(line.code)
                } else {
                    emptyList()
                }
            }.onSuccess { stops ->
                runOnUiThread {
                    mapView.setStops(stops.map {
                        MapStop(it.latitude, it.longitude, it.description, it.identifier)
                    })
                }
            }
        }
    }

    private fun showFavorites() {
        title.text = "FAVORITOS"
        updateNav(2)
        content.removeAllViews()
        val box = box()
        box.addView(panel("FAVORITOS", "Paradas que guardaste."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)

        val favorites = loadFavorites()
        if (favorites.isEmpty()) {
            list.addView(panel("VACÍO", "Todavía no guardaste paradas."))
        } else {
            favorites.forEach { fav ->
                list.addView(
                    card("🚏 " + fav.description, fav.lineName + " · " + fav.street) {
                        val stop = TransitStop(
                            code = fav.stopCode,
                            description = fav.description,
                            identifier = fav.identifier,
                            street = fav.street,
                            intersection = fav.intersection,
                            latitude = fav.latitude,
                            longitude = fav.longitude
                        )
                        val line = TransitLine(code = fav.lineCode, name = fav.lineName)
                        showArrivals(stop, line)
                    },
                    LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) }
                )
            }
        }
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun showNearby() {
        title.text = "PARADAS CERCANAS"
        updateNav(3)
        content.removeAllViews()
        val box = box()
        box.addView(panel("UBICACIÓN", "Buscando paradas cerca de vos..."))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)

        fun requestLocation() {
            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                list.addView(panel("PERMISO", "Necesitamos acceso a la ubicación."))
                return
            }
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc == null) {
                list.addView(panel("SIN GPS", "No se pudo obtener la ubicación actual."))
                return
            }
            executor.execute {
                runCatching { api.getNearbyStops(loc.latitude, loc.longitude) }
                    .onSuccess { stops ->
                        runOnUiThread {
                            list.removeAllViews()
                            stops.forEach { stop ->
                                list.addView(
                                    card("🚏 " + stop.description, stop.street + " " + stop.intersection) {
                                        // sin línea fija
                                    }
                                )
                            }
                            if (stops.isEmpty()) list.addView(panel("SIN RESULTADOS", "No hay paradas cercanas."))
                        }
                    }
                    .onFailure { error ->
                        runOnUiThread { list.removeAllViews(); list.addView(panel("ERROR", error.message ?: "Error")) }
                    }
            }
        }
        requestLocation()
        content.addView(ScrollView(this).apply { addView(box) })
    }

    private fun saveFavorite(stop: TransitStop, line: TransitLine) {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        val key = stop.identifier + "_" + line.code
        val json = JSONObject().apply {
            put("lineCode", line.code)
            put("lineName", line.name)
            put("stopCode", stop.code)
            put("description", stop.description)
            put("identifier", stop.identifier)
            put("street", stop.street)
            put("intersection", stop.intersection)
            put("latitude", stop.latitude)
            put("longitude", stop.longitude)
        }
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun loadFavorites(): List<FavoriteStop> {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        return prefs.all.values.mapNotNull { raw ->
            runCatching {
                val o = JSONObject(raw as String)
                FavoriteStop(
                    lineCode = o.getInt("lineCode"),
                    lineName = o.getString("lineName"),
                    stopCode = o.getInt("stopCode"),
                    description = o.getString("description"),
                    identifier = o.getString("identifier"),
                    street = o.getString("street"),
                    intersection = o.getString("intersection"),
                    latitude = o.getDouble("latitude"),
                    longitude = o.getDouble("longitude")
                )
            }.getOrNull()
        }
    }

    private fun cardHome(icon: String, title: String, subtitle: String, color: Int, action: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setBackgroundColor(panelColor)
            setOnClickListener { action() }
            addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(color)
            })
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 12f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(muted)
            })
        }

    private fun cardHomeImage(assetName: String, color: Int, action: () -> Unit) =
        FrameLayout(this).apply {
            setBackgroundColor(panelColor)
            setOnClickListener { action() }
            val img = ImageView(this@MainActivity).apply {
                val bitmap = assets.open(assetName).use { BitmapFactory.decodeStream(it) }
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            addView(img, FrameLayout.LayoutParams(-1, -1))
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
