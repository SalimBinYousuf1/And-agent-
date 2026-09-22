package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ActionHistoryEntity
import com.example.data.security.KeyStatus
import com.example.ui.speech.SpeechState
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.BorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceLayer
import com.example.ui.theme.SurfaceLayerSecondary
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemOrange
import com.example.ui.theme.SystemRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.ActionSheetState
import com.example.ui.viewmodel.SalimViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: SalimViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistoryDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val inputCommand by viewModel.inputCommand.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val keyStatus by viewModel.keyStatus.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.speechManager.startListening()
        }
    }

    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Success) {
            val spoken = (speechState as SpeechState.Success).recognizedText
            viewModel.updateInputCommand(spoken)
            viewModel.speechManager.resetState()
            viewModel.submitCommand(spoken)
        }
    }

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
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "salim",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Security-First Agent",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceLayer, CircleShape)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Key Status Warning Banner
            if (keyStatus != KeyStatus.CONNECTED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(SystemOrange.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .border(1.dp, SystemOrange.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("api_key_warning_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        tint = SystemOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (keyStatus == KeyStatus.INVALID) "Groq API Key Invalid" else "Groq API Key Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to configure your Groq key in Settings to activate actions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
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

            // In-App Notification / Alert Banner
            notification?.let { notif ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(
                            if (notif.isSuccess) SystemGreen.copy(alpha = 0.08f) else SystemRed.copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (notif.isSuccess) SystemGreen.copy(alpha = 0.25f) else SystemRed.copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (notif.isSuccess) Icons.Outlined.Check else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (notif.isSuccess) SystemGreen else SystemRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notif.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        notif.details?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearNotification() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Command Input Box
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = inputCommand,
                onValueChange = { viewModel.updateInputCommand(it) },
                placeholder = {
                    Text(
                        text = "e.g. text Mom I'm running late",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        // Mic Button
                        val isListening = speechState is SpeechState.Listening
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isListening) 1.25f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        IconButton(
                            onClick = {
                                if (isListening) {
                                    viewModel.speechManager.stopListening()
                                } else {
                                    val hasMic = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasMic) {
                                        viewModel.speechManager.startListening()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .scale(scale)
                                .background(
                                    if (isListening) AppleBlue.copy(alpha = 0.15f) else SurfaceLayer,
                                    CircleShape
                                )
                                .testTag("voice_input_mic_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Outlined.Mic else Icons.Outlined.MicOff,
                                contentDescription = "Voice Input",
                                tint = if (isListening) AppleBlue else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Send Button
                        if (inputCommand.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.submitCommand(inputCommand)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(AppleBlue, CircleShape)
                                    .testTag("submit_command_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Send,
                                    contentDescription = "Submit",
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(20.dp),
                colors = outlinedFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    keyboardController?.hide()
                    viewModel.submitCommand(inputCommand)
                })
            )

            // Speech Status / Processing indicator
            AnimatedVisibility(
                visible = speechState is SpeechState.Listening || speechState is SpeechState.Error || isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AppleBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interpreting command with Groq...",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleBlue
                        )
                    } else if (speechState is SpeechState.Listening) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AppleBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Listening... speak your command",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleBlue,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (speechState is SpeechState.Error) {
                        Text(
                            text = (speechState as SpeechState.Error).errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemOrange
                        )
                    }
                }
            }

            // Quick Example Chips
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "Text Mom I'm running 10 mins late",
                    "Call Alex",
                    "Schedule Team Sync tomorrow at 3pm",
                    "Set alarm for 7:00 AM",
                    "Open Spotify",
                    "Directions to Central Park"
                )
                for (chip in suggestions) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLayer)
                            .clickable {
                                viewModel.updateInputCommand(chip)
                                viewModel.submitCommand(chip)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("suggestion_chip_${chip.take(10)}")
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Activity History Section
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (historyList.isNotEmpty()) {
                    Text(
                        text = "${historyList.size} logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(SurfaceLayer, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(BorderGray.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Activity Logged",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Speak or type a command above. Every verified execution is saved locally in your encrypted Room database.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { it.id }) { item ->
                        HistoryCard(item = item, onClick = { onNavigateToHistoryDetail(item.id) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Confirmation Bottom Sheet
        when (val currentSheet = sheetState) {
            is ActionSheetState.ConfirmAction -> {
                ActionConfirmationSheet(
                    action = currentSheet.action,
                    originalPrompt = currentSheet.originalPrompt,
                    onConfirm = { confirmedAction ->
                        viewModel.executeConfirmedAction(confirmedAction, currentSheet.originalPrompt)
                    },
                    onCancel = {
                        viewModel.cancelAction(currentSheet.action, currentSheet.originalPrompt)
                    }
                )
            }
            is ActionSheetState.Disambiguate -> {
                ContactDisambiguationSheet(
                    state = currentSheet,
                    onSelect = { match ->
                        viewModel.selectDisambiguatedContact(match, currentSheet)
                    },
                    onCancel = {
                        viewModel.dismissSheet()
                    }
                )
            }
            is ActionSheetState.Hidden -> {}
        }
    }
}

@Composable
private fun HistoryCard(
    item: ActionHistoryEntity,
    onClick: () -> Unit
) {
    val icon = when (item.actionType) {
        "SEND_SMS" -> Icons.Outlined.Message
        "MAKE_CALL" -> Icons.Outlined.Call
        "CREATE_CALENDAR_EVENT" -> Icons.Outlined.CalendarToday
        "SET_ALARM" -> Icons.Outlined.Alarm
        "OPEN_APP" -> Icons.Outlined.OpenInNew
        "OPEN_DEEP_LINK" -> Icons.Outlined.Link
        else -> Icons.Outlined.History
    }

    val statusColor = when (item.status) {
        "SUCCESS" -> SystemGreen
        "FAILED" -> SystemRed
        else -> TextSecondary
    }

    val formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLayer, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("history_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(BorderGray.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleBlue,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.originalPrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}
