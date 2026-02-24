package com.lokalpos.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "open_tickets")
data class OpenTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val cartJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
