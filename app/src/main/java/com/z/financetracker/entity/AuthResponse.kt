package com.z.financetracker.entity

data class AuthResponse(
    // Ensure these are explicitly String?
    val token: String? = null,
    val message: String? = null
)