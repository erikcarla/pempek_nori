package com.lokalpos.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.entity.Transaction
import com.lokalpos.app.data.entity.TransactionItem
import com.lokalpos.app.printer.EpsonPrinter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedTransaction: Transaction? = null,
    val selectedItems: List<TransactionItem> = emptyList(),
    val showDetail: Boolean = false,
    val filterStart: Long = todayStart(),
    val filterEnd: Long = todayEnd(),
    val filterLabel: String = "Hari Ini",
    val printMessage: String? = null
)

private fun todayStart(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun todayEnd(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LokalPosApp
    private val repo = app.transactionRepository
    val settings = app.settingsManager
    private val printer = EpsonPrinter(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            repo.getTransactionsByDateRange(_uiState.value.filterStart, _uiState.value.filterEnd)
                .collect { txns ->
                    _uiState.update { it.copy(transactions = txns) }
                }
        }
    }

    fun setFilter(label: String, startTime: Long, endTime: Long) {
        _uiState.update { it.copy(filterStart = startTime, filterEnd = endTime, filterLabel = label) }
        loadTransactions()
    }

    fun filterToday() {
        setFilter("Hari Ini", todayStart(), todayEnd())
    }

    fun filterThisWeek() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        setFilter("Minggu Ini", start, todayEnd())
    }

    fun filterThisMonth() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        setFilter("Bulan Ini", start, todayEnd())
    }

    fun filterCustomRange(startMillis: Long, endMillis: Long) {
        // Adjust start to beginning of day
        val cal = Calendar.getInstance()
        cal.timeInMillis = startMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        // Adjust end to end of day
        cal.timeInMillis = endMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        setFilter("Periode Lainnya", start, end)
    }

    fun showDetail(transaction: Transaction) {
        viewModelScope.launch {
            val items = repo.getTransactionItems(transaction.id)
            _uiState.update { it.copy(selectedTransaction = transaction, selectedItems = items, showDetail = true) }
        }
    }

    fun hideDetail() {
        _uiState.update { it.copy(showDetail = false, selectedTransaction = null, selectedItems = emptyList()) }
    }

    fun refundTransaction(id: Long) {
        viewModelScope.launch {
            repo.refundTransaction(id)
            hideDetail()
        }
    }

    fun reprintReceipt() {
        val tx = _uiState.value.selectedTransaction ?: return
        val items = _uiState.value.selectedItems

        viewModelScope.launch {
            val success = printer.printReceipt(tx, items, settings)
            _uiState.update { it.copy(
                printMessage = if (success) "Struk berhasil dicetak" else "Gagal mencetak struk"
            ) }
        }
    }

    fun dismissPrintMessage() {
        _uiState.update { it.copy(printMessage = null) }
    }
}
