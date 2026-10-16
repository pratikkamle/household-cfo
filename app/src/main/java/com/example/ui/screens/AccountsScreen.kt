package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.MemberProfile
import com.example.data.model.UserRole
import com.example.ui.components.MemberBadge
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyFormatter

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    currentUserRole: UserRole = UserRole.ADMIN,
    isSensitiveRevealed: Boolean = false,
    members: List<MemberProfile> = emptyList(),
    onToggleSensitivity: () -> Unit = {},
    onAddAccount: (
        name: String,
        holder: String,
        accountType: String,
        balance: Double,
        accountNumberMask: String,
        institution: String,
        notes: String,
        creditLimit: Double,
        billingCycleDay: Int?,
        paymentDueDateDay: Int?,
        statementDueAmount: Double,
        userId: Long?
    ) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    androidx.compose.runtime.LaunchedEffect(accounts) {
        com.example.util.CreditCardBillReminderHelper.checkAndNotifyBillReminders(context, accounts)
    }

    // Check Role Access: Only ADMIN and CONTRIBUTOR can view or manage Accounts
    if (!currentUserRole.canViewAccounts) {
        AccountsRestrictedView(currentUserRole = currentUserRole, modifier = modifier)
        return
    }

    // Totals
    val totalBankLiquidity = accounts.filter { it.accountType == "BANK" || it.accountType == "CASH" || it.accountType == "DEBIT_CARD" }.sumOf { it.balance }
    val totalInvestments = accounts.filter {
        it.accountType in listOf("INVESTMENT", "LIC_INSURANCE", "NPS", "EPF_PPF", "FIXED_DEPOSIT", "GOLD", "OTHER")
    }.sumOf { it.balance }
    val totalCreditDues = accounts.filter { it.accountType == "CREDIT_CARD" }.sumOf { card ->
        if (card.statementDueAmount > 0) card.statementDueAmount
        else if (card.balance < 0) -card.balance
        else card.balance
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Sensitivity Eye & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accounts & Assets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bank accounts, cards, LIC, MFs & NPS schemes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sensitivity Eye Button
                    Surface(
                        shape = CircleShape,
                        color = if (isSensitiveRevealed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onToggleSensitivity,
                            modifier = Modifier.testTag("btn_toggle_sensitivity")
                        ) {
                            Icon(
                                imageVector = if (isSensitiveRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isSensitiveRevealed) "Hide Balances" else "Reveal Balances with Authentication",
                                tint = if (isSensitiveRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Add Button
                    Button(
                        onClick = { isAddDialogOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_open_add_account")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Asset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL LIQUID BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSensitiveRevealed) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isSensitiveRevealed) "Unlocked" else "Protected • Tap 👁 to Reveal",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSensitiveRevealed) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isSensitiveRevealed) CurrencyFormatter.formatInr(totalBankLiquidity) else "₹ ••••••••",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald500
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "MFs, NPS & Schemes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isSensitiveRevealed) CurrencyFormatter.formatInr(totalInvestments) else "₹ ••••••••",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(text = "Credit Card Dues", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isSensitiveRevealed) CurrencyFormatter.formatInr(totalCreditDues) else "₹ ••••••••",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Empty State if no accounts exist
        if (accounts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "No Accounts or Assets Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Start fresh with real data! Add your bank accounts, credit/debit cards, LIC policies, mutual funds, or NPS pension schemes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { isAddDialogOpen = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Account / Asset")
                        }
                    }
                }
            }
        } else {
            // Group accounts by type
            val bankAccounts = accounts.filter { it.accountType == "BANK" || it.accountType == "CASH" }
            val cardAccounts = accounts.filter { it.accountType == "CREDIT_CARD" || it.accountType == "DEBIT_CARD" }
            val schemeAccounts = accounts.filter {
                it.accountType in listOf("INVESTMENT", "LIC_INSURANCE", "NPS", "EPF_PPF", "FIXED_DEPOSIT", "GOLD", "OTHER")
            }

            if (bankAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = "Bank & Liquid Accounts (${bankAccounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(bankAccounts, key = { it.id }) { acc ->
                    AccountItemCard(
                        account = acc,
                        isSensitiveRevealed = isSensitiveRevealed,
                        onClick = { editingAccount = acc }
                    )
                }
            }

            if (cardAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = "Credit & Debit Cards (${cardAccounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(cardAccounts, key = { it.id }) { acc ->
                    AccountItemCard(
                        account = acc,
                        isSensitiveRevealed = isSensitiveRevealed,
                        onClick = { editingAccount = acc }
                    )
                }
            }

            if (schemeAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = "Investments, LIC, MFs & Schemes (${schemeAccounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(schemeAccounts, key = { it.id }) { acc ->
                    AccountItemCard(
                        account = acc,
                        isSensitiveRevealed = isSensitiveRevealed,
                        onClick = { editingAccount = acc }
                    )
                }
            }
        }
    }

    // Add Account Dialog
    if (isAddDialogOpen) {
        AddEditAccountDialog(
            initialAccount = null,
            members = members,
            onSave = { _, name, holder, type, bal, mask, inst, notes, limit, cycleDay, dueDay, stmtDue, uId ->
                onAddAccount(name, holder, type, bal, mask, inst, notes, limit, cycleDay, dueDay, stmtDue, uId)
            },
            onDismiss = { isAddDialogOpen = false }
        )
    }

    // Edit Account Dialog
    editingAccount?.let { acc ->
        AddEditAccountDialog(
            initialAccount = acc,
            members = members,
            onSave = { id, name, holder, type, bal, mask, inst, notes, limit, cycleDay, dueDay, stmtDue, uId ->
                onUpdateAccount(
                    acc.copy(
                        name = name,
                        holder = holder,
                        accountType = type,
                        balance = bal,
                        accountNumberMask = mask,
                        institution = inst,
                        notes = notes,
                        creditLimit = limit,
                        billingCycleDay = cycleDay,
                        paymentDueDateDay = dueDay,
                        statementDueAmount = stmtDue,
                        userId = uId
                    )
                )
            },
            onDelete = { id -> onDeleteAccount(id) },
            onDismiss = { editingAccount = null }
        )
    }
}

