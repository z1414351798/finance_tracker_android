package com.z.financetracker.api

import com.z.financetracker.entity.Category
import com.z.financetracker.enums.TraType
import retrofit2.Response
import retrofit2.http.*

interface CategoryService {
    @GET("api/categories")
    suspend fun getCategories(@Query("type") type: TraType): List<Category>

    @POST("api/categories")
    suspend fun addCategory(@Body category: Category): Response<Category>
}