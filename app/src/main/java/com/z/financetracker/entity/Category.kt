package com.z.financetracker.entity

import com.z.financetracker.enums.TraType

data class Category(
    val id: Long,
    val name: String,
    val type: TraType
)