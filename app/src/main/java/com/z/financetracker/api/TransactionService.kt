package com.z.financetracker.api

import com.z.financetracker.entity.DailyTrend
import com.z.financetracker.entity.Transaction
import com.z.financetracker.enums.TraType
import retrofit2.Response
import retrofit2.http.*

interface TransactionService {

    @POST("api/transactions/add")
    suspend fun addTransaction(@Body transaction: Transaction): Response<Transaction>

    @GET("api/transactions/history")
    suspend fun getHistory(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("text") text: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("type") type: TraType? = null,
        @Query("minAmount") minAmount: Double? = null,
        @Query("maxAmount") maxAmount: Double? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("note") note: String? = null
    ): Response<HistoryResponse>

    @GET("api/transactions/summary")
    suspend fun getSummary(): Response<SummaryResponse>

    @GET("api/transactions/trends")
    suspend fun getTrends(
        @Query("range") range: String, // DAY, MONTH, YEAR
        @Query("type") type: TraType
    ): Response<List<TrendPoint>>

    @GET("api/transactions/daily-trends")
    suspend fun getDailyTrends(
        @Query("days") days: Int = 30
    ): Response<List<DailyTrend>>

    @PUT("api/transactions/update/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Int,
        @Body transaction: Transaction
    ): Response<String>
}

// Response models
data class HistoryResponse(
    val content: List<Transaction>,
    val totalElements: Long
)

data class SummaryResponse(
    val cashFlow: CashFlow,
    val incomeCategories: List<CategorySummary>,
    val expenseCategories: List<CategorySummary>
)

data class CashFlow(
    val totalIncome: Double,
    val totalExpense: Double
)

data class CategorySummary(
    val name: String,
    val value: Double
)

data class TrendPoint(
    val timeLabel: String,
    val total: Double
)