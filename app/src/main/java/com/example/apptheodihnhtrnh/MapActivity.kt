package com.example.apptheodihnhtrnh

import android.Manifest
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
import com.example.apptheodihnhtrnh.services.TrackingService
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.*

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
    
    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null
    private val pathPoints = mutableListOf<LatLng>()
    private val checkpointList = mutableListOf<Checkpoint>()

    private var totalDistance = 0f
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private var lastLatLng: LatLng? = null
    private var tempImageUrl: String = ""

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uploadImage(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)
        fabCheckpoint = findViewById(R.id.fabCheckpoint)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                startTrackingService()
                btnStart.isEnabled = false
                btnStop.isEnabled = true
                fabCheckpoint.visibility = View.VISIBLE
                resetUI()
            }
        }

        btnStop.setOnClickListener {
            stopTrackingService()
            saveJourneyToServer()
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            fabCheckpoint.visibility = View.GONE
        }

        fabCheckpoint.setOnClickListener {
            showCheckpointDialog()
        }
    }

    private fun showCheckpointDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Thêm Checkpoint + Ảnh")
        val input = EditText(this)
        input.hint = "Nhập ghi chú tại đây..."
        builder.setView(input)

        builder.setNeutralButton("Chọn Ảnh") { _, _ ->
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectImageLauncher.launch(intent)
        }

        builder.setPositiveButton("Lưu Checkpoint") { _, _ ->
            val note = input.text.toString()
            lastLatLng?.let {
                checkpointList.add(Checkpoint(it.latitude, it.longitude, note, tempImageUrl))
                mMap.addMarker(MarkerOptions().position(it).title(note).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                tempImageUrl = "" // Reset sau khi lưu
            }
        }
        builder.setNegativeButton("Hủy", null)
        builder.show()
    }

    private fun uploadImage(uri: Uri) {
        val file = File(getPathFromUri(uri))
        if (!file.exists()) {
            Toast.makeText(this, "Không tìm thấy file ảnh", Toast.LENGTH_SHORT).show()
            return
        }
        
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        val retrofit = Retrofit.Builder().baseUrl("http://10.0.2.2:3000/").addConverterFactory(GsonConverterFactory.create()).build()
        val apiService = retrofit.create(JourneyApiService::class.java)

        apiService.uploadImage(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful) {
                    tempImageUrl = response.body()?.imageUrl ?: ""
                    Toast.makeText(this@MapActivity, "Ảnh đã được tải lên!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MapActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@MapActivity, "Lỗi kết nối upload", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getPathFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) ?: 0
        cursor?.moveToFirst()
        val path = cursor?.getString(columnIndex) ?: ""
        cursor?.close()
        return path
    }

    private fun saveJourneyToServer() {
        val token = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE).getString("TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val request = JourneyRequest(Date(), Date(), totalDistance, pathPoints.map { Point(it.latitude, it.longitude) }, checkpointList)
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create()
        val retrofit = Retrofit.Builder().baseUrl("http://10.0.2.2:3000/").addConverterFactory(GsonConverterFactory.create(gson)).build()
        val apiService = retrofit.create(JourneyApiService::class.java)

        apiService.saveJourney(token, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) Toast.makeText(this@MapActivity, "Hành trình và Ảnh đã lưu thành công!", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("SAVE_ERROR", t.message ?: "")
            }
        })
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopTrackingService() = stopService(Intent(this, TrackingService::class.java))

    private fun updateTrackingData(newPoint: LatLng) {
        if (pathPoints.isNotEmpty()) {
            val results = FloatArray(1)
            Location.distanceBetween(pathPoints.last().latitude, pathPoints.last().longitude, newPoint.latitude, newPoint.longitude, results)
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
            currentMarker = mMap.addMarker(MarkerOptions().position(newPoint).title("Bạn ở đây"))
        } else currentMarker?.position = newPoint
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 17f))
    }

    private fun updateStatsUI() {
        tvDistance.text = String.format(Locale.getDefault(), "%.2f km", totalDistance / 1000)
    }

    private fun resetUI() {
        mMap.clear()
        pathPoints.clear()
        checkpointList.clear()
        totalDistance = 0f
        polyline = null
        currentMarker = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (checkPermissions()) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc -> loc?.let { lastLatLng = LatLng(it.latitude, it.longitude); mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng!!, 15f)) } }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val listToRequest = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (listToRequest.isNotEmpty()) { ActivityCompat.requestPermissions(this, listToRequest.toTypedArray(), 1); return false }
        return true
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("TRACKING_UPDATE")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val lat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
                val lng = intent?.getDoubleExtra("lng", 0.0) ?: 0.0
                lastLatLng = LatLng(lat, lng)
                updateTrackingData(lastLatLng!!)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }
}
