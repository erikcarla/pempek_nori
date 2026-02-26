package com.lokalpos.app.data.repository

import com.lokalpos.app.data.dao.CustomerDao
import com.lokalpos.app.data.entity.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CustomerRepository(private val customerDao: CustomerDao) {

    fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAll().flowOn(Dispatchers.IO)

    fun searchCustomers(query: String): Flow<List<Customer>> =
        customerDao.search(query).flowOn(Dispatchers.IO)

    suspend fun getCustomerById(id: Long): Customer? = withContext(Dispatchers.IO) {
        customerDao.getById(id)
    }

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        customerDao.insert(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.update(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.delete(customer)
    }

    suspend fun addPurchase(customerId: Long, amount: Double, loyaltyPoints: Int) =
        withContext(Dispatchers.IO) {
            customerDao.addPurchase(customerId, amount, loyaltyPoints)
        }

    suspend fun getCustomerCount(): Int = withContext(Dispatchers.IO) { customerDao.count() }
}
