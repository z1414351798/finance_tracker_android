package com.z.financetracker.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z.financetracker.util.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    var currentTab by remember { mutableStateOf("home") }
    var showRecordSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        floatingActionButton = {
            // Only show FAB when NOT on AI screen
            if (currentTab != "ai") {
                FloatingActionButton(
                    onClick = { showRecordSheet = true },
                    shape = CircleShape,
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction", modifier = Modifier.size(28.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                listOf(
                    NavItem("home",        "Home",         Icons.Default.Home),
                    NavItem("transactions","Transactions",  Icons.Default.Receipt),
                    NavItem("analytics",   "Analytics",    Icons.Default.BarChart),
                    NavItem("profile",     "Profile",      Icons.Default.Person),
                    NavItem("ai", "AI Advisor", Icons.Default.AutoAwesome)
                ).forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item.route,
                        onClick = { currentTab = item.route },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Color(0xFF2563EB),
                            selectedTextColor   = Color(0xFF2563EB),
                            indicatorColor      = Color(0xFFEFF6FF),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentTab) {
                "home"         -> SkylineScreen()
                "transactions" -> HistoryScreen()
                "analytics"    -> AnalyticsWithBudgetAndGoalScreen()
                "profile"      -> ProfileScreen(onLogout = onLogout)
                "ai" -> AiChatScreen()
            }
        }

        // ── Record bottom sheet ────────────────────────────────────
        if (showRecordSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRecordSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = Color.White,
                dragHandle = {
                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 4.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .then(
                                Modifier.padding(0.dp) // keeps alignment
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(width = 40.dp, height = 4.dp),
                            shape = CircleShape,
                            color = Color(0xFFE5E7EB)
                        ) {}
                    }
                }
            ) {
                RecordScreen(
                    onSuccess = { showRecordSheet = false }
                )
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)