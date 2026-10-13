package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.MonthSummary
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates rich, multi-sheet Microsoft Excel (.xlsx) workbooks adhering to
 * standard Office OpenXML (SpreadsheetML) specifications.
 */
object XlsxExportManager {

    private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)

    fun exportToXlsx(
        context: Context,
        summary: MonthSummary,
        transactions: List<TransactionEntity>,
        recurringRules: List<RecurringRuleEntity>,
        budgets: List<CategoryBudgetEntity>,
        goals: List<FinancialGoalEntity>,
        goalInsights: List<GoalFeasibilityInsight>,
        accounts: List<AccountEntity>
    ): File {
        val cleanMonth = summary.monthYear.replace("-", "_")
        val fileName = "Household_CFO_Export_${cleanMonth}.xlsx"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, fileName)

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())

                // 2. _rels/.rels
                writeZipEntry(zos, "_rels/.rels", buildRootRelsXml())

                // 3. xl/_rels/workbook.xml.rels
                writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())

                // 4. xl/workbook.xml
                writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml())

                // 5. xl/styles.xml
                writeZipEntry(zos, "xl/styles.xml", buildStylesXml())

                // 6. Sheet 1: Overview & Cashflow
                writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildOverviewSheet(summary))

                // 7. Sheet 2: Category Expenses
                writeZipEntry(zos, "xl/worksheets/sheet2.xml", buildCategoryExpensesSheet(budgets, transactions, summary.discretionaryBudget))

                // 8. Sheet 3: Scheduled Commitments
                writeZipEntry(zos, "xl/worksheets/sheet3.xml", buildCommitmentsSheet(recurringRules))

                // 9. Sheet 4: Incomes
                writeZipEntry(zos, "xl/worksheets/sheet4.xml", buildIncomesSheet(transactions))

                // 10. Sheet 5: Goals & Tracking
                writeZipEntry(zos, "xl/worksheets/sheet5.xml", buildGoalsSheet(goals, goalInsights))

                // 11. Sheet 6: Accounts & Assets
                writeZipEntry(zos, "xl/worksheets/sheet6.xml", buildAccountsSheet(accounts))

                // 12. Sheet 7: All Transactions Ledger
                writeZipEntry(zos, "xl/worksheets/sheet7.xml", buildTransactionsSheet(transactions))
            }
        }

        return outputFile
    }

    fun shareXlsx(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Household CFO Multi-Sheet Financial Report - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share or Open Excel (.xlsx) Report"))
        } catch (e: Exception) {
            Log.e("XlsxExportManager", "Failed to share xlsx file: ${e.message}", e)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryPath: String, content: String) {
        val entry = ZipEntry(entryPath)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet6.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet7.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet6.xml"/>
  <Relationship Id="rId7" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet7.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Overview &amp; Cashflow" sheetId="1" r:id="rId1"/>
    <sheet name="Category Expenses" sheetId="2" r:id="rId2"/>
    <sheet name="Scheduled Commitments" sheetId="3" r:id="rId3"/>
    <sheet name="Incomes" sheetId="4" r:id="rId4"/>
    <sheet name="Goals &amp; Tracking" sheetId="5" r:id="rId5"/>
    <sheet name="Accounts &amp; Assets" sheetId="6" r:id="rId6"/>
    <sheet name="Transactions Ledger" sheetId="7" r:id="rId7"/>
  </sheets>
</workbook>"""
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font>
      <sz val="11"/>
      <color theme="1"/>
      <name val="Calibri"/>
    </font>
    <font>
      <b/>
      <sz val="11"/>
      <color rgb="FFFFFFFF"/>
      <name val="Calibri"/>
    </font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill>
      <patternFill patternType="solid">
        <fgColor rgb="FF0D9488"/>
      </patternFill>
    </fill>
  </fills>
  <borders count="1">
    <border>
      <left/><right/><top/><bottom/><diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""
    }

    // 1. Overview Sheet
    private fun buildOverviewSheet(summary: MonthSummary): String {
        val b = SheetBuilder()
        b.addRow("Household Executive Financial Summary", isHeader = true)
        b.addRow("Reporting Month", summary.displayMonth)
        b.addRow()
        b.addRow("Metric", "Amount (₹)", isHeader = true)
        b.addRow("Total Household Income", summary.totalIncome)
        b.addRow("Core Scheduled Commitments (EMI, Rent, Bills)", summary.scheduledExpenses)
        b.addRow("Discretionary / Random Spends", summary.randomExpenses)
        b.addRow("Investments & Wealth SIPs", summary.investments)
        b.addRow("Net Monthly Available Cash Flow", summary.availableBalance)
        b.addRow()
        b.addRow("Discretionary Budget Allocation", summary.discretionaryBudget)
        b.addRow("Remaining Discretionary Buffer", summary.remainingDiscretionary)
        return b.build()
    }

    // 2. Category Expenses Sheet
    private fun buildCategoryExpensesSheet(
        budgets: List<CategoryBudgetEntity>,
        transactions: List<TransactionEntity>,
        monthlyDiscretionaryBudget: Double
    ): String {
        val b = SheetBuilder()
        b.addRow("Category-Wise Expense Breakdown & Budget Tracking", isHeader = true)
        b.addRow("Total Discretionary Budget", monthlyDiscretionaryBudget)
        b.addRow()
        b.addRow(
            "Category Name",
            "Allocated Budget (₹)",
            "Actual Spent (₹)",
            "Remaining Buffer (₹)",
            "Utilization (%)",
            "Status",
            isHeader = true
        )

        val expensesByCategory = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }

        // Combine all categories from budgets and transactions
        val allCategories = (budgets.map { it.category } + expensesByCategory.keys).distinct().sorted()

        allCategories.forEach { cat ->
            val budget = budgets.find { it.category.equals(cat, ignoreCase = true) }?.monthlyBudget ?: 0.0
            val spent = expensesByCategory[cat] ?: 0.0
            val remaining = maxOf(0.0, budget - spent)
            val util = if (budget > 0) (spent / budget * 100) else if (spent > 0) 100.0 else 0.0
            val status = when {
                budget > 0 && spent > budget -> "OVER_BUDGET"
                budget > 0 && spent >= budget * 0.9 -> "NEAR_LIMIT"
                spent > 0 -> "WITHIN_BUDGET"
                else -> "UNSPENT"
            }
            b.addRow(
                cat,
                budget,
                spent,
                remaining,
                String.format(Locale.US, "%.1f%%", util),
                status
            )
        }

        return b.build()
    }

    // 3. Scheduled Commitments Sheet
    private fun buildCommitmentsSheet(rules: List<RecurringRuleEntity>): String {
        val b = SheetBuilder()
        b.addRow("Household Scheduled Commitments & Recurring Rules", "Total Rules: ${rules.size}", isHeader = true)
        b.addRow()
        b.addRow("Rule Title", "Category", "Payer", "Amount (₹)", "Type", "Cadence / Frequency", "Next Due Date", "Active Status", isHeader = true)
        rules.forEach { rule ->
            val dueStr = if (rule.dueMonth != null) "Month ${rule.dueMonth}, Day ${rule.dueDay}" else "Day ${rule.dueDay} of month"
            b.addRow(
                rule.title,
                rule.category,
                rule.paidBy,
                rule.amount,
                rule.type,
                rule.frequency,
                dueStr,
                if (rule.isActive) "ACTIVE" else "PAUSED"
            )
        }
        return b.build()
    }

    // 4. Incomes Sheet
    private fun buildIncomesSheet(transactions: List<TransactionEntity>): String {
        val b = SheetBuilder()
        val incomes = transactions.filter { it.type == "INCOME" }
        b.addRow("Household Income Streams", "Total Records: ${incomes.size}", isHeader = true)
        b.addRow()
        b.addRow("Date", "Source / Employer", "Category", "Recipient / Payer", "Amount (₹)", "Payment Method", "Account", "Notes", isHeader = true)
        incomes.forEach { tx ->
            val dateStr = DATE_FORMAT.format(Date(tx.timestamp))
            b.addRow(
                dateStr,
                tx.merchant.ifBlank { "Income" },
                tx.category,
                tx.paidBy,
                tx.amount,
                tx.paymentMethod,
                tx.accountName,
                tx.notes
            )
        }
        return b.build()
    }

    // 5. Goals & Tracking Sheet
    private fun buildGoalsSheet(
        goals: List<FinancialGoalEntity>,
        insights: List<GoalFeasibilityInsight>
    ): String {
        val b = SheetBuilder()
        b.addRow("Financial Goals & Tracking", "Active Goals: ${goals.size}", isHeader = true)
        b.addRow()
        b.addRow(
            "Goal Title",
            "Category",
            "Target (₹)",
            "Saved So Far (₹)",
            "Remaining Target (₹)",
            "Timeline (Months)",
            "Req. Monthly Savings (₹)",
            "Feasibility Status",
            "Priority",
            "Completed",
            "CFO Strategic Advice",
            isHeader = true
        )

        goals.forEach { goal ->
            val insight = insights.find { it.goalId == goal.id }
            val remaining = maxOf(0.0, goal.targetAmount - goal.currentSaved)
            val reqMonthly = insight?.requiredMonthlySavings ?: if (goal.targetMonths > 0) (remaining / goal.targetMonths) else remaining
            val status = insight?.feasibilityStatus?.name ?: if (goal.isCompleted) "COMPLETED" else "IN_PROGRESS"
            val advice = insight?.cfoRecommendation ?: goal.notes

            b.addRow(
                goal.title,
                goal.category,
                goal.targetAmount,
                goal.currentSaved,
                remaining,
                goal.targetMonths,
                reqMonthly,
                status,
                goal.priority,
                if (goal.isCompleted) "YES" else "NO",
                advice
            )
        }
        return b.build()
    }

    // 6. Accounts & Assets Sheet
    private fun buildAccountsSheet(accounts: List<AccountEntity>): String {
        val b = SheetBuilder()
        b.addRow("Linked Bank Accounts & Assets", "Total Count: ${accounts.size}", isHeader = true)
        b.addRow()
        b.addRow("Account Name", "Institution / Bank", "Type", "Holder", "Current Balance (₹)", "Credit Limit (₹)", "Account Number", "Notes", isHeader = true)
        accounts.forEach { acc ->
            b.addRow(
                acc.name,
                acc.institution,
                acc.accountType,
                acc.holder,
                acc.balance,
                acc.creditLimit,
                acc.accountNumberMask.ifBlank { "••••" },
                acc.notes
            )
        }
        return b.build()
    }

    // 7. All Transactions Ledger Sheet
    private fun buildTransactionsSheet(transactions: List<TransactionEntity>): String {
        val b = SheetBuilder()
        b.addRow("Master Financial Transactions Ledger", "Total Transactions: ${transactions.size}", isHeader = true)
        b.addRow()
        b.addRow(
            "Transaction ID",
            "Date",
            "Merchant / Description",
            "Category",
            "Paid By",
            "Amount (₹)",
            "Type",
            "Payment Method",
            "Account",
            "Nature / Commitment",
            "Notes",
            isHeader = true
        )
        transactions.forEach { tx ->
            val dateStr = DATE_FORMAT.format(Date(tx.timestamp))
            b.addRow(
                tx.id,
                dateStr,
                tx.merchant,
                tx.category,
                tx.paidBy,
                tx.amount,
                tx.type,
                tx.paymentMethod,
                tx.accountName,
                tx.nature,
                tx.notes
            )
        }
        return b.build()
    }

    /**
     * XML Sheet Builder creating valid SpreadsheetML sheet XML
     */
    private class SheetBuilder {
        private val sb = StringBuilder()
        private var rowIndex = 1

        init {
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sb.append("""<sheetData>""")
        }

        fun addRow(vararg values: Any?, isHeader: Boolean = false) {
            val styleAttr = if (isHeader) """ s="1"""" else ""
            sb.append("""<row r="$rowIndex">""")
            values.forEachIndexed { colIndex, v ->
                val colRef = getColumnLetter(colIndex + 1) + rowIndex
                if (v != null) {
                    when (v) {
                        is Number -> {
                            sb.append("""<c r="$colRef"$styleAttr><v>$v</v></c>""")
                        }
                        else -> {
                            val escaped = escapeXml(v.toString())
                            sb.append("""<c r="$colRef" t="inlineStr"$styleAttr><is><t>$escaped</t></is></c>""")
                        }
                    }
                } else {
                    sb.append("""<c r="$colRef"$styleAttr/>""")
                }
            }
            sb.append("""</row>""")
            rowIndex++
        }

        fun build(): String {
            sb.append("""</sheetData>""")
            sb.append("""</worksheet>""")
            return sb.toString()
        }

        private fun getColumnLetter(columnNumber: Int): String {
            var num = columnNumber
            val sb = StringBuilder()
            while (num > 0) {
                val modulo = (num - 1) % 26
                sb.insert(0, ('A'.code + modulo).toChar())
                num = (num - modulo) / 26
            }
            return sb.toString()
        }

        private fun escapeXml(input: String): String {
            return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }
    }
}
