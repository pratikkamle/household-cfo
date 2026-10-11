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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FinancialGoalEntity
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.OceanTeal
import com.example.ui.theme.RosePink
import com.example.ui.viewmodel.MonthSummary
import com.example.util.CfoReviewReport
import com.example.util.CurrencyFormatter
import com.example.util.GoalFeasibilityStatus
import com.example.util.GoalSimulationResult
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CfoAdvisorDialog(
    report: CfoReviewReport,
    goals: List<FinancialGoalEntity>,
    monthSummary: MonthSummary,
    onAddGoal: (title: String, category: String, targetAmount: Double, currentSaved: Double, targetMonths: Int, priority: String, notes: String) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddGoalContribution: (Long, Double) -> Unit,
    onToggleGoalCompleted: (Long) -> Unit,
    onSimulateGoal: (title: String, targetAmount: Double, currentSaved: Double, targetMonths: Int) -> GoalSimulationResult,
    onDismiss: () -> Unit
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<FinancialGoalEntity?>(null) }

    // Simulator State
    var simTitle by remember { mutableStateOf("Family Goal") }
    var simCategory by remember { mutableStateOf("CUSTOM") }
    var simTargetText by remember { mutableStateOf("200000") }
    var simSavedText by remember { mutableStateOf("20000") }
    var simMonths by remember { mutableIntStateOf(10) }

    val simTargetAmount = simTargetText.toDoubleOrNull() ?: 0.0
    val simSavedAmount = simSavedText.toDoubleOrNull() ?: 0.0

    val simulationResult = remember(simTitle, simTargetAmount, simSavedAmount, simMonths, report.netSurplus) {
        onSimulateGoal(simTitle, simTargetAmount, simSavedAmount, simMonths)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 12.dp)
                .testTag("cfo_advisor_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                text = "Family CFO Intelligence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live Goals & Cashflow Feasibility",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Health Score Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.healthGrade,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.healthScore}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Emerald500
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Live Tracked Goals Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Household Goals (${goals.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddGoalDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Goal", fontSize = 12.sp)
                    }
                }

                if (goals.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No Goals Tracked Yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Set your own custom goals or ask the CFO simulator below to evaluate any purchase or savings milestone.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    report.goalInsights.forEach { insight ->
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Interactive Simulator: "Ask CFO About Your Goal"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = InvestmentBlue, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Ask CFO: Live Goal Feasibility",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Presets
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GoalPresetChip(
                                label = "🛡️ Emergency Fund",
                                onClick = {
                                    val exp = maxOf(30000.0, monthSummary.scheduledExpenses + monthSummary.randomExpenses)
                                    simTitle = "Emergency Reserve"
                                    simCategory = "EMERGENCY"
                                    simTargetText = (exp * 6).toInt().toString()
                                    simSavedText = "50000"
                                    simMonths = 12
                                }
                            )
                            GoalPresetChip(
                                label = "✈️ Vacation",
                                onClick = {
                                    simTitle = "Family Vacation"
                                    simCategory = "VACATION"
                                    simTargetText = "250000"
                                    simSavedText = "30000"
                                    simMonths = 8
                                }
                            )
                            GoalPresetChip(
                                label = "🏡 Home Upgrade",
                                onClick = {
                                    simTitle = "Home Upgrade"
                                    simCategory = "HOME"
                                    simTargetText = "400000"
                                    simSavedText = "50000"
                                    simMonths = 12
                                }
                            )
                        }

                        OutlinedTextField(
                            value = simTitle,
                            onValueChange = { simTitle = it },
                            label = { Text("What is your goal?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = simTargetText,
                                onValueChange = { simTargetText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Target (₹)") },
                                modifier = Modifier.weight(1.2f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = simSavedText,
                                onValueChange = { simSavedText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Saved (₹)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Timeframe: $simMonths Months", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(3, 6, 12, 18, 24).forEach { m ->
                                    FilterChip(
                                        selected = simMonths == m,
                                        onClick = { simMonths = m },
                                        label = { Text("${m}m", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        // Simulation Verdict
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (simulationResult.feasibilityStatus) {
                                    GoalFeasibilityStatus.ON_TRACK -> Emerald500.copy(alpha = 0.12f)
                                    GoalFeasibilityStatus.MODERATE_STRETCH -> OceanTeal.copy(alpha = 0.12f)
                                    GoalFeasibilityStatus.TIGHT_BUFFER -> GoldAccent.copy(alpha = 0.15f)
                                    GoalFeasibilityStatus.DEFICIT_STRETCH -> RosePink.copy(alpha = 0.15f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = simulationResult.verdict,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = simulationResult.detailedAdvice,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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
                                                "Saved from CFO Simulator"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save & Track This Goal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cfo_dialog_close_bottom_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close Review", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

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
