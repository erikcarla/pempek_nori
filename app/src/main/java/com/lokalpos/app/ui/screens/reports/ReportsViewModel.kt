package com.lokalpos.app.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.dao.DailySales
import com.lokalpos.app.data.dao.PaymentMethodSummary
import com.lokalpos.app.data.entity.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

data class ReportsUiState(
    val totalSales: Double = 0.0,
    val transactionCount: Int = 0,
    val averageSale: Double = 0.0,
    val dailySales: List<DailySales> = emptyList(),
    val paymentSummary: List<PaymentMethodSummary> = emptyList(),
    val topProducts: List<Product> = emptyList(),
    val profit: Double = 0.0,
    val filterLabel: String = "Hari Ini",
    val filterStart: Long = 0L,
    val filterEnd: Long = 0L,
    val isLoading: Boolean = false
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LokalPosApp
    private val transactionRepo = app.transactionRepository
    private val productRepo = app.productRepository
    val settings = app.settingsManager

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        filterToday()
    }

    private fun loadReports(start: Long, end: Long) {
        _uiState.update { it.copy(isLoading = true, filterStart = start, filterEnd = end) }

        viewModelScope.launch {
            val totalSales = transactionRepo.getTotalSales(start, end)
            val txCount = transactionRepo.getTransactionCount(start, end)
            val avgSale = transactionRepo.getAverageSale(start, end)
            val daily = transactionRepo.getDailySales(start, end)
            val payments = transactionRepo.getPaymentMethodSummary(start, end)
            val topProducts = productRepo.getTopSellingProducts(10)

            _uiState.update {
                it.copy(
                    totalSales = totalSales,
                    transactionCount = txCount,
                    averageSale = avgSale,
                    dailySales = daily,
                    paymentSummary = payments,
                    topProducts = topProducts,
                    isLoading = false
                )
            }
        }
    }

    fun filterToday() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        _uiState.update { it.copy(filterLabel = "Hari Ini") }
        loadReports(start, cal.timeInMillis)
    }

    fun filterThisWeek() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        _uiState.update { it.copy(filterLabel = "Minggu Ini") }
        loadReports(start, System.currentTimeMillis())
    }

    fun filterThisMonth() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        _uiState.update { it.copy(filterLabel = "Bulan Ini") }
        loadReports(start, System.currentTimeMillis())
    }

    fun filterThisYear() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        _uiState.update { it.copy(filterLabel = "Tahun Ini") }
        loadReports(start, System.currentTimeMillis())
    }
}
