package com.lokalpos.app.ui.screens.reports

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokalpos.app.ui.theme.SuccessGreen
import com.lokalpos.app.ui.theme.PrimaryBlue
import com.lokalpos.app.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = viewModel.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Penjualan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            selected = state.filterLabel == "Tahun Ini",
                            onClick = { viewModel.filterThisYear() },
                            label = { Text("Tahun Ini") }
                        )
                    }
                }
            }

            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Total Penjualan",
                        value = settings.formatCurrency(state.totalSales),
                        icon = Icons.Filled.AttachMoney,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Transaksi",
                        value = "${state.transactionCount}",
                        icon = Icons.Filled.Receipt,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Rata-rata",
                        value = settings.formatCurrency(state.averageSale),
                        icon = Icons.Filled.TrendingUp,
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Metode Bayar",
                        value = "${state.paymentSummary.size} jenis",
                        icon = Icons.Filled.Payment,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily sales
            if (state.dailySales.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Penjualan Harian",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            state.dailySales.forEach { daily ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(daily.date, style = MaterialTheme.typography.bodyMedium)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            settings.formatCurrency(daily.total),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${daily.count} transaksi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (daily != state.dailySales.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Payment method summary
            if (state.paymentSummary.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Metode Pembayaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            state.paymentSummary.forEach { pm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(pm.paymentMethod, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${pm.count} transaksi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        settings.formatCurrency(pm.total),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top products
            if (state.topProducts.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Produk Terlaris",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            state.topProducts.forEachIndexed { index, product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${index + 1}.",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(28.dp)
                                        )
                                        Text(product.name)
                                    }
                                    Text(
                                        "${product.soldCount} terjual",
                                        style = MaterialTheme.typography.bodySmall,
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
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
