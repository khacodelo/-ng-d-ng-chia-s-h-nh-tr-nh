package com.example.apptheodihnhtrnh

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null
    private var isTracking = false
    private val pathPoints = mutableListOf<LatLng>()

    // Stats variables
    private var totalDistance = 0f
    private var secondsElapsed = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            resetTracking()
            startTracking()
            btnStart.isEnabled = false
            btnStop.isEnabled = true
        }

        btnStop.setOnClickListener {
            stopTracking()
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        startTimer()

        // Khởi tạo callback an toàn
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!isTracking) return
                val location = result.lastLocation ?: return
                val newPoint = LatLng(location.latitude, location.longitude)
                
                if (pathPoints.isNotEmpty()) {
                    val lastPoint = pathPoints.last()
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        lastPoint.latitude, lastPoint.longitude,
                        newPoint.latitude, newPoint.longitude,
                        results
                    )
                    totalDistance += results[0]
                    updateStatsUI()
                }

                updateUI(newPoint)
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(request, it, Looper.getMainLooper())
            }
        }
    }

    private fun stopTracking() {
        isTracking = false
        stopTimer()
        locationCallback?.let { 
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null // Giải phóng bộ nhớ
        }
    }

    private fun resetTracking() {
        mMap.clear()
        pathPoints.clear()
        totalDistance = 0f
        secondsElapsed = 0L
        updateStatsUI()
        currentMarker = null
        polyline = null
    }

    private fun updateUI(newPoint: LatLng) {
        pathPoints.add(newPoint)

        if (polyline == null) {
            polyline = mMap.addPolyline(PolylineOptions()
                .addAll(pathPoints)
                .color(Color.BLUE)
                .width(12f)
                .geodesic(true))
        } else {
            polyline?.points = pathPoints
        }

        if (currentMarker == null) {
            currentMarker = mMap.addMarker(MarkerOptions()
                .position(newPoint)
                .title("Điểm bắt đầu")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        } else {
            currentMarker?.position = newPoint
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 17f))
    }

    private fun updateStatsUI() {
        val distanceKm = totalDistance / 1000
        tvDistance.text = String.format(Locale.getDefault(), "%.2f km", distanceKm)
        
        val hours = secondsElapsed / 3600
        val minutes = (secondsElapsed % 3600) / 60
        val seconds = secondsElapsed % 60
        tvTime.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun startTimer() {
        stopTimer() // Tránh chạy nhiều timer chồng lên nhau
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTracking) {
                    secondsElapsed++
                    updateStatsUI()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { 
            timerHandler.removeCallbacks(it)
            timerRunnable = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate()
        }
    }

    override fun onPause() {
        super.onPause()
        // Lưu ý: Module 5 sẽ xử lý background tracking, 
        // hiện tại nếu app pause thì tracking có thể bị hệ thống tạm dừng.
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking() // Đảm bảo gỡ bỏ GPS và Timer khi thoát
    }
}
