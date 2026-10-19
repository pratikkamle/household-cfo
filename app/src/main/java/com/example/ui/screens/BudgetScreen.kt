package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.MonthSummary
import com.example.data.model.TransactionEntity
import com.example.ui.components.GoalCard
import com.example.ui.theme.Emerald600
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.OceanTeal
import com.example.ui.theme.RosePink
import com.example.util.CurrencyFormatter

@Composable
fun BudgetScreen(
    summary: MonthSummary,
    budgets: List<CategoryBudgetEntity>,
    transactions: List<TransactionEntity>,
    goals: List<FinancialGoalEntity>,
    onSaveBudget: (String, Double, String) -> Unit,
    onAddGoal: (String, Double, Long, String) -> Unit,
    onAddGoalContribution: (FinancialGoalEntity, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingBudget by remember { mutableStateOf<CategoryBudgetEntity?>(null) }
    var isAddGoalOpen by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<FinancialGoalEntity?>(null) }

    val categorySpends = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // 50/30/20 Target vs Actual Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "50/30/20 Household Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val income = if (summary.totalIncome > 0) summary.totalIncome else 1.0
                    val needsPct = ((summary.needsSpend / income) * 100).toInt()
                    val wantsPct = ((summary.wantsSpend / income) * 100).toInt()
                    val savingsPct = (((summary.totalIncome - summary.totalExpense) / income) * 100).toInt().coerceAtLeast(0)

                    BudgetPillarRow(name = "Needs (Housing, Food, Bills)", target = "50%", current = "$needsPct%", color = OceanTeal)
                    Spacer(modifier = Modifier.height(8.dp))
                    BudgetPillarRow(name = "Wants (Dining, Fun, Shopping)", target = "30%", current = "$wantsPct%", color = RosePink)
                    Spacer(modifier = Modifier.height(8.dp))
                    BudgetPillarRow(name = "Savings & Debt Reduction", target = "20%", current = "$savingsPct%", color = Emerald600)
                }
            }
        }

        // Category Budgets Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budget Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        editingBudget = CategoryBudgetEntity(
                            category = "",
                            budgetAmount = 500.0,
                            nature = "NEED"
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Budget")
                }
            }
        }

        // Budget Items
        items(budgets, key = { it.category }) { budget ->
            val spent = categorySpends[budget.category] ?: 0.0
            val progress = if (budget.budgetAmount > 0) (spent / budget.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
            val isOver = spent > budget.budgetAmount

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = budget.category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Category Type: ${budget.nature}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { editingBudget = budget }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${CurrencyFormatter.format(spent)} spent",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Limit: ${CurrencyFormatter.format(budget.budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Financial Goals Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Household Financial Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { isAddGoalOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Goal")
                }
            }
        }

        // Goals List
        items(goals, key = { it.id }) { goal ->
            GoalCard(
                goal = goal,
                onClick = {},
                onContribute = { contributingGoal = goal }
            )
        }
    }

    // Edit Budget Dialog
    if (editingBudget != null) {
        val b = editingBudget!!
        var catName by remember { mutableStateOf(b.category) }
        var limitStr by remember { mutableStateOf(b.budgetAmount.toString()) }
        var nature by remember { mutableStateOf(b.nature) }

        AlertDialog(
            onDismissRequest = { editingBudget = null },
            title = { Text(if (b.category.isEmpty()) "Add Budget Category" else "Edit Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (b.category.isEmpty()) {
                        OutlinedTextField(
                            value = catName,
                            onValueChange = { catName = it },
                            label = { Text("Category Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = { Text("Monthly Budget ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NEED", "WANT", "SAVINGS").forEach { n ->
                            FilterChip(
                                selected = nature == n,
                                onClick = { nature = n },
                                label = { Text(n) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = limitStr.toDoubleOrNull() ?: 500.0
                        if (catName.isNotBlank()) {
                            onSaveBudget(catName, amt, nature)
                        }
                        editingBudget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBudget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Goal Dialog
    if (isAddGoalOpen) {
        var goalTitle by remember { mutableStateOf("") }
        var targetStr by remember { mutableStateOf("") }
        var goalCat by remember { mutableStateOf("Emergency Fund") }

        AlertDialog(
            onDismissRequest = { isAddGoalOpen = false },
            title = { Text("New Financial Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title (e.g. New Car, Vacation)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Target Amount ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = targetStr.toDoubleOrNull() ?: 1000.0
                        if (goalTitle.isNotBlank()) {
                            onAddGoal(goalTitle, amt, System.currentTimeMillis() + 86400000L * 180, goalCat)
                        }
                        isAddGoalOpen = false
                    }
                ) {
                    Text("Create Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddGoalOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Funds to Goal Dialog
    if (contributingGoal != null) {
        val g = contributingGoal!!
        var addAmountStr by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { contributingGoal = null },
            title = { Text("Contribute to ${g.title}") },
            text = {
                OutlinedTextField(
                    value = addAmountStr,
                    onValueChange = { addAmountStr = it },
                    label = { Text("Contribution Amount ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = addAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddGoalContribution(g, amt)
                        }
                        contributingGoal = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { contributingGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BudgetPillarRow(
    name: String,
    target: String,
    current: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Target: $target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Actual: $current",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
