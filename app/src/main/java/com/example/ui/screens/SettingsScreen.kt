package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.security.KeyStatus
import com.example.data.security.SettingsRepository
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.BorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceLayer
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemOrange
import com.example.ui.theme.SystemRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.PermissionUtils
import com.example.ui.viewmodel.SalimViewModel

@Composable
fun SettingsScreen(
    viewModel: SalimViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToHowItWorks: () -> Unit
) {
    val context = LocalContext.current

    val keyStatus by viewModel.keyStatus.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val directSmsEnabled by viewModel.directSmsEnabled.collectAsStateWithLifecycle()
    val directCallEnabled by viewModel.directCallEnabled.collectAsStateWithLifecycle()
    val exactAlarmEnabled by viewModel.exactAlarmEnabled.collectAsStateWithLifecycle()
    val keyValidationInProgress by viewModel.keyValidationInProgress.collectAsStateWithLifecycle()
    val keyValidationResult by viewModel.keyValidationResult.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf(viewModel.settingsRepository.getApiKey().orEmpty()) }
    var keyVisible by remember { mutableStateOf(false) }

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showRemoveKeyDialog by remember { mutableStateOf(false) }
    var showDirectSmsWarningDialog by remember { mutableStateOf(false) }
    var showDirectCallWarningDialog by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val permissionsList = remember { PermissionUtils.getAppPermissionsList(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceLayer, CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // SECTION 1: AI ENGINE
            SettingsSectionHeader(title = "AI ENGINE (GROQ)")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLayer, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = AppleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Groq API Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    // Key Status Badge
                    val (statusLabel, statusColor) = when (keyStatus) {
                        KeyStatus.CONNECTED -> "Connected" to SystemGreen
                        KeyStatus.INVALID -> "Invalid Key" to SystemRed
                        KeyStatus.VALIDATING -> "Testing..." to AppleBlue
                        KeyStatus.NOT_SET -> "Not Set" to SystemOrange
                    }
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        viewModel.clearKeyValidationResult()
                    },
                    placeholder = { Text("gsk_...", color = TextTertiary) },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    apiKeyInput = clip
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_api_key_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                AnimatedVisibility(visible = keyValidationResult != null) {
                    val isSuccess = keyValidationResult == "SUCCESS"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                            contentDescription = null,
                            tint = if (isSuccess) SystemGreen else SystemRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSuccess) "Key verified and securely saved" else keyValidationResult.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) SystemGreen else SystemRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.validateAndSaveApiKey(apiKeyInput) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_api_key_button"),
                        enabled = apiKeyInput.isNotBlank() && !keyValidationInProgress,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleBlue,
                            contentColor = PureWhite
                        )
                    ) {
                        if (keyValidationInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save & Connect", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (viewModel.settingsRepository.hasApiKey()) {
                        OutlinedButton(
                            onClick = { showRemoveKeyDialog = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("remove_api_key_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Remove", color = SystemRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security Explanation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BorderGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted locally with Android Keystore AES-256 in private storage. Never sent to third parties or logged. Note: Local storage cannot guarantee protection on a compromised or rooted device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Model Selector
                Text(
                    text = "Active Groq Model",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PureWhite, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                            .clickable { modelDropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("model_selector_dropdown"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedModel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                        modifier = Modifier.background(PureWhite)
                    ) {
                        for (model in SettingsRepository.AVAILABLE_MODELS) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = model,
                                        fontWeight = if (model == selectedModel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (model == selectedModel) AppleBlue else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setModel(model)
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // SECTION 2: DIRECT-SEND PERMISSIONS
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "DIRECT EXECUTION PERMISSIONS")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLayer, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                // SMS Direct Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct SMS Transmission",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Send texts directly without opening Messages app first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = directSmsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showDirectSmsWarningDialog = true
                            } else {
                                viewModel.setDirectSms(false)
                            }
                        },
                        colors = appleSwitchColors(),
                        modifier = Modifier.testTag("direct_sms_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Direct Call
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct Phone Call",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Place calls directly without opening system dialer first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = directCallEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showDirectCallWarningDialog = true
                            } else {
                                viewModel.setDirectCall(false)
                            }
                        },
                        colors = appleSwitchColors(),
                        modifier = Modifier.testTag("direct_call_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exact Alarm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exact Alarm Timing",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ensure scheduled alarms fire down to the exact minute.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = exactAlarmEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setExactAlarm(enabled)
                            if (enabled && !PermissionUtils.canScheduleExactAlarms(context)) {
                                PermissionUtils.openExactAlarmSettings(context)
                            }
                        },
                        colors = appleSwitchColors(),
                        modifier = Modifier.testTag("exact_alarm_switch")
                    )
                }
            }

            // SECTION 3: SYSTEM PERMISSIONS STATUS
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "PERMISSIONS STATUS")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLayer, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (perm in permissionsList) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = perm.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (perm.isGranted) SystemGreen.copy(alpha = 0.12f) else SystemOrange.copy(alpha = 0.12f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (perm.isGranted) "Granted" else "Not Granted",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (perm.isGranted) SystemGreen else SystemOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = perm.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = { perm.openSettings(context) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("fix_permission_${perm.id}")
                        ) {
                            Text("Manage", style = MaterialTheme.typography.labelSmall, color = AppleBlue)
                        }
                    }
                }
            }

            // SECTION 4: DATA & HISTORY
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "HISTORY & DATA")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLayer, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.exportHistory(context, historyList) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = AppleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Export Action History (CSV)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearHistoryDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = SystemRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Clear All Action History",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = SystemRed
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // SECTION 5: ABOUT & DIAGNOSTICS
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "ABOUT")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLayer, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // How it works
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHowItWorks() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "How salim Works & Security Model",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Diagnostics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDiagnostics() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Diagnostics & Last Request Logs",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("1.0.0 (Build 1)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dialogs
        if (showRemoveKeyDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveKeyDialog = false },
                title = { Text("Remove Groq API Key?") },
                text = { Text("salim will no longer be able to interpret commands until a valid key is provided.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeApiKey()
                        apiKeyInput = ""
                        showRemoveKeyDialog = false
                    }) {
                        Text("Remove", color = SystemRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveKeyDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = PureWhite
            )
        }

        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Action History?") },
                text = { Text("All local records of executed and cancelled actions will be permanently deleted from device storage.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    }) {
                        Text("Clear All", color = SystemRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = PureWhite
            )
        }

        if (showDirectSmsWarningDialog) {
            AlertDialog(
                onDismissRequest = { showDirectSmsWarningDialog = false },
                title = { Text("Enable Direct SMS Transmission?") },
                text = {
                    Text(
                        "Enabling this allows salim to send SMS text messages directly via your cellular carrier without opening your default Messages app. Standard carrier SMS rates may apply. You will still review each message in the confirmation sheet before it is sent."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setDirectSms(true)
                        showDirectSmsWarningDialog = false
                    }) {
                        Text("Enable", color = AppleBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDirectSmsWarningDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = PureWhite
            )
        }

        if (showDirectCallWarningDialog) {
            AlertDialog(
                onDismissRequest = { showDirectCallWarningDialog = false },
                title = { Text("Enable Direct Phone Calls?") },
                text = {
                    Text(
                        "Enabling this allows salim to initiate phone calls directly. Emergency numbers (like 911 or 112) will ALWAYS be safely routed through the system dialer regardless of this setting. You will still review the contact in the confirmation sheet before the call is placed."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setDirectCall(true)
                        showDirectCallWarningDialog = false
                    }) {
                        Text("Enable", color = AppleBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDirectCallWarningDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = PureWhite
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun appleSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = PureWhite,
    checkedTrackColor = AppleBlue,
    uncheckedThumbColor = PureWhite,
    uncheckedTrackColor = BorderGray
)
