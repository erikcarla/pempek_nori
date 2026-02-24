package com.lokalpos.app.data.dao

import androidx.room.*
import com.lokalpos.app.data.entity.OpenTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpenTicketDao {
    @Query("SELECT * FROM open_tickets ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<OpenTicketEntity>>

    @Query("SELECT * FROM open_tickets ORDER BY createdAt DESC")
    suspend fun getAll(): List<OpenTicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OpenTicketEntity): Long

    @Query("DELETE FROM open_tickets WHERE tableName = :tableName")
    suspend fun deleteByTableName(tableName: String)

    @Query("SELECT COUNT(*) FROM open_tickets WHERE tableName = :tableName")
    suspend fun countByTableName(tableName: String): Int

    @Query("DELETE FROM open_tickets")
    suspend fun deleteAll()
}
