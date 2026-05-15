package com.z.financetracker

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.z.financetracker.screen.ConsentScreen
import com.z.financetracker.screen.DashboardScreen
import com.z.financetracker.screen.LoginScreen
import com.z.financetracker.screen.SignupScreen
import com.z.financetracker.ui.theme.FinanceTrackerTheme
import com.z.financetracker.util.TokenManager

// AppCompatActivity is required for AppCompatDelegate.setApplicationLocales()
// to take effect — it hooks into attachBaseContext2() automatically.
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceTrackerTheme {
                FinanceAppNavigation(context = this)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
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

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val hasAcceptedPrivacy = remember { prefs.getBoolean("privacy_accepted", false) }
    val startDest = when {
        !hasAcceptedPrivacy -> "consent"
        tokenManager.getToken() != null -> "dashboard"
        else -> "login"
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("consent") {
            ConsentScreen(
                onAccepted = {
                    val dest = if (tokenManager.getToken() != null) "dashboard" else "login"
                    navController.navigate(dest) {
                        popUpTo("consent") { inclusive = true }
                    }
                }
            )
        }
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
                    popUpTo("dashboard") { inclusive = true }
                }
            })
        }
    }
}