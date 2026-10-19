package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.ui.components.AccountCard
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    isSensitiveRevealed: Boolean,
    onToggleSensitivity: () -> Unit,
    onAddAccount: (String, String, Double, String, String, Boolean) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddAccountOpen by remember { mutableStateOf(false) }
    var selectedAccountForDetail by remember { mutableStateOf<AccountEntity?>(null) }

    val totalAssets = remember(accounts) {
        accounts.filter { it.balance > 0 }.sumOf { it.balance }
    }
    val totalDebts = remember(accounts) {
        accounts.filter { it.balance < 0 }.sumOf { Math.abs(it.balance) }
    }
    val netWorth = totalAssets - totalDebts

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddAccountOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Net Worth Summary Header
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Household Balance Sheet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = onToggleSensitivity) {
                                Icon(
                                    imageVector = if (isSensitiveRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Balance Privacy"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Assets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (!isSensitiveRevealed) "••••••" else CurrencyFormatter.format(totalAssets),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text("Total Liabilities", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (!isSensitiveRevealed) "••••••" else CurrencyFormatter.format(totalDebts),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Column {
                                Text("Net Worth", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (!isSensitiveRevealed) "••••••" else CurrencyFormatter.format(netWorth),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Accounts List
            items(accounts, key = { it.id }) { acc ->
                AccountCard(
                    account = acc,
                    isRevealed = isSensitiveRevealed,
                    onClick = { selectedAccountForDetail = acc }
                )
            }
        }
    }

    // Add Account Dialog
    if (isAddAccountOpen) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("CHECKING") }
        var balanceStr by remember { mutableStateOf("") }
        var institution by remember { mutableStateOf("") }
        var lastFour by remember { mutableStateOf("") }
        var isSensitive by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { isAddAccountOpen = false },
            title = { Text("Add Household Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g. Joint Checking)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Bank / Institution (e.g. Chase, Vanguard)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("Current Balance ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lastFour,
                        onValueChange = { if (it.length <= 4) lastFour = it },
                        label = { Text("Last 4 digits (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CHECKING", "SAVINGS", "CREDIT_CARD", "INVESTMENT").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.take(5)) }
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isSensitive,
                            onCheckedChange = { isSensitive = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hide balance behind privacy mode")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = balanceStr.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            onAddAccount(name, type, bal, institution, lastFour, isSensitive)
                        }
                        isAddAccountOpen = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddAccountOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Account Options / Delete Dialog
    if (selectedAccountForDetail != null) {
        val acc = selectedAccountForDetail!!
        AlertDialog(
            onDismissRequest = { selectedAccountForDetail = null },
            title = { Text(acc.name) },
            text = {
                Text("Type: ${acc.type}\nInstitution: ${acc.institution}\nBalance: ${CurrencyFormatter.format(acc.balance)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(acc)
                        selectedAccountForDetail = null
                    }
                ) {
                    Text("Delete Account", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAccountForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}
