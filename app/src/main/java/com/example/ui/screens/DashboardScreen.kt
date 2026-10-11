package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import com.example.data.model.HouseholdMember
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserRole
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.MemberBadge
import com.example.ui.components.MemberFilterRow
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.UpcomingOrange
import com.example.ui.viewmodel.MonthSummary
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.model.HouseholdEntity
import com.example.data.model.MemberProfile
import com.example.data.sync.SyncState
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun DashboardScreen(
    summary: MonthSummary,
    transactions: List<TransactionEntity>,
    recurringRules: List<RecurringRuleEntity>,
    selectedMember: HouseholdMember?,
    household: HouseholdEntity? = null,
    members: List<MemberProfile> = emptyList(),
    selectedMemberName: String? = null,
    currentUser: FirebaseUser? = null,
    syncState: SyncState = SyncState.Idle,
    onSyncToCloud: () -> Unit = {},
    onSignInClicked: () -> Unit = {},
    onMemberSelected: (HouseholdMember?) -> Unit,
    onMemberNameSelected: (String?) -> Unit = {},
    onOpenHouseholdHub: () -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenSmartDetect: () -> Unit,
    onOpenCfoAdvisor: () -> Unit,
    onIncomeClicked: () -> Unit = {},
    onScheduledClicked: () -> Unit = {},
    onInvestmentsClicked: () -> Unit = {},
    onRandomSpentClicked: () -> Unit = {},
    onMarkRecurringPaid: (RecurringRuleEntity) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    isSensitiveRevealed: Boolean = false,
    currentUserRole: UserRole = UserRole.ADMIN,
    onToggleSensitivity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAllHousehold = selectedMemberName == null
    val jointExpenses = remember(transactions) {
        transactions
            .filter { it.type == "EXPENSE" && it.paidBy.equals("JOINT", ignoreCase = true) }
            .sumOf { it.amount }
    }
    val jointSplitPerMember = remember(jointExpenses, members) {
        if (members.isNotEmpty()) jointExpenses / members.size else 0.0
    }

    val activeDisplaySummary = remember(selectedMemberName, summary, transactions, recurringRules, jointSplitPerMember) {
        if (isAllHousehold) {
            summary
        } else {
            val memberName = selectedMemberName!!
            val mTxIncome = transactions
                .filter { it.type == "INCOME" && it.paidBy.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount }
            val mRuleIncome = recurringRules
                .filter { it.type == "INCOME" && it.isActive && it.paidBy.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount }
            val mTotalIncome = if (mTxIncome > 0) maxOf(mTxIncome, mRuleIncome) else mRuleIncome

            val mScheduled = recurringRules
                .filter { it.nature == "CORE_COMMITMENT" && it.type == "EXPENSE" && it.isActive && it.paidBy.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount }

            val mOwnRandom = transactions
                .filter { it.type == "EXPENSE" && it.nature == "DISCRETIONARY" && it.paidBy.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount }
            val mTotalRandom = mOwnRandom + jointSplitPerMember

            val mInvestments = recurringRules
                .filter { it.type == "INVESTMENT" && it.isActive && it.paidBy.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount }

            val mAvailable = mTotalIncome - (mScheduled + mTotalRandom + mInvestments)

            summary.copy(
                totalIncome = mTotalIncome,
                scheduledExpenses = mScheduled,
                randomExpenses = mTotalRandom,
                investments = mInvestments,
                availableBalance = mAvailable
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Month Selector
        item {
            MonthSelectorHeader(
                displayMonth = summary.displayMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }

        // Generalized Member Filter Chips (All Household / Members 1, 2, 3, 4+ / Joint)
        item {
            MemberFilterRow(
                members = members,
                selectedMemberName = selectedMemberName,
                onMemberNameSelected = onMemberNameSelected
            )
        }

        // Hero Financial Overview Card (Matches user prompt metrics)
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_summary_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Available Balance / Cash Flow Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isAllHousehold) "AVAILABLE CASH FLOW (ALL HOUSEHOLD)" else "AVAILABLE CASH FLOW (${selectedMemberName!!.uppercase()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = onToggleSensitivity,
                                    modifier = Modifier.size(36.dp).testTag("dashboard_toggle_sensitivity")
                                ) {
                                    Icon(
                                        imageVector = if (isSensitiveRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isSensitiveRevealed) "Hide Balances" else "Reveal with Authentication",
                                        tint = if (isSensitiveRevealed) Emerald500 else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSensitiveRevealed) CurrencyFormatter.formatInr(activeDisplaySummary.availableBalance) else "₹ ••••••••",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeDisplaySummary.availableBalance >= 0) Emerald500 else ExpenseRed
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 4 Core Financial Metrics Grid (Interactive: Tapping navigates to respective screen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            dotColor = IncomeGreen,
                            label = if (isAllHousehold) "Total Income" else "Income",
                            amount = if (isSensitiveRevealed) CurrencyFormatter.formatInr(activeDisplaySummary.totalIncome) else "₹ ••••••••",
                            onClick = onIncomeClicked,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            dotColor = ExpenseRed,
                            label = if (isAllHousehold) "Scheduled" else "Fixed / EMI",
                            amount = CurrencyFormatter.formatInr(activeDisplaySummary.scheduledExpenses),
                            onClick = onScheduledClicked,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            dotColor = InvestmentBlue,
                            label = "Investments",
                            amount = if (isSensitiveRevealed) CurrencyFormatter.formatInr(activeDisplaySummary.investments) else "₹ ••••••••",
                            onClick = onInvestmentsClicked,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            dotColor = UpcomingOrange,
                            label = if (isAllHousehold) "Random Spent" else "Spends (incl. joint)",
                            amount = CurrencyFormatter.formatInr(activeDisplaySummary.randomExpenses),
                            onClick = onRandomSpentClicked,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Monthly Spending Progress & Discretionary Budget
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_progress_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val progress = if (summary.discretionaryBudget > 0) {
                        (summary.randomExpenses / summary.discretionaryBudget).toFloat().coerceIn(0f, 1f)
                    } else 0f
                    val percent = (progress * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remaining Discretionary Budget",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$percent% used",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (percent > 85) ExpenseRed else MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (percent > 85) ExpenseRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${CurrencyFormatter.formatInr(summary.randomExpenses)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Remaining: ${CurrencyFormatter.formatInr(summary.remainingDiscretionary)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }
                }
            }
        }

        // Primary Action Buttons (Add Expense, Add Income, Smart Detect) or Read-Only Notice
        item {
            if (currentUserRole.canAddTransactions) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Large Prominent Add Expense Button
                    Button(
                        onClick = onOpenAddExpense,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("big_add_expense_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Expense",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Secondary row: Add Income & Smart Detect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAddIncome,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("add_income_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Income",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenSmartDetect,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("smart_detect_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = UpcomingOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Detect",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().testTag("user_readonly_notice")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Read-Only Mode (${currentUserRole.displayName})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You can view spending analytics and dashboards. Adding or editing expenses is reserved for household members.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun MetricPill(
    dotColor: Color,
    label: String,
    amount: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = if (onClick != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("metric_pill_${label.lowercase().replace(" ", "_").replace("/", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (onClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Text(
                    text = amount,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RecurringQuickItem(
    rule: RecurringRuleEntity,
    onMarkPaid: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryIconBadge(categoryName = rule.category, size = 34)
                Column {
                    Text(text = rule.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MemberBadge(memberId = rule.paidBy)
                        Text(text = "Due: ${CurrencyFormatter.formatDayOfMonth(rule.dueDay)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = CurrencyFormatter.formatInr(rule.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onMarkPaid,
                    modifier = Modifier.size(32.dp).testTag("record_paid_btn_${rule.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark as Paid",
                        tint = Emerald500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    canDelete: Boolean = true
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    val dateStr = dateFormat.format(Date(transaction.timestamp))
    val isIncome = transaction.type == "INCOME"

    val methodDisplay = if (isIncome && (transaction.paymentMethod.contains("DEBIT", ignoreCase = true) || transaction.paymentMethod.contains("AUTO", ignoreCase = true) || transaction.paymentMethod.isBlank())) {
        "Auto Credit"
    } else {
        transaction.paymentMethod
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                CategoryIconBadge(categoryName = transaction.category, size = 38)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = transaction.merchant.ifBlank { transaction.category },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        MemberBadge(memberId = transaction.paidBy)
                        Text(
                            text = methodDisplay,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• $dateStr",
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatInr(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_tx_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
