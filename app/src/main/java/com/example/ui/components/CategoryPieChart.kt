package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// High-contrast, polished Material 3 Palette for category pie slices
private val PieChartPalette = listOf(
    Color(0xFF0D9488), // Teal
    Color(0xFF0284C7), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF59E0B), // Amber
    Color(0xFFE11D48), // Rose
    Color(0xFF10B981), // Emerald
    Color(0xFFEC4899), // Pink
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6), // Cyan
    Color(0xFF84CC16), // Lime
    Color(0xFFF97316), // Orange
    Color(0xFF64748B)  // Slate
)

data class PieSliceData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPieChart(
    categoryExpenses: List<Pair<String, Double>>,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val validEntries = remember(categoryExpenses) {
        categoryExpenses.filter { it.second > 0 }
    }

    if (validEntries.isEmpty() || totalExpense <= 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Expense Distribution by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No expenses logged yet this month to visualize.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Build slices
    val slices = remember(validEntries, totalExpense) {
        var currentAngle = -90f // Start at 12 o'clock
        validEntries.mapIndexed { index, (cat, amount) ->
            val fraction = (amount / totalExpense).toFloat()
            val sweep = fraction * 360f
            val sliceColor = PieChartPalette[index % PieChartPalette.size]
            val slice = PieSliceData(
                category = cat,
                amount = amount,
                percentage = fraction,
                startAngle = currentAngle,
                sweepAngle = sweep,
                color = sliceColor
            )
            currentAngle += sweep
            slice
        }
    }

    val selectedSlice = slices.find { it.category.equals(selectedCategory, ignoreCase = true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_pie_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Expenses Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any slice or legend item for live inspection",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedCategory != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { selectedCategory = null }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Reset Selection",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Interactive Donut Chart Canvas with Center Inspection Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(230.dp)
                        .pointerInput(slices) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                val minDim = minOf(size.width, size.height).toFloat()
                                val outerRadius = (minDim / 2f) * 0.92f
                                val innerRadius = outerRadius * 0.58f

                                // Check if tap falls within the donut ring
                                if (distance in innerRadius..outerRadius) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    // Normalize to [-90, 270)
                                    if (angle < -90f) angle += 360f

                                    val tappedSlice = slices.find { slice ->
                                        val start = slice.startAngle
                                        val end = slice.startAngle + slice.sweepAngle
                                        angle in start..end
                                    }

                                    selectedCategory = if (selectedCategory == tappedSlice?.category) {
                                        null
                                    } else {
                                        tappedSlice?.category
                                    }
                                } else if (distance < innerRadius) {
                                    // Tapping center resets selection
                                    selectedCategory = null
                                }
                            }
                        }
                        .testTag("pie_chart_canvas")
                ) {
                    val strokeWidth = size.minDimension * 0.22f
                    val arcSize = size.minDimension * 0.76f
                    val topLeft = Offset(
                        (size.width - arcSize) / 2f,
                        (size.height - arcSize) / 2f
                    )

                    slices.forEach { slice ->
                        val isSelected = selectedCategory != null &&
                                slice.category.equals(selectedCategory, ignoreCase = true)
                        val isAnySelected = selectedCategory != null

                        val effectiveAlpha = if (isAnySelected) {
                            if (isSelected) 1.0f else 0.35f
                        } else 1.0f

                        val currentStroke = if (isSelected) strokeWidth * 1.15f else strokeWidth

                        drawArc(
                            color = slice.color.copy(alpha = effectiveAlpha),
                            startAngle = slice.startAngle + 1f, // 1 deg gap for crisp separation
                            sweepAngle = (slice.sweepAngle - 2f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center inspection display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (selectedSlice != null) {
                        Text(
                            text = selectedSlice.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = selectedSlice.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatInr(selectedSlice.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%% of total", selectedSlice.percentage * 100),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "TOTAL EXPENSES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatInr(totalExpense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${slices.size} Categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interactive Category Legend Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val isSelected = selectedCategory != null &&
                            slice.category.equals(selectedCategory, ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) slice.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, slice.color) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCategory = if (isSelected) null else slice.category
                            }
                            .testTag("pie_legend_${slice.category.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Text(
                                text = slice.category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(slice.percentage * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
