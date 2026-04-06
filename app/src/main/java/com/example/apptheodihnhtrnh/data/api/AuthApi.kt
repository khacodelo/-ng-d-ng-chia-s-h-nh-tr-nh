package com.example.apptheodihnhtrnh.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String
)

interface AuthApi {
    @POST("auth/register")
    fun register(@Body request: AuthRequest): Call<Void>

    @POST("auth/login")
    fun login(@Body request: AuthRequest): Call<AuthResponse>
}
