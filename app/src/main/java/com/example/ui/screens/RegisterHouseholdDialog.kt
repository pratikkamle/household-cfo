package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed

@Composable
fun RegisterHouseholdDialog(
    initialAdminName: String = "",
    initialAdminEmail: String = "",
    onRegister: (
        adminName: String,
        adminEmail: String,
        householdName: String,
        discretionaryBudget: Double,
        securityPin: String,
        onResult: (Boolean, String) -> Unit
    ) -> Unit,
    onJoinWithCode: (inviteCode: String, memberName: String, onResult: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Create Household, 1 = Join with Code

    // Create tab states - no prefilled values or placeholder hints per user specification
    var adminName by remember { mutableStateOf(initialAdminName) }
    var adminEmail by remember { mutableStateOf(initialAdminEmail) }
    var householdName by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var securityPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Join tab states
    var joinInviteCode by remember { mutableStateOf("") }
    var joinMemberName by remember { mutableStateOf("") }
    var joinMemberEmail by remember { mutableStateOf("") }
    var joinLoading by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { /* Cannot dismiss without registering or joining */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("register_household_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Home else Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Household CFO Setup",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTab == 0) "Create New Household" else "Join Existing Household",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Mode Tabs: Create Household vs Join with Code
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Create Household", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_create_household")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Join with Code", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_join_household")
                    )
                }

                if (selectedTab == 0) {
                    // TAB 0: CREATE NEW HOUSEHOLD
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🔒 Clean Start & Admin Control",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You will be the sole Admin. You can add household members (User, Member, Contributor) and add your bank accounts, cards, LIC, MFs, and NPS schemes from scratch.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Inputs - pristine fields without hints or prefilled values
                    OutlinedTextField(
                        value = adminName,
                        onValueChange = {
                            adminName = it
                            errorMessage = null
                        },
                        label = { Text("Admin Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_admin_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text("Admin Email (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("reg_admin_email"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = householdName,
                        onValueChange = {
                            householdName = it
                            errorMessage = null
                        },
                        label = { Text("Household Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("reg_household_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text("Monthly Discretionary (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f).testTag("reg_budget"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = securityPin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) securityPin = it },
                            label = { Text("Security PIN") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(0.9f).testTag("reg_security_pin"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    errorMessage?.let { err ->
                        Text(text = err, color = ExpenseRed, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (adminName.isBlank()) {
                                errorMessage = "Please enter your name as the Household Admin."
                                return@Button
                            }
                            if (householdName.isBlank()) {
                                errorMessage = "Please enter a unique Household Name."
                                return@Button
                            }
                            val budget = budgetText.toDoubleOrNull() ?: 50000.0
                            val finalPin = if (securityPin.isNotBlank()) securityPin.trim() else "1234"
                            onRegister(
                                adminName.trim(),
                                adminEmail.trim(),
                                householdName.trim(),
                                budget,
                                finalPin
                            ) { success, msg ->
                                if (!success) {
                                    errorMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_register_household"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Register as Admin & Start",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // TAB 1: JOIN WITH INVITE CODE
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🤝 Join a Partner or Family Member",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter the 8-character invite code shared by the Household Admin. You will link directly into their household with real-time sync.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = joinInviteCode,
                        onValueChange = {
                            joinInviteCode = it.uppercase().trim()
                            joinError = null
                        },
                        label = { Text("Household Invite Code *") },
                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("join_invite_code_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = joinMemberName,
                        onValueChange = {
                            joinMemberName = it
                            joinError = null
                        },
                        label = { Text("Your Display Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("join_member_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = joinMemberEmail,
                        onValueChange = { joinMemberEmail = it },
                        label = { Text("Your Email (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("join_member_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    joinError?.let { err ->
                        Text(text = err, color = ExpenseRed, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (joinInviteCode.isBlank()) {
                                joinError = "Please enter an invite code."
                                return@Button
                            }
                            if (joinMemberName.isBlank()) {
                                joinError = "Please enter your display name."
                                return@Button
                            }
                            joinLoading = true
                            joinError = null
                            onJoinWithCode(joinInviteCode, joinMemberName.trim()) { success, msg ->
                                joinLoading = false
                                if (!success) {
                                    joinError = msg
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = joinInviteCode.isNotBlank() && joinMemberName.isNotBlank() && !joinLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_join_household"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        Text(
                            text = if (joinLoading) "Connecting to Household..." else "Join Household & Sync",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
