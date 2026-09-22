package com.example.domain.executor

import android.Manifest
import android.app.AlarmManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.domain.model.AgentAction
import java.util.TimeZone

sealed class ExecutionResult {
    data class Success(val message: String, val detailsJson: String = "") : ExecutionResult()
    data class Failure(val reason: String, val recoverySuggestion: String? = null) : ExecutionResult()
}

class ActionExecutor(private val context: Context) {

    fun execute(action: AgentAction): ExecutionResult {
        return try {
            when (action) {
                is AgentAction.SendSms -> executeSms(action)
                is AgentAction.MakeCall -> executeCall(action)
                is AgentAction.CreateCalendarEvent -> executeCalendar(action)
                is AgentAction.SetAlarm -> executeAlarm(action)
                is AgentAction.OpenApp -> executeApp(action)
                is AgentAction.OpenDeepLink -> executeDeepLink(action)
                is AgentAction.Unknown -> ExecutionResult.Failure(
                    reason = action.reason,
                    recoverySuggestion = action.conversationalResponse ?: "Try rephrasing your command."
                )
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(
                reason = "Execution error: ${e.localizedMessage ?: e.toString()}",
                recoverySuggestion = "Check relevant system permissions in Settings."
            )
        }
    }

    private fun executeSms(action: AgentAction.SendSms): ExecutionResult {
        if (action.phoneNumber.isBlank()) {
            return ExecutionResult.Failure(
                reason = "No phone number available to send SMS.",
                recoverySuggestion = "Please specify or edit the recipient's phone number."
            )
        }

        val hasSendSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (action.isDirectSendOptIn && hasSendSmsPermission) {
            // Direct SMS transmission via SmsManager
            return try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                if (smsManager == null) {
                    throw IllegalStateException("Telephony SMS service unavailable on this device")
                }

                val parts = smsManager.divideMessage(action.message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(action.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(action.phoneNumber, null, action.message, null, null)
                }

                ExecutionResult.Success(
                    message = "SMS sent directly to ${action.recipientName ?: action.phoneNumber}",
                    detailsJson = "{\"type\":\"DIRECT_SMS\",\"recipient\":\"${action.phoneNumber}\"}"
                )
            } catch (e: Exception) {
                // Fall back to opening Messages app if direct send encounters an exception
                openSmsApp(action)
                ExecutionResult.Success(
                    message = "Direct SMS failed (${e.message}). Opened Messages app with pre-filled text.",
                    detailsJson = "{\"fallback\":\"ACTION_SENDTO\"}"
                )
            }
        } else {
            // Standard safety default: ACTION_SENDTO pre-filled
            openSmsApp(action)
            return ExecutionResult.Success(
                message = "Opened Messages pre-filled for ${action.recipientName ?: action.phoneNumber}",
                detailsJson = "{\"type\":\"ACTION_SENDTO\",\"recipient\":\"${action.phoneNumber}\"}"
            )
        }
    }

    private fun openSmsApp(action: AgentAction.SendSms) {
        val uri = Uri.parse("smsto:${action.phoneNumber}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", action.message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun executeCall(action: AgentAction.MakeCall): ExecutionResult {
        if (action.phoneNumber.isBlank()) {
            return ExecutionResult.Failure(
                reason = "No phone number available to call.",
                recoverySuggestion = "Please provide or edit the phone number."
            )
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (action.isDirectCallOptIn && hasCallPermission && !action.isEmergency) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${action.phoneNumber}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ExecutionResult.Success(
                message = "Initiated direct call to ${action.contactName ?: action.phoneNumber}",
                detailsJson = "{\"type\":\"ACTION_CALL\",\"number\":\"${action.phoneNumber}\"}"
            )
        } else {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val note = if (action.isEmergency) " (Emergency number routed safely through dialer)" else ""
            return ExecutionResult.Success(
                message = "Opened dialer for ${action.contactName ?: action.phoneNumber}$note",
                detailsJson = "{\"type\":\"ACTION_DIAL\",\"number\":\"${action.phoneNumber}\"}"
            )
        }
    }

    private fun executeCalendar(action: AgentAction.CreateCalendarEvent): ExecutionResult {
        val hasWriteCalendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (hasWriteCalendar) {
            val calendarId = findPrimaryCalendarId()
            if (calendarId != null) {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, action.startEpochMs)
                    put(CalendarContract.Events.DTEND, action.endEpochMs)
                    put(CalendarContract.Events.TITLE, action.title)
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    action.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
                    action.description?.let { put(CalendarContract.Events.DESCRIPTION, it) }
                }

                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    return ExecutionResult.Success(
                        message = "Scheduled \"${action.title}\" on ${action.formattedTime}",
                        detailsJson = "{\"type\":\"CALENDAR_INSERT\",\"uri\":\"$uri\"}"
                    )
                }
            }
        }

        // Fallback: Launch calendar app insert intent
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, action.startEpochMs)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, action.endEpochMs)
            putExtra(CalendarContract.Events.TITLE, action.title)
            action.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            action.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ExecutionResult.Success(
                message = "Opened Calendar with event \"${action.title}\" pre-filled",
                detailsJson = "{\"fallback\":\"ACTION_INSERT\"}"
            )
        } else {
            ExecutionResult.Failure(
                reason = "No calendar app found on device.",
                recoverySuggestion = "Install Google Calendar or grant Calendar permissions in Settings."
            )
        }
    }

    private fun findPrimaryCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Calendars._ID)
                if (it.moveToFirst() && idIdx >= 0) {
                    return it.getLong(idIdx)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun executeAlarm(action: AgentAction.SetAlarm): ExecutionResult {
        // First try standard Clock Alarm intent which provides the cleanest native confirmation
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", action.hour, action.minute)
            return ExecutionResult.Success(
                message = "Alarm set for $timeStr (${action.message})",
                detailsJson = "{\"type\":\"ACTION_SET_ALARM\",\"time\":\"$timeStr\"}"
            )
        }

        return ExecutionResult.Failure(
            reason = "No compatible Clock application found on this device.",
            recoverySuggestion = "Install Google Clock or verify alarm permissions."
        )
    }

    private fun executeApp(action: AgentAction.OpenApp): ExecutionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
            ?: return ExecutionResult.Failure(
                reason = "Unable to launch ${action.appName} (${action.packageName}).",
                recoverySuggestion = "The application might be disabled or restricted."
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ExecutionResult.Success(
            message = "Launched ${action.appName}",
            detailsJson = "{\"type\":\"OPEN_APP\",\"package\":\"${action.packageName}\"}"
        )
    }

    private fun executeDeepLink(action: AgentAction.OpenDeepLink): ExecutionResult {
        if (action.linkType == "CAMERA") {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                return ExecutionResult.Success("Opened Camera app")
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ExecutionResult.Success("Launched ${action.label}")
        } else {
            ExecutionResult.Failure(
                reason = "No application found to handle this link.",
                recoverySuggestion = "Install a compatible browser or application."
            )
        }
    }
}
