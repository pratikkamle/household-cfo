package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.JointPurple
import com.example.ui.theme.JointPurpleLight
import com.example.ui.theme.OceanTeal
import com.example.ui.theme.OceanTealLight
import com.example.ui.theme.RosePink
import com.example.ui.theme.RosePinkLight

enum class HouseholdMember(
    val id: String,
    val displayName: String,
    val initial: String,
    val color: Color,
    val lightColor: Color
) {
    PRIMARY("PRIMARY", "Primary", "P", OceanTeal, OceanTealLight),
    PARTNER("PARTNER", "Partner", "S", RosePink, RosePinkLight),
    JOINT("JOINT", "Joint", "J", JointPurple, JointPurpleLight);

    companion object {
        val PRATIK = PRIMARY
        val MANASVI = PARTNER

        fun fromId(id: String?): HouseholdMember? =
            if (id == null) null
            else entries.find {
                it.id.equals(id, ignoreCase = true) ||
                (it == PRIMARY && id.equals("PRATIK", ignoreCase = true)) ||
                (it == PARTNER && id.equals("MANASVI", ignoreCase = true))
            } ?: JOINT
    }
}

/**
 * Household roles with permission matrix
 */
enum class UserRole(val displayName: String, val badgeColorHex: String, val description: String) {
    ADMIN("Admin", "#2563EB", "Full household control: Manage members, promote roles, reset data, budget & accounts"),
    CONTRIBUTOR("Contributor", "#0D9488", "Can update accounts, cards, assets, and edit budget targets"),
    MEMBER("Member", "#D97706", "Can view dashboard, log expenses/incomes, and download reports"),
    USER("User", "#64748B", "Read-only access: View dashboard and expense summaries");

    val canViewAccounts: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canEditAccounts: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canEditBudgets: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canManageMembers: Boolean get() = this == ADMIN
    val canResetDatabase: Boolean get() = this == ADMIN
    val canAddTransactions: Boolean get() = this != USER
    val canExportReports: Boolean get() = this != USER

    companion object {
        fun fromString(role: String?): UserRole = when (role?.uppercase()?.trim()) {
            "ADMIN" -> ADMIN
            "CONTRIBUTOR" -> CONTRIBUTOR
            "USER" -> USER
            else -> MEMBER
        }
    }
}

/**
 * Generalized member profile supporting any number of household members (1, 2, 3, 4+)
 */
data class MemberProfile(
    val id: String, // Normalized identifier e.g. "PRATIK", "MANASVI", "ROHAN"
    val displayName: String,
    val initial: String,
    val color: Color,
    val lightColor: Color,
    val role: String = "MEMBER", // "ADMIN", "CONTRIBUTOR", "MEMBER", "USER"
    val userId: Long = 0L,
    val email: String = ""
) {
    val userRole: UserRole get() = UserRole.fromString(role)
}

val JointMemberProfile = MemberProfile(
    id = "JOINT",
    displayName = "Joint / Household",
    initial = "J",
    color = JointPurple,
    lightColor = JointPurpleLight,
    role = "SHARED"
)

val MemberColorPalette = listOf(
    Pair("#0D9488", "#CCFBF1"), // Teal
    Pair("#E11D48", "#FFE4E6"), // Rose
    Pair("#4F46E5", "#EEF2FF"), // Indigo
    Pair("#D97706", "#FEF3C7"), // Amber
    Pair("#059669", "#D1FAE5"), // Emerald
    Pair("#7C3AED", "#EDE9FE"), // Violet
    Pair("#EA580C", "#FFEDD5"), // Orange
    Pair("#0284C7", "#E0F2FE")  // Sky Blue
)

fun parseHexColor(hex: String, defaultColor: Color = OceanTeal): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

fun UserEntity.toMemberProfile(): MemberProfile {
    val baseColor = parseHexColor(colorHex, OceanTeal)
    return MemberProfile(
        id = name.uppercase().trim(),
        displayName = name,
        initial = initial.ifBlank { name.take(1).uppercase() },
        color = baseColor,
        lightColor = baseColor.copy(alpha = 0.16f),
        role = role,
        userId = id,
        email = email
    )
}

fun matchesMember(paidBy: String?, memberName: String?, members: List<MemberProfile> = emptyList()): Boolean {
    if (paidBy.isNullOrBlank() || memberName.isNullOrBlank()) return false
    val p = paidBy.trim().uppercase()
    val m = memberName.trim().uppercase()

    // Joint is shared across all members, never mapped to a single individual member
    if (isJoint(p) || isJoint(m)) return false

    if (p == m) return true

    // Check against members list if available
    if (members.isNotEmpty()) {
        val target = members.find { it.displayName.equals(memberName, ignoreCase = true) || it.id.equals(memberName, ignoreCase = true) }
        val payer = members.find { it.displayName.equals(paidBy, ignoreCase = true) || it.id.equals(paidBy, ignoreCase = true) }
        if (target != null && payer != null && target.userId > 0 && target.userId == payer.userId) return true
        if (target != null && (p == "PRIMARY" || p == "ADMIN") && target.userRole == UserRole.ADMIN) return true
        if (payer != null && (m == "PRIMARY" || m == "ADMIN") && payer.userRole == UserRole.ADMIN) return true
        if (target != null && (p == "PARTNER" || p == "SPOUSE" || p == "MEMBER" || p == "CONTRIBUTOR") && target.userRole != UserRole.ADMIN) return true
        if (payer != null && (m == "PARTNER" || m == "SPOUSE" || m == "MEMBER" || m == "CONTRIBUTOR") && payer.userRole != UserRole.ADMIN) return true
    }

    if (p == "PRIMARY" && (m == "PRATIK" || m == "ADMIN")) return true
    if (m == "PRIMARY" && (p == "PRATIK" || p == "ADMIN")) return true
    if (p == "PARTNER" && (m == "MANASVI" || m == "PARTNER")) return true
    if (m == "PARTNER" && (p == "MANASVI" || p == "PARTNER")) return true
    if (p.startsWith(m) || m.startsWith(p)) return true
    if (p.contains(m) || m.contains(p)) return true
    return false
}

fun isJoint(paidBy: String?): Boolean {
    if (paidBy.isNullOrBlank()) return false
    val p = paidBy.trim().uppercase()
    return p == "JOINT" || p.contains("JOINT") || p.contains("HOUSEHOLD") || p.contains("SHARED")
}
