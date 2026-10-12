package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryConstants
import com.example.data.model.MemberProfile
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    members: List<MemberProfile> = emptyList(),
    onSave: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type == "INCOME"

    var amountText by remember {
        mutableStateOf(if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString() else transaction.amount.toString())
    }
    var merchantText by remember { mutableStateOf(transaction.merchant) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var selectedPaidByName by remember { mutableStateOf(transaction.paidBy) }
    var selectedPaymentMethod by remember {
        mutableStateOf(
            PaymentMethod.entries.find { it.name.equals(transaction.paymentMethod, ignoreCase = true) } ?: PaymentMethod.UPI
        )
    }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var notesText by remember { mutableStateOf(transaction.notes) }
    var selectedTimestamp by remember { mutableLongStateOf(transaction.timestamp) }

    val incomeCategories = listOf("Salary", "Investment Return", "Bonus", "Rental", "Business", "Gift / Other", "Refund")
    val expenseCategories = CategoryConstants.ALL_EXPENSE_CATEGORIES

    val dateTimeFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()) }

    fun showDateTimePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val newCal = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute)
                        }
                        selectedTimestamp = newCal.timeInMillis
                    },
                    currentCal.get(Calendar.HOUR_OF_DAY),
                    currentCal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("edit_transaction_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isIncome) "Edit Income Entry" else "Edit Expense Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) IncomeGreen else ExpenseRed
                        )
                        Text(
                            text = "Update amount, category, payee, or add details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_tx_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Merchant / Payee / Source Field
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text(if (isIncome) "Source / From" else "Merchant / Store / Payee") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_tx_merchant_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val availableCategories = if (isIncome) incomeCategories else expenseCategories
                        availableCategories.forEach { cat ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                leadingIcon = {
                                    CategoryIconBadge(categoryName = cat, size = 18)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isIncome) IncomeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // Member Selection (Paid By / Credited To)
                if (members.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isIncome) "Earned / Credited To" else "Paid By Member",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            members.forEach { m ->
                                val isSelected = selectedPaidByName.equals(m.displayName, ignoreCase = true) ||
                                        selectedPaidByName.equals(m.id, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPaidByName = m.displayName },
                                    label = { Text(m.displayName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                // Payment Mode Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaymentMethod = method },
                                label = { Text(method.displayName, fontSize = 12.sp) },
                                leadingIcon = {
                                    val icon = when (method) {
                                        PaymentMethod.UPI -> Icons.Default.QrCode
                                        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> Icons.Default.CreditCard
                                        PaymentMethod.CASH -> Icons.Default.LocalAtm
                                        else -> Icons.Default.CreditCard
                                    }
                                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }

                // Account Selection
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Associated Account",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = selectedAccountId == acc.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAccountId = acc.id },
                                    label = { Text("${acc.name} (${acc.holder})", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                // Notes / Description
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Additional Comments") },
                    placeholder = { Text("Add comments or context for this transaction") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_tx_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                // Complete Human-Readable Date & Time Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Complete Date & Time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDateTimePicker() }
                            .testTag("edit_tx_date_picker_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = dateTimeFormat.format(Date(selectedTimestamp)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to modify exact date or time",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newAmount = amountText.toDoubleOrNull() ?: transaction.amount
                            val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            val newMonthYear = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                            val acc = accounts.find { it.id == selectedAccountId }

                            val updated = transaction.copy(
                                amount = newAmount,
                                merchant = merchantText.trim().ifBlank { selectedCategory },
                                category = selectedCategory,
                                paidBy = selectedPaidByName.trim().uppercase(Locale.ROOT),
                                paymentMethod = selectedPaymentMethod.name,
                                accountId = acc?.id ?: transaction.accountId,
                                accountName = acc?.name ?: transaction.accountName,
                                notes = notesText.trim(),
                                timestamp = selectedTimestamp,
                                monthYear = newMonthYear
                            )
                            onSave(updated)
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_edited_tx_btn")
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
