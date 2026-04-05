package com.example.apptheodihnhtrnh.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apptheodihnhtrnh.R
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// --- MODELS ---
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterResponse(
    @SerializedName("message") val message: String?
)

// --- API SERVICE ---
interface RegisterApiService {
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>
}

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailEdit = findViewById<EditText>(R.id.edtEmailRegister)
        val passwordEdit = findViewById<EditText>(R.id.edtPasswordRegister)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.155:3000/") 
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(RegisterApiService::class.java)

        btnRegister.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(email, password)

            apiService.register(request).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                        finish() 
                    } else {
                        val error = response.errorBody()?.string() ?: "Đăng ký thất bại"
                        Toast.makeText(this@RegisterActivity, "Lỗi: $error", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Lỗi kết nối: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
