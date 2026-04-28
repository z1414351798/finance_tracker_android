package com.z.financetracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.z.financetracker.screen.DashboardScreen
import com.z.financetracker.screen.LoginScreen
import com.z.financetracker.screen.SignupScreen
import com.z.financetracker.ui.theme.FinanceTrackerTheme
import com.z.financetracker.util.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceTrackerTheme {
                // Call your navigation controller here instead of 'Greeting'
                FinanceAppNavigation(context = this)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FinanceTrackerTheme {
        Greeting("Android")
    }
}

@Composable
fun FinanceAppNavigation(context: Context) {
    val navController = rememberNavController()
    val tokenManager = TokenManager(context)

    // Check if we have a token
    val startDest = if (tokenManager.getToken() != null) "dashboard" else "login"

    // Use startDest here instead of hardcoding "login"
    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                onNavigateToSignup = { navController.navigate("signup") },
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = { navController.navigate("login") }
            )
        }
        composable("dashboard") {
            DashboardScreen(onLogout = {
                navController.navigate("login") {
                    // This clears the dashboard from the history
                    popUpTo("dashboard") { inclusive = true }
                }
            })
        }
    }
}