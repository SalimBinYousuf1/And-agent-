package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AgentAction
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.BorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceLayer
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemOrange
import com.example.ui.theme.SystemRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfirmationSheet(
    action: AgentAction,
    originalPrompt: String,
    onConfirm: (AgentAction) -> Unit,
    onCancel: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = PureWhite,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(BorderGray, RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier.testTag("action_confirmation_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (action) {
                is AgentAction.SendSms -> {
                    var recipientName by remember { mutableStateOf(action.recipientName.orEmpty()) }
                    var phoneNumber by remember { mutableStateOf(action.phoneNumber) }
                    var messageText by remember { mutableStateOf(action.message) }

                    ActionHeader(
                        icon = Icons.Outlined.Message,
                        title = "Review Text Message",
                        subtitle = "Exact message and recipient about to be processed"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (action.isDirectSendOptIn) {
                        NoticeBadge(
                            text = "Direct-Send Mode is Active: Message will be transmitted directly via carrier.",
                            isWarning = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Recipient Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_recipient_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_phone_number_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message Body") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("sms_message_body_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButtons(
                        confirmText = if (action.isDirectSendOptIn) "Send SMS Directly" else "Open Messages App",
                        onConfirm = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onConfirm(
                                action.copy(
                                    recipientName = recipientName.ifBlank { null },
                                    phoneNumber = phoneNumber.trim(),
                                    message = messageText.trim()
                                )
                            )
                        },
                        onCancel = onCancel
                    )
                }

                is AgentAction.MakeCall -> {
                    var contactName by remember { mutableStateOf(action.contactName.orEmpty()) }
                    var phoneNumber by remember { mutableStateOf(action.phoneNumber) }

                    ActionHeader(
                        icon = Icons.Outlined.Call,
                        title = "Review Phone Call",
                        subtitle = "Verify contact and number before initiating"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (action.isEmergency) {
                        NoticeBadge(
                            text = "Emergency Number: Will be opened in dialer for safety review.",
                            isWarning = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (action.isDirectCallOptIn) {
                        NoticeBadge(
                            text = "Direct-Dial Mode is Active: Call will be placed immediately.",
                            isWarning = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Contact Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_contact_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_phone_number_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButtons(
                        confirmText = if (action.isDirectCallOptIn && !action.isEmergency) "Call Directly" else "Open Dialer",
                        onConfirm = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onConfirm(
                                action.copy(
                                    contactName = contactName.ifBlank { null },
                                    phoneNumber = phoneNumber.trim()
                                )
                            )
                        },
                        onCancel = onCancel
                    )
                }

                is AgentAction.CreateCalendarEvent -> {
                    var title by remember { mutableStateOf(action.title) }
                    var location by remember { mutableStateOf(action.location.orEmpty()) }
                    var description by remember { mutableStateOf(action.description.orEmpty()) }

                    ActionHeader(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Schedule Calendar Event",
                        subtitle = action.formattedTime
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (action.hasConflict) {
                        NoticeBadge(
                            text = "Potential Duplicate: Another event with a similar title exists near this time.",
                            isWarning = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_location_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_description_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButtons(
                        confirmText = "Save Event",
                        onConfirm = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onConfirm(
                                action.copy(
                                    title = title.trim(),
                                    location = location.ifBlank { null },
                                    description = description.ifBlank { null }
                                )
                            )
                        },
                        onCancel = onCancel
                    )
                }

                is AgentAction.SetAlarm -> {
                    var message by remember { mutableStateOf(action.message) }
                    var hour by remember { mutableIntStateOf(action.hour) }
                    var minute by remember { mutableIntStateOf(action.minute) }

                    ActionHeader(
                        icon = Icons.Outlined.Alarm,
                        title = "Set Alarm",
                        subtitle = String.format("Scheduled for %02d:%02d", hour, minute)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Alarm Label") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_label_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButtons(
                        confirmText = "Set Alarm",
                        onConfirm = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onConfirm(action.copy(message = message.trim(), hour = hour, minute = minute))
                        },
                        onCancel = onCancel
                    )
                }

                is AgentAction.OpenApp -> {
                    ActionHeader(
                        icon = Icons.Outlined.OpenInNew,
                        title = "Open Application",
                        subtitle = "Target: ${action.appName}"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "salim will launch the verified installed package: ${action.packageName}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButtons(
                        confirmText = "Launch App",
                        onConfirm = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onConfirm(action)
                        },
                        onCancel = onCancel
                    )
                }

                is AgentAction.OpenDeepLink -> {
                    ActionHeader(
                        icon = Icons.Outlined.Link,
                        title = "Open Deep Link",
                        subtitle = action.label
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Allowlisted Target: ${action.uriString}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActionButtons(
                        confirmText = "Open Link",
                        onConfirm = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onConfirm(action)
                        },
                        onCancel = onCancel
                    )
                }

                is AgentAction.Unknown -> {
                    ActionHeader(
                        icon = Icons.Outlined.Warning,
                        title = "Unrecognized Action",
                        subtitle = action.reason
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("action_dismiss_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceLayer,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(SurfaceLayer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleBlue,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun NoticeBadge(text: String, isWarning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isWarning) SystemOrange.copy(alpha = 0.1f) else SystemGreen.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isWarning) SystemOrange.copy(alpha = 0.3f) else SystemGreen.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = if (isWarning) SystemOrange else SystemGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun ActionButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("action_confirm_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleBlue,
                contentColor = PureWhite
            )
        ) {
            Text(
                text = confirmText,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("action_cancel_button")
        ) {
            Text(
                text = "Cancel",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppleBlue,
    unfocusedBorderColor = BorderGray,
    focusedContainerColor = PureWhite,
    unfocusedContainerColor = SurfaceLayer,
    focusedLabelColor = AppleBlue,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
