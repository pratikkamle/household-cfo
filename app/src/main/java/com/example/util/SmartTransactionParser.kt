package com.example.util

import com.example.data.model.CategoryConstants
import com.example.data.model.HouseholdMember
import com.example.data.model.PaymentMethod
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val category: String,
    val paymentMethod: PaymentMethod,
    val paidBy: HouseholdMember,
    val rawText: String,
    val confidenceNotes: String
)

object SmartTransactionParser {

    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:rs\\.?|inr|₹|debited by|spent|vpa)\\s*[:.]?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val ALT_AMOUNT_PATTERN = Pattern.compile(
        "([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|spent|paid)",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(text: String): ParsedTransaction? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        var amount: Double? = null

        // Try primary pattern
        val m1 = AMOUNT_PATTERN.matcher(clean)
        if (m1.find()) {
            val numStr = m1.group(1)?.replace(",", "")
            amount = numStr?.toDoubleOrNull()
        }

        // Try alt pattern
        if (amount == null) {
            val m2 = ALT_AMOUNT_PATTERN.matcher(clean)
            if (m2.find()) {
                val numStr = m2.group(1)?.replace(",", "")
                amount = numStr?.toDoubleOrNull()
            }
        }

        if (amount == null) {
            // General fallback: first occurrence of numbers
            val mFallback = Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)").matcher(clean)
            if (mFallback.find()) {
                amount = mFallback.group(1)?.toDoubleOrNull()
            }
        }

        if (amount == null || amount <= 0.0) return null

        val lower = clean.lowercase(Locale.ROOT)

        // Detect payment mode
        val paymentMethod = when {
            lower.contains("upi") || lower.contains("vpa") || lower.contains("gpay") || lower.contains("phonepe") -> PaymentMethod.UPI
            lower.contains("credit card") || lower.contains("card xx") || lower.contains("diners") || lower.contains("visa") || lower.contains("mastercard") -> PaymentMethod.CREDIT_CARD
            lower.contains("debit card") -> PaymentMethod.DEBIT_CARD
            lower.contains("net banking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps") -> PaymentMethod.NET_BANKING
            lower.contains("auto-debit") || lower.contains("ecs") || lower.contains("ach") -> PaymentMethod.AUTO_DEBIT
            else -> PaymentMethod.UPI
        }

        val paidBy = HouseholdMember.JOINT

        // Detect merchant
        var merchant = ""
        val atMatcher = Pattern.compile("(?:at|to|info/|vpa\\s+)\\s*([A-Za-z0-9&.\\- ]{2,30})", Pattern.CASE_INSENSITIVE).matcher(clean)
        if (atMatcher.find()) {
            merchant = atMatcher.group(1)?.trim()?.replace(Regex("(?i)(on|via|ref|bal|available).*"), "")?.trim() ?: ""
        }

        if (merchant.isBlank()) {
            // Check known merchants
            val known = listOf("Starbucks", "Swiggy", "Zomato", "Uber", "Ola", "Shell", "D-Mart", "Zara", "Amazon", "Flipkart", "Nature's Basket", "Blinkit", "Zepto", "Apollo Pharmacy", "Cult.fit")
            for (k in known) {
                if (lower.contains(k.lowercase(Locale.ROOT))) {
                    merchant = k
                    break
                }
            }
        }

        if (merchant.isBlank()) {
            merchant = "UPI Merchant"
        }

        // Infer Category
        val category = inferCategory(merchant, lower)

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            category = category,
            paymentMethod = paymentMethod,
            paidBy = paidBy,
            rawText = clean,
            confidenceNotes = "Auto-detected ₹$amount at $merchant via ${paymentMethod.displayName}"
        )
    }

    fun inferCategory(merchant: String, rawText: String = ""): String {
        val m = (merchant + " " + rawText).lowercase(Locale.ROOT)
        return when {
            m.contains("starbucks") || m.contains("swiggy") || m.contains("zomato") || m.contains("cafe") ||
                    m.contains("restaurant") || m.contains("mcdonald") || m.contains("burger") || m.contains("food") ||
                    m.contains("blue tokai") || m.contains("subway") || m.contains("pizza") -> "Food & Dining"

            m.contains("nature's basket") || m.contains("d-mart") || m.contains("dmart") || m.contains("grocery") ||
                    m.contains("blinkit") || m.contains("zepto") || m.contains("bigbasket") || m.contains("freshtohome") ||
                    m.contains("supermarket") -> "Groceries"

            m.contains("fuel") || m.contains("petrol") || m.contains("diesel") || m.contains("shell") ||
                    m.contains("hp") || m.contains("ioc") || m.contains("bpcl") || m.contains("uber") ||
                    m.contains("ola") || m.contains("metro") || m.contains("toll") -> "Fuel & Travel"

            m.contains("zara") || m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
                    m.contains("clothing") || m.contains("mall") || m.contains("h&m") || m.contains("uniqlo") -> "Shopping"

            m.contains("movie") || m.contains("pvr") || m.contains("inox") || m.contains("netflix") ||
                    m.contains("prime") || m.contains("hotstar") || m.contains("spotify") || m.contains("bookmyshow") -> "Entertainment"

            m.contains("hospital") || m.contains("pharmacy") || m.contains("apollo") || m.contains("1mg") ||
                    m.contains("clinic") || m.contains("doctor") || m.contains("med") -> "Medical & Healthcare"

            m.contains("electricity") || m.contains("bill") || m.contains("bescom") || m.contains("water") ||
                    m.contains("airtel") || m.contains("jio") || m.contains("broadband") || m.contains("gas") -> "Utilities & Bills"

            m.contains("flight") || m.contains("hotel") || m.contains("makemytrip") || m.contains("airbnb") ||
                    m.contains("indigo") || m.contains("vistara") -> "Travel & Holidays"

            m.contains("sip") || m.contains("mutual fund") || m.contains("zerodha") || m.contains("groww") -> "Mutual Fund SIP"
            m.contains("lic") || m.contains("insurance") || m.contains("hdi") -> "Insurance (LIC)"
            m.contains("home loan") || m.contains("emi") -> "Home Loan / EMI"

            else -> "Miscellaneous"
        }
    }
}
