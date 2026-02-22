package com.lokalpos.app.ui.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.dao.AggregatedSalesItem
import com.lokalpos.app.ui.screens.history.HistoryScreen
import com.lokalpos.app.ui.screens.pos.PosScreen
import com.lokalpos.app.ui.screens.products.ProductEditScreen
import com.lokalpos.app.ui.screens.products.ProductsScreen
import com.lokalpos.app.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Pos : Screen("pos", "Kasir", Icons.Filled.PointOfSale)
    data object Products : Screen("products", "Produk", Icons.Filled.Inventory2)
    data object History : Screen("history", "Riwayat", Icons.Filled.Receipt)
    data object Settings : Screen("settings", "Pengaturan", Icons.Filled.Settings)
    data object ProductEdit : Screen("product_edit/{productId}", "Edit Produk", Icons.Filled.Edit) {
        fun createRoute(productId: Long = -1L) = "product_edit/$productId"
    }
}

private val drawerItems = listOf(
    Screen.Pos,
    Screen.Products,
    Screen.History,
    Screen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val context = LocalContext.current
    val app = context.applicationContext as LokalPosApp

    var showEmailReportDialog by remember { mutableStateOf(false) }

    if (showEmailReportDialog) {
        EmailReportDialog(
            onDismiss = { showEmailReportDialog = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    app.settingsManager.storeName.ifBlank { "LokalPOS" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(8.dp))

                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Pos.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Email, contentDescription = "Kirim Laporan") },
                    label = { Text("Kirim Laporan Harian") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (app.settingsManager.emailReportEnabled) {
                            showEmailReportDialog = true
                        } else {
                            Toast.makeText(context, "Fitur email laporan belum diaktifkan. Aktifkan di Pengaturan.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Pos.route,
        ) {
            composable(Screen.Pos.route) {
                PosScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.Products.route) {
                ProductsScreen(
                    onAddProduct = { navController.navigate(Screen.ProductEdit.createRoute()) },
                    onEditProduct = { navController.navigate(Screen.ProductEdit.createRoute(it)) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
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

@Composable
private fun EmailReportDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as LokalPosApp
    val settings = app.settingsManager
    val transactionRepo = app.transactionRepository
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var aggregatedItems by remember { mutableStateOf<List<AggregatedSalesItem>>(emptyList()) }
    var totalSales by remember { mutableStateOf(0.0) }
    var txCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        aggregatedItems = transactionRepo.getAggregatedSalesItems(startTime, endTime)
        totalSales = transactionRepo.getTotalSales(startTime, endTime)
        txCount = transactionRepo.getTransactionCount(startTime, endTime)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Laporan Penjualan Hari Ini") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (aggregatedItems.isEmpty()) {
                Text("Belum ada penjualan hari ini.")
            } else {
                Column {
                    Text(
                        "$txCount transaksi - Total: ${settings.formatCurrency(totalSales)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    aggregatedItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.productName} x${item.totalQty}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                settings.formatCurrency(item.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", fontWeight = FontWeight.Bold)
                        Text(settings.formatCurrency(totalSales), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                    val today = dateFormat.format(Date())

                    val sb = StringBuilder()
                    sb.appendLine("Laporan Penjualan Harian")
                    sb.appendLine("${settings.storeName} - $today")
                    sb.appendLine("================================")
                    sb.appendLine("$txCount transaksi")
                    sb.appendLine()

                    aggregatedItems.forEach { item ->
                        sb.appendLine("${item.productName} x${item.totalQty} = ${settings.formatCurrency(item.totalAmount)}")
                    }
                    sb.appendLine()
                    sb.appendLine("================================")
                    sb.appendLine("TOTAL: ${settings.formatCurrency(totalSales)}")

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(settings.emailReportAddress))
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan ${settings.storeName} - $today")
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                    }

                    try {
                        context.startActivity(Intent.createChooser(intent, "Kirim laporan via..."))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
                enabled = aggregatedItems.isNotEmpty()
            ) {
                Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Kirim Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
