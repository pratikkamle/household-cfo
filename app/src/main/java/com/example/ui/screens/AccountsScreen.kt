package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.AccountEntity
import com.example.ui.components.SensitiveText
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CreditCardBillReminderHelper
import com.example.util.CurrencyFormatter

@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    onOpenAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val isRevealed by viewModel.isSensitiveRevealed.collectAsState()
    val activeRole by viewModel.activeUserRole.collectAsState()

    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val bankAccounts = remember(accounts) { accounts.filter { it.type == "BANK" || it.type == "SAVINGS" || it.type == "CHECKING" } }
    val creditCards = remember(accounts) { accounts.filter { it.type == "CREDIT_CARD" } }
    val cashWallets = remember(accounts) { accounts.filter { it.type == "CASH" || it.type == "WALLET" } }

    val totalAssets = remember(accounts) {
        accounts.filter { it.type != "CREDIT_CARD" }.sumOf { it.balance }
    }
    val totalLiabilities = remember(accounts) {
        accounts.filter { it.type == "CREDIT_CARD" }.sumOf { it.balance }
    }
    val netWorth = totalAssets - totalLiabilities

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accounts & Cards",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage bank balances, credit limits & cash",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activeRole.canManageHousehold) {
                    FilledTonalButton(
                        onClick = onOpenAddAccount,
                        modifier = Modifier.testTag("add_account_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }

        // Net Worth Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Household Net Worth",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = { viewModel.toggleSensitivity() }) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle privacy",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SensitiveText(
                        amount = netWorth,
                        isRevealed = isRevealed,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Assets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            SensitiveText(
                                amount = totalAssets,
                                isRevealed = isRevealed,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Credit Card Debt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            SensitiveText(
                                amount = totalLiabilities,
                                isRevealed = isRevealed,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Bank Accounts Section
        if (bankAccounts.isNotEmpty()) {
            item {
                Text(
                    text = "Bank Accounts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(bankAccounts, key = { it.id }) { acc ->
                AccountItemCard(
                    account = acc,
                    isRevealed = isRevealed,
                    canDelete = activeRole.canManageHousehold,
                    onDelete = { accountToDelete = acc }
                )
            }
        }

        // Credit Cards Section
        if (creditCards.isNotEmpty()) {
            item {
                Text(
                    text = "Credit Cards",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(creditCards, key = { it.id }) { card ->
                CreditCardItemCard(
                    account = card,
                    isRevealed = isRevealed,
                    canDelete = activeRole.canManageHousehold,
                    onDelete = { accountToDelete = card }
                )
            }
        }

        // Cash & Wallets Section
        if (cashWallets.isNotEmpty()) {
            item {
                Text(
                    text = "Cash & Wallets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(cashWallets, key = { it.id }) { cash ->
                AccountItemCard(
                    account = cash,
                    isRevealed = isRevealed,
                    canDelete = activeRole.canManageHousehold,
                    onDelete = { accountToDelete = cash }
                )
            }
        }
    }

    // Delete Confirmation
    if (accountToDelete != null) {
        val acc = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to remove '${acc.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(acc.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccountItemCard(
    account: AccountEntity,
    isRevealed: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.type == "CASH") Icons.Default.Money else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (account.isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Primary",
                                color = PrimaryBlue,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${account.institution} • ${account.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                SensitiveText(
                    amount = account.balance,
                    isRevealed = isRevealed,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardItemCard(
    account: AccountEntity,
    isRevealed: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val reminder = remember(account) { CreditCardBillReminderHelper.calculateBillStatus(account) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credit_card_${account.id}"),
        shape = RoundedCornerShape(16.dp),
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ExpenseRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${account.institution} • Billing Day: ${account.billingCycleDay ?: 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SensitiveText(
                        amount = account.balance,
                        isRevealed = isRevealed,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed
                    )
                }
            }

            if (account.creditLimit != null && account.creditLimit > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val utilization = (account.balance / account.creditLimit).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { utilization },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (utilization > 0.5f) ExpenseRed else PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Limit: ${CurrencyFormatter.formatInr(account.creditLimit, obscure = !isRevealed)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(utilization * 100).toInt()}% Used",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (utilization > 0.5f) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (reminder != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (reminder.isOverdue) ExpenseRed.copy(alpha = 0.1f) else WarningAmber.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = if (reminder.isOverdue) ExpenseRed else WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reminder.reminderMessage,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (reminder.isOverdue) ExpenseRed else WarningAmber
                        )
                    }
                }
            }
        }
    }
}
