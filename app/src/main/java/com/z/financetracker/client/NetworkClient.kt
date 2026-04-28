package com.z.financetracker.client

import com.z.financetracker.BuildConfig
import android.content.Context
import com.z.financetracker.api.*
import com.z.financetracker.interceptor.AuthInterceptor
import com.z.financetracker.util.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    // Regular client — 10s timeout (default)
    private fun getRetrofit(context: Context): Retrofit {
        val tokenManager = TokenManager(context)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .connectTimeout(600, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(600, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // AI client — 3 minute timeout because Llama 3 is slow
    private fun getAiRetrofit(context: Context): Retrofit {
        val tokenManager = TokenManager(context)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)   // 3 min read timeout
            .writeTimeout(300, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getCategoryApi(context: Context): CategoryService =
        getRetrofit(context).create(CategoryService::class.java)

    fun getTransactionApi(context: Context): TransactionService =
        getRetrofit(context).create(TransactionService::class.java)

    fun getAuthApi(context: Context): AuthService =
        getRetrofit(context).create(AuthService::class.java)

    fun getBudgetApi(context: Context): BudgetService =
        getRetrofit(context).create(BudgetService::class.java)

    fun getProfileApi(context: Context): ProfileService =
        getRetrofit(context).create(ProfileService::class.java)

    fun getGoalApi(context: Context): GoalService =
        getRetrofit(context).create(GoalService::class.java)

    // ← Uses the slow-tolerant client
    fun getAiApi(context: Context): AiService =
        getAiRetrofit(context).create(AiService::class.java)
}