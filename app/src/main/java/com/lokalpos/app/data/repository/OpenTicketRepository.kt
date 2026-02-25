package com.lokalpos.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lokalpos.app.data.dao.OpenTicketDao
import com.lokalpos.app.data.dao.ProductDao
import com.lokalpos.app.data.entity.OpenTicketEntity
import com.lokalpos.app.data.entity.Product
import com.lokalpos.app.ui.screens.pos.CartItem
import com.lokalpos.app.ui.screens.pos.OpenTicket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private data class CartItemJson(
    val productId: Long,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val notes: String = ""
)

class OpenTicketRepository(
    private val openTicketDao: OpenTicketDao,
    private val productDao: ProductDao
) {
    private val gson = Gson()
    private val listType = object : TypeToken<List<CartItemJson>>() {}.type

    fun getAllTickets(): Flow<Map<String, OpenTicket>> = flow {
        openTicketDao.getAllFlow().collect { entities ->
            emit(buildMap {
                for (entity in entities) {
                    put(entity.tableName, entity.toOpenTicket())
                }
            })
        }
    }

    suspend fun getAllTicketsSync(): Map<String, OpenTicket> {
        val entities = openTicketDao.getAll()
        return buildMap {
            for (entity in entities) {
                put(entity.tableName, entity.toOpenTicket())
            }
        }
    }

    private suspend fun OpenTicketEntity.toOpenTicket(): OpenTicket {
        val items: List<CartItemJson> = gson.fromJson(cartJson, listType) ?: emptyList()
        val cart = items.map { json ->
            val product = productDao.getById(json.productId)
                ?: Product(
                    id = json.productId,
                    name = json.productName,
                    price = json.price,
                    categoryId = null
                )
            CartItem(product = product, quantity = json.quantity, notes = json.notes)
        }
        return OpenTicket(tableName = tableName, cart = cart, createdAt = createdAt)
    }

    suspend fun saveTicket(tableName: String, cart: List<CartItem>) {
        val items = cart.map {
            CartItemJson(
                productId = it.product.id,
                productName = it.product.name,
                price = it.product.price,
                quantity = it.quantity,
                notes = it.notes
            )
        }
        val json = gson.toJson(items)
        openTicketDao.insert(OpenTicketEntity(tableName = tableName, cartJson = json))
    }

    suspend fun deleteTicket(tableName: String) {
        openTicketDao.deleteByTableName(tableName)
    }

    suspend fun ticketExists(tableName: String): Boolean {
        return openTicketDao.countByTableName(tableName) > 0
    }

    suspend fun replaceTicket(tableName: String, cart: List<CartItem>) {
        openTicketDao.deleteByTableName(tableName)
        saveTicket(tableName, cart)
    }
}
