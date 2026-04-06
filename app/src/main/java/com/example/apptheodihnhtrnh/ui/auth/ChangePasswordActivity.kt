package com.example.apptheodihnhtrnh.ui.auth

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apptheodihnhtrnh.R
import com.example.apptheodihnhtrnh.data.api.AuthApi
import com.example.apptheodihnhtrnh.data.api.ChangePasswordRequest
import com.example.apptheodihnhtrnh.data.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val edtOldPassword = findViewById<EditText>(R.id.edtOldPassword)
        val edtNewPassword = findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitChangePass)
        val btnBack = findViewById<ImageButton>(R.id.btnBackChangePass)

        btnBack.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val oldPass = edtOldPassword.text.toString()
            val newPass = edtNewPassword.text.toString()
            val confirmPass = edtConfirmPassword.text.toString()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "Mật khẩu mới không trùng khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Mật khẩu mới phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(oldPass, newPass)
        }
    }

    private fun changePassword(oldPass: String, newPass: String) {
        val sharedPref = getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        if (token.isEmpty()) return

        val request = ChangePasswordRequest(oldPass, newPass)
        RetrofitClient.authApi.changePassword(token, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Lỗi khi đổi mật khẩu"
                    Toast.makeText(this@ChangePasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@ChangePasswordActivity, "Lỗi kết nối: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
