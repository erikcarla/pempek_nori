package com.lokalpos.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val loyaltyPoints: Int = 0,
    val totalSpent: Double = 0.0,
    val visitCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
