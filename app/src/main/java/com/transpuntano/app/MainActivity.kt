package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.util.Base64
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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOpen) toggleDrawer() else finish()
            }
        })
        showHome()
    }

    // FILE TOO LARGE - will restore via multi-part - TEMP STUB TO FIX CRASH
    private fun buildShell() {
        val tv = TextView(this).apply {
            text = "Restaurando..."
            setTextColor(cyan)
            textSize = 18f
            gravity = Gravity.CENTER
        }
        setContentView(tv)
    }
    private fun toggleDrawer() {}
    private fun showHome() {}
    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
