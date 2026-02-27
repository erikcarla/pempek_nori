package com.lokalpos.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lokalpos.app.ui.screens.pos.CartItem
import com.lokalpos.app.ui.screens.pos.OpenTicket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OpenTicketRepository(context: Context) {
    private val prefs = context.getSharedPreferences("open_tickets", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val ticketsKey = "tickets"

    fun getAllTickets(): Flow<Map<String, OpenTicket>> {
        return flowOf(loadTickets())
    }

    fun getAllTicketsSync(): Map<String, OpenTicket> {
        return loadTickets()
    }

    suspend fun ticketExists(tableName: String): Boolean {
        return loadTickets().containsKey(tableName)
    }

    suspend fun saveTicket(tableName: String, cart: List<CartItem>) {
        val tickets = loadTickets().toMutableMap()
        tickets[tableName] = OpenTicket(tableName, cart)
        saveTickets(tickets)
    }

    suspend fun replaceTicket(tableName: String, cart: List<CartItem>) {
        val tickets = loadTickets().toMutableMap()
        tickets[tableName] = OpenTicket(tableName, cart)
        saveTickets(tickets)
    }

    suspend fun deleteTicket(tableName: String) {
        val tickets = loadTickets().toMutableMap()
        tickets.remove(tableName)
        saveTickets(tickets)
    }

    private fun loadTickets(): Map<String, OpenTicket> {
        val json = prefs.getString(ticketsKey, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, OpenTicket>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveTickets(tickets: Map<String, OpenTicket>) {
        val json = gson.toJson(tickets)
        prefs.edit().putString(ticketsKey, json).apply()
    }
}

