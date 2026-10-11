package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.draw.clip
import com.example.ui.theme.GoldAccent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.sync.SyncState
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddExpenseSheet
import com.example.ui.screens.AddIncomeSheet
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CfoAdvisorDialog
import com.example.ui.screens.CfoReviewScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditBudgetPlanDialog
import com.example.ui.screens.HouseholdHubDialog
import com.example.ui.screens.RecurringScreen
import com.example.ui.screens.RegisterHouseholdDialog
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SensitivityAuthDialog
import com.example.ui.screens.SmartDetectDialog
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.Emerald500
import com.example.ui.theme.HouseholdCfoTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CfoAdvisorEngine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Firebase init warning: ${e.message}")
        }
        enableEdgeToEdge()

        setContent {
            HouseholdCfoTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    // State Collection
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val summary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMember.collectAsStateWithLifecycle()
    val selectedMemberName by viewModel.selectedMemberName.collectAsStateWithLifecycle()
    val household by viewModel.household.collectAsStateWithLifecycle()
    val members by viewModel.memberProfiles.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val activeUserRole by viewModel.activeUserRole.collectAsStateWithLifecycle()
    val isSensitiveRevealed by viewModel.isSensitiveRevealed.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()

    var showSensitivityAuthDialog by remember { mutableStateOf(false) }

    val isAddExpenseOpen by viewModel.isAddExpenseOpen.collectAsStateWithLifecycle()
    val isAddIncomeOpen by viewModel.isAddIncomeOpen.collectAsStateWithLifecycle()
    val isSmartDetectOpen by viewModel.isSmartDetectOpen.collectAsStateWithLifecycle()
    val isCfoAdvisorOpen by viewModel.isCfoAdvisorOpen.collectAsStateWithLifecycle()
    val isHouseholdHubOpen by viewModel.isHouseholdHubOpen.collectAsStateWithLifecycle()
    val isEditBudgetPlanOpen by viewModel.isEditBudgetPlanOpen.collectAsStateWithLifecycle()

    val suggestedTx by viewModel.suggestedTransaction.collectAsStateWithLifecycle()
    val merchantSuggestion by viewModel.merchantSuggestion.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val cfoReport = remember(summary, transactions, recurringRules, budgets, goals) {
        CfoAdvisorEngine.generateReport(summary, transactions, recurringRules, budgets, goals)
    }

    // Handle system back navigation: return to Dashboard tab if on another tab
    BackHandler(enabled = currentTab != 0) {
        viewModel.setTab(0)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Drawer Header - Tapping navigates to Household Hub & Profile
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .clickable {
                            scope.launch { drawerState.close() }
                            viewModel.openHouseholdHub()
                        }
                        .testTag("drawer_header_profile_hub")
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (activeUser?.name?.take(1) ?: "C"),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = household?.name ?: "Household CFO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeUser?.name ?: "Member"} • ${activeUserRole.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Manage Hub & Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald500.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Google: ${currentUser?.email ?: "Linked"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = currentTab == 0,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.setTab(0)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_dashboard")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Recent Transactions") },
                    selected = currentTab == 5,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.setTab(5)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_transactions")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text("Scheduled Commitments") },
                    selected = currentTab == 1,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.setTab(1)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_scheduled")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    label = { Text("Monthly Budget") },
                    selected = currentTab == 2,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.setTab(2)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_budget")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Reports & Analytics") },
                    selected = currentTab == 3,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.setTab(3)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_reports")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("CFO Review & Goals") },
                    selected = currentTab == 6,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.setTab(6)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_cfo")
                )

                if (activeUserRole.canViewAccounts) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        label = { Text("Bank Accounts") },
                        selected = currentTab == 4,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.setTab(4)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_accounts")
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (currentTab) {
                                    0 -> "Household CFO"
                                    1 -> "Scheduled Commitments"
                                    2 -> "Monthly Budgets"
                                    3 -> "Financial Reports"
                                    4 -> "Linked Accounts"
                                    5 -> "Recent Transactions"
                                    6 -> "CFO Intelligence"
                                    else -> "Household CFO"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = household?.name ?: "Family Finances",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentTab == 6) {
                            IconButton(
                                onClick = { viewModel.setTab(0) },
                                modifier = Modifier.testTag("top_back_to_dashboard")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
                            }
                        } else {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("top_hamburger_menu")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu")
                            }
                        }
                    },
                    actions = {
                        if (currentTab == 6) {
                            IconButton(
                                onClick = { viewModel.setTab(0) },
                                modifier = Modifier.testTag("top_cfo_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close CFO Review",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // CFO Review button on the header task bar just left to profile icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldAccent.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setTab(6) }
                                    .testTag("top_cfo_review_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CFO Review",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { viewModel.openHouseholdHub() },
                            modifier = Modifier.testTag("top_profile_hub_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (currentUser != null) {
                                        Text(
                                            text = (currentUser?.displayName?.take(1) ?: "U").uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Profile & Household Hub",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = "Scheduled") },
                        label = { Text("Scheduled") },
                        modifier = Modifier.testTag("nav_tab_recurring")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Budget") },
                        label = { Text("Budget") },
                        modifier = Modifier.testTag("nav_tab_budget")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { viewModel.setTab(3) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
                        label = { Text("Reports") },
                        modifier = Modifier.testTag("nav_tab_reports")
                    )
                    if (activeUserRole.canViewAccounts) {
                        NavigationBarItem(
                            selected = currentTab == 4,
                            onClick = { viewModel.setTab(4) },
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                            label = { Text("Accounts") },
                            modifier = Modifier.testTag("nav_tab_accounts")
                        )
                    }
                }
            },
            floatingActionButton = {
                if ((currentTab == 0 || currentTab == 5) && activeUserRole.canAddTransactions) {
                    FloatingActionButton(
                        onClick = { viewModel.openAddExpense() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("main_fab_add_expense")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                ) {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            summary = summary,
                            transactions = transactions,
                            recurringRules = recurringRules,
                            selectedMember = selectedMember,
                            household = household,
                            members = members,
                            selectedMemberName = selectedMemberName,
                            currentUser = currentUser,
                            syncState = syncState,
                            onSyncToCloud = { viewModel.syncToCloud() },
                            onSignInClicked = {
                                if (activity != null) viewModel.signInWithGoogle(activity)
                            },
                            onMemberSelected = { viewModel.setMember(it) },
                            onMemberNameSelected = { viewModel.setMemberByName(it) },
                            onOpenHouseholdHub = { viewModel.openHouseholdHub() },
                            onPreviousMonth = { viewModel.setPreviousMonth() },
                            onNextMonth = { viewModel.setNextMonth() },
                            onOpenAddExpense = { viewModel.openAddExpense() },
                            onOpenAddIncome = { viewModel.openAddIncome() },
                            onOpenSmartDetect = { viewModel.openSmartDetect() },
                            onOpenCfoAdvisor = { viewModel.setTab(6) },
                            onIncomeClicked = { viewModel.openAddIncome() },
                            onScheduledClicked = { viewModel.setTab(1) },
                            onInvestmentsClicked = { viewModel.setTab(1) },
                            onRandomSpentClicked = { viewModel.setTab(5) },
                            onMarkRecurringPaid = { viewModel.markRecurringAsPaid(it) },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            isSensitiveRevealed = isSensitiveRevealed,
                            currentUserRole = activeUserRole,
                            onToggleSensitivity = {
                                if (isSensitiveRevealed) {
                                    viewModel.setSensitivityRevealed(false)
                                } else {
                                    showSensitivityAuthDialog = true
                                }
                            }
                        )
                        1 -> RecurringScreen(
                            recurringRules = recurringRules,
                            transactionsForMonth = transactions,
                            monthYear = viewModel.selectedMonth.value,
                            accounts = accounts,
                            onAddRecurring = { viewModel.addRecurringRule(it) },
                            onUpdateRecurring = { viewModel.updateRecurringRule(it) },
                            onDeleteRecurring = { viewModel.deleteRecurringRule(it.id) }
                        )
                        2 -> BudgetScreen(
                            budgets = budgets,
                            transactions = transactions,
                            onSaveBudget = { cat, amt -> viewModel.saveBudget(cat, amt) },
                            onOpenEditBudgetPlan = { viewModel.openEditBudgetPlan() },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            currentUserRole = activeUserRole,
                            monthlyDiscretionaryBudget = household?.monthlyDiscretionaryBudget ?: summary.discretionaryBudget
                        )
                        3 -> ReportsScreen(
                            summary = summary,
                            transactions = transactions,
                            onExportCsv = { viewModel.exportCsvData() },
                            onResetDatabase = { reseed -> viewModel.resetDatabase(reseed) },
                            currentUserRole = activeUserRole,
                            expectedPin = household?.securityPin ?: "1234"
                        )
                        4 -> AccountsScreen(
                            accounts = accounts,
                            currentUserRole = activeUserRole,
                            isSensitiveRevealed = isSensitiveRevealed,
                            members = members,
                            onToggleSensitivity = {
                                if (isSensitiveRevealed) {
                                    viewModel.setSensitivityRevealed(false)
                                } else {
                                    showSensitivityAuthDialog = true
                                }
                            },
                            onAddAccount = { name, holder, type, bal, mask, inst, notes, limit, uid ->
                                viewModel.addAccount(
                                    name = name,
                                    holder = holder,
                                    accountType = type,
                                    balance = bal,
                                    accountNumberMask = mask,
                                    institution = inst,
                                    notes = notes,
                                    creditLimit = limit,
                                    userId = uid
                                )
                            },
                            onUpdateAccount = { viewModel.updateAccount(it) },
                            onDeleteAccount = { viewModel.deleteAccount(it) }
                        )
                        5 -> TransactionsScreen(
                            transactions = transactions,
                            onOpenAddExpense = { viewModel.openAddExpense() },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            currentUserRole = activeUserRole
                        )
                        6 -> CfoReviewScreen(
                            report = cfoReport,
                            goals = goals,
                            monthSummary = summary,
                            onAddGoal = { title, cat, target, saved, months, prio, notes ->
                                viewModel.addGoal(title, cat, target, saved, months, prio, notes)
                            },
                            onDeleteGoal = { viewModel.deleteGoal(it) },
                            onAddGoalContribution = { id, amt -> viewModel.addGoalContribution(id, amt) },
                            onToggleGoalCompleted = { viewModel.toggleGoalCompleted(it) },
                            onSimulateGoal = { title, target, saved, months -> viewModel.simulateGoal(title, target, saved, months) },
                            onNavigateBack = { viewModel.setTab(0) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets and Dialogs
    if (isAddExpenseOpen) {
        AddExpenseSheet(
            accounts = accounts,
            suggestedFromHistory = merchantSuggestion,
            onMerchantChanged = { viewModel.onMerchantTyped(it) },
            members = members,
            onSave = { amount, category, paidBy, method, accId, accName, merchant, notes, timestamp ->
                viewModel.addExpense(
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    paymentMethod = method,
                    accountId = accId,
                    accountName = accName,
                    merchant = merchant,
                    notes = notes,
                    timestamp = timestamp
                )
            },
            onDismiss = { viewModel.closeAddExpense() }
        )
    }

    if (isAddIncomeOpen) {
        AddIncomeSheet(
            accounts = accounts,
            members = members,
            onSave = { amount, category, paidBy, method, accId, accName, source, notes ->
                viewModel.addIncome(
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    paymentMethod = method,
                    accountId = accId,
                    accountName = accName,
                    source = source,
                    notes = notes
                )
            },
            onDismiss = { viewModel.closeAddIncome() }
        )
    }

    if (isSmartDetectOpen) {
        SmartDetectDialog(
            parsedTransaction = suggestedTx,
            onParseText = { viewModel.parseSmsTransaction(it) },
            onConfirm = { viewModel.confirmSuggestedTransaction() },
            onCaptureNotification = { item -> viewModel.captureNotificationTransaction(item) },
            onDismiss = { viewModel.closeSmartDetect() }
        )
    }

    if (isCfoAdvisorOpen) {
        val report = viewModel.generateCfoReport()
        CfoAdvisorDialog(
            report = report,
            goals = goals,
            monthSummary = summary,
            onAddGoal = { title, cat, target, saved, months, prio, notes ->
                viewModel.addGoal(title, cat, target, saved, months, prio, notes)
            },
            onDeleteGoal = { viewModel.deleteGoal(it) },
            onAddGoalContribution = { id, amt -> viewModel.addGoalContribution(id, amt) },
            onToggleGoalCompleted = { viewModel.toggleGoalCompleted(it) },
            onSimulateGoal = { title, target, saved, months -> viewModel.simulateGoal(title, target, saved, months) },
            onDismiss = { viewModel.closeCfoAdvisor() }
        )
    }

    if (isHouseholdHubOpen) {
        HouseholdHubDialog(
            household = household,
            members = members,
            users = users,
            activeUser = activeUser,
            currentUserRole = activeUserRole,
            currentUser = currentUser,
            syncState = syncState,
            onSelectActiveUser = { viewModel.setActiveUser(it) },
            onAddMember = { name, email, role, colorHex ->
                viewModel.addHouseholdMember(name, email, role, colorHex)
            },
            onUpdateMemberRole = { userId, newRole ->
                viewModel.updateMemberRole(userId, newRole)
            },
            onRemoveMember = { userId ->
                viewModel.removeMember(userId)
            },
            onUpdateHouseholdName = { viewModel.updateHouseholdName(it) },
            onShareInvite = { viewModel.shareHouseholdInvite(it) },
            onJoinHousehold = { code, name, onResult ->
                viewModel.joinHousehold(code, name, onResult)
            },
            onOpenEditBudgetPlan = {
                viewModel.closeHouseholdHub()
                viewModel.openEditBudgetPlan()
            },
            onSignInWithGoogle = { if (activity != null) viewModel.signInWithGoogle(activity) },
            onDismiss = { viewModel.closeHouseholdHub() }
        )
    }

    if (isEditBudgetPlanOpen) {
        EditBudgetPlanDialog(
            initialDiscretionaryBudget = household?.monthlyDiscretionaryBudget ?: 66500.0,
            currentBudgets = budgets,
            onSaveBudgetPlan = { monthlyBudget, catBudgets ->
                viewModel.updateHouseholdBudgetPlan(monthlyBudget, catBudgets)
            },
            onDismiss = { viewModel.closeEditBudgetPlan() }
        )
    }

    // First-Time Registration: Prompt when no household is registered yet
    if (household == null) {
        RegisterHouseholdDialog(
            initialAdminName = "",
            initialAdminEmail = "",
            onRegister = { adminName, adminEmail, householdName, discretionaryBudget, securityPin, onResult ->
                viewModel.registerHousehold(
                    adminName = adminName,
                    adminEmail = adminEmail,
                    householdName = householdName,
                    discretionaryBudget = discretionaryBudget,
                    securityPin = securityPin,
                    onResult = onResult
                )
            },
            onJoinWithCode = { inviteCode, memberName, onResult ->
                viewModel.joinHousehold(inviteCode, memberName, onResult)
            }
        )
    }

    // Biometric / Device Credential / Security PIN Sensitivity Authentication
    if (showSensitivityAuthDialog) {
        SensitivityAuthDialog(
            title = "Device Security & PIN Verification",
            subtitle = "Authenticate using fingerprint, device lock, or security PIN to view sensitive balances.",
            expectedPin = household?.securityPin ?: "1234",
            onAuthenticated = {
                viewModel.setSensitivityRevealed(true)
                showSensitivityAuthDialog = false
            },
            onDismiss = {
                showSensitivityAuthDialog = false
            }
        )
    }
}
