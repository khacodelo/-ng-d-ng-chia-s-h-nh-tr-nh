package com.example.apptheodihnhtrnh.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apptheodihnhtrnh.MapActivity
import com.example.apptheodihnhtrnh.R
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// --- MODELS ---
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("message") val message: String?
)

// --- API SERVICE ---
interface AuthApiService {
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEdit = findViewById<EditText>(R.id.edtEmail)
        val passwordEdit = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(AuthApiService::class.java)

        btnLogin.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(email, password)
            apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.token != null) {
                            // LƯU TOKEN (Quan trọng cho các Module sau)
                            val sharedPref = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
                            sharedPref.edit().putString("TOKEN", body.token).apply()

                            Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MapActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Lỗi: Server không trả về Token", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Đọc lỗi chi tiết từ Server
                        val errorBody = response.errorBody()?.string()
                        Log.e("LOGIN_ERROR", "Error body: $errorBody")
                        
                        val errorMessage = try {
                            val errorResponse = Gson().fromJson(errorBody, LoginResponse::class.java)
                            errorResponse.message ?: "Sai email hoặc mật khẩu"
                        } catch (e: Exception) {
                            "Lỗi hệ thống: ${response.code()}"
                        }
                        
                        Toast.makeText(this@LoginActivity, "Đăng nhập thất bại: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("LOGIN_FAILURE", t.message ?: "Unknown error")
                    Toast.makeText(this@LoginActivity, "Lỗi kết nối Server: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
