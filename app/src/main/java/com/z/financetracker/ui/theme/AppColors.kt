package com.z.financetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object AppColors {
    val incomeBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isSystemInDarkTheme()) Color(0xFF052E16) else Color(0xFFD1FAE5)

    val expensBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isSystemInDarkTheme()) Color(0xFF3B0A0A) else Color(0xFFFEE2E2)

    val incomeText: Color
        @Composable @ReadOnlyComposable get() =
            if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF059669)

    val expenseText: Color
        @Composable @ReadOnlyComposable get() =
            if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFDC2626)

    val warningBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isSystemInDarkTheme()) Color(0xFF422006) else Color(0xFFFFFBEB)

    val errorBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isSystemInDarkTheme()) Color(0xFF3B0A0A) else Color(0xFFFEF2F2)
}
