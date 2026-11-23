package com.launcher.dd
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity(), LocationListener {
    private lateinit var speed: SpeedView; private lateinit var road: RoadView; private lateinit var map: MapView
    private val track = Polyline(); private var lastW = 0L
    private var freq = 106.2f
    private val prefs by lazy { getSharedPreferences("slots", MODE_PRIVATE) }
    private val radioPkgs = listOf("com.syu.radio", "com.microntek.radio", "com.android.fmradio", "com.mediatek.fmradio", "com.hct.radio", "com.qualcomm.fmradio")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)
        speed = SpeedView(this); road = RoadView(this); map = MapView(this)
        findViewById<FrameLayout>(R.id.speedBox).addView(speed)
        findViewById<FrameLayout>(R.id.roadBox).addView(road)
        findViewById<FrameLayout>(R.id.mapBox).addView(map)
        map.setTileSource(TileSourceFactory.MAPNIK); map.setMultiTouchControls(true); map.controller.setZoom(17.0)
        track.outlinePaint.color = 0xFF2979FF.toInt(); track.outlinePaint.strokeWidth = 12f
        map.overlays.add(track)
        map.overlays.add(MyLocationNewOverlay(GpsMyLocationProvider(this), map).apply { enableMyLocation(); enableFollowLocation() })
        buildBigButtons(); buildDock(); setupRadio()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startGps()
        else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
    }
    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) { if (g.isNotEmpty() && g[0] == 0) startGps() }
    private fun startGps() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this) } catch (_: SecurityException) {}
    }
    override fun onLocationChanged(l: Location) {
        val kmh = if (l.hasSpeed()) l.speed * 3.6f else 0f
        val v = if (kmh < 2f) 0f else kmh
        speed.setSpeed(v); road.setSpeed(v)
        val gp = GeoPoint(l.latitude, l.longitude)
        if (v > 0) track.addPoint(gp)
        map.controller.animateTo(gp)
        if (System.currentTimeMillis() - lastW > 15 * 60_000L) { lastW = System.currentTimeMillis(); loadWeather(l.latitude, l.longitude) }
    }
    private fun loadWeather(la: Double, lo: Double) = thread {
        try {
            val u = "https://api.open-meteo.com/v1/forecast?latitude=$la&longitude=$lo&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset&forecast_days=5&timezone=auto"
            val j = JSONObject(URL(u).readText()); val c = j.getJSONObject("current"); val d = j.getJSONObject("daily")
            val city = try { Geocoder(this).getFromLocation(la, lo, 1)?.firstOrNull()?.locality ?: "" } catch (_: Exception) { "" }
            val days = d.getJSONArray("time"); val mx = d.getJSONArray("temperature_2m_max"); val mn = d.getJSONArray("temperature_2m_min"); val wc = d.getJSONArray("weather_code")
            val fc = (0 until days.length()).joinToString("   ") { "${days.getString(it).takeLast(5)} ${icon(wc.getInt(it))} ${mx.getDouble(it).toInt()}°/${mn.getDouble(it).toInt()}°" }
            runOnUiThread {
                findViewById<TextView>(R.id.tvCity).text = "📍 $city"
                findViewById<TextView>(R.id.tvTemp).text = "${c.getDouble("temperature_2m").toInt()}°C ${icon(c.getInt("weather_code"))}"
                findViewById<TextView>(R.id.tvInfo).text = "Sensación: ${c.getDouble("apparent_temperature").toInt()}°  Viento: ${c.getDouble("wind_speed_10m").toInt()} km/h\n☀ ${d.getJSONArray("sunrise").getString(0).takeLast(5)}  🌙 ${d.getJSONArray("sunset").getString(0).takeLast(5)}"
                findViewById<TextView>(R.id.tvForecast).text = fc
            }
        } catch (_: Exception) { lastW = 0 }
    }
    private fun icon(c: Int) = when { c == 0 -> "☀️"; c < 4 -> "⛅"; c < 50 -> "🌫"; c < 70 -> "🌧"; c < 80 -> "❄️"; c < 90 -> "🌦"; else -> "⛈" }

    private fun bigBtn(ic: Int, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(ic); setBackgroundResource(R.drawable.big_btn); setPadding(40, 40, 40, 40); setOnClickListener { onClick() }
        layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0; height = 0; setMargins(6, 6, 6, 6) }
    }
    private fun buildBigButtons() {
        val g = findViewById<GridLayout>(R.id.bigGrid)
        g.addView(bigBtn(R.drawable.ic_bt) { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) })
        g.addView(bigBtn(R.drawable.ic_car) { startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) })
        g.addView(bigBtn(R.drawable.ic_tools) { startActivity(Intent(Settings.ACTION_SETTINGS)) })
        g.addView(bigBtn(R.drawable.ic_wifi) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) })
    }
    private fun buildDock() {
        val dock = findViewById<LinearLayout>(R.id.dock); dock.removeAllViews()
        for (i in 0 until 5) dock.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(10, 8, 10, 8) }; setPadding(14, 14, 14, 14); setBackgroundResource(R.drawable.slot)
            refreshSlot(this, i)
            setOnClickListener { val p = prefs.getString("s$i", null); if (p == null) pick(i, this) else packageManager.getLaunchIntentForPackage(p)?.let { startActivity(it) } }
            setOnLongClickListener { pick(i, this); true }
        })
        dock.addView(TextView(this).apply { text = "⠿"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(-1)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f); setOnClickListener { pick(-1, null) } })
    }
    private fun refreshSlot(v: ImageView, i: Int) {
        val p = prefs.getString("s$i", null)
        try { if (p != null) v.setImageDrawable(packageManager.getApplicationIcon(p)) else v.setImageResource(android.R.drawable.ic_input_add) } catch (_: Exception) { v.setImageResource(android.R.drawable.ic_input_add) }
    }
    private fun pick(slot: Int, v: ImageView?) {
        val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val names = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle(if (slot < 0) "Aplicaciones" else "Elegir app").setItems(names) { _, w ->
            val pkg = apps[w].activityInfo.packageName
            if (slot < 0) packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            else { prefs.edit().putString("s$slot", pkg).apply(); v?.let { refreshSlot(it, slot) } }
        }.show()
    }
    private fun setupRadio() {
        val tv = findViewById<TextView>(R.id.tvFreq)
        findViewById<Button>(R.id.fPrev).setOnClickListener { freq = (freq - 0.1f).coerceAtLeast(87.5f); tv.text = "%.1f".format(freq) }
        findViewById<Button>(R.id.fNext).setOnClickListener { freq = (freq + 0.1f).coerceAtMost(108f); tv.text = "%.1f".format(freq) }
        findViewById<Button>(R.id.fRadio).setOnClickListener {
            radioPkgs.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }?.let { startActivity(it) }
                ?: Toast.makeText(this, "No se encontró la app de radio del equipo", Toast.LENGTH_LONG).show()
        }
    }
    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onBackPressed() {}
}
