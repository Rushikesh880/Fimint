package com.moneytracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_mappings")
data class MerchantCategoryMapping(
    @PrimaryKey val merchantKey: String, // Normalized: merchant.trim().uppercase()
    val category: String
)

