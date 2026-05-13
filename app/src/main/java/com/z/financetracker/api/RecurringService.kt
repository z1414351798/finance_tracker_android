package com.z.financetracker.api

import com.z.financetracker.entity.RecurringTransaction
import retrofit2.Response
import retrofit2.http.*

interface RecurringService {

    @GET("api/recurring")
    suspend fun getAll(): Response<List<RecurringTransaction>>

    @GET("api/recurring/upcoming")
    suspend fun getUpcoming(
        @Query("days") days: Int = 7
    ): Response<List<RecurringTransaction>>

    @POST("api/recurring/add")
    suspend fun add(@Body r: RecurringTransaction): Response<RecurringTransaction>

    @PUT("api/recurring/update/{id}")
    suspend fun update(
        @Path("id") id: Int,
        @Body r: RecurringTransaction
    ): Response<Map<String, String>>

    @DELETE("api/recurring/delete/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Map<String, String>>

    @POST("api/recurring/{id}/pay")
    suspend fun pay(@Path("id") id: Int): Response<Map<String, String>>

    @GET("api/recurring/detect")
    suspend fun detect(): Response<List<Map<String, Any>>>
}
