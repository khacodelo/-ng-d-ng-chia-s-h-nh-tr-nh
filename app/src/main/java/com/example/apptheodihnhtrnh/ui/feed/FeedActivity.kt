package com.example.apptheodihnhtrnh.ui.feed

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apptheodihnhtrnh.R
import com.example.apptheodihnhtrnh.data.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET

interface FeedApiService {
    @GET("journey/all")
    fun getAllJourneys(): Call<List<JourneyData>>
}

class FeedActivity : AppCompatActivity() {

    private lateinit var rvFeed: RecyclerView
    private var adapter: JourneyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        rvFeed = findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(this)

        loadJourneys()
    }

    private fun loadJourneys() {
        val apiService = RetrofitClient.createService(FeedApiService::class.java)
        apiService.getAllJourneys().enqueue(object : Callback<List<JourneyData>> {
            override fun onResponse(call: Call<List<JourneyData>>, response: Response<List<JourneyData>>) {
                if (response.isSuccessful) {
                    val journeys = response.body() ?: emptyList()
                    if (journeys.isEmpty()) {
                        Toast.makeText(this@FeedActivity, "Chưa có hành trình nào", Toast.LENGTH_SHORT).show()
                    }
                    adapter = JourneyAdapter(journeys)
                    rvFeed.adapter = adapter
                } else {
                    Log.e("FEED_ERROR", "Code: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@FeedActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<JourneyData>>, t: Throwable) {
                Log.e("FEED_FAILURE", "Error: ${t.message}")
                Toast.makeText(this@FeedActivity, "Lỗi kết nối: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
