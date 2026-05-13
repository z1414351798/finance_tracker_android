package com.z.financetracker.interceptor

import com.z.financetracker.util.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Presigned MinIO URLs carry X-Amz-* query params for auth.
        // Adding an Authorization header on top causes MinIO to return
        // SignatureDoesNotMatch — skip JWT injection for those requests.
        val isPresigned = request.url.queryParameter("X-Amz-Signature") != null
        if (isPresigned) return chain.proceed(request)

        val token = tokenManager.getToken()
        val requestBuilder = request.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}