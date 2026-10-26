package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.AppIcons
import com.example.ui.components.MonthSelectorSlider
import com.example.ui.viewmodel.HouseholdCfoViewModel
import com.example.util.SmartDetector

@Composable
fun TransactionsScreen(
    viewModel: HouseholdCfoViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonthIndex.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredTransactions = transactions.filter { tx ->
        val matchesSearch = tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.category.displayName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == null || tx.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp)
                .testTag("transactions_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                MonthSelectorSlider(
                    currentMonthIndex = selectedMonth,
                    onMonthChanged = { viewModel.setMonth(it) }
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, merchants, notes...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_search_input")
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedCategoryFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedCategoryFilter = null }
                        ) {
                            Text(
                                text = "All (${transactions.size})",
                                color = if (selectedCategoryFilter == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(ExpenseCategory.entries.toTypedArray()) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(cat.defaultColor) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedCategoryFilter = if (isSelected) null else cat
                            }
                        ) {
                            Text(
                                text = cat.displayName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Transaction items
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching transactions found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTransactionModal(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, category, isRecurring, notes ->
                viewModel.addExpenseWithSmartDetect(title, amount, category, isRecurring, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddTransactionModal(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, ExpenseCategory, Boolean, String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.MISCELLANEOUS) }
    var isRecurring by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }
    var smartBadgeText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Record (Smart Detect)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { input ->
                        titleInput = input
                        val detected = SmartDetector.detect(input)
                        if (detected.suggestedCategory != ExpenseCategory.MISCELLANEOUS) {
                            selectedCategory = detected.suggestedCategory
                            smartBadgeText = "Detected: ${detected.suggestedCategory.displayName}"
                        }
                        if (detected.extractedAmount != null && amountInput.isEmpty()) {
                            amountInput = detected.extractedAmount.toString()
                        }
                        if (detected.isRecurring) {
                            isRecurring = true
                        }
                    },
                    label = { Text("Description / Merchant") },
                    placeholder = { Text("e.g. Chevron gas $45.00") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_title_input")
                )

                if (smartBadgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = smartBadgeText!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_amount_input")
                )

                Text("Category:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExpenseCategory.entries.toTypedArray()) { cat ->
                        val isSel = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(cat.defaultColor) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.displayName,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Recurring Monthly Payment", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Optional Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeAmount = amountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (titleInput.isNotBlank() && safeAmount > 0) {
                        onConfirm(titleInput, safeAmount, selectedCategory, isRecurring, notesInput)
                    }
                },
                enabled = titleInput.isNotBlank() && (amountInput.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("add_tx_confirm_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
