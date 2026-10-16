package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AccountEntity
import com.example.data.model.MemberProfile
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed

data class AccountTypeMeta(
    val type: String,
    val label: String,
    val icon: ImageVector,
    val defaultInstitution: String,
    val defaultName: String
)

val AvailableAccountTypes = listOf(
    AccountTypeMeta("BANK", "Bank A/C", Icons.Default.AccountBalance, "HDFC Bank", "Salary Account"),
    AccountTypeMeta("CREDIT_CARD", "Credit Card", Icons.Default.CreditCard, "HDFC / ICICI", "Credit Card"),
    AccountTypeMeta("DEBIT_CARD", "Debit Card", Icons.Default.CreditCard, "State Bank of India", "Debit Card"),
    AccountTypeMeta("INVESTMENT", "Mutual Funds & Stocks", Icons.Default.TrendingUp, "Zerodha / AMC", "MF Portfolio"),
    AccountTypeMeta("LIC_INSURANCE", "LIC / Insurance", Icons.Default.Shield, "LIC of India", "Life Insurance Policy"),
    AccountTypeMeta("NPS", "NPS & Pension", Icons.Default.Savings, "NSDL / Protean", "NPS Tier-1"),
    AccountTypeMeta("EPF_PPF", "EPF / PPF", Icons.Default.Savings, "EPFO / Post Office", "PPF Account"),
    AccountTypeMeta("FIXED_DEPOSIT", "FD / RD", Icons.Default.AccountBalance, "Bank FD", "Fixed Deposit"),
    AccountTypeMeta("GOLD", "Gold / SGB", Icons.Default.Savings, "RBI SGB / Tanishq", "SGB Investment"),
    AccountTypeMeta("CASH", "Cash", Icons.Default.LocalAtm, "Cash", "Cash in Hand"),
    AccountTypeMeta("OTHER", "Other Scheme", Icons.Default.Savings, "Institution", "Asset / Scheme")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    initialAccount: AccountEntity? = null,
    members: List<MemberProfile> = emptyList(),
    onSave: (
        id: Long,
        name: String,
        holder: String,
        accountType: String,
        balance: Double,
        accountNumberMask: String,
        institution: String,
        notes: String,
        creditLimit: Double,
        billingCycleDay: Int?,
        paymentDueDateDay: Int?,
        statementDueAmount: Double,
        userId: Long?
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isEditing = initialAccount != null

    var selectedType by remember {
        mutableStateOf(initialAccount?.accountType ?: "BANK")
    }
    var name by remember {
        mutableStateOf(initialAccount?.name ?: "")
    }
    var institution by remember {
        mutableStateOf(initialAccount?.institution ?: "")
    }
    var balanceText by remember {
        mutableStateOf(if (initialAccount != null) initialAccount.balance.toString() else "0")
    }
    var accountMask by remember {
        mutableStateOf(initialAccount?.accountNumberMask ?: "")
    }
    var creditLimitText by remember {
        mutableStateOf(if (initialAccount != null && initialAccount.creditLimit > 0) initialAccount.creditLimit.toString() else "")
    }
    var billingCycleDayText by remember {
        mutableStateOf(initialAccount?.billingCycleDay?.toString() ?: "")
    }
    var paymentDueDateDayText by remember {
        mutableStateOf(initialAccount?.paymentDueDateDay?.toString() ?: "")
    }
    var statementDueAmountText by remember {
        mutableStateOf(if (initialAccount != null && initialAccount.statementDueAmount > 0) initialAccount.statementDueAmount.toString() else "")
    }
    var notes by remember {
        mutableStateOf(initialAccount?.notes ?: "")
    }

    // Holder selection
    val defaultHolder = initialAccount?.holder ?: members.firstOrNull()?.displayName ?: "ADMIN"
    var selectedHolderName by remember { mutableStateOf(defaultHolder) }
    var holderDropdownExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("add_edit_account_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Account / Asset" else "Add Account, Card or Scheme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Account Type Chips
                Text(
                    text = "Category / Asset Type",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvailableAccountTypes.forEach { meta ->
                        val isSelected = selectedType == meta.type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = meta.type
                                if (name.isBlank()) name = meta.defaultName
                                if (institution.isBlank()) institution = meta.defaultInstitution
                            },
                            label = { Text(meta.label, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = meta.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Account / Scheme Name *") },
                    placeholder = { Text("e.g. HDFC Salary A/C, LIC Tech Term, Zerodha MF") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Institution Field
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Bank / Institution / AMC") },
                    placeholder = { Text("e.g. HDFC Bank, LIC of India, SBI AMC, NSDL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_institution_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Account Holder Dropdown
                ExposedDropdownMenuBox(
                    expanded = holderDropdownExpanded,
                    onExpandedChange = { holderDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedHolderName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Holder / Owner") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = holderDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = holderDropdownExpanded,
                        onDismissRequest = { holderDropdownExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.displayName} (${m.role})") },
                                onClick = {
                                    selectedHolderName = m.displayName
                                    holderDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Joint / Household Reserve") },
                            onClick = {
                                selectedHolderName = "JOINT"
                                holderDropdownExpanded = false
                            }
                        )
                    }
                }

                // Balance & Card Mask Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = {
                            Text(
                                if (selectedType == "CREDIT_CARD") "Outstanding Due (₹)"
                                else "Current Balance / Value (₹)"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("account_balance_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = accountMask,
                        onValueChange = { accountMask = it },
                        label = { Text("Mask/Number") },
                        placeholder = { Text("••4092") },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("account_mask_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Credit Card Specific Fields: Credit Limit, Billing Cycle & Payment Due Date
                if (selectedType == "CREDIT_CARD") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { creditLimitText = it },
                            label = { Text("Total Credit Limit (₹)") },
                            placeholder = { Text("e.g. 150000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("credit_card_limit_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = billingCycleDayText,
                                onValueChange = {
                                    val num = it.filter { c -> c.isDigit() }
                                    if (num.isEmpty() || (num.toIntOrNull() ?: 0) in 1..31) {
                                        billingCycleDayText = num
                                    }
                                },
                                label = { Text("Bill Cycle Day") },
                                placeholder = { Text("e.g. 15") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("credit_card_cycle_day_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = paymentDueDateDayText,
                                onValueChange = {
                                    val num = it.filter { c -> c.isDigit() }
                                    if (num.isEmpty() || (num.toIntOrNull() ?: 0) in 1..31) {
                                        paymentDueDateDayText = num
                                    }
                                },
                                label = { Text("Payment Due Day") },
                                placeholder = { Text("e.g. 5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("credit_card_due_day_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = statementDueAmountText,
                            onValueChange = { statementDueAmountText = it },
                            label = { Text("Current Statement Due Payment (₹)") },
                            placeholder = { Text("e.g. 12450 (Tallied into Cashflow)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("credit_card_due_amount_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Payment due dates trigger automatic bill reminders & tally into the monthly cashflow calculation.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Scheme Details (Optional)") },
                    placeholder = { Text("e.g. Maturity Year 2035, Monthly SIP ₹5,000, 5% Cashback") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                errorMessage?.let { err ->
                    Text(text = err, color = ExpenseRed, fontSize = 12.sp)
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && onDelete != null && initialAccount != null) {
                        TextButton(
                            onClick = {
                                onDelete(initialAccount.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed),
                            modifier = Modifier.testTag("btn_delete_account")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    errorMessage = "Please enter an account or scheme name."
                                    return@Button
                                }
                                val bal = balanceText.toDoubleOrNull() ?: 0.0
                                val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                                val cycleDay = billingCycleDayText.toIntOrNull()
                                val dueDay = paymentDueDateDayText.toIntOrNull()
                                val statementDue = statementDueAmountText.toDoubleOrNull() ?: if (selectedType == "CREDIT_CARD" && bal > 0) bal else 0.0
                                val matchedUser = members.find { it.displayName.equals(selectedHolderName, ignoreCase = true) }

                                onSave(
                                    initialAccount?.id ?: 0L,
                                    name.trim(),
                                    selectedHolderName.trim().uppercase(),
                                    selectedType,
                                    bal,
                                    accountMask.trim(),
                                    institution.trim(),
                                    notes.trim(),
                                    limit,
                                    cycleDay,
                                    dueDay,
                                    statementDue,
                                    matchedUser?.userId
                                )
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_save_account")
                        ) {
                            Text(if (isEditing) "Save Changes" else "Add Asset")
                        }
                    }
                }
            }
        }
    }
}
