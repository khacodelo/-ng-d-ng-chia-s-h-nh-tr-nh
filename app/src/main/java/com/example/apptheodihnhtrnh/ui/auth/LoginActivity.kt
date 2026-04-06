package com.example.apptheodihnhtrnh.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apptheodihnhtrnh.MapActivity
import com.example.apptheodihnhtrnh.R
import com.example.apptheodihnhtrnh.data.api.AuthApi
import com.example.apptheodihnhtrnh.data.api.AuthRequest
import com.example.apptheodihnhtrnh.data.api.AuthResponse
import com.example.apptheodihnhtrnh.data.api.RetrofitClient
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("TOKEN", null)
        
        if (savedToken != null) {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val emailEdit = findViewById<EditText>(R.id.edtEmail)
        val passwordEdit = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = AuthRequest(email, password)
            RetrofitClient.authApi.login(loginRequest).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.token != null) {
                            sharedPref.edit().putString("TOKEN", body.token).apply()
                            Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MapActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Lỗi: Server không trả về Token", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@LoginActivity, "Đăng nhập thất bại: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Lỗi kết nối Server: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
