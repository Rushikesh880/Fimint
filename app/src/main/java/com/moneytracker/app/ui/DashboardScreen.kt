package com.moneytracker.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.CategoryHelper
import com.moneytracker.app.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val categoryMappings by viewModel.categoryMappings.collectAsState()

    val periodExpenses by viewModel.periodExpenses.collectAsState()
    val periodIncome by viewModel.periodIncome.collectAsState()
    val periodNet by viewModel.periodNet.collectAsState()

    val monthSummaries by viewModel.monthSummaries.collectAsState()
    val periodCategories by viewModel.periodCategories.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Feed, 1: Insights
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTransactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionForDetails by remember { mutableStateOf<Transaction?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val periodLabel = remember(selectedYear, selectedMonth) {
        when {
            selectedYear == null -> "All Time"
            selectedMonth == null -> "$selectedYear"
            else -> "${monthNames[selectedMonth!!]} $selectedYear"
        }
    }

    Scaffold(
        containerColor = Color(0xFF090D16),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF047857))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Buckwheat",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "SMART FINANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rescan button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF151C2C))
                            .clickable(enabled = !isScanning) { viewModel.scanInbox(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Scan SMS Inbox",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Overflow Menu
                    Box {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF151C2C))
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF141B2D))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rescan SMS Inbox", color = Color(0xFFF8FAFC)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.scanInbox(context)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF10B981))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear All Data", color = Color(0xFFF43F5E)) },
                                onClick = {
                                    showMenu = false
                                    showClearAllConfirm = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFF43F5E))
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF090D16))
        ) {
            // Modern Year Filter Pill Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableYears.forEach { yr ->
                    ModernFilterPill(
                        label = "$yr",
                        isSelected = selectedYear == yr,
                        onClick = { viewModel.selectYear(yr) }
                    )
                }

                ModernFilterPill(
                    label = "All Time",
                    isSelected = selectedYear == null,
                    onClick = { viewModel.selectYear(null) }
                )
            }

            // Month Pills (Smooth animated visibility when a year is selected)
            AnimatedVisibility(
                visible = selectedYear != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernMonthPill(
                        label = "All Months",
                        isSelected = selectedMonth == null,
                        onClick = { viewModel.selectMonth(null) }
                    )

                    monthNames.forEachIndexed { idx, mName ->
                        ModernMonthPill(
                            label = mName,
                            isSelected = selectedMonth == idx,
                            onClick = {
                                if (selectedMonth == idx) {
                                    viewModel.selectMonth(null)
                                } else {
                                    viewModel.selectMonth(idx)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Premium Hero Financial Card
            HeroBalanceCard(
                netBalance = periodNet,
                income = periodIncome,
                expenses = periodExpenses,
                periodLabel = periodLabel
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Modern Segmented Tab Bar
            ModernSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab 0: Transactions Feed (Month-Grouped)
            if (selectedTab == 0) {
                if (monthSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF131927)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No transactions found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tap the refresh icon on top to scan your inbox for financial SMS.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        monthSummaries.forEach { monthSummary ->
                            // Sleek Month Header
                            item(key = "header_${monthSummary.monthLabel}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = monthSummary.monthLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "+ ${formatCurrency(monthSummary.totalEarned)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFF43F5E).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "- ${formatCurrency(monthSummary.totalSpent)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF43F5E),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Modern Transaction Items for this Month
                            items(monthSummary.transactions, key = { it.id }) { tx ->
                                val category = viewModel.resolveCategory(tx, categoryMappings)
                                ModernTransactionCard(
                                    transaction = tx,
                                    category = category,
                                    onClick = { selectedTransactionForDetails = tx },
                                    onLongClick = { selectedTransactionToDelete = tx }
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Category Breakdown & Month-by-Month Flow
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Spend by Category",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            color = Color(0xFFF8FAFC)
                        )
                    }

                    if (periodCategories.isEmpty()) {
                        item {
                            Text(
                                text = "No expenses recorded for this period.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        items(periodCategories, key = { it.category }) { catSpend ->
                            ModernCategoryCard(catSpend = catSpend)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Monthly Overview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            color = Color(0xFFF8FAFC)
                        )
                    }

                    items(monthSummaries, key = { "flow_${it.monthLabel}" }) { monthSummary ->
                        ModernMonthlyFlowCard(monthSummary = monthSummary)
                    }
                }
            }
        }
    }

    // Transaction Details Dialog (Shows raw SMS body + Category Editor)
    selectedTransactionForDetails?.let { tx ->
        val currentCategory = viewModel.resolveCategory(tx, categoryMappings)
        ModernTransactionDetailDialog(
            transaction = tx,
            category = currentCategory,
            onUpdateCategory = { newCategory, remember ->
                viewModel.updateCategory(tx, newCategory, remember)
                // Refresh local selected dialog reference if open
                selectedTransactionForDetails = tx.copy(category = newCategory)
                Toast.makeText(
                    context,
                    if (remember) "Updated & remembered as $newCategory for ${tx.merchant}!" else "Updated to $newCategory!",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { selectedTransactionForDetails = null },
            onDelete = {
                viewModel.deleteTransaction(tx)
                selectedTransactionForDetails = null
            }
        )
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        ModernAddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, merchant, type, account, category ->
                viewModel.addManualTransaction(amount, merchant, type, account, category)
                showAddDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (selectedTransactionToDelete != null) {
        AlertDialog(
            containerColor = Color(0xFF141B2D),
            shape = RoundedCornerShape(20.dp),
            onDismissRequest = { selectedTransactionToDelete = null },
            title = { Text("Delete Transaction", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this record from local storage?", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTransactionToDelete?.let { viewModel.deleteTransaction(it) }
                        selectedTransactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTransactionToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            containerColor = Color(0xFF141B2D),
            shape = RoundedCornerShape(20.dp),
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear All Transactions?", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all transactions from the local database. You can then tap 'Rescan SMS Inbox' to import only verified bank and UPI transactions.", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTransactions()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Modern UI Components
// -------------------------------------------------------------------------------------------------

@Composable
fun HeroBalanceCard(
    netBalance: Double,
    income: Double,
    expenses: Double,
    periodLabel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(Color(0x336366F1), Color(0x10FFFFFF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFF43F5E))
                    )
                    Text(
                        text = "NET BALANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = Color(0xFF1A2338),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = periodLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Big Bold Amount
            Text(
                text = formatCurrency(netBalance),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = if (netBalance >= 0) Color(0xFFF8FAFC) else Color(0xFFF43F5E)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Sub-cards for Earned & Spent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Earned Tile
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161E31))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Received",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(income),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Spent Tile
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161E31))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Spent",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(expenses),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected)
                    Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                else
                    Brush.horizontalGradient(listOf(Color(0xFF131927), Color(0xFF131927)))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun ModernMonthPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Color(0xFF1E293B) else Color(0xFF0F1522)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF10B981) else Color(0xFF64748B)
        )
    }
}

@Composable
fun ModernSegmentedTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111726))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tab 0: Transactions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 0) Color(0xFF1A2338) else Color.Transparent)
                    .clickable { onTabSelected(0) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = if (selectedTab == 0) Color(0xFF10B981) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Transactions",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Color(0xFFF8FAFC) else Color(0xFF64748B)
                    )
                }
            }

            // Tab 1: Insights & Categories
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 1) Color(0xFF1A2338) else Color.Transparent)
                    .clickable { onTabSelected(1) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = if (selectedTab == 1) Color(0xFF6366F1) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Insights & Flow",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color(0xFFF8FAFC) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernTransactionCard(
    transaction: Transaction,
    category: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val categoryColor = remember(category) { CategoryHelper.getCategoryColor(category) }
    val categoryIcon = remember(category) { CategoryHelper.getCategoryIcon(category) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
        border = BorderStroke(1.dp, Color(0xFF1A2338))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Squircle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.merchant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = categoryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        Surface(
                            color = Color(0xFF1A2338),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (transaction.isManual) "Cash" else "A/c *${transaction.account}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = formatDate(transaction.timestamp),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Amount
            Text(
                text = "${if (transaction.type == "DEBIT") "-" else "+"} ${formatCurrency(transaction.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (transaction.type == "DEBIT") Color(0xFFF43F5E) else Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun ModernCategoryCard(catSpend: DashboardViewModel.CategorySpend) {
    val color = CategoryHelper.getCategoryColor(catSpend.category)
    val icon = CategoryHelper.getCategoryIcon(catSpend.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
        border = BorderStroke(1.dp, Color(0xFF1A2338))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = catSpend.category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "${catSpend.count} transactions",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(catSpend.amount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", catSpend.percentage)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rounded gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1A2338))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (catSpend.percentage / 100f).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun ModernMonthlyFlowCard(monthSummary: DashboardViewModel.MonthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
        border = BorderStroke(1.dp, Color(0xFF1A2338))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthSummary.monthLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
                Surface(
                    color = if (monthSummary.net >= 0) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF43F5E).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Net: ${formatCurrency(monthSummary.net)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (monthSummary.net >= 0) Color(0xFF10B981) else Color(0xFFF43F5E),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Received", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        "+ ${formatCurrency(monthSummary.totalEarned)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Spent", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        "- ${formatCurrency(monthSummary.totalSpent)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E)
                    )
                }
            }

            // Top categories tags
            if (monthSummary.categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    monthSummary.categories.take(3).forEach { cat ->
                        Surface(
                            color = CategoryHelper.getCategoryColor(cat.category).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${cat.category}: ${formatCurrency(cat.amount)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = CategoryHelper.getCategoryColor(cat.category),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTransactionDetailDialog(
    transaction: Transaction,
    category: String,
    onUpdateCategory: (newCategory: String, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val categoryColor = remember(category) { CategoryHelper.getCategoryColor(category) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = Color(0xFF131929),
        shape = RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF8FAFC)
                )
                Surface(
                    color = if (transaction.type == "DEBIT") Color(0xFFF43F5E).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.type,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == "DEBIT") Color(0xFFF43F5E) else Color(0xFF10B981)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Amount", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            formatCurrency(transaction.amount),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (transaction.type == "DEBIT") Color(0xFFF43F5E) else Color(0xFF10B981)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Account", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            if (transaction.isManual) "Cash" else "A/c *${transaction.account}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text("Merchant", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            transaction.merchant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF8FAFC)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Category Pill with Edit Icon
                        Surface(
                            color = categoryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { showCategoryPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = CategoryHelper.getCategoryIcon(category),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Category",
                                    tint = categoryColor,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Date & Time", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            formatDate(transaction.timestamp),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E283E))

                Text(
                    text = if (transaction.isManual) "Source: Manual Entry" else "Raw SMS Body (Original Message):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0C1019),
                    border = BorderStroke(1.dp, Color(0xFF1E283E))
                ) {
                    SelectionContainer {
                        Text(
                            text = transaction.rawSmsBody,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 17.sp
                            ),
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                if (!transaction.isManual) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(transaction.rawSmsBody))
                            Toast.makeText(context, "SMS text copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy SMS Text", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF43F5E))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF94A3B8))
            }
        }
    )

    if (showCategoryPicker) {
        CategoryPickerDialog(
            currentCategory = category,
            merchantName = transaction.merchant,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = { newCat, remember ->
                onUpdateCategory(newCat, remember)
                showCategoryPicker = false
            }
        )
    }
}

@Composable
fun CategoryPickerDialog(
    currentCategory: String,
    merchantName: String,
    onDismiss: () -> Unit,
    onCategorySelected: (category: String, remember: Boolean) -> Unit
) {
    var selectedCat by remember { mutableStateOf(currentCategory) }
    var rememberForMerchant by remember { mutableStateOf(true) }

    AlertDialog(
        containerColor = Color(0xFF131929),
        shape = RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Expense Type",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFFF8FAFC)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select a category for \"$merchantName\":",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CategoryHelper.ALL_CATEGORIES) { cat ->
                        val isSelected = cat == selectedCat
                        val color = CategoryHelper.getCategoryColor(cat)
                        val icon = CategoryHelper.getCategoryIcon(cat)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCat = cat },
                            color = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF1A2338),
                            border = if (isSelected) BorderStroke(1.dp, color) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = cat,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Remember Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { rememberForMerchant = !rememberForMerchant }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = rememberForMerchant,
                        onCheckedChange = { rememberForMerchant = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF10B981),
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = "Remember this category for all \"$merchantName\" transactions",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCategorySelected(selectedCat, rememberForMerchant)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Category", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, merchant: String, type: String, account: String, category: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") }
    var selectedCategory by remember { mutableStateOf(CategoryHelper.FOOD) }

    AlertDialog(
        containerColor = Color(0xFF131929),
        shape = RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Log", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Selector Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = "DEBIT" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "DEBIT") Color(0xFFF43F5E) else Color(0xFF1E283E)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Expense", color = Color.White)
                    }
                    Button(
                        onClick = { selectedType = "CREDIT" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "CREDIT") Color(0xFF10B981) else Color(0xFF1E283E)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Income", color = Color.White)
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1E283E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1E283E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector Carousel
                Column {
                    Text("Category:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryHelper.ALL_CATEGORIES.forEach { cat ->
                            val isSel = cat == selectedCategory
                            val col = CategoryHelper.getCategoryColor(cat)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) col.copy(alpha = 0.2f) else Color(0xFF1E283E),
                                border = if (isSel) BorderStroke(1.dp, col) else null,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Account (e.g. 1234 or Cash)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1E283E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && merchant.isNotBlank()) {
                        onConfirm(amountVal, merchant, selectedType, account.ifBlank { "Cash" }, selectedCategory)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Record", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

fun formatCurrency(amount: Double): String {
    val format = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount).replace("INR", "₹").replace("Rs.", "₹").trim()
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
