package com.lokalpos.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokalpos.app.ui.theme.ErrorRed
import com.lokalpos.app.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDrawer: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = viewModel.settings
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    // Show dialog only for phone layout
    if (!isTablet && state.showDetail && state.selectedTransaction != null) {
        TransactionDetailDialog(
            transaction = state.selectedTransaction!!,
            items = state.selectedItems,
            settings = settings,
            dateFormat = dateFormat,
            onPrint = { viewModel.reprintReceipt() },
            onRefund = { viewModel.refundTransaction(state.selectedTransaction!!.id) },
            onDismiss = { viewModel.hideDetail() }
        )
    }

    if (showDateRangePicker) {
        DateRangePickerDialog(
            onConfirm = { startMillis, endMillis ->
                viewModel.filterCustomRange(startMillis, endMillis)
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }

    if (isTablet) {
        // Tablet layout: 30% list, 70% detail
        Row(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Left side: Transaction list (30%)
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = { Text("Riwayat") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // Filter chips
                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.filterLabel == "Hari Ini",
                            onClick = { viewModel.filterToday() },
                            label = { Text("Hari Ini", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.filterLabel == "Minggu Ini",
                            onClick = { viewModel.filterThisWeek() },
                            label = { Text("Minggu", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.filterLabel == "Bulan Ini",
                            onClick = { viewModel.filterThisMonth() },
                            label = { Text("Bulan", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.filterLabel == "Periode Lainnya",
                            onClick = { showDateRangePicker = true },
                            label = { Icon(Icons.Filled.DateRange, null, Modifier.size(16.dp)) }
                        )
                    }
                }

                // Transaction list
                if (state.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Receipt,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada transaksi", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.transactions, key = { it.id }) { transaction ->
                            val isSelected = state.selectedTransaction?.id == transaction.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.showDetail(transaction) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isSelected) 4.dp else 1.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            transaction.receiptNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            settings.formatCurrency(transaction.totalAmount),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (transaction.status == "refunded") ErrorRed else SuccessGreen
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            dateFormat.format(Date(transaction.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (transaction.status == "refunded") {
                                            Text(
                                                "REFUND",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ErrorRed
                                            )
                                        } else {
                                            Text(
                                                transaction.paymentMethod,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right side: Transaction detail (70%)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                TopAppBar(
                    title = { Text("Detail Transaksi") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                if (state.selectedTransaction != null && state.showDetail) {
                    TransactionDetailPanel(
                        transaction = state.selectedTransaction!!,
                        items = state.selectedItems,
                        settings = settings,
                        dateFormat = dateFormat,
                        onPrint = { viewModel.reprintReceipt() },
                        onRefund = { viewModel.refundTransaction(state.selectedTransaction!!.id) }
                    )
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.TouchApp,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Pilih transaksi untuk melihat detail",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Phone layout (original)
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, "Menu")
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
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filterLabel == "Hari Ini",
                        onClick = { viewModel.filterToday() },
                        label = { Text("Hari Ini") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterLabel == "Minggu Ini",
                        onClick = { viewModel.filterThisWeek() },
                        label = { Text("Minggu Ini") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterLabel == "Bulan Ini",
                        onClick = { viewModel.filterThisMonth() },
                        label = { Text("Bulan Ini") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterLabel == "Periode Lainnya",
                        onClick = { showDateRangePicker = true },
                        label = { Text("Periode Lainnya") },
                        trailingIcon = {
                            Icon(Icons.Filled.DateRange, null, Modifier.size(16.dp))
                        }
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Receipt,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Belum ada transaksi", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.transactions, key = { it.id }) { transaction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showDetail(transaction) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        transaction.receiptNumber,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        dateFormat.format(Date(transaction.createdAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        settings.formatCurrency(transaction.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.status == "refunded") ErrorRed else SuccessGreen
                                    )
                                    Text(
                                        transaction.paymentMethod,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (transaction.status == "refunded") {
                                        Text(
                                            "REFUND",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ErrorRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } // End of else (phone layout)
}

@Composable
private fun TransactionDetailPanel(
    transaction: com.lokalpos.app.data.entity.Transaction,
    items: List<com.lokalpos.app.data.entity.TransactionItem>,
    settings: com.lokalpos.app.util.SettingsManager,
    dateFormat: SimpleDateFormat,
    onPrint: () -> Unit,
    onRefund: () -> Unit
) {
    var showRefundConfirm by remember { mutableStateOf(false) }

    if (showRefundConfirm) {
        AlertDialog(
            onDismissRequest = { showRefundConfirm = false },
            title = { Text("Konfirmasi Refund") },
            text = { Text("Yakin ingin melakukan refund transaksi ${transaction.receiptNumber}?") },
            confirmButton = {
                Button(
                    onClick = { onRefund(); showRefundConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Refund") }
            },
            dismissButton = {
                TextButton(onClick = { showRefundConfirm = false }) { Text("Batal") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable content area
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transaction header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    transaction.receiptNumber,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    dateFormat.format(Date(transaction.createdAt)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (transaction.status == "refunded") {
                                Surface(
                                    color = ErrorRed.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "REFUND",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Items list card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Daftar Item (${items.size} item)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                    }
                }
            }

            // Individual items (each item scrollable)
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.productName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "${item.quantity} x ${settings.formatCurrency(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            settings.formatCurrency(item.subtotal),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal")
                            Text(settings.formatCurrency(transaction.subtotal))
                        }
                        if (transaction.taxAmount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PB1")
                                Text(settings.formatCurrency(transaction.taxAmount))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                settings.formatCurrency(transaction.totalAmount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SuccessGreen
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bayar (${transaction.paymentMethod})")
                            Text(settings.formatCurrency(transaction.amountPaid))
                        }
                        if (transaction.changeAmount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembalian")
                                Text(settings.formatCurrency(transaction.changeAmount))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons (fixed at bottom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (transaction.status == "completed") {
                OutlinedButton(
                    onClick = { showRefundConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Icon(Icons.Filled.Undo, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refund")
                }
            }
            Button(
                onClick = onPrint,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Print, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cetak Ulang")
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: com.lokalpos.app.data.entity.Transaction,
    items: List<com.lokalpos.app.data.entity.TransactionItem>,
    settings: com.lokalpos.app.util.SettingsManager,
    dateFormat: SimpleDateFormat,
    onPrint: () -> Unit,
    onRefund: () -> Unit,
    onDismiss: () -> Unit
) {
    var showRefundConfirm by remember { mutableStateOf(false) }

    if (showRefundConfirm) {
        AlertDialog(
            onDismissRequest = { showRefundConfirm = false },
            title = { Text("Konfirmasi Refund") },
            text = { Text("Yakin ingin melakukan refund transaksi ${transaction.receiptNumber}?") },
            confirmButton = {
                Button(
                    onClick = { onRefund(); showRefundConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Refund") }
            },
            dismissButton = {
                TextButton(onClick = { showRefundConfirm = false }) { Text("Batal") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Detail Transaksi")
                Text(
                    transaction.receiptNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                Text(dateFormat.format(Date(transaction.createdAt)))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${item.quantity} x ${settings.formatCurrency(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(settings.formatCurrency(item.subtotal), fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal")
                    Text(settings.formatCurrency(transaction.subtotal))
                }
                if (transaction.taxAmount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PB1")
                        Text(settings.formatCurrency(transaction.taxAmount))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontWeight = FontWeight.Bold)
                    Text(settings.formatCurrency(transaction.totalAmount), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bayar (${transaction.paymentMethod})")
                    Text(settings.formatCurrency(transaction.amountPaid))
                }
                if (transaction.changeAmount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kembalian")
                        Text(settings.formatCurrency(transaction.changeAmount))
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (transaction.status == "completed") {
                    TextButton(onClick = { showRefundConfirm = true }) {
                        Text("Refund", color = ErrorRed)
                    }
                }
                OutlinedButton(onClick = onPrint) {
                    Icon(Icons.Filled.Print, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak")
                }
                Button(onClick = onDismiss) { Text("Tutup") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L), // 1 week ago
        initialSelectedEndDateMillis = System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                         dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("Pilih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    "Pilih Periode",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            headline = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    val startText = dateRangePickerState.selectedStartDateMillis?.let {
                        dateFormat.format(Date(it))
                    } ?: "Tanggal Mulai"
                    val endText = dateRangePickerState.selectedEndDateMillis?.let {
                        dateFormat.format(Date(it))
                    } ?: "Tanggal Akhir"
                    Text(startText)
                    Text("-")
                    Text(endText)
                }
            },
            showModeToggle = false,
            modifier = Modifier.heightIn(max = 500.dp)
        )
    }
}
