package com.moneytracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val type: String, // "DEBIT" or "CREDIT"
    val timestamp: Long,
    val rawSmsBody: String,
    val account: String,
    val isManual: Boolean = false,
    val category: String? = null
)
