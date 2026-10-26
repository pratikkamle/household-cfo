package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryConstants
import com.example.ui.components.SensitiveText
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyFormatter

@Composable
fun BudgetScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val monthSummary by viewModel.monthSummary.collectAsState()
    val isRevealed by viewModel.isSensitiveRevealed.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.currentMonthAllTransactions.collectAsState()
    val household by viewModel.household.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val months = listOf("2026-10", "2026-09", "2026-08")

    // Calculate actual spending per category for selected month
    val categorySpending = remember(transactions) {
        transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector & Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Household Budget",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Monthly category limits & allocation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { viewModel.openEditBudgetPlan() },
                    modifier = Modifier.testTag("edit_budget_plan_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Plan")
                }
            }
        }

        // Month Selector Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(months) { m ->
                    val isSelected = m == selectedMonth
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedMonth(m) },
                        label = {
                            Text(
                                text = when (m) {
                                    "2026-10" -> "October 2026"
                                    "2026-09" -> "September 2026"
                                    else -> "August 2026"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Overall Budget Health Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overall_budget_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Discretionary Cap",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { viewModel.toggleSensitivity() }) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle privacy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    SensitiveText(
                        amount = monthSummary.monthlyDiscretionaryBudget,
                        isRevealed = isRevealed,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val totalSpent = monthSummary.totalExpense
                    val budgetCap = monthSummary.monthlyDiscretionaryBudget
                    val progress = if (budgetCap > 0) (totalSpent / budgetCap).toFloat().coerceIn(0f, 1f) else 0f
                    val isExceeded = totalSpent > budgetCap

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isExceeded) ExpenseRed else if (progress > 0.85f) WarningAmber else IncomeGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SensitiveText(
                                amount = totalSpent,
                                isRevealed = isRevealed,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isExceeded) "Over Budget By" else "Remaining Buffer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SensitiveText(
                                amount = if (isExceeded) totalSpent - budgetCap else budgetCap - totalSpent,
                                isRevealed = isRevealed,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isExceeded) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Category Allocations
        item {
            Text(
                text = "Category Budget Targets",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // List of Category Budgets (Explicitly NO recent transactions list)
        items(CategoryConstants.CATEGORIES) { cat ->
            val budgetEntity = budgets.firstOrNull { it.category == cat }
            val allocated = budgetEntity?.monthlyBudget ?: 0.0
            val spent = categorySpending[cat] ?: 0.0
            val progress = if (allocated > 0) (spent / allocated).toFloat().coerceIn(0f, 1f) else if (spent > 0) 1f else 0f
            val isOver = spent > allocated && allocated > 0
            val catColor = runCatching { Color(android.graphics.Color.parseColor(CategoryConstants.getColorHex(cat))) }.getOrDefault(PrimaryBlue)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_budget_$cat"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.take(2).uppercase(),
                                color = catColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (allocated > 0) {
                                    if (isOver) "Over budget by ${CurrencyFormatter.formatInr(spent - allocated, obscure = !isRevealed)}"
                                    else "${CurrencyFormatter.formatInr(allocated - spent, obscure = !isRevealed)} left"
                                } else "No budget allocated",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            SensitiveText(
                                amount = spent,
                                isRevealed = isRevealed,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of ${CurrencyFormatter.formatInr(allocated, obscure = !isRevealed)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isOver) ExpenseRed else if (progress > 0.8f) WarningAmber else catColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
