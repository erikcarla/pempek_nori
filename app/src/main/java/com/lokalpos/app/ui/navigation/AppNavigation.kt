package com.lokalpos.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.lokalpos.app.ui.screens.customers.CustomersScreen
import com.lokalpos.app.ui.screens.history.HistoryScreen
import com.lokalpos.app.ui.screens.pos.PosScreen
import com.lokalpos.app.ui.screens.products.ProductEditScreen
import com.lokalpos.app.ui.screens.products.ProductsScreen
import com.lokalpos.app.ui.screens.reports.ReportsScreen
import com.lokalpos.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Pos : Screen("pos", "Kasir", Icons.Filled.PointOfSale)
    data object Products : Screen("products", "Produk", Icons.Filled.Inventory2)
    data object History : Screen("history", "Riwayat", Icons.Filled.Receipt)
    data object Reports : Screen("reports", "Laporan", Icons.Filled.BarChart)
    data object Customers : Screen("customers", "Pelanggan", Icons.Filled.People)
    data object Settings : Screen("settings", "Pengaturan", Icons.Filled.Settings)
    data object ProductEdit : Screen("product_edit/{productId}", "Edit Produk", Icons.Filled.Edit) {
        fun createRoute(productId: Long = -1L) = "product_edit/$productId"
    }
}

private val bottomNavItems = listOf(
    Screen.Pos,
    Screen.Products,
    Screen.History,
    Screen.Reports,
    Screen.Customers,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, style = MaterialTheme.typography.bodySmall) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Pos.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pos.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Pos.route) {
                PosScreen(navController = navController)
            }
            composable(Screen.Products.route) {
                ProductsScreen(
                    onAddProduct = { navController.navigate(Screen.ProductEdit.createRoute()) },
                    onEditProduct = { navController.navigate(Screen.ProductEdit.createRoute(it)) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
            composable(Screen.Customers.route) {
                CustomersScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.ProductEdit.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
                ProductEditScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
