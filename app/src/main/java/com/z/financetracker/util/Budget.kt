package com.z.financetracker.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Budget(
    val categoryName: String,
    val limit: Double,
    val type: String = "EXPENSE"
)

class BudgetManager(context: Context) {
    private val prefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveBudgets(budgets: List<Budget>) {
        prefs.edit().putString("budget_list", gson.toJson(budgets)).apply()
    }

    fun getBudgets(): List<Budget> {
        val json = prefs.getString("budget_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Budget>>() {}.type
        return gson.fromJson(json, type)
    }
}