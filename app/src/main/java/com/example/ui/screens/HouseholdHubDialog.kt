package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import com.example.data.model.HouseholdEntity
import com.example.data.model.MemberColorPalette
import com.example.data.model.MemberProfile
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.parseHexColor
import com.example.data.sync.SyncState
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.OceanTeal
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdHubDialog(
    household: HouseholdEntity?,
    members: List<MemberProfile>,
    users: List<UserEntity> = emptyList(),
    activeUser: UserEntity? = null,
    currentUserRole: UserRole = UserRole.ADMIN,
    currentUser: FirebaseUser?,
    syncState: SyncState,
    onSelectActiveUser: (UserEntity) -> Unit = {},
    onAddMember: (name: String, email: String, role: String, colorHex: String) -> Unit,
    onUpdateMemberRole: (userId: Long, newRole: String) -> Unit = { _, _ -> },
    onRemoveMember: (userId: Long) -> Unit = {},
    onUpdateHouseholdName: (newName: String) -> Unit,
    onShareInvite: (Context) -> Unit,
    onJoinHousehold: (inviteCode: String, memberName: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onOpenEditBudgetPlan: () -> Unit,
    onSignInWithGoogle: (Activity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val currentHouseholdName = household?.name ?: "Household CFO"
    val inviteCode = household?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
    val adminName = household?.adminName ?: "Admin"

    var isAddingMember by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberEmail by remember { mutableStateOf("") }
    var newMemberRole by remember { mutableStateOf("MEMBER") }
    var selectedColorPair by remember { mutableStateOf(MemberColorPalette[2]) } // Indigo default
    var addMemberError by remember { mutableStateOf<String?>(null) }

    var isJoiningHousehold by remember { mutableStateOf(false) }
    var joinInviteCode by remember { mutableStateOf("") }
    var joinMemberName by remember { mutableStateOf("") }
    var joinLoading by remember { mutableStateOf(false) }

    var isEditingName by remember { mutableStateOf(false) }
    var editedHouseholdName by remember { mutableStateOf(currentHouseholdName) }

    var memberPendingAdminPromotion by remember { mutableStateOf<MemberProfile?>(null) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

    var userPendingAdminSwitch by remember { mutableStateOf<UserEntity?>(null) }
    var switchAdminPinInput by remember { mutableStateOf("") }
    var switchAdminPinError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("household_hub_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Household Hub & Roles",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Role-Based Access & Real-Time Sync",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Active Persona / Role Info Card
                if (users.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTIVE PROFILE:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${activeUser?.name ?: "Member"} • Role: ${currentUserRole.displayName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (users.size > 1) {
                                Text(
                                    text = "Tap to switch active profile:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    users.forEach { u ->
                                        val isSelected = (activeUser?.id == u.id)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (u.role.equals("ADMIN", ignoreCase = true) && activeUser?.id != u.id) {
                                                    userPendingAdminSwitch = u
                                                    switchAdminPinInput = ""
                                                    switchAdminPinError = null
                                                } else {
                                                    onSelectActiveUser(u)
                                                }
                                            },
                                            label = { Text("${u.name} (${u.role})", fontSize = 11.sp) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(u.colorHex)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = u.initial.ifBlank { u.name.take(1) },
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (currentUserRole == UserRole.ADMIN) 
                                    "You are logged in as Admin. Role management and administrative changes require Security PIN."
                                else
                                    "You are logged in as ${currentUserRole.displayName}. Administrative settings are read-only.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Household Identity & Invite Code Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isEditingName) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = currentHouseholdName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (currentUserRole == UserRole.ADMIN) {
                                        IconButton(
                                            onClick = { isEditingName = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit name",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = editedHouseholdName,
                                        onValueChange = { editedHouseholdName = it },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    IconButton(onClick = {
                                        onUpdateHouseholdName(editedHouseholdName)
                                        isEditingName = false
                                    }) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = Emerald500)
                                    }
                                }
                            }

                            // Admin Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Admin: $adminName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Invite Code Box & Shareable Link
                        Text(
                            text = "Household Invite Code & Shareable Link",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = inviteCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Household Code", inviteCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Invite code copied: $inviteCode", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Button(
                                    onClick = { onShareInvite(context) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Live Sync Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (syncState is SyncState.Live) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (syncState is SyncState.Live) Emerald500 else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (syncState is SyncState.Live) "Live Cloud Sync Active" else "Ready for live syncing",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (syncState is SyncState.Live) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (currentUser == null && activity != null) {
                                TextButton(
                                    onClick = { onSignInWithGoogle(activity) },
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 2. Members List Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Household Members (${members.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentUserRole == UserRole.ADMIN) {
                            TextButton(
                                onClick = { isAddingMember = !isAddingMember },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAddingMember) Icons.Default.Close else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAddingMember) "Cancel" else "Add Person", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Add Member Form
                    AnimatedVisibility(visible = isAddingMember && currentUserRole == UserRole.ADMIN) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Add Member to Household", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = newMemberName,
                                    onValueChange = {
                                        newMemberName = it
                                        addMemberError = null
                                    },
                                    label = { Text("Person's Name *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newMemberEmail,
                                    onValueChange = { newMemberEmail = it },
                                    label = { Text("Email (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                // Role Selector
                                Text("Assign Member Role", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Triple("CONTRIBUTOR", "Contributor", "Edit budgets & accounts"),
                                        Triple("MEMBER", "Member", "Log expenses"),
                                        Triple("USER", "User", "Read-only")
                                    ).forEach { (rCode, rLabel, _) ->
                                        FilterChip(
                                            selected = newMemberRole == rCode,
                                            onClick = { newMemberRole = rCode },
                                            label = { Text(rLabel, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Color Palette Selection
                                Text("Choose Profile Color", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MemberColorPalette.forEach { colorPair ->
                                        val c = try {
                                            Color(android.graphics.Color.parseColor(colorPair.first))
                                        } catch (e: Exception) {
                                            OceanTeal
                                        }
                                        val isSelected = selectedColorPair.first == colorPair.first
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .clickable { selectedColorPair = colorPair }
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 0.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                addMemberError?.let { err ->
                                    Text(text = err, color = ExpenseRed, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val trimmed = newMemberName.trim()
                                        if (trimmed.isBlank()) {
                                            addMemberError = "Please enter member name."
                                            return@Button
                                        }
                                        if (members.any { it.displayName.equals(trimmed, ignoreCase = true) }) {
                                            addMemberError = "A member with name '$trimmed' already exists. Names must be unique."
                                            return@Button
                                        }
                                        onAddMember(trimmed, newMemberEmail.trim(), newMemberRole, selectedColorPair.first)
                                        newMemberName = ""
                                        newMemberEmail = ""
                                        addMemberError = null
                                        isAddingMember = false
                                        Toast.makeText(context, "Added member to household!", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = newMemberName.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Household", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Member list cards with Role promotions
                    members.forEach { member ->
                        var showRoleMenu by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(member.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.initial,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = member.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = member.userRole.description.take(45) + "...",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Role Badge (Clickable for Admin to Promote/Demote OTHER members)
                                val canAdminEditThis = currentUserRole == UserRole.ADMIN && (activeUser != null && member.userId != activeUser.id)
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(android.graphics.Color.parseColor(member.userRole.badgeColorHex)).copy(alpha = 0.15f),
                                        modifier = Modifier.clickable(enabled = canAdminEditThis) {
                                            showRoleMenu = true
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = member.userRole.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(android.graphics.Color.parseColor(member.userRole.badgeColorHex))
                                            )
                                            if (canAdminEditThis) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Change Role",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color(android.graphics.Color.parseColor(member.userRole.badgeColorHex))
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showRoleMenu,
                                        onDismissRequest = { showRoleMenu = false }
                                    ) {
                                        Text(
                                            text = "Set Role for ${member.displayName}:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                        DropdownMenuItem(
                                            text = { Text("⭐ Promote to Contributor (Edit Budgets & Accounts)") },
                                            onClick = {
                                                onUpdateMemberRole(member.userId, "CONTRIBUTOR")
                                                showRoleMenu = false
                                                Toast.makeText(context, "${member.displayName} promoted to Contributor!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Member (Log Expenses & Reports)") },
                                            onClick = {
                                                onUpdateMemberRole(member.userId, "MEMBER")
                                                showRoleMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("User (Read-only Viewer)") },
                                            onClick = {
                                                onUpdateMemberRole(member.userId, "USER")
                                                showRoleMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🔒 Promote to Admin (Requires PIN)") },
                                            onClick = {
                                                memberPendingAdminPromotion = member
                                                adminPinInput = ""
                                                adminPinError = null
                                                showRoleMenu = false
                                            }
                                        )
                                        if (member.userRole != UserRole.ADMIN) {
                                            DropdownMenuItem(
                                                text = { Text("Remove from Household", color = ExpenseRed) },
                                                onClick = {
                                                    onRemoveMember(member.userId)
                                                    showRoleMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Quick promote button if Member (Only Admin can promote, never self)
                                if (canAdminEditThis && member.userRole == UserRole.MEMBER) {
                                    TextButton(
                                        onClick = {
                                            onUpdateMemberRole(member.userId, "CONTRIBUTOR")
                                            Toast.makeText(context, "${member.displayName} promoted to Contributor!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("+ Contributor", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Household Budget Plan Card (Admin & Contributor)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Household Budget Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Monthly Discretionary: ₹${household?.monthlyDiscretionaryBudget?.toInt() ?: 66500}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (currentUserRole.canEditBudgets) {
                                OutlinedButton(
                                    onClick = onOpenEditBudgetPlan,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Edit Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 4. Join another household option
                TextButton(
                    onClick = { isJoiningHousehold = !isJoiningHousehold },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isJoiningHousehold) "Cancel Join" else "Want to connect to another household? Tap here")
                }

                AnimatedVisibility(visible = isJoiningHousehold) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Connect to Existing Household", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedTextField(
                                value = joinInviteCode,
                                onValueChange = { joinInviteCode = it.uppercase() },
                                label = { Text("Invite Code (e.g. CFO-8821)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = joinMemberName,
                                onValueChange = { joinMemberName = it },
                                label = { Text("Your Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    joinLoading = true
                                    onJoinHousehold(joinInviteCode, joinMemberName) { success, msg ->
                                        joinLoading = false
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) isJoiningHousehold = false
                                    }
                                },
                                enabled = joinInviteCode.isNotBlank() && joinMemberName.isNotBlank() && !joinLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (joinLoading) "Connecting..." else "Join Household")
                            }
                        }
                    }
                }
            }
        }
    }

    // Admin Promotion PIN Authorization Dialog
    if (memberPendingAdminPromotion != null) {
        val target = memberPendingAdminPromotion!!
        AlertDialog(
            onDismissRequest = {
                memberPendingAdminPromotion = null
                adminPinInput = ""
                adminPinError = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Authorize Admin Promotion",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Promoting '${target.displayName}' to Admin grants full control over household members, financial data, and security rules.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter the 4-digit Household Security PIN to authorize this change:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                adminPinInput = it
                                adminPinError = null
                            }
                        },
                        label = { Text("Admin PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = adminPinError != null,
                        supportingText = adminPinError?.let { { Text(it, color = ExpenseRed) } },
                        modifier = Modifier.fillMaxWidth().testTag("admin_promotion_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = household?.securityPin?.ifBlank { "1234" } ?: "1234"
                        if (adminPinInput == expectedPin) {
                            onUpdateMemberRole(target.userId, "ADMIN")
                            Toast.makeText(context, "${target.displayName} promoted to Admin!", Toast.LENGTH_SHORT).show()
                            memberPendingAdminPromotion = null
                            adminPinInput = ""
                            adminPinError = null
                        } else {
                            adminPinError = "Incorrect Security PIN. Admin promotion denied."
                        }
                    },
                    modifier = Modifier.testTag("confirm_admin_promotion_btn")
                ) {
                    Text("Authorize Admin")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        memberPendingAdminPromotion = null
                        adminPinInput = ""
                        adminPinError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Admin Persona Switch PIN Authorization Dialog
    if (userPendingAdminSwitch != null) {
        val target = userPendingAdminSwitch!!
        AlertDialog(
            onDismissRequest = {
                userPendingAdminSwitch = null
                switchAdminPinInput = ""
                switchAdminPinError = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Admin Access Authorization",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Switching to '${target.name}' grants Administrator authority over household finances, accounts, and member roles.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter the 4-digit Household Security PIN to switch to Admin:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = switchAdminPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                switchAdminPinInput = it
                                switchAdminPinError = null
                            }
                        },
                        label = { Text("Household Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = switchAdminPinError != null,
                        supportingText = switchAdminPinError?.let { { Text(it, color = ExpenseRed) } },
                        modifier = Modifier.fillMaxWidth().testTag("admin_switch_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = household?.securityPin?.ifBlank { "1234" } ?: "1234"
                        if (switchAdminPinInput == expectedPin) {
                            onSelectActiveUser(target)
                            Toast.makeText(context, "Switched to ${target.name} (Admin)", Toast.LENGTH_SHORT).show()
                            userPendingAdminSwitch = null
                            switchAdminPinInput = ""
                            switchAdminPinError = null
                        } else {
                            switchAdminPinError = "Incorrect Security PIN. Admin access denied."
                        }
                    },
                    modifier = Modifier.testTag("confirm_admin_switch_btn")
                ) {
                    Text("Verify & Switch")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        userPendingAdminSwitch = null
                        switchAdminPinInput = ""
                        switchAdminPinError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
