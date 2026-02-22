package com.lokalpos.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("createdAt"), Index("receiptNumber")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val customerId: Long? = null,
    val customerName: String? = null,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String = "Tunai",
    val amountPaid: Double = 0.0,
    val changeAmount: Double = 0.0,
    val status: String = "completed",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
