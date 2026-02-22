package com.lokalpos.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    if (state.showDetail && state.selectedTransaction != null) {
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
                        selected = state.filterLabel == "Semua",
                        onClick = { viewModel.filterAll() },
                        label = { Text("Semua") }
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
