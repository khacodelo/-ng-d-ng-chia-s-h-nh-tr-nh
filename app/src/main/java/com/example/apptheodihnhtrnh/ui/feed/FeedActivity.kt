package com.example.apptheodihnhtrnh.ui.feed

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apptheodihnhtrnh.R
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
        val gson = GsonBuilder()
            .setLenient()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.155:3000/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val apiService = retrofit.create(FeedApiService::class.java)
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
                    Toast.makeText(this@FeedActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<JourneyData>>, t: Throwable) {
                Toast.makeText(this@FeedActivity, "Lỗi kết nối: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