@Composable
fun AccountItemCard(
    account: AccountEntity,
    isSensitiveRevealed: Boolean,
    onClick: () -> Unit
) {
    val (icon, typeLabel) = when (account.accountType) {
        "CREDIT_CARD" -> Pair(Icons.Default.CreditCard, "Credit Card")
        "DEBIT_CARD" -> Pair(Icons.Default.CreditCard, "Debit Card")
        "INVESTMENT" -> Pair(Icons.Default.TrendingUp, "Mutual Funds / Stocks")
        "LIC_INSURANCE" -> Pair(Icons.Default.Shield, "LIC / Insurance")
        "NPS" -> Pair(Icons.Default.Savings, "NPS / Pension")
        "EPF_PPF" -> Pair(Icons.Default.Savings, "EPF / PPF")
        "FIXED_DEPOSIT" -> Pair(Icons.Default.AccountBalance, "Fixed Deposit")
        "GOLD" -> Pair(Icons.Default.Savings, "Gold / SGB")
        "CASH" -> Pair(Icons.Default.LocalAtm, "Cash in Hand")
        else -> Pair(Icons.Default.AccountBalance, "Bank Account")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("account_card_${account.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = account.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        MemberBadge(memberId = account.holder)
                    }

                    Text(
                        text = buildString {
                            if (account.institution.isNotBlank()) append("${account.institution} • ")
                            append(typeLabel)
                            if (account.accountNumberMask.isNotBlank()) {
                                append(" • ")
                                append(if (isSensitiveRevealed) account.accountNumberMask else "••••")
                            }
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (account.notes.isNotBlank()) {
                        Text(
                            text = account.notes,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (account.accountType == "CREDIT_CARD") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (account.billingCycleDay != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Cycle: ${CurrencyFormatter.formatDayOfMonth(account.billingCycleDay)}",
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (account.paymentDueDateDay != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ExpenseRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Due: ${CurrencyFormatter.formatDayOfMonth(account.paymentDueDateDay)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (account.statementDueAmount > 0) {
                            Text(
                                text = if (isSensitiveRevealed) "Statement Due: ${CurrencyFormatter.formatInr(account.statementDueAmount)}" else "Statement Due: ₹••••••",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isSensitiveRevealed) {
                        CurrencyFormatter.formatInr(account.balance)
                    } else {
                        "₹ ••••••••"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (account.balance < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
                if (account.accountType == "CREDIT_CARD" && account.creditLimit > 0) {
                    Text(
                        text = if (isSensitiveRevealed) "Limit: ${CurrencyFormatter.formatInr(account.creditLimit)}" else "Limit: ₹••••••",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsRestrictedView(
    currentUserRole: UserRole,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Accounts & Assets Restricted",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Viewing and editing bank accounts, credit cards, LIC policies, MFs, and NPS schemes is restricted to Household Admin and Contributors.\n\nYour current role is '${currentUserRole.displayName}'. You can view dashboard analytics, log household transactions, and download monthly reports.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
