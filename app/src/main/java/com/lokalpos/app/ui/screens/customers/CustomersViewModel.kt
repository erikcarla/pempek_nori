package com.lokalpos.app.ui.screens.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.entity.Customer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomersUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val showEditDialog: Boolean = false,
    val editingCustomer: Customer? = null
)

class CustomersViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LokalPosApp
    private val repo = app.customerRepository
    val settings = app.settingsManager

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllCustomers().collect { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                repo.getAllCustomers().collect { _uiState.update { s -> s.copy(customers = it) } }
            } else {
                repo.searchCustomers(query).collect { _uiState.update { s -> s.copy(customers = it) } }
            }
        }
    }

    fun showEditDialog(customer: Customer? = null) {
        _uiState.update { it.copy(showEditDialog = true, editingCustomer = customer) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingCustomer = null) }
    }

    fun saveCustomer(name: String, phone: String, email: String, address: String, notes: String) {
        viewModelScope.launch {
            val existing = _uiState.value.editingCustomer
            if (existing != null) {
                repo.updateCustomer(
                    existing.copy(
                        name = name,
                        phone = phone.ifBlank { null },
                        email = email.ifBlank { null },
                        address = address.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )
                )
            } else {
                repo.insertCustomer(
                    Customer(
                        name = name,
                        phone = phone.ifBlank { null },
                        email = email.ifBlank { null },
                        address = address.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )
                )
            }
            hideEditDialog()
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repo.deleteCustomer(customer) }
    }
}
