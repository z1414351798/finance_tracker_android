package com.z.financetracker.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.FinanceInput
import com.z.financetracker.entity.User
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope() // The "Worker" for async tasks
    val context = LocalContext.current // Add this line

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CREATE ACCOUNT", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black))

        Spacer(modifier = Modifier.height(32.dp))

        FinanceInput(value = username, onValueChange = { username = it }, label = "Username")
        Spacer(modifier = Modifier.height(16.dp))
        FinanceInput(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
        Spacer(modifier = Modifier.height(16.dp))
        FinanceInput(value = retypePassword, onValueChange = { retypePassword = it }, label = "Re-type Password", isPassword = true)

        Spacer(modifier = Modifier.height(32.dp))

        // FIXED BUTTON: High padding and specific height to prevent cutoff
        Button(
            onClick = {
                if (password == retypePassword && username.isNotEmpty()) {
                    scope.launch {
                        try {
                            // Change 'authApi' to 'getAuthApi(context)'
                            val response = NetworkClient.getAuthApi(context).signup(
                                User(username = username, password = password)
                            )

                            if (response.isSuccessful) {
                                onSignupSuccess()
                            } else {
                                // Show error toast
                            }
                        } catch (e: Exception) {
                            // Show network error toast
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                "CREATE ACCOUNT",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                maxLines = 1 // Ensures it doesn't wrap and hide
            )
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("Back to Login", color = Color.Gray)
        }
    }
}