package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter

@Composable
fun DashboardScreen(
    summary: MonthSummary,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    recurringRules: List<RecurringTransactionEntity>,
    isSensitiveRevealed: Boolean,
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenCfoReview: () -> Unit,
    onOpenSmartDetect: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onMarkRecurringPaid: (RecurringTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAssets = remember(accounts) {
        accounts.filter { it.balance > 0 }.sumOf { it.balance }
    }
    val totalDebts = remember(accounts) {
        accounts.filter { it.balance < 0 }.sumOf { Math.abs(it.balance) }
    }
    val netWorth = totalAssets - totalDebts

    val pieSlices = remember(transactions) {
        val expenseTx = transactions.filter { it.type == "EXPENSE" }
        val categoryTotals = expenseTx.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val palette = listOf(
            Emerald600, OceanTeal, JointPurple, UpcomingOrange, GoldAccent,
            ExpenseRed, InvestmentBlue, Color(0xFF0284C7), Color(0xFFD946EF)
        )
        var colorIdx = 0
        categoryTotals.map { (cat, sum) ->
            val color = palette[colorIdx % palette.size]
            colorIdx++
            PieSliceData(label = cat, amount = sum, color = color)
        }.sortedByDescending { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Hero Net Worth & Monthly Cashflow Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Household Net Worth",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.format(netWorth),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly Income",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = CurrencyFormatter.format(summary.totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                        Column {
                            Text(
                                text = "Monthly Spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = CurrencyFormatter.format(summary.totalExpense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                        Column {
                            Text(
                                text = "Net Savings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = CurrencyFormatter.format(summary.netSavings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.netSavings >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "Add Expense",
                    icon = Icons.Default.Add,
                    color = ExpenseRed,
                    onClick = onOpenAddExpense,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Add Income",
                    icon = Icons.Default.ArrowDownward,
                    color = IncomeGreen,
                    onClick = onOpenAddIncome,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "CFO Advisor",
                    icon = Icons.Default.AutoGraph,
                    color = JointPurple,
                    onClick = onOpenCfoReview,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Smart Scan",
                    icon = Icons.Default.DocumentScanner,
                    color = OceanTeal,
                    onClick = onOpenSmartDetect,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 50/30/20 Rule Breakdown Cards
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "50/30/20 Budget Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BudgetPillarMiniCard(
                            label = "Needs (50%)",
                            spent = summary.needsSpend,
                            color = OceanTeal,
                            modifier = Modifier.weight(1f)
                        )
                        BudgetPillarMiniCard(
                            label = "Wants (30%)",
                            spent = summary.wantsSpend,
                            color = RosePink,
                            modifier = Modifier.weight(1f)
                        )
                        BudgetPillarMiniCard(
                            label = "Savings (20%)",
                            spent = summary.netSavings.coerceAtLeast(0.0),
                            color = IncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Category Spend Pie Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spending Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CategoryPieChart(
                        slices = pieSlices,
                        totalAmount = summary.totalExpense
                    )
                }
            }
        }

        // Upcoming Bills & Subscriptions
        item {
            val pendingRules = recurringRules.filter { it.isActive }.take(4)
            if (pendingRules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Recurring Bills",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    pendingRules.forEach { rule ->
                        RecurringCard(
                            rule = rule,
                            onMarkPaid = { onMarkRecurringPaid(rule) }
                        )
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onViewAllTransactions) {
                        Text("View All")
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val recent = transactions.take(5)
                    if (recent.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            recent.forEachIndexed { index, tx ->
                                TransactionRow(
                                    transaction = tx,
                                    onClick = onViewAllTransactions
                                )
                                if (index < recent.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BudgetPillarMiniCard(
    label: String,
    spent: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.format(spent),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
