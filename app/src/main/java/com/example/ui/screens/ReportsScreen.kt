package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.MemberColorPalette
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserRole
import com.example.data.model.parseHexColor
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CategoryPieChart
import com.example.ui.screens.SensitivityAuthDialog
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.JointPurple
import com.example.ui.theme.OceanTeal
import com.example.ui.viewmodel.MonthSummary
import com.example.util.CurrencyFormatter
import com.example.util.GoalFeasibilityInsight
import com.example.util.XlsxExportManager

@Composable
fun ReportsScreen(
    summary: MonthSummary,
    transactions: List<TransactionEntity>,
    recurringRules: List<RecurringRuleEntity> = emptyList(),
    budgets: List<CategoryBudgetEntity> = emptyList(),
    goals: List<FinancialGoalEntity> = emptyList(),
    goalInsights: List<GoalFeasibilityInsight> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    onExportCsv: () -> String,
    onResetDatabase: (Boolean) -> Unit = {},
    currentUserRole: UserRole = UserRole.ADMIN,
    expectedPin: String = "",
    onSetPin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showAuthDialogForReset by remember { mutableStateOf(false) }

    // Calculations
    val expenseTx = transactions.filter { it.type == "EXPENSE" }
    val totalExpense = expenseTx.sumOf { it.amount }

    // Top Categories
    val topCategories = expenseTx
        .groupBy { it.category }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Top Merchants
    val topMerchants = expenseTx
        .groupBy { it.merchant.ifBlank { it.category } }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Room Database") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This feature clears all Room database tables while keeping the schema completely intact, allowing for a fresh start for your manual data entry.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Baseline Reset: Clears transactions and recurring rules while restoring clean profiles & 0-balance account envelopes ready for data entry.\n• Complete Wipe: Clears every table without reseeding.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            onResetDatabase(true)
                            showResetDialog = false
                            Toast.makeText(context, "Database reset! Clean schema ready for manual entry.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Database (Fresh Start)")
                    }
                    OutlinedButton(
                        onClick = {
                            onResetDatabase(false)
                            showResetDialog = false
                            Toast.makeText(context, "All tables wiped clean.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wipe All Tables Completely")
                    }
                    TextButton(
                        onClick = { showResetDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            },
            dismissButton = null
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clean Title Header (Without crammed buttons)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Financial Reports & Insights",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${summary.displayMonth} • Household Overview & Analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category Expense Distribution Interactive Pie Chart
        item {
            CategoryPieChart(
                categoryExpenses = topCategories,
                totalExpense = totalExpense,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_expense_pie_chart")
            )
        }

        // Generalized Household Member Spending Distribution Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_spending_distribution_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Household Member Spending Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val memberSpendEntries = if (expenseTx.isNotEmpty()) {
                        expenseTx
                            .groupBy { it.paidBy.uppercase().trim() }
                            .mapValues { it.value.sumOf { tx -> tx.amount } }
                            .toList()
                            .sortedByDescending { it.second }
                    } else {
                        emptyList()
                    }

                    if (memberSpendEntries.isEmpty()) {
                        Text(
                            text = "No expenses recorded this month yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Multi-Segment Progress Bar
                        if (totalExpense > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                            ) {
                                memberSpendEntries.forEach { (memberKey, amount) ->
                                    val ratio = (amount / totalExpense).toFloat().coerceAtLeast(0.02f)
                                    val col = when (memberKey) {
                                        "JOINT" -> JointPurple
                                        else -> {
                                            val hash = Math.abs(memberKey.hashCode()) % MemberColorPalette.size
                                            parseHexColor(MemberColorPalette[hash].first, OceanTeal)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(ratio)
                                            .height(14.dp)
                                            .background(col)
                                    )
                                }
                            }
                        }

                        // Member breakdown rows
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            memberSpendEntries.forEach { (memberKey, amount) ->
                                val ratio = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                                val col = when (memberKey) {
                                    "JOINT" -> JointPurple
                                    else -> {
                                        val hash = Math.abs(memberKey.hashCode()) % MemberColorPalette.size
                                        parseHexColor(MemberColorPalette[hash].first, OceanTeal)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(col))
                                        Text(
                                            text = memberKey.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = "${CurrencyFormatter.formatInr(amount)} (${(ratio * 100).toInt()}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Spending Categories
        if (topCategories.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Top Spending Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        topCategories.take(6).forEachIndexed { index, (category, amount) ->
                            val ratio = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                            val medal = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${index + 1}"
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = medal, fontSize = 14.sp)
                                        CategoryIconBadge(categoryName = category, size = 28)
                                        Text(text = category, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatInr(amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Merchants
        if (topMerchants.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Top Merchants / Outlets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        topMerchants.forEach { (merchant, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = merchant, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(text = CurrencyFormatter.formatInr(amount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Section for Export CSV (Preserved cleanly as separate div / section for mobile phones)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_data_section")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Data Export & Spreadsheets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Multi-Sheet Excel (.xlsx) & CSV Supported",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Export your complete financial records into separate sheets (Commitments, Incomes, Goals & Tracking, Category Expenses, Accounts & Assets, and Full Transactions Ledger).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ledger size: ${transactions.size} records • ${recurringRules.size} rules • ${goals.size} goals",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (currentUserRole.canExportReports) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val xlsxFile = XlsxExportManager.exportToXlsx(
                                            context = context,
                                            summary = summary,
                                            transactions = transactions,
                                            recurringRules = recurringRules,
                                            budgets = budgets,
                                            goals = goals,
                                            goalInsights = goalInsights,
                                            accounts = accounts
                                        )
                                        XlsxExportManager.shareXlsx(context, xlsxFile)
                                        Toast.makeText(
                                            context,
                                            "Exported multi-sheet Excel file (${xlsxFile.name})!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("export_xlsx_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                            ) {
                                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Export Multi-Sheet Excel (.xlsx)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val csv = onExportCsv()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Household CFO CSV", csv)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        "Copied ${transactions.size} records to clipboard! Ready to paste into Excel.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("export_csv_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Copy CSV Text to Clipboard", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Export reports is reserved for household members and contributors (Your role: ${currentUserRole.displayName}).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Data Reset Section (Admin Only with Authentication)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("data_reset_section")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = ExpenseRed)
                        Text(
                            text = "Reset Database (Fresh Start)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Clears all Room database tables while preserving the schema, providing a fresh start for your manual data entry.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentUserRole.canResetDatabase) {
                        OutlinedButton(
                            onClick = { showAuthDialogForReset = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("reset_database_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Authenticate & Reset Database...", color = ExpenseRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Reset Database is restricted to Admin only. Your role is '${currentUserRole.displayName}'.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAuthDialogForReset) {
        SensitivityAuthDialog(
            title = "Admin Security Required",
            subtitle = "Authenticate with biometric or PIN to confirm resetting database tables.",
            expectedPin = expectedPin,
            onSetPin = onSetPin,
            onAuthenticated = {
                showAuthDialogForReset = false
                showResetDialog = true
            },
            onDismiss = {
                showAuthDialogForReset = false
            }
        )
    }
}
