package com.moneytracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalExpensesFlow(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT'")
    fun getTotalIncomeFlow(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions WHERE rawSmsBody = :smsBody AND timestamp = :timestamp")
    suspend fun getCountByBodyAndTimestamp(smsBody: String, timestamp: Long): Int

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateTransactionCategory(id: Long, category: String)

    @Query("UPDATE transactions SET merchant = :merchant WHERE id = :id")
    suspend fun updateTransactionMerchant(id: Long, merchant: String)

    @Query("UPDATE transactions SET category = :category WHERE UPPER(TRIM(merchant)) = UPPER(TRIM(:merchant))")
    suspend fun updateCategoryForMerchant(merchant: String, category: String)

    @Query("SELECT * FROM merchant_category_mappings")
    fun getAllCategoryMappings(): Flow<List<MerchantCategoryMapping>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryMapping(mapping: MerchantCategoryMapping)

    @Query("SELECT category FROM merchant_category_mappings WHERE merchantKey = :merchantKey LIMIT 1")
    suspend fun getCategoryForMerchantKey(merchantKey: String): String?
}
