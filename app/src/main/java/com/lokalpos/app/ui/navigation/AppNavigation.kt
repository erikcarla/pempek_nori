package com.lokalpos.app.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.ui.screens.history.HistoryScreen
import com.lokalpos.app.ui.screens.pos.PosScreen
import com.lokalpos.app.ui.screens.products.ProductEditScreen
import com.lokalpos.app.ui.screens.products.ProductsScreen
import com.lokalpos.app.ui.screens.settings.SettingsScreen
import com.lokalpos.app.util.EmailSender
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
    Screen.History,
    Screen.Products,
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
        EmailReportDatePicker(
            onSend = { selectedDate ->
                showEmailReportDialog = false
                scope.launch {
                    val settings = app.settingsManager
                    val transactionRepo = app.transactionRepository

                    val cal = Calendar.getInstance().apply { time = selectedDate }
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

                    val items = transactionRepo.getAggregatedSalesItems(startTime, endTime)
                    val totalSales = transactionRepo.getTotalSales(startTime, endTime)
                    val txCount = transactionRepo.getTransactionCount(startTime, endTime)

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                    val dateStr = dateFormat.format(selectedDate)

                    val sb = StringBuilder()
                    sb.appendLine("Laporan Penjualan Harian")
                    sb.appendLine("${settings.storeName} - $dateStr")
                    sb.appendLine("================================")
                    sb.appendLine("$txCount transaksi")
                    sb.appendLine()
                    items.forEach { item ->
                        sb.appendLine("${item.productName} x${item.totalQty} = ${settings.formatCurrency(item.totalAmount)}")
                    }
                    sb.appendLine()
                    sb.appendLine("================================")
                    sb.appendLine("TOTAL: ${settings.formatCurrency(totalSales)}")

                    val subject = "Laporan Penjualan ${settings.storeName} - $dateStr"
                    val body = sb.toString()
                    val senderEmail = settings.emailSenderAddress
                    val senderPassword = settings.emailSenderPassword

                    if (senderEmail.isNotBlank() && senderPassword.isNotBlank()) {
                        Toast.makeText(context, "Mengirim laporan...", Toast.LENGTH_SHORT).show()
                        val result = EmailSender.send(
                            senderEmail = senderEmail,
                            senderPassword = senderPassword,
                            recipientEmail = settings.emailReportAddress,
                            subject = subject,
                            body = body
                        )
                        if (result.isSuccess) {
                            Toast.makeText(context, "Laporan terkirim!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal kirim: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Email pengirim belum diatur di Pengaturan", Toast.LENGTH_LONG).show()
                    }
                }
            },
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
                            val shouldNavigate = currentRoute != screen.route
                            scope.launch {
                                drawerState.close()
                                if (shouldNavigate) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Pos.route) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
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
                        val emailEnabled = app.settingsManager.emailReportEnabled
                        scope.launch {
                            drawerState.close()
                            if (emailEnabled) {
                                showEmailReportDialog = true
                            } else {
                                Toast.makeText(context, "Fitur email laporan belum diaktifkan. Aktifkan di Pengaturan.", Toast.LENGTH_LONG).show()
                            }
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
                PosScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
            composable(Screen.Products.route) {
                ProductsScreen(
                    onAddProduct = { navController.navigate(Screen.ProductEdit.createRoute()) },
                    onEditProduct = { navController.navigate(Screen.ProductEdit.createRoute(it)) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.ProductEdit.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
                ProductEditScreen(productId = productId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailReportDatePicker(
    onSend: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    onSend(Date(millis))
                }
            ) {
                Icon(Icons.Filled.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Kirim Laporan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    "Pilih tanggal laporan",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        )
    }
}
