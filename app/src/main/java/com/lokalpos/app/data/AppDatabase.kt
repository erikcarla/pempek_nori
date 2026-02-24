package com.lokalpos.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lokalpos.app.data.dao.CategoryDao
import com.lokalpos.app.data.dao.CustomerDao
import com.lokalpos.app.data.dao.OpenTicketDao
import com.lokalpos.app.data.dao.ProductDao
import com.lokalpos.app.data.dao.TransactionDao
import com.lokalpos.app.data.entity.*

@Database(
    entities = [
        Category::class,
        Product::class,
        Customer::class,
        Transaction::class,
        TransactionItem::class,
        OpenTicketEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun openTicketDao(): OpenTicketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lokalpos.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
