package com.z.financetracker.api

import com.z.financetracker.entity.AuthResponse
import com.z.financetracker.entity.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("api/auth/signup")
    suspend fun signup(@Body user: User): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body user: User): Response<AuthResponse>
}