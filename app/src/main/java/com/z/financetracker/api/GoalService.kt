package com.z.financetracker.api

import com.z.financetracker.entity.SavingsGoal
import retrofit2.Response
import retrofit2.http.*

interface GoalService {

    @GET("api/goals")
    suspend fun getGoals(): Response<List<SavingsGoal>>

    @POST("api/goals")
    suspend fun createGoal(@Body goal: SavingsGoal): Response<SavingsGoal>

    @PUT("api/goals/{id}")
    suspend fun updateGoal(@Path("id") id: Long, @Body goal: SavingsGoal): Response<SavingsGoal>

    @POST("api/goals/{id}/contribute")
    suspend fun contribute(
        @Path("id") id: Long,
        @Body body: Map<String, Double>
    ): Response<SavingsGoal>

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Long): Response<Unit>
}