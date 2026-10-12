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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.AccountEntity
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
import com.example.data.model.CategoryConstants
import com.example.data.model.isJoint
import com.example.data.model.matchesMember
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
    accounts: List<AccountEntity> = emptyList(),
    onUpdateTransaction: (TransactionEntity) -> Unit = {},
    onToggleSensitivity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAllHousehold = selectedMemberName == null
    val jointExpenses = remember(transactions) {
        transactions
            .filter { it.type == "EXPENSE" && isJoint(it.paidBy) && !CategoryConstants.isCommitment(it.category, it.nature, it.recurringRuleId, it.merchant) }
            .sumOf { it.amount }
    }
    val jointSplitPerMember = remember(jointExpenses, members) {
        if (members.isNotEmpty()) jointExpenses / members.size else 0.0
    }

    val jointScheduledRules = remember(recurringRules) {
        recurringRules
            .filter { it.type == "EXPENSE" && it.isActive && isJoint(it.paidBy) && !CategoryConstants.isInvestment(it.category, it.type, it.nature) }
            .sumOf { it.amount }
    }
    val jointScheduledTx = remember(transactions, recurringRules) {
        transactions
            .filter { tx ->
                tx.type == "EXPENSE" && isJoint(tx.paidBy) && CategoryConstants.isCommitment(tx.category, tx.nature, tx.recurringRuleId, tx.merchant) &&
                (tx.recurringRuleId == null || recurringRules.none { r -> r.id == tx.recurringRuleId && r.isActive })
            }
            .sumOf { it.amount }
    }
    val jointScheduledPerMember = remember(jointScheduledRules, jointScheduledTx, members) {
        if (members.isNotEmpty()) (jointScheduledRules + jointScheduledTx) / members.size else 0.0
    }

    val jointInvestmentsRules = remember(recurringRules) {
        recurringRules.filter { it.isActive && isJoint(it.paidBy) && CategoryConstants.isInvestment(it.category, it.type, it.nature) }.sumOf { it.amount }
    }
    val jointInvestmentsTx = remember(transactions, recurringRules) {
        transactions.filter { tx ->
            isJoint(tx.paidBy) && CategoryConstants.isInvestment(tx.category, tx.type, tx.nature) &&
            (tx.recurringRuleId == null || recurringRules.none { r -> r.id == tx.recurringRuleId && r.isActive })
        }.sumOf { it.amount }
    }
    val jointInvestmentsPerMember = remember(jointInvestmentsRules, jointInvestmentsTx, members) {
        if (members.isNotEmpty()) (jointInvestmentsRules + jointInvestmentsTx) / members.size else 0.0
    }

    val activeDisplaySummary = remember(selectedMemberName, summary, transactions, recurringRules, jointSplitPerMember, jointScheduledPerMember, jointInvestmentsPerMember) {
        if (isAllHousehold) {
            summary
        } else {
            val memberName = selectedMemberName!!
            val mTxIncome = transactions
                .filter { it.type == "INCOME" && matchesMember(it.paidBy, memberName, members) }
                .sumOf { it.amount }
            val mPendingRuleIncome = recurringRules
                .filter { rule ->
                    rule.type == "INCOME" && rule.isActive && matchesMember(rule.paidBy, memberName, members) &&
                    transactions.none { it.recurringRuleId == rule.id || (matchesMember(it.paidBy, rule.paidBy, members) && it.merchant.equals(rule.title, ignoreCase = true)) }
                }
                .sumOf { it.amount }
            val mTotalIncome = mTxIncome + mPendingRuleIncome

            val mActiveRuleScheduled = recurringRules
                .filter { it.type == "EXPENSE" && it.isActive && !CategoryConstants.isInvestment(it.category, it.type, it.nature) && matchesMember(it.paidBy, memberName, members) }
                .sumOf { it.amount }
            val mUnlinkedTxScheduled = transactions
                .filter { tx ->
                    tx.type == "EXPENSE" && CategoryConstants.isCommitment(tx.category, tx.nature, tx.recurringRuleId, tx.merchant) &&
                    matchesMember(tx.paidBy, memberName, members) &&
                    (tx.recurringRuleId == null || recurringRules.none { r -> r.id == tx.recurringRuleId && r.isActive })
                }
                .sumOf { it.amount }
            val mScheduled = mActiveRuleScheduled + mUnlinkedTxScheduled + jointScheduledPerMember

            val mActiveRuleInvestments = recurringRules
                .filter { it.isActive && CategoryConstants.isInvestment(it.category, it.type, it.nature) && matchesMember(it.paidBy, memberName, members) }
                .sumOf { it.amount }
            val mUnlinkedTxInvestments = transactions
                .filter { tx ->
                    CategoryConstants.isInvestment(tx.category, tx.type, tx.nature) &&
                    matchesMember(tx.paidBy, memberName, members) &&
                    (tx.recurringRuleId == null || recurringRules.none { r -> r.id == tx.recurringRuleId && r.isActive })
                }
                .sumOf { it.amount }
            val mInvestments = mActiveRuleInvestments + mUnlinkedTxInvestments + jointInvestmentsPerMember

            val mOwnRandom = transactions
                .filter { tx ->
                    tx.type == "EXPENSE" &&
                    !CategoryConstants.isCommitment(tx.category, tx.nature, tx.recurringRuleId, tx.merchant) &&
                    !CategoryConstants.isInvestment(tx.category, tx.type, tx.nature) &&
                    matchesMember(tx.paidBy, memberName, members)
                }
                .sumOf { it.amount }
            val mTotalRandom = mOwnRandom + jointSplitPerMember

            val mAvailable = mTotalIncome - (mScheduled + mTotalRandom + mInvestments)
            val memberBudgetShare = if (members.isNotEmpty()) summary.discretionaryBudget / members.size else summary.discretionaryBudget
            val memberRemainingShare = maxOf(0.0, memberBudgetShare - mTotalRandom)

            summary.copy(
                totalIncome = mTotalIncome,
                scheduledExpenses = mScheduled,
                randomExpenses = mTotalRandom,
                investments = mInvestments,
                availableBalance = mAvailable,
                discretionaryBudget = memberBudgetShare,
                remainingDiscretionary = memberRemainingShare
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

        // Discretionary Budget vs Actual Health Card (Matches Budget Screen Layout)
        item {
            val totalBudget = activeDisplaySummary.discretionaryBudget
            val totalSpent = activeDisplaySummary.randomExpenses
            val totalRemaining = activeDisplaySummary.remainingDiscretionary
            val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
            val percentUtilized = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0
            val isOverBudget = totalSpent > totalBudget

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_progress_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isOverBudget) ExpenseRed else Emerald500,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isOverBudget) "Attention: Overspending" else "On Track: Disciplined Pace",
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) ExpenseRed else Emerald500,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "$percentUtilized% utilized",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isOverBudget) ExpenseRed else Emerald500,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Budget", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatInr(totalBudget), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text(text = "Total Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatInr(totalSpent), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                        }
                        Column {
                            Text(text = "Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CurrencyFormatter.formatInr(totalRemaining), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald500)
                        }
                    }
                }
            }
        }

        // Household Members Cashflow & Contributions Breakdown (Aggregates all member submissions)
        if (isAllHousehold && members.isNotEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("household_members_cashflow_breakdown_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Member Contributions & Cashflow",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${members.size} members",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        members.forEach { m ->
                            val mIncome = transactions
                                .filter { it.type == "INCOME" && matchesMember(it.paidBy, m.displayName, members) }
                                .sumOf { it.amount } + recurringRules
                                .filter { r -> r.type == "INCOME" && r.isActive && matchesMember(r.paidBy, m.displayName, members) &&
                                    transactions.none { it.recurringRuleId == r.id || (matchesMember(it.paidBy, r.paidBy, members) && it.merchant.equals(r.title, ignoreCase = true)) }
                                }
                                .sumOf { it.amount }

                            val mRuleCommitments = recurringRules
                                .filter { it.type == "EXPENSE" && it.isActive && !CategoryConstants.isInvestment(it.category, it.type, it.nature) && matchesMember(it.paidBy, m.displayName, members) }
                                .sumOf { it.amount }
                            val mTxCommitments = transactions
                                .filter { tx ->
                                    tx.type == "EXPENSE" && CategoryConstants.isCommitment(tx.category, tx.nature, tx.recurringRuleId, tx.merchant) &&
                                    matchesMember(tx.paidBy, m.displayName, members) &&
                                    (tx.recurringRuleId == null || recurringRules.none { r -> r.id == tx.recurringRuleId && r.isActive })
                                }
                                .sumOf { it.amount }
                            val mScheduled = mRuleCommitments + mTxCommitments + jointScheduledPerMember

                            val mRandomSpent = transactions
                                .filter { tx ->
                                    tx.type == "EXPENSE" &&
                                    !CategoryConstants.isCommitment(tx.category, tx.nature, tx.recurringRuleId, tx.merchant) &&
                                    !CategoryConstants.isInvestment(tx.category, tx.type, tx.nature) &&
                                    matchesMember(tx.paidBy, m.displayName, members)
                                }
                                .sumOf { it.amount } + jointSplitPerMember

                            val mNetAvailable = mIncome - (mScheduled + mRandomSpent)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onMemberNameSelected(m.displayName) }
                                    .testTag("member_contribution_row_${m.displayName.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(m.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = m.initial,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = m.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(android.graphics.Color.parseColor(m.userRole.badgeColorHex)).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = m.userRole.displayName,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(android.graphics.Color.parseColor(m.userRole.badgeColorHex)),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Income: ${CurrencyFormatter.formatInr(mIncome)}",
                                                    fontSize = 11.sp,
                                                    color = IncomeGreen,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "• EMI: ${CurrencyFormatter.formatInr(mScheduled)}",
                                                    fontSize = 11.sp,
                                                    color = ExpenseRed,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Net Cash Flow",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatInr(mNetAvailable),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (mNetAvailable >= 0) Emerald500 else ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
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
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
    canDelete: Boolean = true,
    canEdit: Boolean = true
) {
    val fullDateTimeFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val fullDateStr = fullDateTimeFormat.format(Date(transaction.timestamp))
    val isIncome = transaction.type == "INCOME"

    val methodDisplay = if (isIncome && (transaction.paymentMethod.contains("DEBIT", ignoreCase = true) || transaction.paymentMethod.contains("AUTO", ignoreCase = true) || transaction.paymentMethod.isBlank())) {
        "Auto Credit"
    } else {
        transaction.paymentMethod
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("transaction_card_${transaction.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Top Row: Icon, Merchant/Category & Member, Amount & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CategoryIconBadge(categoryName = transaction.category, size = 40)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = transaction.merchant.ifBlank { transaction.category },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MemberBadge(memberId = transaction.paidBy)
                            Text(
                                text = "• ${transaction.category}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isIncome) IncomeGreen else ExpenseRed
                    )
                    if (canEdit && onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp).testTag("edit_tx_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Transaction",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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

            // Dedicated small section displaying the proper complete datetime value in a human readable format
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = fullDateStr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = methodDisplay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Additional comments / notes if present
            if (transaction.notes.isNotBlank()) {
                Text(
                    text = "Note: ${transaction.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
