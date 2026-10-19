package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.ui.components.RecurringCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    recurringRules: List<RecurringTransactionEntity>,
    accounts: List<AccountEntity>,
    onAddRule: (String, Double, String, String, Long, Long?, String, String) -> Unit,
    onMarkPaid: (RecurringTransactionEntity) -> Unit,
    onDeleteRule: (RecurringTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddRuleOpen by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddRuleOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Subscriptions & Fixed Bills",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Automated tracking for household utilities, mortgages, and services",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (recurringRules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recurring bills set up",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(recurringRules, key = { it.id }) { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                RecurringCard(
                                    rule = rule,
                                    onMarkPaid = { onMarkPaid(rule) }
                                )
                            }
                            IconButton(onClick = { ruleToDelete = rule }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddRuleOpen) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Utilities & Internet") }
        var frequency by remember { mutableStateOf("MONTHLY") }
        var nature by remember { mutableStateOf("NEED") }

        AlertDialog(
            onDismissRequest = { isAddRuleOpen = false },
            title = { Text("Add Recurring Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Bill / Service Title (e.g. Netflix, Gym)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NEED", "WANT").forEach { n ->
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
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0) {
                            val account = accounts.firstOrNull()
                            onAddRule(
                                title,
                                amt,
                                category,
                                frequency,
                                System.currentTimeMillis() + 86400000L * 15,
                                account?.id,
                                account?.name ?: "Primary Account",
                                nature
                            )
                        }
                        isAddRuleOpen = false
                    }
                ) {
                    Text("Save Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddRuleOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (ruleToDelete != null) {
        val r = ruleToDelete!!
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Bill") },
            text = { Text("Stop tracking recurring bill '${r.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRule(r)
                        ruleToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
