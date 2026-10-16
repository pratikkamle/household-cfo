package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryConstants
import com.example.data.model.ExpenseNature
import com.example.data.model.HouseholdMember
import com.example.data.model.JointMemberProfile
import com.example.data.model.MemberProfile
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.util.SmartTransactionParser
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    accounts: List<AccountEntity>,
    suggestedFromHistory: TransactionEntity?,
    onMerchantChanged: (String) -> Unit,
    members: List<MemberProfile> = emptyList(),
    initialPaidByName: String? = null,
    onSave: (
        amount: Double,
        category: String,
        paidBy: String,
        paymentMethod: PaymentMethod,
        accountId: Long?,
        accountName: String,
        merchant: String,
        notes: String,
        timestamp: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayMembers = remember(members) {
        if (members.isNotEmpty()) {
            members + JointMemberProfile
        } else {
            listOf(JointMemberProfile)
        }
    }

    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedPaidByName by remember(displayMembers, initialPaidByName) {
        val defaultPayer = initialPaidByName?.takeIf { it.isNotBlank() }
            ?: displayMembers.firstOrNull()?.displayName
            ?: "Primary"
        mutableStateOf(defaultPayer)
    }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var selectedAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var notesText by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var userOverrodeCategory by remember { mutableStateOf(false) }

    // When a merchant from history is found, automatically apply learned category if user hasn't explicitly selected one
    androidx.compose.runtime.LaunchedEffect(suggestedFromHistory) {
        if (suggestedFromHistory != null && !userOverrodeCategory) {
            selectedCategory = suggestedFromHistory.category
            if (suggestedFromHistory.paidBy.isNotBlank()) {
                selectedPaidByName = suggestedFromHistory.paidBy
            }
            PaymentMethod.entries.find {
                it.name.equals(suggestedFromHistory.paymentMethod, ignoreCase = true)
            }?.let { selectedPaymentMethod = it }
        }
    }

    // Pre-select account when paidBy changes
    val currentAccount = accounts.find { it.id == selectedAccountId }
        ?: accounts.find { it.holder.equals(selectedPaidByName, ignoreCase = true) }
        ?: accounts.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
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
                        text = "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Record in 5–10 seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_add_expense_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Amount Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            placeholder = { Text("0", fontSize = 34.sp, color = MaterialTheme.colorScheme.outline) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .testTag("expense_amount_input")
                        )
                    }

                    // Quick increment chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf(100, 500, 1000, 2000).forEach { increment ->
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val cur = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = (cur + increment).toInt().toString()
                                    },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "+₹$increment",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Merchant / Store Input with auto-learn / suggestion
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Merchant / Store / Payee",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = {
                        merchantText = it
                        userOverrodeCategory = false
                        onMerchantChanged(it)
                        // auto-detect category on type as fallback if not learned
                        val autoCat = SmartTransactionParser.inferCategory(it)
                        if (autoCat != "Miscellaneous" && suggestedFromHistory == null) {
                            selectedCategory = autoCat
                        }
                    },
                    placeholder = { Text("e.g. Starbucks, Swiggy, Shell, D-Mart") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_merchant_input")
                )

                // History suggestion chip (Learns from previous entries!)
                if (suggestedFromHistory != null) {
                    Surface(
                        color = Emerald500.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = suggestedFromHistory.category
                                selectedPaidByName = suggestedFromHistory.paidBy
                                selectedPaymentMethod = PaymentMethod.entries.find {
                                    it.name.equals(suggestedFromHistory.paymentMethod, ignoreCase = true)
                                } ?: PaymentMethod.UPI
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Auto-suggested from previous: ${suggestedFromHistory.category} • ${suggestedFromHistory.paidBy} • ${suggestedFromHistory.paymentMethod}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Paid By (Dynamic Members: 1, 2, 3, 4+ or Joint)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Paid By (Household Member)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    displayMembers.forEach { member ->
                        val isSelected = selectedPaidByName.equals(member.displayName, ignoreCase = true) ||
                                         selectedPaidByName.equals(member.id, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPaidByName = member.displayName
                                    val acc = accounts.find {
                                        it.holder.equals(member.id, ignoreCase = true) ||
                                        it.holder.equals(member.displayName, ignoreCase = true)
                                    }
                                    if (acc != null) selectedAccountId = acc.id
                                },
                            color = if (isSelected) member.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(member.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.initial,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = member.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) member.color else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (suggestedFromHistory != null && selectedCategory == suggestedFromHistory.category) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Emerald500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Learned from history",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryConstants.QUICK_EXPENSE_CATEGORIES.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = category
                                userOverrodeCategory = true
                            },
                            label = { Text(category, fontSize = 12.sp) },
                            leadingIcon = {
                                CategoryIconBadge(categoryName = category, size = 20)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Payment Mode
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
                                    else -> Icons.Default.AccountBalance
                                }
                                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Associated Bank Account or Credit Card Selection
            if (accounts.isNotEmpty()) {
                val relevantAccounts = remember(selectedPaymentMethod, accounts) {
                    when (selectedPaymentMethod) {
                        PaymentMethod.CREDIT_CARD -> accounts.filter { it.accountType == "CREDIT_CARD" }
                        PaymentMethod.UPI, PaymentMethod.NET_BANKING -> accounts.filter { it.accountType in listOf("BANK", "CASH", "DEBIT_CARD") }
                        PaymentMethod.DEBIT_CARD -> accounts.filter { it.accountType in listOf("BANK", "DEBIT_CARD") }
                        else -> accounts
                    }.ifEmpty { accounts }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when (selectedPaymentMethod) {
                            PaymentMethod.CREDIT_CARD -> "Choose Credit Card"
                            PaymentMethod.UPI, PaymentMethod.NET_BANKING -> "Choose Bank Account / UPI Source"
                            else -> "Associated Account or Card"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relevantAccounts.forEach { acc ->
                            val isSelected = selectedAccountId == acc.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAccountId = acc.id },
                                label = {
                                    Text(
                                        text = "${acc.name} (${acc.holder})${if (acc.accountNumberMask.isNotBlank()) " • ${acc.accountNumberMask}" else ""}",
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (acc.accountType == "CREDIT_CARD") Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Optional Note
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes / Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Transaction Date & Time Selector
            val dateTimeFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()) }
            val isToday = remember(selectedTimestamp) {
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
            }

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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Transaction Date & Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDateTimePicker() }
                        .testTag("expense_date_picker_btn")
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = dateTimeFormat.format(Date(selectedTimestamp)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isToday) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Emerald500.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Today",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald500,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Tap to change day or time",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
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

            // Save Expense Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val acc = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
                        onSave(
                            amount,
                            selectedCategory,
                            selectedPaidByName,
                            selectedPaymentMethod,
                            acc?.id,
                            acc?.name ?: "Primary Account",
                            merchantText.ifBlank { selectedCategory },
                            notesText,
                            selectedTimestamp
                        )
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
