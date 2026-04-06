package com.example.apptheodihnhtrnh.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.example.apptheodihnhtrnh.R
import com.google.android.gms.maps.model.LatLng

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val handler = Handler(Looper.getMainLooper())
    private var timeRunnable: Runnable? = null
    
    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1
        var isServiceRunning = false
        var timeInSeconds = 0L 
        val pathPoints = mutableListOf<LatLng>()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            pathPoints.clear()
            timeInSeconds = 0L
            
            val notification = createNotification("Đang ghi nhận hành trình và thời gian...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            
            startLocationUpdates()
            startTimeCounter()
        }
        return START_STICKY
    }

    // --- TÍNH THỜI GIAN HÀNH TRÌNH ---
    private fun startTimeCounter() {
        timeRunnable = object : Runnable {
            override fun run() {
                if (isServiceRunning) {
                    timeInSeconds++
                    val intent = Intent("TRACKING_UPDATE")
                    intent.putExtra("TIME_VALUE", timeInSeconds)
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        handler.postDelayed(timeRunnable!!, 1000L)
    }

    // --- TỐI ƯU GPS DƯỚI NỀN VÀ PIN ---
    private fun startLocationUpdates() {
        // Cấu hình: Ưu tiên chính xác cao, cập nhật mỗi 3-5 giây, 
        // và CHỈ cập nhật nếu di chuyển trên 2 mét để tiết kiệm pin.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(2f) 
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val newPoint = LatLng(location.latitude, location.longitude)
                
                synchronized(pathPoints) {
                    pathPoints.add(newPoint)
                }

                val intent = Intent("TRACKING_UPDATE")
                intent.setPackage(packageName) 
                sendBroadcast(intent)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            isServiceRunning = false
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hành Trình Đang Được Ghi Lại")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Tracking Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        timeRunnable?.let { handler.removeCallbacks(it) }
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
