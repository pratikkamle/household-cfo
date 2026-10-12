package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryMeta(
    val name: String,
    val group: String, // "Essential", "Lifestyle", "Financial", "Income"
    val icon: ImageVector,
    val defaultNature: ExpenseNature
)

object CategoryConstants {
    val ESSENTIAL = "Essential"
    val LIFESTYLE = "Lifestyle"
    val FINANCIAL = "Financial"
    val INCOME = "Income"

    val ALL_CATEGORIES = listOf(
        // Lifestyle (Discretionary)
        CategoryMeta("Food & Dining", LIFESTYLE, Icons.Default.Fastfood, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Groceries", ESSENTIAL, Icons.Default.ShoppingCart, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Fuel & Travel", LIFESTYLE, Icons.Default.DirectionsCar, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Shopping", LIFESTYLE, Icons.Default.ShoppingBag, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Entertainment", LIFESTYLE, Icons.Default.Movie, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Travel & Holidays", LIFESTYLE, Icons.Default.Flight, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Personal Care", LIFESTYLE, Icons.Default.Spa, ExpenseNature.DISCRETIONARY),
        CategoryMeta("Gifts & Social", LIFESTYLE, Icons.Default.CardGiftcard, ExpenseNature.DISCRETIONARY),

        // Essential Core Commitments
        CategoryMeta("Home Loan / EMI", ESSENTIAL, Icons.Default.Home, ExpenseNature.CORE_COMMITMENT),
        CategoryMeta("Personal Loan / EMI", ESSENTIAL, Icons.Default.AccountBalance, ExpenseNature.CORE_COMMITMENT),
        CategoryMeta("Utilities & Bills", ESSENTIAL, Icons.Default.Receipt, ExpenseNature.CORE_COMMITMENT),
        CategoryMeta("Medical & Healthcare", ESSENTIAL, Icons.Default.LocalHospital, ExpenseNature.CORE_COMMITMENT),
        CategoryMeta("Insurance (LIC)", ESSENTIAL, Icons.Default.Security, ExpenseNature.CORE_COMMITMENT),

        // Financial Investments
        CategoryMeta("Mutual Fund SIP", FINANCIAL, Icons.Default.Savings, ExpenseNature.INVESTMENT),
        CategoryMeta("NPS Retirement", FINANCIAL, Icons.Default.LocalAtm, ExpenseNature.INVESTMENT),
        CategoryMeta("Loan Prepayment", FINANCIAL, Icons.Default.AccountBalance, ExpenseNature.INVESTMENT),

        // Income
        CategoryMeta("Salary", INCOME, Icons.Default.Work, ExpenseNature.CORE_COMMITMENT),
        CategoryMeta("Rental Income", INCOME, Icons.Default.Home, ExpenseNature.CORE_COMMITMENT),

        // Misc
        CategoryMeta("Miscellaneous", LIFESTYLE, Icons.Default.MoreHoriz, ExpenseNature.DISCRETIONARY)
    )

    fun getMeta(categoryName: String): CategoryMeta {
        return ALL_CATEGORIES.find { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryMeta(categoryName, LIFESTYLE, Icons.Default.MoreHoriz, ExpenseNature.DISCRETIONARY)
    }

    val QUICK_EXPENSE_CATEGORIES = listOf(
        "Food & Dining",
        "Groceries",
        "Fuel & Travel",
        "Shopping",
        "Home Loan / EMI",
        "Personal Loan / EMI",
        "Utilities & Bills",
        "Medical & Healthcare",
        "Entertainment",
        "Miscellaneous"
    )

    fun isCommitment(categoryName: String, nature: String = "", recurringRuleId: Long? = null, merchant: String = ""): Boolean {
        if (nature.equals("CORE_COMMITMENT", ignoreCase = true)) return true
        if (recurringRuleId != null && recurringRuleId > 0) return true
        val meta = getMeta(categoryName)
        if (meta.defaultNature == ExpenseNature.CORE_COMMITMENT) return true
        val c = categoryName.lowercase()
        val m = merchant.lowercase()
        return c.contains("loan") || c.contains("emi") || c.contains("rent") ||
               c.contains("insurance") || c.contains("lic") || c.contains("mortgage") ||
               m.contains("loan") || m.contains("emi") || m.contains("mortgage")
    }

    fun isInvestment(categoryName: String, type: String = "", nature: String = ""): Boolean {
        if (type.equals("INVESTMENT", ignoreCase = true) || nature.equals("INVESTMENT", ignoreCase = true)) return true
        val meta = getMeta(categoryName)
        if (meta.defaultNature == ExpenseNature.INVESTMENT) return true
        val c = categoryName.lowercase()
        return c.contains("sip") || c.contains("mutual fund") || c.contains("nps") || c.contains("investment")
    }

    val ALL_EXPENSE_CATEGORIES: List<String> = ALL_CATEGORIES.filter { it.group != INCOME }.map { it.name }
}
