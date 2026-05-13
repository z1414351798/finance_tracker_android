package com.z.financetracker

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.z.financetracker.interceptor.AuthInterceptor
import com.z.financetracker.util.TokenManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Application class — runs once at process start.
 *
 * By implementing [ImageLoaderFactory], every [coil.compose.AsyncImage] in the
 * app automatically uses this OkHttpClient, which attaches the JWT token and the
 * ngrok-skip header. Images are now loaded from authenticated backend endpoints
 * instead of public MinIO URLs.
 */
class FinanceTrackerApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val tokenManager = TokenManager(this)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .build()
                )
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
