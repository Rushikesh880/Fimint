package com.moneytracker.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryHelper {
    const val SALARY = "Salary"
    const val SELF_TRANSFER = "Self Transfer"
    const val FOOD = "Food & Dining"
    const val GROCERIES = "Groceries"
    const val SHOPPING = "Shopping"
    const val BILLS = "Bills & Utilities"
    const val TRAVEL = "Travel & Fuel"
    const val ENTERTAINMENT = "Entertainment"
    const val INVESTMENTS = "Investments"
    const val HEALTH = "Health & Medical"
    const val TRANSFERS = "Transfers & UPI"
    const val OTHER = "Other"

    val ALL_CATEGORIES = listOf(
        SALARY,
        SELF_TRANSFER,
        FOOD,
        GROCERIES,
        SHOPPING,
        BILLS,
        TRAVEL,
        ENTERTAINMENT,
        INVESTMENTS,
        HEALTH,
        TRANSFERS,
        OTHER
    )

    fun getCategory(merchant: String): String {
        val m = merchant.lowercase()
        return when {
            m.contains("salary") || m.contains("payroll") || m.contains("sal credit") ||
            m.contains("stipend") || m.contains("wages") || m.contains("bonus") ||
            m.contains("employer") -> SALARY

            m.contains("self") || m.contains("own acc") || m.contains("own a/c") ||
            m.contains("self transfer") || m.contains("to self") ||
            m.contains("from self") -> SELF_TRANSFER

            m.contains("swiggy") || m.contains("zomato") || m.contains("starbucks") ||
            m.contains("mcdonald") || m.contains("domino") || m.contains("pizza") ||
            m.contains("burger") || m.contains("kfc") || m.contains("cafe") ||
            m.contains("restaurant") || m.contains("chai") || m.contains("bakery") ||
            m.contains("diner") || m.contains("food") -> FOOD

            m.contains("blinkit") || m.contains("zepto") || m.contains("bigbasket") ||
            m.contains("instamart") || m.contains("grocery") || m.contains("mart") ||
            m.contains("supermarket") || m.contains("kirana") || m.contains("milk") ||
            m.contains("vegetable") || m.contains("fruits") -> GROCERIES

            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
            m.contains("meesho") || m.contains("reliance") || m.contains("dmart") ||
            m.contains("zara") || m.contains("h&m") || m.contains("shoppers") ||
            m.contains("croma") || m.contains("lifestyle") || m.contains("store") ||
            m.contains("mall") || m.contains("retail") || m.contains("fashion") -> SHOPPING

            m.contains("airtel") || m.contains("jio") || m.contains("vi ") ||
            m.contains("bescom") || m.contains("electricity") || m.contains("water") ||
            m.contains("gas") || m.contains("broadband") || m.contains("wifi") ||
            m.contains("bill") || m.contains("dth") || m.contains("recharge") ||
            m.contains("tata power") || m.contains("adani") -> BILLS

            m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
            m.contains("irctc") || m.contains("makemytrip") || m.contains("cleartrip") ||
            m.contains("indigo") || m.contains("metro") || m.contains("toll") ||
            m.contains("petrol") || m.contains("fuel") ||
            m.contains("indian oil") || m.contains("hpcl") || m.contains("bpcl") ||
            m.contains("shell") -> TRAVEL

            m.contains("netflix") || m.contains("spotify") || m.contains("prime") ||
            m.contains("hotstar") || m.contains("bookmyshow") || m.contains("cinema") ||
            m.contains("pvr") || m.contains("inox") || m.contains("youtube") ||
            m.contains("movie") || m.contains("game") -> ENTERTAINMENT

            m.contains("zerodha") || m.contains("groww") || m.contains("upstox") ||
            m.contains("mutual") || m.contains("sip") || m.contains("angel") ||
            m.contains("coin") || m.contains("navi") || m.contains("stock") ||
            m.contains("invest") || m.contains("shares") -> INVESTMENTS

            m.contains("apollo") || m.contains("netmeds") || m.contains("1mg") ||
            m.contains("pharma") || m.contains("pharmacy") || m.contains("hospital") ||
            m.contains("clinic") || m.contains("doctor") || m.contains("med") ||
            m.contains("dentist") || m.contains("diagnostic") -> HEALTH

            m.contains("upi") || m.contains("transfer") || m.contains("bank") ||
            m.contains("imps") || m.contains("neft") || m.contains("vpa") ||
            m.contains("p2p") || m.contains("cash") -> TRANSFERS

            else -> OTHER
        }
    }

    fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            SALARY -> Icons.Default.Payments
            SELF_TRANSFER -> Icons.Default.SyncAlt
            FOOD -> Icons.Default.Restaurant
            GROCERIES -> Icons.Default.LocalGroceryStore
            SHOPPING -> Icons.Default.ShoppingCart
            BILLS -> Icons.AutoMirrored.Filled.ReceiptLong
            TRAVEL -> Icons.Default.DirectionsCar
            ENTERTAINMENT -> Icons.Default.Movie
            INVESTMENTS -> Icons.AutoMirrored.Filled.TrendingUp
            HEALTH -> Icons.Default.LocalHospital
            TRANSFERS -> Icons.Default.SwapHoriz
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(category: String): Color {
        return when (category) {
            SALARY -> Color(0xFF00E676)         // Mint Emerald
            SELF_TRANSFER -> Color(0xFF8B5CF6)  // Bright Violet
            FOOD -> Color(0xFFF59E0B)           // Amber
            GROCERIES -> Color(0xFF10B981)      // Emerald
            SHOPPING -> Color(0xFFEC4899)       // Pink / Fuchsia
            BILLS -> Color(0xFF06B6D4)          // Cyan
            TRAVEL -> Color(0xFF6366F1)         // Indigo
            ENTERTAINMENT -> Color(0xFFD946EF)  // Magenta
            INVESTMENTS -> Color(0xFF14B8A6)    // Teal
            HEALTH -> Color(0xFFF43F5E)         // Rose
            TRANSFERS -> Color(0xFF3B82F6)      // Blue
            else -> Color(0xFF64748B)           // Slate
        }
    }
}
