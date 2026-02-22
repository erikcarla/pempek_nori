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
import androidx.compose.ui.unit.dp
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.printer.EpsonPrinter
import com.lokalpos.app.util.SettingsManager
import kotlinx.coroutines.launch

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
    var autoDeleteDays by remember { mutableStateOf(settings.autoDeleteDays.toString()) }
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var printerEnabled by remember { mutableStateOf(settings.printerEnabled) }
    var receiptWidth by remember { mutableStateOf(settings.receiptWidth.toString()) }
    var loyaltyEnabled by remember { mutableStateOf(settings.loyaltyEnabled) }
    var loyaltyPointsPerAmount by remember { mutableStateOf(settings.loyaltyPointsPerAmount.toString()) }

    fun save() {
        settings.storeName = storeName
        settings.storeAddress = storeAddress
        settings.storePhone = storePhone
        settings.receiptHeader = receiptHeader
        settings.receiptFooter = receiptFooter
        settings.taxEnabled = taxEnabled
        settings.taxPercent = taxPercent.toDoubleOrNull() ?: 11.0
        settings.autoDeleteDays = autoDeleteDays.toIntOrNull() ?: 30
        settings.currencySymbol = currencySymbol
        settings.printerEnabled = printerEnabled
        settings.receiptWidth = receiptWidth.toIntOrNull() ?: 42
        settings.loyaltyEnabled = loyaltyEnabled
        settings.loyaltyPointsPerAmount = loyaltyPointsPerAmount.toIntOrNull() ?: 10000
        Toast.makeText(context, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
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
            // Store Info Section
            SectionHeader("Informasi Toko", Icons.Filled.Store)
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Toko") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = storeAddress,
                onValueChange = { storeAddress = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Alamat Toko") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = storePhone,
                onValueChange = { storePhone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telepon Toko") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider()

            // Receipt Section
            SectionHeader("Struk / Receipt", Icons.Filled.Receipt)
            OutlinedTextField(
                value = receiptHeader,
                onValueChange = { receiptHeader = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Header Struk (teks tambahan)") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("Teks tambahan di bawah info toko") }
            )
            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Footer Struk") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("Teks di bagian bawah struk") }
            )

            HorizontalDivider()

            // Printer Section
            SectionHeader("Printer", Icons.Filled.Print)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Aktifkan Printer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Epson TM-U220D (USB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = printerEnabled, onCheckedChange = { printerEnabled = it })
            }

            if (printerEnabled) {
                OutlinedTextField(
                    value = receiptWidth,
                    onValueChange = { receiptWidth = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lebar Struk (karakter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Default 42 untuk Font A, 56 untuk Font B") }
                )

                Button(
                    onClick = {
                        scope.launch {
                            val printer = EpsonPrinter(context)
                            val device = printer.findPrinter()
                            if (device != null) {
                                Toast.makeText(context, "Printer ditemukan: ${device.productName ?: device.deviceName}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Printer tidak ditemukan. Pastikan kabel USB terhubung.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Print, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test Koneksi Printer")
                }
            }

            HorizontalDivider()

            // Tax Section
            SectionHeader("Pajak", Icons.Filled.AccountBalance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aktifkan Pajak", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = taxEnabled, onCheckedChange = { taxEnabled = it })
            }
            if (taxEnabled) {
                OutlinedTextField(
                    value = taxPercent,
                    onValueChange = { taxPercent = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Persentase Pajak") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            HorizontalDivider()

            // Loyalty Section
            SectionHeader("Loyalti Pelanggan", Icons.Filled.CardGiftcard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aktifkan Program Loyalti", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = loyaltyEnabled, onCheckedChange = { loyaltyEnabled = it })
            }
            if (loyaltyEnabled) {
                OutlinedTextField(
                    value = loyaltyPointsPerAmount,
                    onValueChange = { loyaltyPointsPerAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("1 poin per berapa Rupiah") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            HorizontalDivider()

            // Data Management
            SectionHeader("Manajemen Data", Icons.Filled.Storage)
            OutlinedTextField(
                value = autoDeleteDays,
                onValueChange = { autoDeleteDays = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hapus transaksi otomatis setelah (hari)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("Transaksi lebih lama dari ini akan otomatis dihapus. Set 0 untuk menonaktifkan.") }
            )
            OutlinedTextField(
                value = currencySymbol,
                onValueChange = { currencySymbol = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Simbol Mata Uang") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Pengaturan")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
