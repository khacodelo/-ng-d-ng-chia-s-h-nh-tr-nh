package com.example.apptheodihnhtrnh.ui.history

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.apptheodihnhtrnh.R
import com.example.apptheodihnhtrnh.ui.feed.CheckpointInfo
import com.example.apptheodihnhtrnh.ui.feed.PointData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var points: List<PointData> = emptyList()
    private var checkpoints: List<CheckpointInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_map)

        // Nhận dữ liệu từ Intent
        val pointsJson = intent.getStringExtra("POINTS_JSON")
        val checkpointsJson = intent.getStringExtra("CHECKPOINTS_JSON")

        val gson = Gson()
        if (!pointsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<PointData>>() {}.type
            points = gson.fromJson(pointsJson, type)
        }
        if (!checkpointsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<CheckpointInfo>>() {}.type
            checkpoints = gson.fromJson(checkpointsJson, type)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.historyMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (points.isNotEmpty()) {
            val latLngList = points.map { LatLng(it.lat, it.lng) }
            
            // 1. Vẽ đường hành trình
            mMap.addPolyline(PolylineOptions()
                .addAll(latLngList)
                .color(Color.RED)
                .width(12f)
                .geodesic(true))

            // 2. Hiển thị Checkpoints (nếu có)
            checkpoints.forEach { cp ->
                if (cp.lat != null && cp.lng != null) {
                    mMap.addMarker(MarkerOptions()
                        .position(LatLng(cp.lat, cp.lng))
                        .title(cp.note ?: "Checkpoint")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                }
            }

            // 3. Đánh dấu điểm Đầu và điểm Cuối
            mMap.addMarker(MarkerOptions().position(latLngList.first()).title("Bắt đầu").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
            mMap.addMarker(MarkerOptions().position(latLngList.last()).title("Kết thúc").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

            // 4. Di chuyển camera bao quát toàn bộ hành trình
            val boundsBuilder = LatLngBounds.Builder()
            latLngList.forEach { boundsBuilder.include(it) }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }
    }
}
