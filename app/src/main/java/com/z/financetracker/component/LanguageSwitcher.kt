package com.z.financetracker.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.z.financetracker.util.LocaleManager

@Composable
fun LanguageSwitcher(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentLang by remember {
        mutableStateOf(LocaleManager.getLanguage(context))
    }

    Row(
        modifier = modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        LanguageTab(
            label = "EN",
            selected = currentLang == LocaleManager.LANG_ENGLISH,
            onClick = {
                if (currentLang != LocaleManager.LANG_ENGLISH) {
                    currentLang = LocaleManager.LANG_ENGLISH
                    // AppCompatDelegate automatically recreates activity
                    LocaleManager.setLocale(context, LocaleManager.LANG_ENGLISH)
                }
            }
        )
        LanguageTab(
            label = "中文",
            selected = currentLang == LocaleManager.LANG_CHINESE,
            onClick = {
                if (currentLang != LocaleManager.LANG_CHINESE) {
                    currentLang = LocaleManager.LANG_CHINESE
                    LocaleManager.setLocale(context, LocaleManager.LANG_CHINESE)
                }
            }
        )
    }
}

@Composable
private fun LanguageTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Color.White else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF6B7280)
        )
    }
}