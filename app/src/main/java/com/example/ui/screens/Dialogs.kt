package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.MonthSummary
import com.example.data.model.ParsedTransaction
import com.example.util.CfoAdvisorEngine
import com.example.util.CurrencyFormatter

@Composable
fun AddExpenseDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long?, String, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries & Food") }
    var nature by remember { mutableStateOf("NEED") }
    var notes by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    val categories = listOf("Groceries & Food", "Housing & Mortgage", "Utilities & Internet", "Transportation", "Dining Out", "Entertainment", "Healthcare", "Shopping", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Store") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                nature = if (cat in listOf("Dining Out", "Entertainment", "Shopping")) "WANT" else "NEED"
                            },
                            label = { Text(cat.split(" ").first()) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                nature = if (cat in listOf("Dining Out", "Entertainment", "Shopping")) "WANT" else "NEED"
                            },
                            label = { Text(cat.split(" ").first()) }
                        )
                    }
                }
                Text("Expense Nature (50/30/20)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NEED", "WANT").forEach { n ->
                        FilterChip(
                            selected = nature == n,
                            onClick = { nature = n },
                            label = { Text(n) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val acc = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
                        onSave(amt, category, merchant, acc?.id, acc?.name ?: "Primary Account", nature, notes)
                    }
                    onDismiss()
                }
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddIncomeDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long?, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("Salary / Paycheck") }
    var category by remember { mutableStateOf("Salary") }
    var notes by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Income") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Income Amount ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source / Employer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val acc = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
                        onSave(amt, category, source, acc?.id, acc?.name ?: "Checking", notes)
                    }
                    onDismiss()
                }
            ) {
                Text("Save Income")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SmartDetectDialog(
    detectedTx: ParsedTransaction?,
    onDismiss: () -> Unit,
    onConfirm: (ParsedTransaction) -> Unit
) {
    if (detectedTx == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bank Alert Detected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Smart notification listener extracted the following transaction:")
                Text("Amount: ${CurrencyFormatter.format(detectedTx.amount)}")
                Text("Merchant: ${detectedTx.merchant}")
                Text("Category: ${detectedTx.category} (${detectedTx.nature})")
                Text("Type: ${detectedTx.type}")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(detectedTx)
                    onDismiss()
                }
            ) {
                Text("Add to Ledger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ignore")
            }
        }
    )
}

@Composable
fun CfoAdvisorDialog(
    summary: MonthSummary,
    accounts: List<AccountEntity>,
    goals: List<FinancialGoalEntity>,
    onDismiss: () -> Unit
) {
    val report = remember(summary, accounts, goals) {
        CfoAdvisorEngine.generateReport(summary, accounts, goals)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Intelligent CFO Advisor") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Financial Health Grade: ${report.healthGrade} (${report.healthScore}/100)")
                Text("Net Worth: ${CurrencyFormatter.format(report.netWorth)}")
                Text("Savings Runway: ${report.runwayMonths} months")
                HorizontalDivider()
                Text("Insights:", style = MaterialTheme.typography.titleSmall)
                report.keyInsights.forEach { Text("• $it") }
                if (report.actionItems.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Action Items:", style = MaterialTheme.typography.titleSmall)
                    report.actionItems.forEach { Text("✔ $it") }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
