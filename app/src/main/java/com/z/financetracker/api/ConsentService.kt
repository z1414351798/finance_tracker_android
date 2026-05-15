package com.z.financetracker.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ConsentRequest(
    val platform: String,
    val policyVersion: String
)

interface ConsentService {
    @POST("api/consent")
    suspend fun recordConsent(@Body request: ConsentRequest): Response<Unit>
}
