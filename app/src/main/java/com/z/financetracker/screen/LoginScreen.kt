package com.z.financetracker.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.z.financetracker.api.ConsentRequest
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.GoogleSignInButton
import com.z.financetracker.entity.User
import com.z.financetracker.util.TokenManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // Floating orb animation
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 18f, label = "orb",
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Background ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0C29),
                            Color(0xFF1a1363),
                            Color(0xFF24243e)
                        )
                    )
                )
        )

        // ── Decorative Orbs ────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-40 + orbOffset).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x402563EB), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (80 - orbOffset).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x308B5CF6), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = (orbOffset).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x2510B981), Color.Transparent)),
                    CircleShape
                )
        )

        // ── Content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Welcome Back",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Sign in to your account",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(40.dp))

            // ── Glass Card ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    // Username field
                    GlassTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        leadingIcon = Icons.Rounded.Person
                    )

                    Spacer(Modifier.height(16.dp))

                    // Password field
                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )

                    Spacer(Modifier.height(28.dp))

                    // Login button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isLoading) {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val response = NetworkClient.getAuthApi(context).login(
                                            User(username = username, password = password)
                                        )
                                        if (response.isSuccessful) {
                                            val token = response.body()?.token
                                            if (token != null) {
                                                TokenManager(context).saveToken(token)
                                                try {
                                                    NetworkClient.getConsentApi(context).recordConsent(
                                                        ConsentRequest(platform = "android", policyVersion = "2026-05")
                                                    )
                                                } catch (_: Exception) { }
                                                onLoginSuccess()
                                            }
                                        } else {
                                            snackbar.showSnackbar("Invalid username or password")
                                        }
                                    } catch (e: Exception) {
                                        snackbar.showSnackbar("Network error: ${e.message}")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // OR divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.15f)
                )
                Text(
                    "  or continue with  ",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.15f)
                )
            }

            Spacer(Modifier.height(16.dp))

            GoogleSignInButton(
                onSuccess = { onLoginSuccess() },
                onError = { error -> scope.launch { snackbar.showSnackbar(error) } }
            )

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Don't have an account? ",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Text(
                    "Sign Up",
                    color = Color(0xFF60A5FA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToSignup() }
                )
            }

            Spacer(Modifier.height(40.dp))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
public fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null
) {
    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                cursorColor = Color(0xFF2563EB),
                focusedLeadingIconColor = Color(0xFF60A5FA),
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.4f),
                focusedTrailingIconColor = Color(0xFF60A5FA),
                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.4f)
            ),
            leadingIcon = { Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onPasswordToggle?.invoke() }) {
                        Icon(
                            if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
            ),
            singleLine = true
        )
    }
}
