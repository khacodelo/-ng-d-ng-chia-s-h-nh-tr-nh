package com.example.apptheodihnhtrnh

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.apptheodihnhtrnh.services.TrackingService
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.*

// --- MODELS ---
data class Point(val lat: Double, val lng: Double)
data class JourneyRequest(
    val startTime: Date,
    val endTime: Date,
    val distance: Float,
    val points: List<Point>
)

interface JourneyApiService {
    @POST("journey/save") // Đảm bảo khớp với backend
    fun saveJourney(
        @Header("x-auth-token") token: String,
        @Body request: JourneyRequest
    ): Call<Void>
}

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null
    private val pathPoints = mutableListOf<LatLng>()

    private var totalDistance = 0f
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    
    private var startTime: Date? = null

    private val trackingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lng = intent?.getDoubleExtra("lng", 0.0) ?: 0.0
            updateTrackingData(LatLng(lat, lng))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                startTime = Date()
                startTrackingService()
                btnStart.isEnabled = false
                btnStop.isEnabled = true
                resetUI()
            }
        }

        btnStop.setOnClickListener {
            stopTrackingService()
            saveJourneyToServer()
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun saveJourneyToServer() {
        val endTime = Date()
        val sharedPref = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""
        
        if (token.isEmpty()) {
            Toast.makeText(this, "Lỗi: Cần đăng nhập lại", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startTime == null || pathPoints.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để lưu", Toast.LENGTH_SHORT).show()
            return
        }

        val pointsList = pathPoints.map { Point(it.latitude, it.longitude) }
        val request = JourneyRequest(startTime!!, endTime, totalDistance, pointsList)

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        // SỬA: Đảm bảo baseUrl kết thúc bằng dấu / và @POST không bắt đầu bằng dấu /
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") 
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val apiService = retrofit.create(JourneyApiService::class.java)
        
        apiService.saveJourney(token, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MapActivity, "Lưu hành trình thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("SAVE_JOURNEY", "Lỗi 404: Hãy kiểm tra server.js đã có app.use('/journey') chưa")
                    Toast.makeText(this@MapActivity, "Lỗi lưu: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("SAVE_JOURNEY", "Failure: ${t.message}")
                Toast.makeText(this@MapActivity, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun stopTrackingService() = stopService(Intent(this, TrackingService::class.java))

    private fun updateTrackingData(newPoint: LatLng) {
        if (pathPoints.isNotEmpty()) {
            val results = FloatArray(1)
            Location.distanceBetween(pathPoints.last().latitude, pathPoints.last().longitude, 
                                     newPoint.latitude, newPoint.longitude, results)
            totalDistance += results[0]
        }
        pathPoints.add(newPoint)
        updateMapUI(newPoint)
        updateStatsUI()
    }

    private fun updateMapUI(newPoint: LatLng) {
        if (polyline == null) {
            polyline = mMap.addPolyline(PolylineOptions().addAll(pathPoints).color(Color.BLUE).width(12f))
        } else polyline?.points = pathPoints

        if (currentMarker == null) {
            currentMarker = mMap.addMarker(MarkerOptions().position(newPoint).title("Vị trí hiện tại"))
        } else currentMarker?.position = newPoint
        
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 17f))
    }

    private fun updateStatsUI() {
        tvDistance.text = String.format(Locale.getDefault(), "%.2f km", totalDistance / 1000)
    }

    private fun resetUI() {
        mMap.clear()
        pathPoints.clear()
        totalDistance = 0f
        polyline = null
        currentMarker = null
        updateStatsUI()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (checkPermissions()) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f)) }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        
        val listToRequest = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }

        if (listToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listToRequest.toTypedArray(), 1)
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("TRACKING_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(trackingReceiver, filter, Context.RECEIVER_EXPORTED)
        } else registerReceiver(trackingReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(trackingReceiver) } catch (e: Exception) {}
    }
}
