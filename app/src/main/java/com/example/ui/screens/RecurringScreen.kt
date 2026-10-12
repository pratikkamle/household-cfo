package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.data.model.AccountEntity
import com.example.data.model.MemberProfile
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.MemberBadge
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.UpcomingOrange
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    recurringRules: List<RecurringTransactionEntity>,
    transactionsForMonth: List<TransactionEntity>,
    monthYear: String,
    accounts: List<AccountEntity> = emptyList(),
    members: List<MemberProfile> = emptyList(),
    selectedMemberName: String? = null,
    onAddRecurring: (RecurringTransactionEntity) -> Unit = {},
    onUpdateRecurring: (RecurringTransactionEntity) -> Unit = {},
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit = {},
    onMarkPaid: (RecurringTransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "LOANS", "INVESTMENTS", "INCOME", "BILLS"
    var isAmountsVisible by remember { mutableStateOf(false) }
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var ruleToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val paidRuleIds = transactionsForMonth.mapNotNull { it.recurringRuleId }.toSet()

    val filteredRules = when (selectedTab) {
        "LOANS" -> recurringRules.filter { it.category.contains("Loan") || it.category.contains("EMI") }
        "INVESTMENTS" -> recurringRules.filter { it.nature == "INVESTMENT" || it.type == "INVESTMENT" }
        "INCOME" -> recurringRules.filter { it.type == "INCOME" }
        "BILLS" -> recurringRules.filter { it.category.contains("Insurance") || it.category.contains("Bill") || it.category.contains("Rent") }
        else -> recurringRules
    }

    // Add Commitment Sheet
    if (isAddSheetOpen) {
        AddRecurringSheet(
            sheetState = sheetState,
            accounts = accounts,
            members = members,
            initialPaidByName = selectedMemberName,
            onDismiss = { isAddSheetOpen = false },
            onSaveRecurring = { newRule ->
                onAddRecurring(newRule)
                isAddSheetOpen = false
            }
        )
    }

    // Edit Commitment Sheet
    if (ruleToEdit != null) {
        AddRecurringSheet(
            sheetState = sheetState,
            accounts = accounts,
            members = members,
            initialPaidByName = selectedMemberName,
            initialRule = ruleToEdit,
            onDismiss = { ruleToEdit = null },
            onSaveRecurring = { updatedRule ->
                onUpdateRecurring(updatedRule)
                ruleToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (ruleToDelete != null) {
        val target = ruleToDelete!!
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = {
                Text(
                    text = "Remove Commitment?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${target.title}' (${CurrencyFormatter.formatInr(target.amount)})? It will not be automated in upcoming months.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecurring(target)
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_delete_recurring_btn")
                ) {
                    Text("Delete Commitment")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("recurring_screen")
    ) {
        // Fixed Top 1/3rd Section (Consistent summary cards and filter buttons)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header with Title, Eye Toggle, and "Schedule" button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            Text(
                                text = "Scheduled Commitments",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automated Loans, SIPs, Insurance & Salaries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { isAmountsVisible = !isAmountsVisible },
                            modifier = Modifier.size(38.dp).testTag("recurring_toggle_visibility")
                        ) {
                            Icon(
                                imageVector = if (isAmountsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isAmountsVisible) "Hide Scheduled Amounts" else "View Scheduled Amounts",
                                tint = if (isAmountsVisible) Emerald500 else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Button(
                            onClick = { isAddSheetOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp).testTag("open_add_recurring_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Schedule", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Filter Chips Row for all commitment types
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to "All Commitments",
                        "LOANS" to "Loans & EMI",
                        "INVESTMENTS" to "SIP & Wealth",
                        "INCOME" to "Salary & Income",
                        "BILLS" to "Bills & Insurance"
                    )
                    filterOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedTab == key,
                            onClick = { selectedTab = key },
                            shape = RoundedCornerShape(10.dp),
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == key) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        // Below: Dedicated Scrollable Section to view scheduled commitments
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            if (filteredRules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No commitments scheduled yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Tap the 'Schedule' button above to add recurring Home Loans, Personal EMIs, Mutual Fund SIPs, or Monthly Salaries.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredRules, key = { it.id }) { rule ->
                    val isPaid = paidRuleIds.contains(rule.id)
                    RecurringRuleCard(
                        rule = rule,
                        isPaid = isPaid,
                        isAmountsVisible = isAmountsVisible,
                        onEdit = { ruleToEdit = rule },
                        onDelete = { ruleToDelete = rule }
                    )
                }
            }
        }
    }
}

@Composable
fun RecurringRuleCard(
    rule: RecurringTransactionEntity,
    isPaid: Boolean,
    isAmountsVisible: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title and Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryIconBadge(categoryName = rule.category, size = 42)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MemberBadge(memberId = rule.paidBy)
                            Text(
                                text = "• ${rule.category}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• ${rule.frequency.lowercase().replaceFirstChar { it.uppercase() }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = if (isAmountsVisible) CurrencyFormatter.formatInr(rule.amount) else "₹ ••••••••",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = when (rule.type) {
                        "INCOME" -> IncomeGreen
                        "INVESTMENT" -> InvestmentBlue
                        else -> ExpenseRed
                    }
                )
            }

            // Automated Schedule Information & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dayStr = if (rule.dueDay >= 28) "Last day of month" else CurrencyFormatter.formatDayOfMonth(rule.dueDay)
                val scheduleText = when (rule.type) {
                    "INCOME" -> "Auto-credit on $dayStr • ${rule.paymentMethod}"
                    else -> "Auto-debit on $dayStr • ${rule.paymentMethod}"
                }

                Text(
                    text = scheduleText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )

                if (isPaid) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald500.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Emerald500, modifier = Modifier.size(13.dp))
                            Text(text = "Auto-Processed", fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                            Text(text = "Auto-Scheduled", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action Buttons: Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Automated commitment",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("edit_recurring_rule_${rule.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed),
                        modifier = Modifier.height(32.dp).testTag("delete_recurring_rule_${rule.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", fontSize = 12.sp, color = ExpenseRed)
                    }
                }
            }
        }
    }
}
