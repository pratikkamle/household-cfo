package com.example.data.model

enum class UserRole(
    val displayName: String,
    val badgeColorHex: String,
    val description: String
) {
    ADMIN("Admin", "#2563EB", "Full household control: Manage members, promote roles, reset data, budget & accounts"),
    CONTRIBUTOR("Contributor", "#0D9488", "Can update accounts, cards, assets, and edit budget targets"),
    MEMBER("Member", "#D97706", "Can view dashboard, log expenses/incomes, and download reports"),
    USER("User", "#64748B", "Read-only access: View dashboard and expense summaries");

    val canViewAccounts: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canEditAccounts: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canEditBudgets: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canManageHousehold: Boolean get() = this == ADMIN || this == CONTRIBUTOR
    val canManageMembers: Boolean get() = this == ADMIN
    val canResetDatabase: Boolean get() = this == ADMIN
    val canAddTransactions: Boolean get() = this != USER
    val canExportReports: Boolean get() = this != USER

    companion object {
        fun fromString(role: String?): UserRole {
            val normalized = role?.uppercase()?.trim() ?: return MEMBER
            return entries.firstOrNull { it.name == normalized } ?: MEMBER
        }
    }
}

data class HouseholdMember(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarInitial: String,
    val colorHex: String,
    val photoUrl: String? = null
)

data class MemberProfile(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    val avatarInitial: String,
    val colorHex: String,
    val photoUrl: String? = null,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0
)

data class AuthAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val isAnonymous: Boolean = false,
    val photoUrl: String? = null
)
