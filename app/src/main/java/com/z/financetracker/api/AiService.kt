package com.z.financetracker.api

import com.z.financetracker.entity.AiChatRequest
import retrofit2.Response
import retrofit2.http.*

interface AiService {

    @POST("api/ai/chat")
    suspend fun chat(@Body body: AiChatRequest): Response<Map<String, String>>

    @GET("api/ai/summary")
    suspend fun getSummary(): Response<Map<String, String>>
}