package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryConstants
import com.example.data.model.HouseholdMember
import com.example.data.model.MemberColorPalette
import com.example.data.model.MemberProfile
import com.example.data.model.parseHexColor
import com.example.ui.theme.JointPurple
import com.example.ui.theme.OceanTeal
import com.example.ui.theme.OceanTealLight

@Composable
fun MemberBadge(
    memberId: String,
    modifier: Modifier = Modifier,
    size: Int = 24
) {
    val clean = memberId.trim().uppercase()
    val (color, displayName, initial) = when (clean) {
        "JOINT" -> Triple(JointPurple, "Joint", "J")
        else -> {
            val hash = Math.abs(clean.hashCode()) % MemberColorPalette.size
            val c = parseHexColor(MemberColorPalette[hash].first, OceanTeal)
            Triple(c, memberId.replaceFirstChar { it.uppercase() }, memberId.take(1).uppercase())
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(maxOf(14.dp, (size - 10).dp))
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = displayName,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CategoryIconBadge(
    category: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val icon = getCategoryIcon(category)
    val color = getCategoryColor(category)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun CategoryIconBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Int = 40
) = CategoryIconBadge(category = categoryName, modifier = modifier, size = size.dp)

@Composable
fun MonthSelectorHeader(
    displayMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag("btn_prev_month")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }
        Text(
            text = displayMonth,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext, modifier = Modifier.testTag("btn_next_month")) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food & dining", "dining", "restaurants", "swiggy", "zomato" -> Icons.Default.LocalDining
        "groceries", "supermarket", "blinkit", "zepto" -> Icons.Default.ShoppingCart
        "shopping", "clothing", "myntra", "amazon" -> Icons.Default.Checkroom
        "fuel & travel", "fuel", "petrol", "uber", "ola" -> Icons.Default.LocalGasStation
        "travel & holidays", "flights", "hotels" -> Icons.Default.Flight
        "entertainment", "movies", "netflix", "spotify" -> Icons.Default.Movie
        "utilities & bills", "electricity", "broadband", "mobile" -> Icons.Default.Receipt
        "medical & healthcare", "pharmacy", "doctor" -> Icons.Default.LocalHospital
        "personal care", "salon", "grooming" -> Icons.Default.SelfImprovement
        "fitness & gym", "cult" -> Icons.AutoMirrored.Filled.DirectionsRun
        "insurance & lic", "lic", "car insurance", "health insurance" -> Icons.Default.Shield
        "emi & loans", "home loan", "car loan" -> Icons.Default.CarRental
        "investments & sip", "sip", "mutual funds", "zerodha" -> Icons.Default.TrendingUp
        "subscriptions" -> Icons.Default.Subscriptions
        "home maintenance", "maid", "cook" -> Icons.Default.Handyman
        "education", "courses" -> Icons.Default.School
        "gifts & donations" -> Icons.Default.CardGiftcard
        "car commute" -> Icons.Default.Commute
        else -> Icons.Default.Category
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food & dining" -> Color(0xFFF97316)
        "groceries" -> Color(0xFF10B981)
        "shopping" -> Color(0xFFEC4899)
        "fuel & travel" -> Color(0xFFEAB308)
        "travel & holidays" -> Color(0xFF06B6D4)
        "entertainment" -> Color(0xFF8B5CF6)
        "utilities & bills" -> Color(0xFF64748B)
        "medical & healthcare" -> Color(0xFFEF4444)
        "personal care" -> Color(0xFFD946EF)
        "fitness & gym" -> Color(0xFF14B8A6)
        "insurance & lic" -> Color(0xFF3B82F6)
        "emi & loans" -> Color(0xFF6366F1)
        "investments & sip" -> Color(0xFF059669)
        "subscriptions" -> Color(0xFFA855F7)
        "home maintenance" -> Color(0xFF78716C)
        else -> Color(0xFF6B7280)
    }
}

/**
 * Generalized Member Filter Row supporting 1, 2, 3, 4+ household members and Joint
 */
@Composable
fun MemberFilterRow(
    members: List<MemberProfile> = emptyList(),
    selectedMemberName: String? = null,
    onMemberNameSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMembers = remember(members) {
        members
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedMemberName == null,
            onClick = { onMemberNameSelected(null) },
            label = { Text("All Household") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_all_household")
        )

        displayMembers.forEach { member ->
            val isSelected = selectedMemberName.equals(member.displayName, ignoreCase = true) ||
                             selectedMemberName.equals(member.id, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onMemberNameSelected(member.displayName) },
                label = { Text(member.displayName) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(member.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initial,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = member.color.copy(alpha = 0.2f),
                    selectedLabelColor = member.color
                ),
                modifier = Modifier.testTag("filter_${member.displayName.lowercase()}")
            )
        }
    }
}

// Backward-compatible overload for HouseholdMember
@Composable
fun MemberFilterRow(
    selectedMember: HouseholdMember?,
    onMemberSelected: (HouseholdMember?) -> Unit,
    modifier: Modifier = Modifier
) {
    MemberFilterRow(
        members = emptyList(),
        selectedMemberName = selectedMember?.name,
        onMemberNameSelected = { name ->
            onMemberSelected(if (name == null) null else HouseholdMember.fromId(name))
        },
        modifier = modifier
    )
}
