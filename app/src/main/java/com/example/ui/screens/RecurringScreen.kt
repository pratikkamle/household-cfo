package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.model.RecurringTransactionEntity
import com.example.ui.components.SensitiveText
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyFormatter

@Composable
fun RecurringScreen(
    viewModel: FinanceViewModel,
    onOpenAddRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recurringRules by viewModel.recurringRules.collectAsState()
    val isRevealed by viewModel.isSensitiveRevealed.collectAsState()
    val activeRole by viewModel.activeUserRole.collectAsState()

    var ruleToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    val totalCommitted = remember(recurringRules) {
        recurringRules.filter { it.isActive }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recurring_screen"),
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
                        text = "Recurring & Bills",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Subscriptions, utilities & fixed commitments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activeRole.canManageHousehold) {
                    FilledTonalButton(
                        onClick = onOpenAddRecurring,
                        modifier = Modifier.testTag("add_recurring_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total Monthly Fixed Commitments",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SensitiveText(
                        amount = totalCommitted,
                        isRevealed = isRevealed,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${recurringRules.count { it.isActive }} active subscriptions and bills",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Active Commitments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (recurringRules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recurring bills set up yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recurringRules, key = { it.id }) { rule ->
                RecurringItemCard(
                    rule = rule,
                    isRevealed = isRevealed,
                    canManage = activeRole.canManageHousehold,
                    onMarkPaid = { viewModel.markRecurringAsPaid(rule) },
                    onToggleActive = { viewModel.updateRecurringRule(rule.copy(isActive = !rule.isActive)) },
                    onDelete = { ruleToDelete = rule }
                )
            }
        }
    }

    if (ruleToDelete != null) {
        val rule = ruleToDelete!!
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Recurring Commitment?") },
            text = { Text("Are you sure you want to delete '${rule.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecurringRule(rule.id)
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                ) {
                    Text("Delete")
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

@Composable
fun RecurringItemCard(
    rule: RecurringTransactionEntity,
    isRevealed: Boolean,
    canManage: Boolean,
    onMarkPaid: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recurring_card_${rule.id}"),
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${rule.category} • Due day ${rule.dueDayOfMonth} • ${rule.frequency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    SensitiveText(
                        amount = rule.amount,
                        isRevealed = isRevealed,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (rule.isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (rule.isActive) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canManage) {
                    TextButton(onClick = onToggleActive) {
                        Text(if (rule.isActive) "Pause" else "Resume")
                    }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)) {
                        Text("Delete")
                    }
                }

                Button(
                    onClick = onMarkPaid,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark as Paid", fontSize = 12.sp)
                }
            }
        }
    }
}
