package com.z.financetracker.entity

data class User(
    val id: Int? = null,
    val username: String = "",
    val password: String = "",
    val email: String? = null,
    val profileImageUrl: String? = null
)