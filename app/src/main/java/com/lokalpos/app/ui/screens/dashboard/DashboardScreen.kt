package com.lokalpos.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lokalpos.app.LokalPosApp
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as LokalPosApp
    val settings = app.settingsManager
    val transactionRepo = app.transactionRepository
    val scope = rememberCoroutineScope()

    var isUnlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var todaySales by remember { mutableStateOf(0.0) }
    var todayCount by remember { mutableStateOf(0) }
    var weekSales by remember { mutableStateOf(0.0) }
    var weekCount by remember { mutableStateOf(0) }
    var monthSales by remember { mutableStateOf(0.0) }
    var monthCount by remember { mutableStateOf(0) }

    fun loadStats() {
        scope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val todayEnd = cal.timeInMillis

            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val weekStart = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis

            todaySales = transactionRepo.getTotalSales(todayStart, todayEnd)
            todayCount = transactionRepo.getTransactionCount(todayStart, todayEnd)
            weekSales = transactionRepo.getTotalSales(weekStart, todayEnd)
            weekCount = transactionRepo.getTransactionCount(weekStart, todayEnd)
            monthSales = transactionRepo.getTotalSales(monthStart, todayEnd)
            monthCount = transactionRepo.getTransactionCount(monthStart, todayEnd)
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) loadStats()
    }

    if (!isUnlocked) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Masukkan Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorMessage != null,
                    supportingText = { errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (passwordInput == settings.appPassword) {
                            isUnlocked = true
                        } else {
                            errorMessage = "Password salah"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Masuk")
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard("Hari Ini", todaySales, todayCount, settings)
                StatCard("Minggu Ini", weekSales, weekCount, settings)
                StatCard("Bulan Ini", monthSales, monthCount, settings)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    total: Double,
    count: Int,
    settings: com.lokalpos.app.util.SettingsManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                settings.formatCurrency(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("$count transaksi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
