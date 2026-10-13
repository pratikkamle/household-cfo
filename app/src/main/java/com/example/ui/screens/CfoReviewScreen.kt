package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FinancialGoalEntity
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.OceanTeal
import com.example.ui.theme.RosePink
import com.example.ui.viewmodel.MonthSummary
import com.example.util.CfoAdvisorEngine
import com.example.util.CfoReviewReport
import com.example.util.CurrencyFormatter
import com.example.util.GoalFeasibilityInsight
import com.example.util.GoalFeasibilityStatus
import com.example.util.GoalSimulationResult
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CfoReviewScreen(
    report: CfoReviewReport,
    goals: List<FinancialGoalEntity>,
    monthSummary: MonthSummary,
    onAddGoal: (title: String, category: String, targetAmount: Double, currentSaved: Double, targetMonths: Int, priority: String, notes: String) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddGoalContribution: (Long, Double) -> Unit,
    onToggleGoalCompleted: (Long) -> Unit,
    onSimulateGoal: (title: String, targetAmount: Double, currentSaved: Double, targetMonths: Int) -> GoalSimulationResult,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<FinancialGoalEntity?>(null) }

    // Interactive Ask CFO / Goal Simulator State
    var simTitle by remember { mutableStateOf("Family Dream Vacation") }
    var simCategory by remember { mutableStateOf("VACATION") }
    var simTargetText by remember { mutableStateOf("250000") }
    var simSavedText by remember { mutableStateOf("50000") }
    var simMonths by remember { mutableIntStateOf(10) }

    val currentSurplus = report.netSurplus

    val simTargetAmount = simTargetText.toDoubleOrNull() ?: 0.0
    val simSavedAmount = simSavedText.toDoubleOrNull() ?: 0.0

    val simulationResult = remember(simTitle, simTargetAmount, simSavedAmount, simMonths, currentSurplus) {
        onSimulateGoal(simTitle, simTargetAmount, simSavedAmount, simMonths)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cfo_review_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header with back arrow and close button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("cfo_screen_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Family CFO Review",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Executive Live Goal Tracking & Intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("cfo_screen_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Executive Financial Health Score Card
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.healthGrade,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Emerald500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Savings Rate: ${String.format(Locale.US, "%.1f", report.savingsRatePercent)}% • Monthly Surplus: ${CurrencyFormatter.formatInr(report.netSurplus)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.healthScore}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Emerald500
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Key Financial Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricColumn(label = "Monthly Income", value = CurrencyFormatter.formatInr(report.totalIncome))
                        MetricColumn(label = "Core Commitments", value = CurrencyFormatter.formatInr(report.coreCommitments))
                        MetricColumn(label = "Investments / SIP", value = CurrencyFormatter.formatInr(report.investments))
                        MetricColumn(label = "Free Surplus", value = CurrencyFormatter.formatInr(report.netSurplus), highlight = true)
                    }
                }
            }
        }

        // Live Goals Header & Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = OceanTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Active Household Goals (${goals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active Goals List
        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Household Goals Active Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Define targets for emergency reserve, vacation, vehicle, or home down payment to get live cashflow-backed CFO insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showAddGoalDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("cfo_empty_set_goal_btn")
                        ) {
                            Text("Set Your First Family Goal")
                        }
                    }
                }
            }
        } else {
            items(report.goalInsights, key = { it.goalId }) { insight ->
                val originalGoal = goals.find { it.id == insight.goalId }
                GoalTrackingCard(
                    insight = insight,
                    onAddSavings = {
                        originalGoal?.let { contributingGoal = it }
                    },
                    onToggleComplete = { onToggleGoalCompleted(insight.goalId) },
                    onDelete = { onDeleteGoal(insight.goalId) }
                )
            }
        }

        // "Ask CFO: Live Goal Feasibility & Simulator" Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cfo_ask_simulator_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(InvestmentBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = InvestmentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Ask CFO: Live Goal Feasibility Simulator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Evaluate custom goals in real time using your actual ${CurrencyFormatter.formatInr(report.netSurplus)} monthly surplus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Goal Templates / Starters
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GoalPresetChip(
                            label = "🛡️ Emergency Reserve (6 Mo)",
                            onClick = {
                                val monthlyExpenses = maxOf(30000.0, monthSummary.scheduledExpenses + monthSummary.randomExpenses)
                                simTitle = "Emergency Reserve (6 Months)"
                                simCategory = "EMERGENCY"
                                simTargetText = (monthlyExpenses * 6).toInt().toString()
                                simSavedText = "50000"
                                simMonths = 12
                            }
                        )
                        GoalPresetChip(
                            label = "✈️ Europe Vacation",
                            onClick = {
                                simTitle = "Family Vacation to Europe"
                                simCategory = "VACATION"
                                simTargetText = "350000"
                                simSavedText = "30000"
                                simMonths = 8
                            }
                        )
                        GoalPresetChip(
                            label = "🏡 Home Renovation",
                            onClick = {
                                simTitle = "Home Interior & Renovation"
                                simCategory = "HOME"
                                simTargetText = "500000"
                                simSavedText = "100000"
                                simMonths = 12
                            }
                        )
                        GoalPresetChip(
                            label = "💻 Tech Equipment",
                            onClick = {
                                simTitle = "Workstation & Tech Upgrade"
                                simCategory = "CUSTOM"
                                simTargetText = "180000"
                                simSavedText = "20000"
                                simMonths = 6
                            }
                        )
                    }

                    // Simulator Inputs
                    OutlinedTextField(
                        value = simTitle,
                        onValueChange = { simTitle = it },
                        label = { Text("What is your goal?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_goal_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = simTargetText,
                            onValueChange = { simTargetText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Target Amount (₹)") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("sim_goal_target_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = simSavedText,
                            onValueChange = { simSavedText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Saved Already (₹)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_goal_saved_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Target Months Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target Timeframe: $simMonths Months",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3, 6, 12, 18, 24).forEach { m ->
                                FilterChip(
                                    selected = simMonths == m,
                                    onClick = { simMonths = m },
                                    label = { Text("${m}m", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Live Simulation Result Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (simulationResult.feasibilityStatus) {
                                GoalFeasibilityStatus.ON_TRACK -> Emerald500.copy(alpha = 0.12f)
                                GoalFeasibilityStatus.MODERATE_STRETCH -> OceanTeal.copy(alpha = 0.12f)
                                GoalFeasibilityStatus.TIGHT_BUFFER -> GoldAccent.copy(alpha = 0.15f)
                                GoalFeasibilityStatus.DEFICIT_STRETCH -> RosePink.copy(alpha = 0.15f)
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CFO Live Assessment",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = when (simulationResult.feasibilityStatus) {
                                        GoalFeasibilityStatus.ON_TRACK -> Emerald700
                                        GoalFeasibilityStatus.MODERATE_STRETCH -> OceanTeal
                                        GoalFeasibilityStatus.TIGHT_BUFFER -> GoldAccent
                                        GoalFeasibilityStatus.DEFICIT_STRETCH -> RosePink
                                    }
                                )
                                Text(
                                    text = "Req: ${CurrencyFormatter.formatInr(simulationResult.requiredMonthlySavings)}/mo",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = simulationResult.verdict,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = simulationResult.detailedAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // One-Click Button: "Save & Track this Goal"
                            Button(
                                onClick = {
                                    if (simTargetAmount > 0 && simTitle.isNotBlank()) {
                                        onAddGoal(
                                            simTitle,
                                            simCategory,
                                            simTargetAmount,
                                            simSavedAmount,
                                            simMonths,
                                            "MEDIUM",
                                            "Created from CFO Simulator"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cfo_save_simulated_goal_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save & Track This Goal Live", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // CFO Executive Recommendations Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CFO Strategic Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    report.recommendations.forEach { rec ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = rec,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (report.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        report.warnings.forEach { warn ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = RosePink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = warn,
                                    fontSize = 13.sp,
                                    color = RosePink,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Set New Goal Dialog
    if (showAddGoalDialog) {
        SetGoalDialog(
            monthlySurplus = report.netSurplus,
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, cat, target, saved, months, prio, notes ->
                onAddGoal(title, cat, target, saved, months, prio, notes)
                showAddGoalDialog = false
            }
        )
    }

    // Add Contribution Dialog
    contributingGoal?.let { goal ->
        AddGoalContributionDialog(
            goal = goal,
            onDismiss = { contributingGoal = null },
            onConfirm = { amount ->
                onAddGoalContribution(goal.id, amount)
                contributingGoal = null
            }
        )
    }
}

@Composable
fun GoalTrackingCard(
    insight: GoalFeasibilityInsight,
    onAddSavings: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (insight.feasibilityStatus) {
        GoalFeasibilityStatus.ON_TRACK -> Emerald500
        GoalFeasibilityStatus.MODERATE_STRETCH -> OceanTeal
        GoalFeasibilityStatus.TIGHT_BUFFER -> GoldAccent
        GoalFeasibilityStatus.DEFICIT_STRETCH -> RosePink
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${insight.goalId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category Icon, Title, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(insight.category),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = insight.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${insight.targetMonths} Mo Target • ${CurrencyFormatter.formatInr(insight.requiredMonthlySavings)}/mo needed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("goal_toggle_complete_${insight.goalId}")
                    ) {
                        Icon(
                            imageVector = if (insight.isCompleted) Icons.Default.CheckCircle else Icons.Default.Star,
                            contentDescription = "Toggle Complete",
                            tint = if (insight.isCompleted) Emerald500 else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("goal_delete_${insight.goalId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Goal",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Progress Bar & Numbers
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved: ${CurrencyFormatter.formatInr(insight.currentSaved)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Target: ${CurrencyFormatter.formatInr(insight.targetAmount)} (${String.format(Locale.US, "%.0f", insight.progressPercent)}%)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                LinearProgressIndicator(
                    progress = { insight.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (insight.remainingAmount > 0) {
                    Text(
                        text = "Remaining to save: ${CurrencyFormatter.formatInr(insight.remainingAmount)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // CFO Live Feasibility Badge & Advice
            Card(
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CFO Status: ${insight.statusLabel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = statusColor
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f", insight.surplusSharePercent)}% of surplus",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = insight.cfoVerdict,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = insight.cfoRecommendation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Add Savings Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onAddSavings,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("goal_add_contribution_${insight.goalId}")
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Savings Contribution", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SetGoalDialog(
    monthlySurplus: Double,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, target: Double, saved: Double, months: Int, priority: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("CUSTOM") }
    var targetText by remember { mutableStateOf("") }
    var savedText by remember { mutableStateOf("0") }
    var months by remember { mutableIntStateOf(12) }
    var priority by remember { mutableStateOf("MEDIUM") }
    var notes by remember { mutableStateOf("") }

    val targetVal = targetText.toDoubleOrNull() ?: 0.0
    val savedVal = savedText.toDoubleOrNull() ?: 0.0
    val remaining = maxOf(0.0, targetVal - savedVal)
    val requiredMonthly = if (months > 0 && remaining > 0) remaining / months else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("add_goal_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Set Financial Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. New Home Down Payment)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_goal_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Amount (₹)") },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("new_goal_target_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = savedText,
                        onValueChange = { savedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Saved (₹)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_goal_saved_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Horizon & Priority
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Horizon: $months Months", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(6, 12, 24, 36).forEach { m ->
                            FilterChip(
                                selected = months == m,
                                onClick = { months = m },
                                label = { Text("${m}m", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Instant CFO Feasibility Check
                if (targetVal > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Monthly Required: ${CurrencyFormatter.formatInr(requiredMonthly)}/mo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (monthlySurplus >= requiredMonthly)
                                    "✓ Supported by current monthly surplus of ${CurrencyFormatter.formatInr(monthlySurplus)}"
                                else
                                    "⚠️ Exceeds current surplus (${CurrencyFormatter.formatInr(monthlySurplus)}). Consider longer horizon.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && targetVal > 0) {
                                onSave(title, category, targetVal, savedVal, months, priority, notes)
                            }
                        },
                        enabled = title.isNotBlank() && targetVal > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_new_goal_btn")
                    ) {
                        Text("Create Goal")
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalContributionDialog(
    goal: FinancialGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("10000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_contribution_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Savings to Goal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Goal: ${goal.title}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Contribution Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contribution_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5000, 10000, 25000, 50000).forEach { quick ->
                        OutlinedButton(
                            onClick = { amountText = quick.toString() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("+₹${quick / 1000}k", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onConfirm(amt)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_contribution_btn")
                    ) {
                        Text("Add Savings")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Emerald500 else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GoalPresetChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.uppercase()) {
        "EMERGENCY" -> Icons.Default.Shield
        "VACATION" -> Icons.Default.FlightTakeoff
        "HOME" -> Icons.Default.Home
        "VEHICLE" -> Icons.Default.DirectionsCar
        "EDUCATION" -> Icons.Default.School
        else -> Icons.Default.TrackChanges
    }
}
