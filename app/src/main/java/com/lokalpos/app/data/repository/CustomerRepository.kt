package com.lokalpos.app.data.repository

import com.lokalpos.app.data.dao.CustomerDao
import com.lokalpos.app.data.entity.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {

    fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAll()

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.search(query)

    suspend fun getCustomerById(id: Long): Customer? = customerDao.getById(id)

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insert(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.update(customer)

    suspend fun deleteCustomer(customer: Customer) = customerDao.delete(customer)

    suspend fun addPurchase(customerId: Long, amount: Double, loyaltyPoints: Int) =
        customerDao.addPurchase(customerId, amount, loyaltyPoints)

    suspend fun getCustomerCount(): Int = customerDao.count()
}
