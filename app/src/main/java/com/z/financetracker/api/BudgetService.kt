package com.z.financetracker.api

import com.z.financetracker.entity.Budget
import com.z.financetracker.entity.BudgetProgress
import retrofit2.Response
import retrofit2.http.*

interface BudgetService {
    @GET("api/budgets/progress")
    suspend fun getBudgetProgress(@Query("month") month: String): List<BudgetProgress>

    @GET("api/budgets")
    suspend fun getBudgets(@Query("month") month: String): List<Budget>

    @POST("api/budgets")
    suspend fun createBudget(@Body budget: Budget): Response<Budget>

    @PUT("api/budgets/{id}")
    suspend fun updateBudget(@Path("id") id: Long, @Body budget: Budget): Response<String>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Long): Response<String>
}