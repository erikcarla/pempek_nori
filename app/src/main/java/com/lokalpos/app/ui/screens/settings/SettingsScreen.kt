package com.lokalpos.app.ui.screens.settings

import android.widget.Toast
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
import com.lokalpos.app.printer.EpsonPrinter
import com.lokalpos.app.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as LokalPosApp
    val settings = app.settingsManager
    val scope = rememberCoroutineScope()

    var storeName by remember { mutableStateOf(settings.storeName) }
    var storeAddress by remember { mutableStateOf(settings.storeAddress) }
    var storePhone by remember { mutableStateOf(settings.storePhone) }
    var receiptHeader by remember { mutableStateOf(settings.receiptHeader) }
    var receiptFooter by remember { mutableStateOf(settings.receiptFooter) }
    var taxEnabled by remember { mutableStateOf(settings.taxEnabled) }
    var taxPercent by remember { mutableStateOf(settings.taxPercent.toString()) }
    var taxInclusive by remember { mutableStateOf(settings.taxInclusive) }
    var autoDeleteDays by remember { mutableStateOf(settings.autoDeleteDays.toString()) }
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var printerEnabled by remember { mutableStateOf(settings.printerEnabled) }
    var receiptWidth by remember { mutableStateOf(settings.receiptWidth.toString()) }
    var emailReportEnabled by remember { mutableStateOf(settings.emailReportEnabled) }
    var emailSenderAddress by remember { mutableStateOf(settings.emailSenderAddress) }
    var emailSenderPassword by remember { mutableStateOf(settings.emailSenderPassword) }
    var displayMode by remember { mutableStateOf(settings.displayMode) }

    // Dialog states
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailRecipientDialog by remember { mutableStateOf(false) }
    var showDashboardDialog by remember { mutableStateOf(false) }
    var dashboardUnlocked by remember { mutableStateOf(false) }

    fun save() {
        settings.storeName = storeName
        settings.storeAddress = storeAddress
        settings.storePhone = storePhone
        settings.receiptHeader = receiptHeader
        settings.receiptFooter = receiptFooter
        settings.taxEnabled = taxEnabled
        settings.taxPercent = taxPercent.toDoubleOrNull() ?: 10.0
        settings.taxInclusive = taxInclusive
        settings.autoDeleteDays = autoDeleteDays.toIntOrNull() ?: 30
        settings.currencySymbol = currencySymbol
        settings.printerEnabled = printerEnabled
        settings.receiptWidth = receiptWidth.toIntOrNull() ?: 33
        settings.emailReportEnabled = emailReportEnabled
        settings.emailSenderAddress = emailSenderAddress
        settings.emailSenderPassword = emailSenderPassword
        settings.displayMode = displayMode
        Toast.makeText(context, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
    }

    // Password change dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { oldPwd, newPwd ->
                if (settings.changePassword(oldPwd, newPwd)) {
                    Toast.makeText(context, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                    showPasswordDialog = false
                } else {
                    Toast.makeText(context, "Password lama salah", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    // Email recipient change dialog
    if (showEmailRecipientDialog) {
        ChangeEmailRecipientDialog(
            currentEmail = settings.emailReportAddress,
            onConfirm = { password, newEmail ->
                if (settings.validatePassword(password)) {
                    settings.emailReportAddress = newEmail
                    Toast.makeText(context, "Email penerima berhasil diubah", Toast.LENGTH_SHORT).show()
                    showEmailRecipientDialog = false
                } else {
                    Toast.makeText(context, "Password salah", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showEmailRecipientDialog = false }
        )
    }

    // Dashboard password dialog
    if (showDashboardDialog && !dashboardUnlocked) {
        PasswordInputDialog(
            title = "Masukkan Password",
            onConfirm = { password ->
                if (settings.validatePassword(password)) {
                    dashboardUnlocked = true
                } else {
                    Toast.makeText(context, "Password salah", Toast.LENGTH_SHORT).show()
                    showDashboardDialog = false
                }
            },
            onDismiss = { showDashboardDialog = false }
        )
    }

    // Dashboard dialog
    if (dashboardUnlocked) {
        DashboardDialog(
            app = app,
            settings = settings,
            onDismiss = {
                dashboardUnlocked = false
                showDashboardDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
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
            // Dashboard button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { showDashboardDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Dashboard, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Dashboard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Lihat statistik penjualan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                    Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider()

            SectionHeader("Tampilan", Icons.Filled.ViewModule)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tampilan Produk", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = displayMode == "grid",
                        onClick = { displayMode = "grid" },
                        label = { Text("Grid") },
                        leadingIcon = { Icon(Icons.Filled.GridView, null, Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = displayMode == "list",
                        onClick = { displayMode = "list" },
                        label = { Text("List") },
                        leadingIcon = { Icon(Icons.Filled.ViewList, null, Modifier.size(16.dp)) }
                    )
                }
            }

            HorizontalDivider()

            SectionHeader("Informasi Toko", Icons.Filled.Store)
            OutlinedTextField(
                value = storeName, onValueChange = { storeName = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Nama Toko") },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = storeAddress, onValueChange = { storeAddress = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Alamat Toko") },
                maxLines = 3, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = storePhone, onValueChange = { storePhone = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Telepon Toko") },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider()

            SectionHeader("Struk / Receipt", Icons.Filled.Receipt)
            OutlinedTextField(
                value = receiptHeader, onValueChange = { receiptHeader = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Header Struk") },
                maxLines = 3, shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = receiptFooter, onValueChange = { receiptFooter = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Footer Struk") },
                maxLines = 3, shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider()

            SectionHeader("Printer", Icons.Filled.Print)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Aktifkan Printer", style = MaterialTheme.typography.bodyLarge)
                    Text("Epson TM-U220D (USB)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = printerEnabled, onCheckedChange = { printerEnabled = it })
            }
            if (printerEnabled) {
                OutlinedTextField(
                    value = receiptWidth, onValueChange = { receiptWidth = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Lebar Struk (karakter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("42 untuk Font A, 56 untuk Font B") }
                )
                Button(
                    onClick = {
                        scope.launch {
                            val printer = EpsonPrinter(context)
                            val device = printer.findPrinter()
                            if (device != null) {
                                Toast.makeText(context, "Printer ditemukan: ${device.productName ?: device.deviceName}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Printer tidak ditemukan", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test Koneksi Printer")
                }
            }

            HorizontalDivider()

            SectionHeader("Pajak (PB1)", Icons.Filled.AccountBalance)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aktifkan PB1", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = taxEnabled, onCheckedChange = { taxEnabled = it })
            }
            if (taxEnabled) {
                OutlinedTextField(
                    value = taxPercent, onValueChange = { taxPercent = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Persentase PB1") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
                Text("Tipe Pajak", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = taxInclusive, onClick = { taxInclusive = true }, label = { Text("Inclusive") })
                    FilterChip(selected = !taxInclusive, onClick = { taxInclusive = false }, label = { Text("Exclusive") })
                }
                Text(
                    if (taxInclusive) "Harga produk sudah termasuk PB1" else "PB1 ditambahkan di atas harga produk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            SectionHeader("Email Laporan", Icons.Filled.Email)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kirim Laporan Harian", style = MaterialTheme.typography.bodyLarge)
                    Text("Ke: ${settings.emailReportAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = emailReportEnabled, onCheckedChange = { emailReportEnabled = it })
            }
            OutlinedButton(
                onClick = { showEmailRecipientDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ubah Email Penerima")
            }
            if (emailReportEnabled) {
                OutlinedTextField(
                    value = emailSenderAddress, onValueChange = { emailSenderAddress = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Email Pengirim (Gmail)") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Gmail account untuk mengirim laporan") }
                )
                OutlinedTextField(
                    value = emailSenderPassword, onValueChange = { emailSenderPassword = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("App Password") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text("Buat di myaccount.google.com > Security > App Passwords") }
                )
            }

            HorizontalDivider()

            SectionHeader("Keamanan", Icons.Filled.Security)
            OutlinedButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ubah Password Aplikasi")
            }

            HorizontalDivider()

            SectionHeader("Manajemen Data", Icons.Filled.Storage)
            OutlinedTextField(
                value = autoDeleteDays, onValueChange = { autoDeleteDays = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Hapus transaksi setelah (hari)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, shape = RoundedCornerShape(12.dp),
                supportingText = { Text("Set 0 untuk menonaktifkan") }
            )
            OutlinedTextField(
                value = currencySymbol, onValueChange = { currencySymbol = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Simbol Mata Uang") },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Pengaturan")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChangePasswordDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password Lama") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password Baru") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Konfirmasi Password Baru") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmPassword
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun ChangeEmailRecipientDialog(
    currentEmail: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Email Penerima") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Masukkan password untuk mengubah email penerima laporan", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Penerima Baru") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password, newEmail) },
                enabled = password.isNotBlank() && newEmail.isNotBlank() && newEmail.contains("@")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun PasswordInputDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Buka")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun DashboardDialog(
    app: LokalPosApp,
    settings: com.lokalpos.app.util.SettingsManager,
    onDismiss: () -> Unit
) {
    var todaySales by remember { mutableStateOf(0.0) }
    var todayTransactions by remember { mutableStateOf(0) }
    var weekSales by remember { mutableStateOf(0.0) }
    var weekTransactions by remember { mutableStateOf(0) }
    var monthSales by remember { mutableStateOf(0.0) }
    var monthTransactions by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val repo = app.transactionRepository
        val cal = Calendar.getInstance()

        // Today
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val todayEnd = cal.timeInMillis

        todaySales = repo.getTotalSales(todayStart, todayEnd)
        todayTransactions = repo.getTransactionCount(todayStart, todayEnd)

        // This week
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val weekStart = cal.timeInMillis

        weekSales = repo.getTotalSales(weekStart, todayEnd)
        weekTransactions = repo.getTransactionCount(weekStart, todayEnd)

        // This month
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val monthStart = cal.timeInMillis

        monthSales = repo.getTotalSales(monthStart, todayEnd)
        monthTransactions = repo.getTransactionCount(monthStart, todayEnd)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dashboard, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Dashboard")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "Hari Ini",
                    sales = settings.formatCurrency(todaySales),
                    transactions = todayTransactions,
                    color = MaterialTheme.colorScheme.primary
                )
                DashboardCard(
                    title = "Minggu Ini",
                    sales = settings.formatCurrency(weekSales),
                    transactions = weekTransactions,
                    color = MaterialTheme.colorScheme.secondary
                )
                DashboardCard(
                    title = "Bulan Ini",
                    sales = settings.formatCurrency(monthSales),
                    transactions = monthTransactions,
                    color = SuccessGreen
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun DashboardCard(
    title: String,
    sales: String,
    transactions: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text(
                sales,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                "$transactions transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

