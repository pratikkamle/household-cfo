package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.HouseholdMember
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTransactionEntity

enum class RecurringItemType(val label: String, val categoryDefault: String, val type: String, val nature: String) {
    INCOME("Income / Salary", "Salary", "INCOME", "CORE_COMMITMENT"),
    LOAN_EMI("Loan / EMI", "Home Loan / EMI", "EXPENSE", "CORE_COMMITMENT"),
    SIP_WEALTH("SIP / Wealth", "Mutual Fund SIP", "INVESTMENT", "INVESTMENT"),
    INSURANCE_BILL("Insurance / Bill", "Insurance (LIC)", "EXPENSE", "CORE_COMMITMENT"),
    OTHER("Other", "Other Commitment", "EXPENSE", "CORE_COMMITMENT")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringSheet(
    sheetState: SheetState,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSaveRecurring: (RecurringTransactionEntity) -> Unit,
    initialRule: RecurringTransactionEntity? = null
) {
    val isEditing = initialRule != null

    val initialType = remember(initialRule) {
        if (initialRule == null) RecurringItemType.LOAN_EMI
        else when {
            initialRule.type == "INCOME" -> RecurringItemType.INCOME
            initialRule.nature == "INVESTMENT" -> RecurringItemType.SIP_WEALTH
            initialRule.category.contains("Insurance", ignoreCase = true) || initialRule.category.contains("LIC", ignoreCase = true) -> RecurringItemType.INSURANCE_BILL
            initialRule.category.contains("Loan", ignoreCase = true) || initialRule.category.contains("EMI", ignoreCase = true) -> RecurringItemType.LOAN_EMI
            else -> RecurringItemType.OTHER
        }
    }

    var title by remember(initialRule) {
        mutableStateOf(initialRule?.title?.substringBefore(" (") ?: "")
    }
    var amountText by remember(initialRule) {
        mutableStateOf(initialRule?.let { if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString() } ?: "")
    }
    var selectedItemType by remember(initialRule) { mutableStateOf(initialType) }
    var customCategory by remember(initialRule) {
        mutableStateOf(if (initialType == RecurringItemType.OTHER) (initialRule?.category ?: "") else "")
    }
    var descriptionNote by remember(initialRule) {
        val note = if (initialRule != null && initialRule.title.contains("(") && initialRule.title.endsWith(")")) {
            initialRule.title.substringAfter("(").substringBeforeLast(")")
        } else ""
        mutableStateOf(note)
    }
    var paidBy by remember(initialRule) {
        val member = HouseholdMember.values().find { it.name.equals(initialRule?.paidBy, ignoreCase = true) } ?: HouseholdMember.PRATIK
        mutableStateOf(member)
    }
    var dueDay by remember(initialRule) { mutableIntStateOf(initialRule?.dueDay ?: 5) }
    var frequency by remember(initialRule) { mutableStateOf(initialRule?.frequency ?: "MONTHLY") }
    var paymentMethod by remember(initialRule) {
        val pm = PaymentMethod.values().find { it.name.equals(initialRule?.paymentMethod, ignoreCase = true) } ?: PaymentMethod.AUTO_DEBIT
        mutableStateOf(pm)
    }
    var selectedAccountId by remember(initialRule) {
        mutableStateOf(initialRule?.accountId ?: (accounts.firstOrNull()?.id ?: 1L))
    }

    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_recurring_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditing) "Edit Scheduled Commitment" else "Schedule Recurring Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditing) "Modify amount, due date or category details" else "Set up automated EMIs, SIPs, Incomes & Bills",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Recurring Item Type Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Commitment Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecurringItemType.values().forEach { itemType ->
                        FilterChip(
                            selected = selectedItemType == itemType,
                            onClick = { selectedItemType = itemType },
                            label = { Text(itemType.label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Custom category and description when OTHER is chosen
            if (selectedItemType == RecurringItemType.OTHER) {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text("Custom Category / Type") },
                    placeholder = { Text("e.g., Child School Fee, Subscription, Society Maintenance") },
                    modifier = Modifier.fillMaxWidth().testTag("recurring_custom_category_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Payee") },
                placeholder = {
                    Text(
                        when (selectedItemType) {
                            RecurringItemType.INCOME -> "e.g., Monthly Salary, Flat Rent"
                            RecurringItemType.LOAN_EMI -> "e.g., HDFC Home Loan, Car Loan"
                            RecurringItemType.SIP_WEALTH -> "e.g., Zerodha Nifty Index SIP"
                            RecurringItemType.INSURANCE_BILL -> "e.g., HDFC Ergo Health, LIC"
                            RecurringItemType.OTHER -> "e.g., DPS Annual School Fee, Netflix"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("recurring_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Optional Description for what the commitment is about
            OutlinedTextField(
                value = descriptionNote,
                onValueChange = { descriptionNote = it },
                label = { Text("Description / What this commitment is about (Optional)") },
                placeholder = { Text("e.g., Quarterly payment due on 5th, auto-debit from salary account") },
                modifier = Modifier.fillMaxWidth().testTag("recurring_description_input"),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("recurring_amount_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Member Selector: Primary / Partner / Joint
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Who Pays / Receives", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(HouseholdMember.PRIMARY, HouseholdMember.PARTNER, HouseholdMember.JOINT).forEach { member ->
                        FilterChip(
                            selected = paidBy == member,
                            onClick = { paidBy = member },
                            label = { Text(member.displayName, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Due Day of the Month (1 - 31)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dueDay.toString(),
                    onValueChange = {
                        val num = it.toIntOrNull() ?: 1
                        dueDay = num.coerceIn(1, 31)
                    },
                    label = { Text("Due Day of Month") },
                    modifier = Modifier.weight(1f).testTag("recurring_due_day_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                // Frequency: Monthly / Annual
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Frequency", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = frequency == "MONTHLY",
                            onClick = { frequency = "MONTHLY" },
                            label = { Text("Monthly", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = frequency == "ANNUAL",
                            onClick = { frequency = "ANNUAL" },
                            label = { Text("Annual", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Linked Account
            if (accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Deduct From / Credit To Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.take(3).forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            val isFormValid = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val userId = when (paidBy) {
                        HouseholdMember.PRATIK -> 1L
                        HouseholdMember.MANASVI -> 2L
                        else -> null
                    }
                    val finalPaymentMethod = if (selectedItemType.type == "INCOME") "Auto Credit" else paymentMethod.displayName
                    val finalCategory = if (selectedItemType == RecurringItemType.OTHER && customCategory.isNotBlank()) {
                        customCategory.trim()
                    } else {
                        selectedItemType.categoryDefault
                    }
                    val finalTitle = if (descriptionNote.isNotBlank()) {
                        "${title.trim()} (${descriptionNote.trim()})"
                    } else {
                        title.trim()
                    }
                    val entity = if (initialRule != null) {
                        initialRule.copy(
                            title = finalTitle,
                            amount = amount,
                            category = finalCategory,
                            paidBy = paidBy.name,
                            type = selectedItemType.type,
                            nature = selectedItemType.nature,
                            dueDay = dueDay,
                            frequency = frequency,
                            accountId = selectedAccount?.id,
                            accountName = selectedAccount?.name ?: "",
                            paymentMethod = finalPaymentMethod
                        )
                    } else {
                        RecurringTransactionEntity(
                            householdId = 1L,
                            userId = userId,
                            title = finalTitle,
                            amount = amount,
                            category = finalCategory,
                            paidBy = paidBy.name,
                            type = selectedItemType.type,
                            nature = selectedItemType.nature,
                            dueDay = dueDay,
                            frequency = frequency,
                            accountId = selectedAccount?.id,
                            accountName = selectedAccount?.name ?: "",
                            paymentMethod = finalPaymentMethod,
                            isActive = true
                        )
                    }
                    onSaveRecurring(entity)
                    onDismiss()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_recurring_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Schedule Commitment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
