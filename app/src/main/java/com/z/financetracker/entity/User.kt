package com.z.financetracker.entity

data class User(
    val id: Int? = null,
    val username: String = "",
    val password: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val presignedImageUrl: String? = null
)