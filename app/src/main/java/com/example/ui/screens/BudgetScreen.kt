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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.MemberProfile
import com.example.data.model.TransactionEntity
import com.example.data.model.UserRole
import com.example.ui.components.CategoryIconBadge
import com.example.ui.screens.EditTransactionDialog
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.UpcomingOrange
import androidx.compose.material.icons.filled.Tune
import com.example.util.CategoryBudgetScaler
import com.example.util.CurrencyFormatter

@Composable
fun BudgetScreen(
    budgets: List<CategoryBudgetEntity>,
    transactions: List<TransactionEntity>,
    onSaveBudget: (category: String, amount: Double) -> Unit,
    onOpenEditBudgetPlan: () -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    currentUserRole: UserRole = UserRole.ADMIN,
    monthlyDiscretionaryBudget: Double = 0.0,
    accounts: List<AccountEntity> = emptyList(),
    members: List<MemberProfile> = emptyList(),
    onUpdateTransaction: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingCategory by remember { mutableStateOf<CategoryBudgetEntity?>(null) }
    var editAmountText by remember { mutableStateOf("") }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    // Compute actual spending per category for this month
    val spendMap = transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val categoryBudgetSum = budgets.sumOf { it.monthlyBudget }
    val totalBudget = if (monthlyDiscretionaryBudget > 0) monthlyDiscretionaryBudget else if (categoryBudgetSum > 0) categoryBudgetSum else 30000.0

    // Ensure category allowances strictly divide totalBudget according to the percentage distribution of the 76,500 baseline
    val activeBudgets = remember(budgets, totalBudget) {
        if (totalBudget > 0 && (budgets.isEmpty() || kotlin.math.abs(categoryBudgetSum - totalBudget) > 1.0)) {
            CategoryBudgetScaler.scaleBudgets(totalBudget)
        } else {
            budgets
        }
    }

    val totalActual = activeBudgets.sumOf { b -> spendMap[b.category] ?: 0.0 }
    val totalRemaining = maxOf(0.0, totalBudget - totalActual)
    val overallProgress = if (totalBudget > 0) (totalActual / totalBudget).toFloat().coerceIn(0f, 1f) else 0f

    val isOverBudget = totalActual > totalBudget

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Household Budget Plan Admin Action Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("household_budget_plan_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Household Budget Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Admin can modify category targets and monthly budget limits. Syncs across all members.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentUserRole.canEditBudgets) {
                        Button(
                            onClick = onOpenEditBudgetPlan,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "View Only (${currentUserRole.displayName})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Discretionary Budget vs Actual",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Excel CFO Model Live Discipline & Health Check",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // CFO Budget Health Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isOverBudget) ExpenseRed else Emerald500,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isOverBudget) "Attention: Overspending" else "On Track: Disciplined Pace",
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) ExpenseRed else Emerald500,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "${(overallProgress * 100).toInt()}% utilized",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isOverBudget) ExpenseRed else Emerald500,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Budget", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatInr(totalBudget), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text(text = "Total Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatInr(totalActual), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                        }
                        Column {
                            Text(text = "Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatInr(totalRemaining), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald500)
                        }
                    }
                }
            }
        }

        // Category breakdown table/list
        item {
            Text(
                text = "Category Allowances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(activeBudgets, key = { it.category }) { budget ->
            val actual = spendMap[budget.category] ?: 0.0
            val remaining = budget.monthlyBudget - actual
            val progress = if (budget.monthlyBudget > 0) (actual / budget.monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
            val isCategoryOver = actual > budget.monthlyBudget

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryIconBadge(categoryName = budget.category, size = 36)
                            Column {
                                Text(
                                    text = budget.category,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Budget: ${CurrencyFormatter.formatInr(budget.monthlyBudget)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Spent: ${CurrencyFormatter.formatInr(actual)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCategoryOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isCategoryOver) "Over by ${CurrencyFormatter.formatInr(-remaining)}" else "Left: ${CurrencyFormatter.formatInr(remaining)}",
                                    fontSize = 11.sp,
                                    color = if (isCategoryOver) ExpenseRed else Emerald500,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (currentUserRole.canEditBudgets) {
                                IconButton(
                                    onClick = {
                                        editingCategory = budget
                                        editAmountText = budget.monthlyBudget.toInt().toString()
                                    },
                                    modifier = Modifier.size(32.dp).testTag("edit_budget_${budget.category}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Budget",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isCategoryOver) ExpenseRed else if (progress > 0.8f) UpcomingOrange else Emerald500,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        // Section for Budget Transactions Breakdown / Recent Transactions
        if (transactions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${transactions.size} records",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(transactions.take(20), key = { it.id }) { tx ->
                TransactionCard(
                    transaction = tx,
                    onEdit = { transactionToEdit = tx },
                    onDelete = { transactionToDelete = tx },
                    canDelete = currentUserRole.canAddTransactions,
                    canEdit = currentUserRole.canAddTransactions
                )
            }
        }
    }

    // Edit Transaction Dialog
    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            accounts = accounts,
            members = members,
            onSave = { updated ->
                onUpdateTransaction(updated)
                transactionToEdit = null
            },
            onDismiss = { transactionToEdit = null }
        )
    }

    // Delete confirmation for budget transactions
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${tx.merchant.ifBlank { tx.category }}' (${CurrencyFormatter.formatInr(tx.amount)})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(tx)
                        transactionToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Budget Dialog
    if (editingCategory != null) {
        val cat = editingCategory!!
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit Budget: ${cat.category}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Set monthly target budget (₹):")
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_budget_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = editAmountText.toDoubleOrNull() ?: cat.monthlyBudget
                        onSaveBudget(cat.category, newAmount)
                        editingCategory = null
                    },
                    modifier = Modifier.testTag("save_edited_budget_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
