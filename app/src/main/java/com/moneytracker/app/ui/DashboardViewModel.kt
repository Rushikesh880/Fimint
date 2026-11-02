package com.moneytracker.app.ui

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.CategoryHelper
import com.moneytracker.app.data.MerchantCategoryMapping
import com.moneytracker.app.data.Transaction
import com.moneytracker.app.data.TransactionDao
import com.moneytracker.app.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(private val dao: TransactionDao) : ViewModel() {

    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()

    val categoryMappings: StateFlow<Map<String, String>> = dao.getAllCategoryMappings().map { list ->
        list.associate { it.merchantKey to it.category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val calendar = Calendar.getInstance()
    val currentYear: Int = calendar.get(Calendar.YEAR)

    // Year selection: null means "All Years"
    private val _selectedYear = MutableStateFlow<Int?>(currentYear)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    // Month selection: null means "All Months", 0..11 means Jan..Dec
    private val _selectedMonth = MutableStateFlow<Int?>(null)
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()

    fun selectYear(year: Int?) {
        _selectedYear.value = year
        if (year == null) {
            _selectedMonth.value = null
        }
    }

    fun selectMonth(month: Int?) {
        _selectedMonth.value = month
    }

    fun resolveCategory(tx: Transaction, mappings: Map<String, String> = categoryMappings.value): String {
        if (!tx.category.isNullOrBlank()) return tx.category
        val mapped = mappings[tx.merchant.trim().uppercase()]
        if (!mapped.isNullOrBlank()) return mapped
        return CategoryHelper.getCategory(tx.merchant)
    }

    // List of available years extracted from transactions, always descending and including currentYear
    val availableYears: StateFlow<List<Int>> = allTransactions.map { list ->
        val cal = Calendar.getInstance()
        val years = list.map {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR)
        }.distinct().toMutableSet()
        years.add(currentYear)
        years.sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(currentYear))

    // Transactions filtered by selectedYear and selectedMonth
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _selectedYear,
        _selectedMonth
    ) { list, year, month ->
        val cal = Calendar.getInstance()
        list.filter { tx ->
            cal.timeInMillis = tx.timestamp
            val txYear = cal.get(Calendar.YEAR)
            val txMonth = cal.get(Calendar.MONTH)

            val matchYear = (year == null || txYear == year)
            val matchMonth = (month == null || txMonth == month)
            matchYear && matchMonth
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Top Totals for the selected period
    val periodExpenses: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == "DEBIT" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val periodIncome: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == "CREDIT" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val periodNet: StateFlow<Double> = combine(periodIncome, periodExpenses) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Month-wise summaries for the selected year
    data class MonthSummary(
        val monthNumber: Int, // 0..11
        val year: Int,
        val monthLabel: String, // e.g. "November 2026"
        val totalSpent: Double,
        val totalEarned: Double,
        val net: Double,
        val transactions: List<Transaction>,
        val categories: List<CategorySpend>
    )

    data class CategorySpend(
        val category: String,
        val amount: Double,
        val percentage: Float, // 0..100
        val count: Int
    )

    // Grouping by month within the selected year
    val monthSummaries: StateFlow<List<MonthSummary>> = combine(
        allTransactions,
        _selectedYear,
        _selectedMonth,
        categoryMappings
    ) { list, year, selectedM, mappings ->
        val cal = Calendar.getInstance()
        val yearFiltered = if (year != null) {
            list.filter {
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.YEAR) == year
            }
        } else {
            list
        }

        // Group by Year-Month
        val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val grouped = yearFiltered.groupBy {
            cal.timeInMillis = it.timestamp
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            y * 100 + m // unique key for sorting
        }

        grouped.entries.sortedByDescending { it.key }.map { (key, txs) ->
            val y = key / 100
            val m = key % 100
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            val label = monthSdf.format(cal.time)

            val spent = txs.filter { it.type == "DEBIT" }.sumOf { it.amount }
            val earned = txs.filter { it.type == "CREDIT" }.sumOf { it.amount }

            // Category breakdown for this month
            val debits = txs.filter { it.type == "DEBIT" }
            val catGroups = debits.groupBy { resolveCategory(it, mappings) }
            val catSpends = catGroups.map { (cat, catTxs) ->
                val catSum = catTxs.sumOf { it.amount }
                val pct = if (spent > 0) ((catSum / spent) * 100).toFloat() else 0f
                CategorySpend(cat, catSum, pct, catTxs.size)
            }.sortedByDescending { it.amount }

            MonthSummary(
                monthNumber = m,
                year = y,
                monthLabel = label,
                totalSpent = spent,
                totalEarned = earned,
                net = earned - spent,
                transactions = txs,
                categories = catSpends
            )
        }.filter {
            if (selectedM != null) it.monthNumber == selectedM else true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Category breakdown for the active filtered period
    val periodCategories: StateFlow<List<CategorySpend>> = combine(
        filteredTransactions,
        categoryMappings
    ) { list, mappings ->
        val debits = list.filter { it.type == "DEBIT" }
        val totalSpent = debits.sumOf { it.amount }
        val groups = debits.groupBy { resolveCategory(it, mappings) }
        groups.map { (cat, catTxs) ->
            val catSum = catTxs.sumOf { it.amount }
            val pct = if (totalSpent > 0) ((catSum / totalSpent) * 100).toFloat() else 0f
            CategorySpend(cat, catSum, pct, catTxs.size)
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun updateCategory(
        transaction: Transaction,
        newCategory: String,
        rememberForMerchant: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update individual transaction
            dao.updateTransactionCategory(transaction.id, newCategory)

            // If user wants to remember this category for future transactions from this merchant:
            if (rememberForMerchant && transaction.merchant.isNotBlank()) {
                val normKey = transaction.merchant.trim().uppercase()
                dao.insertCategoryMapping(MerchantCategoryMapping(normKey, newCategory))
                dao.updateCategoryForMerchant(transaction.merchant, newCategory)
            }
        }
    }

    fun updateMerchant(transaction: Transaction, newMerchant: String) {
        val trimmed = newMerchant.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                dao.updateTransactionMerchant(transaction.id, trimmed)
            }
        }
    }

    fun addManualTransaction(
        amount: Double,
        merchant: String,
        type: String,
        account: String,
        category: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalCategory = if (!category.isNullOrBlank()) {
                category
            } else {
                dao.getCategoryForMerchantKey(merchant.trim().uppercase())
            }
            val tx = Transaction(
                amount = amount,
                merchant = merchant,
                type = type,
                timestamp = System.currentTimeMillis(),
                rawSmsBody = "Manually Entered Transaction",
                account = account,
                isManual = true,
                category = finalCategory
            )
            dao.insertTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTransaction(transaction)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
        }
    }

    fun scanInbox(context: Context) {
        if (_isScanning.value) return
        _isScanning.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inboxUri = Uri.parse("content://sms/inbox")
                val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS)
                val cursor = context.contentResolver.query(inboxUri, projection, null, null, "date DESC")

                cursor?.use { c ->
                    val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)

                    while (c.moveToNext()) {
                        val body = c.getString(bodyIndex) ?: continue
                        val timestamp = c.getLong(dateIndex)
                        val sender = c.getString(addressIndex) ?: ""

                        val parsed = SmsParser.parse(body, sender)
                        if (parsed != null) {
                            val count = dao.getCountByBodyAndTimestamp(body, timestamp)
                            if (count == 0) {
                                val savedCat = dao.getCategoryForMerchantKey(parsed.merchant.trim().uppercase())
                                val tx = Transaction(
                                    amount = parsed.amount,
                                    merchant = parsed.merchant,
                                    type = parsed.type,
                                    timestamp = timestamp,
                                    rawSmsBody = body,
                                    account = parsed.account,
                                    isManual = false,
                                    category = savedCat
                                )
                                dao.insertTransaction(tx)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Failed to scan SMS inbox", e)
            } finally {
                _isScanning.value = false
            }
        }
    }
}

class DashboardViewModelFactory(private val dao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
