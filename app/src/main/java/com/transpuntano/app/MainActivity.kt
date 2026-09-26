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
            // Crear hotspots solo con el panel VISIBLE (GONE → height=0)
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
        // Hotspots se crean al abrir el drawer (ensureDrawerHotspots)
    }

    private fun ensureDrawerHotspots() {
        if (drawerHotspotsReady) return

        drawerPanel.post {
            var panelHeight = drawerPanel.height
            if (panelHeight <= 0) {
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

        // Artwork drawer_menu.webp: 300 × 1269 px
        // Ratios medidos sobre esa altura; se escalan al alto real del panel
        val items = listOf(
            Triple("INICIO", 0, 0.15f),
            Triple("LÍNEAS", 1, 0.215f),
            Triple("MAPA", 2, 0.28f),
            Triple("FAVORITOS", 3, 0.345f),
            Triple("PARADAS CERCANAS", 4, 0.41f)
        )

        // ~95 px sobre 1269; mínimo táctil 48 dp
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
                .onFailure { error -> runOnUiThread { list.removeAllViews(); list.addView(panel("ERROR", error.message ?: "Sin conexión")) } }
        }
    }

    private fun showNearby() {
        title.text = "PARADAS CERCANAS"
        updateNav(3)
        content.removeAllViews()

        val box = box()

        box.addView(
            panel(
                "PARADAS CERCANAS",
                "Buscando paradas próximas a tu ubicación."
            )
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        box.addView(
            list,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(12)
            }
        )

        box.addView(
            button("◉ ACTUALIZAR PARADAS", cyan) {
                loadNearbyList(list)
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(12)
            }
        )

        content.addView(
            ScrollView(this).apply {
                addView(box)
            }
        )

        loadNearbyList(list)
    }

    private fun loadNearbyList(list: LinearLayout) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                42
            )
            return
        }

        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val location =
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            status.text = "● SIN UBICACIÓN"
            list.removeAllViews()
            list.addView(
                panel(
                    "UBICACIÓN NO DISPONIBLE",
                    "Activá la ubicación e intentá nuevamente."
                )
            )
            return
        }

        status.text = "● BUSCANDO PARADAS"

        list.removeAllViews()
        list.addView(
            panel(
                "BUSCANDO...",
                "Consultando las paradas cercanas."
            )
        )

        executor.execute {
            runCatching {
                api.getNearby(location.latitude, location.longitude)
            }
                .onSuccess { stops ->
                    runOnUiThread {
                        list.removeAllViews()

                        val nearby = stops.take(60)

                        if (nearby.isEmpty()) {
                            list.addView(
                                panel(
                                    "SIN PARADAS",
                                    "No se encontraron paradas cercanas."
                                )
                            )
                        } else {
                            nearby.forEach { stop ->
                                val locationText = listOf(
                                    stop.street,
                                    stop.intersection
                                )
                                    .filter { value -> value.isNotBlank() }
                                    .joinToString(" · ")

                                val secondary = if (locationText.isBlank()) {
                                    "CÓDIGO " + stop.code
                                } else {
                                    "CÓDIGO " + stop.code + " · " + locationText
                                }

                                list.addView(
                                    card(
                                        "🚏 " + stop.description,
                                        secondary
                                    ) {
                                        toast(
                                            "PARADA " + stop.code +
                                                if (locationText.isBlank()) ""
                                                else " · " + locationText
                                        )
                                    },
                                    LinearLayout.LayoutParams(-1, dp(72)).apply {
                                        bottomMargin = dp(8)
                                    }
                                )
                            }
                        }

                        status.text = "● " + nearby.size + " PARADAS CERCANAS"
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        list.removeAllViews()
                        list.addView(
                            panel(
                                "ERROR",
                                error.message ?: "No se pudieron cargar las paradas."
                            )
                        )
                        status.text = "● SIN CONEXIÓN"
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
            text = "MAPA  •  UBICACIÓN Y PARADAS CERCANAS"
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xCC05070C.toInt())
        }

        root.addView(
            info,
            FrameLayout.LayoutParams(-1, dp(44)).apply {
                gravity = Gravity.TOP
            }
        )

        root.addView(
            button("◉ MI UBICACIÓN / PARADAS CERCANAS", cyan) {
                loadNearbyOnMap(map, info)
            },
            FrameLayout.LayoutParams(-1, dp(52)).apply {
                gravity = Gravity.BOTTOM
                leftMargin = dp(16)
                rightMargin = dp(16)
                bottomMargin = dp(16)
            }
        )

        map.setOnStopTap { stop ->
            toast(
                stop.title +
                    if (stop.subtitle.isBlank()) ""
                    else " · " + stop.subtitle
            )
        }

        content.addView(root)

        if (line != null) {
            executor.execute {
                runCatching { api.getRoute(line.code) }
                    .onSuccess { route ->
                        runOnUiThread {
                            map.setRoute(route)
                        }
                    }
                    .onFailure { error ->
                        runOnUiThread {
                            toast(
                                error.message
                                    ?: "No se pudo cargar el recorrido"
                            )
                        }
                    }
            }
        }
    }

    private fun loadNearbyOnMap(
        map: CyberMapView,
        info: TextView
    ) {
        if (
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            info.text = "MAPA  •  SE NECESITA UBICACIÓN PRECISA"

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                42
            )

            toast("Permití ubicación precisa para centrarte correctamente")
            return
        }

        val manager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        status.text = "● OBTENIENDO UBICACIÓN"
        info.text = "MAPA  •  OBTENIENDO UBICACIÓN ACTUAL"

        fun useLocation(location: android.location.Location?) {
            if (location == null) {
                runOnUiThread {
                    status.text = "● SIN UBICACIÓN"
                    info.text = "MAPA  •  UBICACIÓN NO DISPONIBLE"
                    toast("No se pudo obtener una ubicación actual")
                }
                return
            }

            runOnUiThread {
                map.setUserLocation(
                    location.latitude,
                    location.longitude,
                    true
                )

                val accuracyText =
                    if (location.hasAccuracy()) {
                        " ±" + location.accuracy.toInt() + " m"
                    } else {
                        ""
                    }

                status.text = "● UBICACIÓN ACTUAL" + accuracyText

                info.text =
                    "MAPA  •  UBICACIÓN ACTUAL" +
                    accuracyText +
                    "  •  BUSCANDO PARADAS"
            }

            executor.execute {
                runCatching {
                    api.getNearby(
                        location.latitude,
                        location.longitude
                    )
                }
                    .onSuccess { stops ->
                        val markers = stops
                            .take(60)
                            .map {
                                MapStop(
                                    it.code,
                                    "🚏 " + it.description,
                                    listOf(
                                        it.street,
                                        it.intersection
                                    )
                                        .filter { value ->
                                            value.isNotBlank()
                                        }
                                        .joinToString(" · "),
                                    it.latitude,
                                    it.longitude
                                )
                            }
                            .filter {
                                it.latitude != 0.0 &&
                                it.longitude != 0.0
                            }

                        runOnUiThread {
                            map.setStops(
                                markers,
                                fit = false
                            )

                            status.text =
                                "● " + markers.size +
                                " PARADAS CERCANAS"

                            info.text =
                                "MAPA  •  " +
                                markers.size +
                                " PARADAS CERCANAS" +
                                if (location.hasAccuracy()) {
                                    "  •  ±" +
                                    location.accuracy.toInt() +
                                    " m"
                                } else {
                                    ""
                                }
                        }
                    }
                    .onFailure { error ->
                        runOnUiThread {
                            status.text = "● SIN CONEXIÓN"
                            info.text =
                                "MAPA  •  ERROR AL CARGAR PARADAS"

                            toast(
                                error.message
                                    ?: "No se pudieron cargar las paradas"
                            )
                        }
                    }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val provider =
                when {
                    manager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                    ) -> LocationManager.GPS_PROVIDER

                    manager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    ) -> LocationManager.NETWORK_PROVIDER

                    else -> null
                }

            if (provider == null) {
                status.text = "● UBICACIÓN DESACTIVADA"
                info.text =
                    "MAPA  •  ACTIVÁ LA UBICACIÓN DEL TELÉFONO"

                toast("Activá la ubicación del teléfono")
                return
            }

            manager.getCurrentLocation(
                provider,
                null,
                mainExecutor
            ) { location ->

                if (location != null) {
                    useLocation(location)

                } else if (
                    provider != LocationManager.NETWORK_PROVIDER &&
                    manager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )
                ) {
                    manager.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        null,
                        mainExecutor
                    ) { networkLocation ->
                        useLocation(networkLocation)
                    }

                } else {
                    useLocation(null)
                }
            }

        } else {

            val location =
                manager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )
                    ?: manager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )

            useLocation(location)
        }
    }

    private fun cardHomeImage(assetName: String, color: Int, action: () -> Unit) = FrameLayout(this).apply {
        setBackgroundColor(panelColor)
        setOnClickListener { action() }
        isClickable = true
        isFocusable = true

        val image = ImageView(this@MainActivity).apply {
            val bitmap = assets.open(assetName).use { BitmapFactory.decodeStream(it) }
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(bg)
            contentDescription = "LÍNEAS · Recorridos y calles"
        }

        addView(image, FrameLayout.LayoutParams(-1, -1))

        addView(
            View(this@MainActivity).apply {
                setBackgroundColor(color)
                alpha = 0.85f
            },
            FrameLayout.LayoutParams(dp(3), -1).apply { gravity = Gravity.START }
        )
    }

    private fun cardHome(icon: String, title: String, subtitle: String, color: Int, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setBackgroundColor(panelColor)
        gravity = Gravity.CENTER_VERTICAL
        setOnClickListener { action() }

        addView(
            View(this@MainActivity).apply {
                setBackgroundColor(color)
            },
            LinearLayout.LayoutParams(dp(4), -1)
        )

        val textContent = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        textContent.addView(
            TextView(this@MainActivity).apply {
                text = icon
                textSize = 32f
                setTextColor(color)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(48))
        )

        textContent.addView(
            TextView(this@MainActivity).apply {
                text = title
                textSize = 13f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            }
        )

        textContent.addView(
            TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 10f
                setTextColor(muted)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(4)
            }
        )

        addView(
            textContent,
            LinearLayout.LayoutParams(0, -1, 1f).apply {
                leftMargin = dp(10)
            }
        )
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
