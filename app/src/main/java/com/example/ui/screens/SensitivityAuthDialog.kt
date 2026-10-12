package com.example.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ExpenseRed

private const val PREFS_NAME = "security_settings"
private const val KEY_BIOMETRIC_CONSENT = "biometric_auth_enabled"

@Composable
fun SensitivityAuthDialog(
    title: String = "Security Authentication",
    subtitle: String = "Authenticate to reveal sensitive balances or perform administrative actions.",
    expectedPin: String = "",
    onSetPin: (String) -> Unit = {},
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs: SharedPreferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isPinConfigured = expectedPin.isNotBlank()
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Consent state for fingerprint/biometric authentication
    // Default is false so that users are prompted for permission first!
    var isBiometricEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean(KEY_BIOMETRIC_CONSENT, false))
    }
    var showBiometricConsentPrompt by remember { mutableStateOf(false) }

    val keyguardManager = remember {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }
    val isDeviceSecure = remember(keyguardManager) {
        keyguardManager?.isDeviceSecure == true
    }

    // Device credential launcher (Fingerprint, Face, Device PIN, Pattern)
    val deviceUnlockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onAuthenticated()
        } else {
            errorMessage = "Fingerprint verification cancelled. Enter security PIN below."
        }
    }

    fun launchDeviceUnlock() {
        if (keyguardManager != null && isDeviceSecure) {
            try {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    title,
                    subtitle
                )
                if (intent != null) {
                    deviceUnlockLauncher.launch(intent)
                    return
                }
            } catch (e: Exception) {
                // Fall back to in-app PIN
            }
        }
        errorMessage = "Biometric authentication not supported on this device. Use PIN."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("sensitivity_auth_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isPinConfigured) Icons.Default.Pin else if (isDeviceSecure && isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (!isPinConfigured) "Set Security PIN" else title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!isPinConfigured) "No security PIN is currently set for your household. Please set a 4-digit PIN to secure your sensitive figures." else subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (!isPinConfigured) {
                    // SET PIN FLOW
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    pinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Create 4-Digit PIN") },
                            placeholder = { Text("e.g. 2468") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Pin, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("create_pin_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    confirmPinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Confirm PIN") },
                            placeholder = { Text("Re-enter PIN") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        errorMessage?.let { err ->
                            Text(
                                text = err,
                                color = ExpenseRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pinInput.length < 4) {
                                    errorMessage = "PIN must be at least 4 digits."
                                    return@Button
                                }
                                if (pinInput != confirmPinInput) {
                                    errorMessage = "PINs do not match. Please re-enter."
                                    return@Button
                                }
                                onSetPin(pinInput)
                                onAuthenticated()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_and_reveal_pin_btn")
                        ) {
                            Text("Set PIN & Reveal")
                        }
                    }
                } else {
                    // VERIFY PIN FLOW
                    // BIOMETRIC OPTION WITH EXPLICIT PERMISSION/CONSENT
                    if (isDeviceSecure) {
                        if (isBiometricEnabled) {
                            // User already permitted fingerprint authentication
                            OutlinedButton(
                                onClick = { launchDeviceUnlock() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Authenticate with Fingerprint",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text = "— OR ENTER SECURITY PIN —",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (showBiometricConsentPrompt) {
                            // Asking user permission to enable fingerprint auth
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Enable Fingerprint Sign-in?",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = "Would you like to use your device fingerprint sensor for faster authentication to reveal sensitive financial figures?",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showBiometricConsentPrompt = false }) {
                                            Text("No, Use PIN", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = {
                                                sharedPrefs.edit().putBoolean(KEY_BIOMETRIC_CONSENT, true).apply()
                                                isBiometricEnabled = true
                                                showBiometricConsentPrompt = false
                                                launchDeviceUnlock()
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Enable & Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Prompt affordance to enable fingerprint
                            OutlinedButton(
                                onClick = { showBiometricConsentPrompt = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enable Fingerprint Authentication",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // PIN Input Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    pinInput = it
                                    errorMessage = null
                                    if (it == expectedPin) {
                                        onAuthenticated()
                                    }
                                }
                            },
                            label = { Text("Security PIN") },
                            placeholder = { Text("Enter PIN") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Pin, contentDescription = null)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_pin_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            isError = errorMessage != null
                        )

                        errorMessage?.let { err ->
                            Text(
                                text = err,
                                color = ExpenseRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Text(
                            text = "Enter your household security PIN to reveal sensitive figures.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pinInput == expectedPin) {
                                    onAuthenticated()
                                } else {
                                    errorMessage = "Incorrect security PIN. Please try again."
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("security_pin_submit")
                        ) {
                            Text("Verify & Reveal")
                        }
                    }
                }
            }
        }
    }
}
