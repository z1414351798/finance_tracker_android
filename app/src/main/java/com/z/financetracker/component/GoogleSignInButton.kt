package com.z.financetracker.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.util.TokenManager
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in context chain")
}

@Composable
fun GoogleSignInButton(
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Web Client ID from Google Console → Credentials → OAuth client ID (type: Web application)
    val webClientId = "180806298382-q7ofrc16oqep2k7jsd5budl10o1qkqn6.apps.googleusercontent.com"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading) {
                scope.launch {
                    isLoading = true
                    try {
                        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(signInOption)
                            .build()
                        val credentialManager = CredentialManager.create(context)
                        val result = credentialManager.getCredential(
                            context = context.findActivity(),
                            request = request
                        )
                        val credential = result.credential
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            val resp = NetworkClient.getAuthApi(context)
                                .googleLogin(mapOf("idToken" to idToken))
                            if (resp.isSuccessful) {
                                val token = resp.body()?.token
                                if (token != null) {
                                    TokenManager(context).saveToken(token)
                                    onSuccess()
                                } else {
                                    onError("No token received from server")
                                }
                            } else {
                                onError("Server login failed: ${resp.code()}")
                            }
                        } else {
                            onError("Unexpected credential type: ${credential.type}")
                        }
                    } catch (e: GetCredentialException) {
                        onError("Google sign-in failed: ${e.type} — ${e.message}")
                    } catch (e: Exception) {
                        onError("Error: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "G",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
                Text(
                    "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
