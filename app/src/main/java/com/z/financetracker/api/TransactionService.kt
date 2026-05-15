package com.z.financetracker.api

import com.z.financetracker.entity.DailyTrend
import com.z.financetracker.entity.Transaction
import com.z.financetracker.enums.TraType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
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
    ): Response<Map<String, String>>

    @DELETE("api/transactions/delete/{id}")
    suspend fun deleteTransaction(@Path("id") id: Int): Response<Map<String, String>>

    @Multipart
    @POST("api/transactions/{id}/image")
    suspend fun uploadTransactionImage(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<Map<String, String>>

    @DELETE("api/transactions/{id}/image")
    suspend fun deleteTransactionImage(
        @Path("id") id: Int
    ): Response<Map<String, String>>

    @GET("api/transactions/{id}/image-url")
    suspend fun getTransactionImageUrl(
        @Path("id") id: Int
    ): Response<String>

    @GET("api/transactions/monthly-breakdown")
    suspend fun getMonthlyBreakdown(
        @Query("months") months: Int = 6
    ): Response<List<MonthlyBreakdown>>

    @GET("api/transactions/biggest-transactions")
    suspend fun getBiggestTransactions(
        @Query("limit") limit: Int = 5
    ): Response<List<BigTransaction>>

    @GET("api/transactions/export/csv")
    @Streaming
    suspend fun exportCsv(): Response<ResponseBody>
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

data class MonthlyBreakdown(
    val month: String,              // "2025-04"
    val income: Double,
    val expense: Double,            // stored as negative in DB → will be negative here
    val transactionCount: Long
)

data class BigTransaction(
    val text: String,
    val amount: Double,             // negative (expense)
    val date: String,
    val categoryName: String?
)