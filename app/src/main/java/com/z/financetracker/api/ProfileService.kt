package com.z.financetracker.api

import com.z.financetracker.entity.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProfileService {

    @GET("api/profile")
    suspend fun getProfile(): Response<User>

    @PUT("api/profile")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<User>

    @PUT("api/profile/password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<Map<String, String>>

    @Multipart
    @POST("api/profile/avatar")
    suspend fun uploadAvatar(
        @Part image: MultipartBody.Part
    ): Response<Map<String, String>>

    @DELETE("api/profile/account")
    suspend fun deleteAccount(): Response<Map<String, String>>
}