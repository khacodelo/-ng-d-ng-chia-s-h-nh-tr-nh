package com.example.apptheodihnhtrnh

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.apptheodihnhtrnh.data.api.RetrofitClient
import com.example.apptheodihnhtrnh.services.TrackingService
import com.example.apptheodihnhtrnh.ui.auth.LoginActivity
import com.example.apptheodihnhtrnh.ui.feed.FeedActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*
import retrofit2.http.*

// --- MODELS ---
data class Point(val lat: Double, val lng: Double)
data class Checkpoint(val lat: Double, val lng: Double, val note: String, val imageUrl: String = "")
data class UploadResponse(val imageUrl: String)

data class JourneyRequest(
    val startTime: Date,
    val endTime: Date,
    val distance: Float,
    val points: List<Point>,
    val checkpoints: List<Checkpoint>
)

interface JourneyApiService {
    @POST("journey/save")
    fun saveJourney(@Header("x-auth-token") token: String, @Body request: JourneyRequest): Call<Void>

    @Multipart
    @POST("journey/upload")
    fun uploadImage(@Part image: MultipartBody.Part): Call<UploadResponse>
}

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var fabCheckpoint: FloatingActionButton
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    
    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null
    private val checkpointList = mutableListOf<Checkpoint>()

    private var totalDistance = 0f
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private var lastLatLng: LatLng? = null
    private var tempImageUrl: String = ""

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            setupInitialMapState()
            if (shouldStartAfterPermission) performStartTracking()
        }
    }
    private var shouldStartAfterPermission = false

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == RESULT_OK) res.data?.data?.let { handleImageSelection(it) }
    }

    private val trackingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val timeSec = intent?.getLongExtra("TIME_VALUE", TrackingService.timeInSeconds) ?: TrackingService.timeInSeconds
            updateTimeUI(timeSec)
            redrawFullRoute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)
        fabCheckpoint = findViewById(R.id.fabCheckpoint)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> true
                R.id.nav_feed -> { startActivity(Intent(this, FeedActivity::class.java)); false }
                R.id.nav_logout -> { logout(); false }
                else -> false
            }
        }

        btnStart.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                performStartTracking()
            } else {
                shouldStartAfterPermission = true
                requestLocationPermissions()
            }
        }

        btnStop.setOnClickListener {
            stopTrackingService()
            saveJourneyToServer()
            updateButtonStates(false)
        }

        fabCheckpoint.setOnClickListener { showCheckpointDialog() }
        
        if (TrackingService.isServiceRunning) {
            updateButtonStates(true)
            updateTimeUI(TrackingService.timeInSeconds)
        } else {
            updateTimeUI(0L)
        }
    }

    private fun requestLocationPermissions() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun performStartTracking() {
        startTrackingService()
        resetUIForNewTracking()
        updateButtonStates(true)
        shouldStartAfterPermission = false
    }

    private fun updateButtonStates(isRunning: Boolean) {
        btnStart.isEnabled = !isRunning
        btnStop.isEnabled = isRunning
        fabCheckpoint.visibility = if (isRunning) View.VISIBLE else View.GONE
    }

    private fun updateTimeUI(seconds: Long) {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        runOnUiThread {
            tvTime.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
        }
    }

    private fun redrawFullRoute() {
        if (!::mMap.isInitialized) return
        val points = TrackingService.pathPoints.toList()
        if (points.isEmpty()) return
        polyline?.remove()
        polyline = mMap.addPolyline(PolylineOptions().addAll(points).color(Color.BLUE).width(12f).geodesic(true))
        val currentPos = points.last()
        lastLatLng = currentPos
        if (currentMarker == null) {
            currentMarker = mMap.addMarker(MarkerOptions().position(currentPos).title("Bạn ở đây").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        } else {
            currentMarker?.position = currentPos
        }
        calculateDistance(points)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 17f))
    }

    private fun calculateDistance(points: List<LatLng>) {
        if (points.size < 2) return
        var dist = 0f
        for (i in 0 until points.size - 1) {
            val res = FloatArray(1)
            Location.distanceBetween(points[i].latitude, points[i].longitude, points[i+1].latitude, points[i+1].longitude, res)
            dist += res[0]
        }
        totalDistance = dist
        tvDistance.text = String.format(Locale.getDefault(), "%.2f km", totalDistance / 1000)
    }

    private fun logout() {
        getSharedPreferences("APP_DATA", Context.MODE_PRIVATE).edit().remove("TOKEN").apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showCheckpointDialog() {
        val b = AlertDialog.Builder(this)
        b.setTitle("Thêm Checkpoint")
        val input = EditText(this)
        input.hint = "Ghi chú..."
        b.setView(input)
        b.setNeutralButton("Ảnh") { _, _ -> selectImageLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
        b.setPositiveButton("Lưu") { _, _ ->
            val note = input.text.toString()
            lastLatLng?.let {
                checkpointList.add(Checkpoint(it.latitude, it.longitude, note, tempImageUrl))
                if (::mMap.isInitialized) mMap.addMarker(MarkerOptions().position(it).title(note).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                tempImageUrl = ""
            }
        }
        b.show()
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            uploadImage(file)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi xử lý ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImage(file: File) {
        val body = MultipartBody.Part.createFormData("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
        val apiService = RetrofitClient.createService(JourneyApiService::class.java)
        
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show()
        
        apiService.uploadImage(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful) {
                    tempImageUrl = response.body()?.imageUrl ?: ""
                    Toast.makeText(this@MapActivity, "Đã tải ảnh thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val map = Gson().fromJson(errorBody, Map::class.java)
                        map["message"]?.toString() ?: "Lỗi server"
                    } catch (e: Exception) {
                        "Lỗi server: ${response.code()}"
                    }
                    Toast.makeText(this@MapActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@MapActivity, "Lỗi kết nối: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveJourneyToServer() {
        val token = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE).getString("TOKEN", "") ?: ""
        val points = TrackingService.pathPoints.map { Point(it.latitude, it.longitude) }
        if (token.isEmpty() || points.isEmpty()) return
        val startTime = Date(System.currentTimeMillis() - (TrackingService.timeInSeconds * 1000))
        val endTime = Date()
        val req = JourneyRequest(startTime, endTime, totalDistance, points, checkpointList)
        val apiService = RetrofitClient.createService(JourneyApiService::class.java)
        apiService.saveJourney(token, req).enqueue(object : Callback<Void> {
            override fun onResponse(c: Call<Void>, r: Response<Void>) { if (r.isSuccessful) Toast.makeText(this@MapActivity, "Hành trình đã lưu!", Toast.LENGTH_SHORT).show() }
            override fun onFailure(c: Call<Void>, t: Throwable) {}
        })
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopTrackingService() = stopService(Intent(this, TrackingService::class.java))

    private fun resetUIForNewTracking() {
        if (::mMap.isInitialized) mMap.clear()
        checkpointList.clear()
        totalDistance = 0f
        polyline = null
        currentMarker = null
        tvDistance.text = "0.00 km"
        updateTimeUI(0L)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupInitialMapState()
    }

    private fun setupInitialMapState() {
        if (!::mMap.isInitialized) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            if (TrackingService.isServiceRunning) {
                redrawFullRoute()
                updateTimeUI(TrackingService.timeInSeconds)
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { loc -> 
                if (loc != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("TRACKING_UPDATE")
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.RECEIVER_NOT_EXPORTED else 0
        ContextCompat.registerReceiver(this, trackingReceiver, filter, flag)
        if (TrackingService.isServiceRunning) {
            redrawFullRoute()
            updateTimeUI(TrackingService.timeInSeconds)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(trackingReceiver) } catch (e: Exception) {}
    }
}
