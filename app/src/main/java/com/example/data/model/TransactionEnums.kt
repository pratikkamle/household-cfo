package com.example.data.model

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    INVESTMENT("Investment"),
    TRANSFER("Transfer")
}

enum class ExpenseNature(val displayName: String) {
    DISCRETIONARY("Random / Discretionary"),
    CORE_COMMITMENT("Core Commitment / Loan / Fixed"),
    INVESTMENT("Wealth / SIP / NPS")
}

enum class PaymentMethod(val displayName: String) {
    UPI("UPI"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    NET_BANKING("Net Banking"),
    AUTO_DEBIT("Auto Debit / ECS"),
    AUTO_CREDIT("Auto Credit"),
    CASH("Cash")
}
