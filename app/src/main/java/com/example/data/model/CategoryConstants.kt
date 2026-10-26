package com.example.data.model

data class CategoryMeta(
    val name: String,
    val colorHex: String,
    val isEssential: Boolean,
    val defaultWeight: Double = 0.1
)

object CategoryConstants {
    val DEFAULT_CATEGORIES = listOf(
        CategoryMeta("Groceries", "#10B981", isEssential = true, defaultWeight = 0.28),
        CategoryMeta("Dining & Food", "#F59E0B", isEssential = false, defaultWeight = 0.16),
        CategoryMeta("Utilities & Bills", "#3B82F6", isEssential = true, defaultWeight = 0.14),
        CategoryMeta("Transportation & Fuel", "#6366F1", isEssential = true, defaultWeight = 0.12),
        CategoryMeta("Shopping", "#EC4899", isEssential = false, defaultWeight = 0.10),
        CategoryMeta("Entertainment", "#8B5CF6", isEssential = false, defaultWeight = 0.08),
        CategoryMeta("Healthcare", "#EF4444", isEssential = true, defaultWeight = 0.07),
        CategoryMeta("Miscellaneous", "#64748B", isEssential = false, defaultWeight = 0.05)
    )

    val CATEGORIES: List<String> = DEFAULT_CATEGORIES.map { it.name }

    fun getColorHex(category: String): String {
        return DEFAULT_CATEGORIES.firstOrNull { it.name.equals(category, ignoreCase = true) }?.colorHex ?: "#64748B"
    }
}
