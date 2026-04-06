package com.example.apptheodihnhtrnh.ui.auth

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apptheodihnhtrnh.R
import com.example.apptheodihnhtrnh.data.api.RetrofitClient
import com.example.apptheodihnhtrnh.data.api.UserProfile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtDob: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtDisplayName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        edtFullName = findViewById(R.id.edtFullName)
        edtDob = findViewById(R.id.edtDob)
        edtPhone = findViewById(R.id.edtPhone)
        edtDisplayName = findViewById(R.id.edtDisplayName)
        btnSave = findViewById(R.id.btnSaveProfile)
        btnBack = findViewById(R.id.btnBackProfile)

        btnBack.setOnClickListener { finish() }

        loadProfileData()

        btnSave.setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadProfileData() {
        val sharedPref = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        if (token.isEmpty()) return

        RetrofitClient.authApi.getProfile(token).enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful) {
                    val profile = response.body()
                    edtFullName.setText(profile?.fullName)
                    edtDob.setText(profile?.dob)
                    edtPhone.setText(profile?.phone)
                    edtDisplayName.setText(profile?.displayName)
                } else {
                    Toast.makeText(this@ProfileActivity, "Không thể tải thông tin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfileData() {
        val sharedPref = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        val profile = UserProfile(
            email = null, // Email không cho phép sửa ở đây
            fullName = edtFullName.text.toString().trim(),
            dob = edtDob.text.toString().trim(),
            phone = edtPhone.text.toString().trim(),
            displayName = edtDisplayName.text.toString().trim()
        )

        RetrofitClient.authApi.updateProfile(token, profile).enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Đã lưu thông tin", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ProfileActivity, "Lỗi khi lưu thông tin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
