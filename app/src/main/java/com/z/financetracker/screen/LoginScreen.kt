package com.z.financetracker.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import com.z.financetracker.component.FinanceInput
import androidx.compose.ui.text.TextStyle
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.entity.User
import com.z.financetracker.util.TokenManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope() // The "Worker" for async tasks
    val context = LocalContext.current // This gets the Context automatically

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            // Ensure you aren't putting parentheses () after FontWeight.Black
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        FinanceInput(value = username, onValueChange = { username = it }, label = "Username")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FinanceInput(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        // Notice the () after getAuthApi
                        val response = NetworkClient.getAuthApi(context).login(
                            User(username = username, password = password)
                        )

                        if (response.isSuccessful) {
                            val body = response.body()
                            val token = body?.token // This is now a String?

                            if (token != null) {
                                val tokenManager = TokenManager(context)
                                tokenManager.saveToken(token)
                                onLoginSuccess()
                            }
                        } else {
                            // Tip: Use a Toast to show error
                            android.widget.Toast.makeText(context, "Login Failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Network Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("LOGIN")
        }

        TextButton(onClick = onNavigateToSignup) {
            Text("Don't have an account? Sign Up", color = Color.Gray)
        }
    }
}