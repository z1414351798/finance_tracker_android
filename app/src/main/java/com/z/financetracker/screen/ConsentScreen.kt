package com.z.financetracker.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z.financetracker.R

@Composable
fun ConsentScreen(onAccepted: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val hasAccepted = remember { prefs.getBoolean("privacy_accepted", false) }

    if (hasAccepted) {
        LaunchedEffect(Unit) { onAccepted() }
        return
    }

    val uriHandler = LocalUriHandler.current
    var isChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val indigoColor = Color(0xFF4F46E5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with indigo background circle
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(indigoColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        // Title
        Text(
            text = stringResource(R.string.before_you_begin),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // Subtitle
        Text(
            text = stringResource(R.string.privacy_subtitle),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Feature rows
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                PrivacyFeatureRow(
                    icon = Icons.Default.Lock,
                    iconTint = indigoColor,
                    text = stringResource(R.string.privacy_point_1)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                PrivacyFeatureRow(
                    icon = Icons.Default.VisibilityOff,
                    iconTint = indigoColor,
                    text = stringResource(R.string.privacy_point_2)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                PrivacyFeatureRow(
                    icon = Icons.Default.Delete,
                    iconTint = indigoColor,
                    text = stringResource(R.string.privacy_point_3)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                PrivacyFeatureRow(
                    icon = Icons.Default.Security,
                    iconTint = indigoColor,
                    text = stringResource(R.string.privacy_point_4)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Privacy policy and terms links
        val privacyText = stringResource(R.string.privacy_policy)
        val termsText = stringResource(R.string.terms_of_service)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = {
                uriHandler.openUri("https://www.wisefintrakr.com/privacy")
            }) {
                Text(
                    text = privacyText,
                    color = indigoColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
            }

            Text(
                text = "  |  ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            TextButton(onClick = {
                uriHandler.openUri("https://www.wisefintrakr.com/terms")
            }) {
                Text(
                    text = termsText,
                    color = indigoColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = indigoColor,
                    uncheckedColor = Color(0xFF9CA3AF)
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.privacy_agree_checkbox),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Continue button
        Button(
            onClick = {
                prefs.edit().putBoolean("privacy_accepted", true).apply()
                onAccepted()
            },
            enabled = isChecked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = indigoColor,
                disabledContainerColor = Color(0xFFC7D2FE)
            )
        ) {
            Text(
                text = stringResource(R.string.continue_btn),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PrivacyFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
