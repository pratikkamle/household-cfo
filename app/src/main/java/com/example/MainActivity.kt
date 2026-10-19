package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.HouseholdCFOTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Firebase init warning: ${e.message}")
        }

        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w("MainActivity", "EdgeToEdge warning: ${t.message}")
        }

        setContent {
            HouseholdCFOTheme {
                val isAuth by viewModel.isUserAuthenticated.collectAsStateWithLifecycle()
                if (!isAuth) {
                    AuthScreen(
                        onSignInWithEmail = { email, pass, onResult ->
                            viewModel.signInWithEmail(email, pass, onResult)
                        },
                        onRegisterWithEmail = { name, email, pass, onResult ->
                            viewModel.registerWithEmail(name, email, pass, onResult)
                        },
                        onSignInWithGoogle = { act ->
                            viewModel.signInWithGoogle(act)
                        },
                        onContinueOffline = { name, email ->
                            viewModel.continueAsOfflineAdmin(name, email)
                        }
                    )
                } else {
                    MainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: FinanceViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val summary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val household by viewModel.household.collectAsStateWithLifecycle()
    val isSensitiveRevealed by viewModel.isSensitiveRevealed.collectAsStateWithLifecycle()
    val latestDetectedTx by viewModel.latestDetectedTx.collectAsStateWithLifecycle()

    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var isAddIncomeOpen by remember { mutableStateOf(false) }
    var isCfoAdvisorOpen by remember { mutableStateOf(false) }
    var isSmartDetectOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (activeTab) {
                                0 -> "Household CFO"
                                1 -> "Ledger & Activity"
                                2 -> "50/30/20 Budget"
                                3 -> "Accounts & Assets"
                                4 -> "Recurring & Bills"
                                else -> "Executive Reports"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = household?.name ?: "Family Finances",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isCfoAdvisorOpen = true }) {
                        Icon(Icons.Default.AutoGraph, contentDescription = "CFO Advisor")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
                    label = { Text("Ledger") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Budget") },
                    label = { Text("Budget") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                    label = { Text("Accounts") }
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.Autorenew, contentDescription = "Recurring") },
                    label = { Text("Bills") }
                )
                NavigationBarItem(
                    selected = activeTab == 5,
                    onClick = { viewModel.selectTab(5) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Audit") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeTab) {
                0 -> DashboardScreen(
                    summary = summary,
                    transactions = transactions,
                    accounts = accounts,
                    recurringRules = recurringRules,
                    isSensitiveRevealed = isSensitiveRevealed,
                    onOpenAddExpense = { isAddExpenseOpen = true },
                    onOpenAddIncome = { isAddIncomeOpen = true },
                    onOpenCfoReview = { isCfoAdvisorOpen = true },
                    onOpenSmartDetect = { isSmartDetectOpen = true },
                    onViewAllTransactions = { viewModel.selectTab(1) },
                    onMarkRecurringPaid = { rule -> viewModel.markRecurringAsPaid(rule) }
                )
                1 -> TransactionsScreen(
                    transactions = transactions,
                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                    onOpenAddExpense = { isAddExpenseOpen = true }
                )
                2 -> BudgetScreen(
                    summary = summary,
                    budgets = budgets,
                    transactions = transactions,
                    goals = goals,
                    onSaveBudget = { cat, amt, nature -> viewModel.saveBudget(cat, amt, nature) },
                    onAddGoal = { title, amt, date, cat -> viewModel.addGoal(title, amt, date, cat) },
                    onAddGoalContribution = { goal, amt -> viewModel.addGoalContribution(goal, amt) }
                )
                3 -> AccountsScreen(
                    accounts = accounts,
                    isSensitiveRevealed = isSensitiveRevealed,
                    onToggleSensitivity = { viewModel.toggleSensitivityRevealed() },
                    onAddAccount = { name, type, bal, inst, last4, sens ->
                        viewModel.addAccount(name, type, bal, inst, last4, sens)
                    },
                    onDeleteAccount = { acc -> viewModel.deleteAccount(acc) }
                )
                4 -> RecurringScreen(
                    recurringRules = recurringRules,
                    accounts = accounts,
                    onAddRule = { title, amt, cat, freq, due, accId, accName, nature ->
                        viewModel.addRecurringRule(title, amt, cat, freq, due, accId, accName, nature)
                    },
                    onMarkPaid = { rule -> viewModel.markRecurringAsPaid(rule) },
                    onDeleteRule = { rule -> viewModel.deleteRecurringRule(rule) }
                )
                5 -> ReportsScreen(
                    summary = summary,
                    accounts = accounts,
                    goals = goals,
                    household = household,
                    onResetDatabase = { viewModel.resetDatabase() }
                )
            }
        }
    }

    if (isAddExpenseOpen) {
        AddExpenseDialog(
            accounts = accounts,
            onDismiss = { isAddExpenseOpen = false },
            onSave = { amt, cat, merch, accId, accName, nature, notes ->
                viewModel.addExpense(amt, cat, merch, accId, accName, nature, notes)
            }
        )
    }

    if (isAddIncomeOpen) {
        AddIncomeDialog(
            accounts = accounts,
            onDismiss = { isAddIncomeOpen = false },
            onSave = { amt, cat, source, accId, accName, notes ->
                viewModel.addIncome(amt, cat, source, accId, accName, notes)
            }
        )
    }

    if (isCfoAdvisorOpen) {
        CfoAdvisorDialog(
            summary = summary,
            accounts = accounts,
            goals = goals,
            onDismiss = { isCfoAdvisorOpen = false }
        )
    }

    if (isSmartDetectOpen || latestDetectedTx != null) {
        SmartDetectDialog(
            detectedTx = latestDetectedTx,
            onDismiss = {
                isSmartDetectOpen = false
                viewModel.dismissDetectedTx()
            },
            onConfirm = { parsed ->
                viewModel.addExpense(
                    amount = parsed.amount,
                    category = parsed.category,
                    merchant = parsed.merchant,
                    accountId = accounts.firstOrNull()?.id,
                    accountName = accounts.firstOrNull()?.name ?: "Checking",
                    nature = parsed.nature,
                    notes = "Auto-detected alert"
                )
            }
        )
    }
}
