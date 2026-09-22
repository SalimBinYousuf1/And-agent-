package com.example.ui.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionInfo(
    val id: String,
    val title: String,
    val manifestPermission: String?,
    val rationale: String,
    val isGranted: Boolean,
    val isOptInOnly: Boolean = false,
    val openSettings: (Context) -> Unit
)

object PermissionUtils {

    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            openAppDetailsSettings(context)
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun getAppPermissionsList(context: Context): List<PermissionInfo> {
        val list = mutableListOf<PermissionInfo>()

        list.add(
            PermissionInfo(
                id = "contacts",
                title = "Contacts",
                manifestPermission = Manifest.permission.READ_CONTACTS,
                rationale = "Resolves contact names like 'Mom' or 'Sam' into verified phone numbers.",
                isGranted = checkPermission(context, Manifest.permission.READ_CONTACTS),
                openSettings = { openAppDetailsSettings(it) }
            )
        )

        list.add(
            PermissionInfo(
                id = "calendar",
                title = "Calendar",
                manifestPermission = Manifest.permission.WRITE_CALENDAR,
                rationale = "Allows writing appointments directly to the device calendar and checking for duplicate events.",
                isGranted = checkPermission(context, Manifest.permission.WRITE_CALENDAR) &&
                            checkPermission(context, Manifest.permission.READ_CALENDAR),
                openSettings = { openAppDetailsSettings(it) }
            )
        )

        list.add(
            PermissionInfo(
                id = "mic",
                title = "Microphone",
                manifestPermission = Manifest.permission.RECORD_AUDIO,
                rationale = "Enables on-device speech-to-text recognition when speaking commands.",
                isGranted = checkPermission(context, Manifest.permission.RECORD_AUDIO),
                openSettings = { openAppDetailsSettings(it) }
            )
        )

        list.add(
            PermissionInfo(
                id = "sms",
                title = "SMS (Direct-Send)",
                manifestPermission = Manifest.permission.SEND_SMS,
                rationale = "Optional: Send text messages directly without opening Messages app first.",
                isGranted = checkPermission(context, Manifest.permission.SEND_SMS),
                isOptInOnly = true,
                openSettings = { openAppDetailsSettings(it) }
            )
        )

        list.add(
            PermissionInfo(
                id = "phone",
                title = "Phone (Direct-Dial)",
                manifestPermission = Manifest.permission.CALL_PHONE,
                rationale = "Optional: Place phone calls directly without opening the system dialer first.",
                isGranted = checkPermission(context, Manifest.permission.CALL_PHONE),
                isOptInOnly = true,
                openSettings = { openAppDetailsSettings(it) }
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionInfo(
                    id = "notifications",
                    title = "Notifications",
                    manifestPermission = Manifest.permission.POST_NOTIFICATIONS,
                    rationale = "Delivers timely alerts and status updates for scheduled actions.",
                    isGranted = checkPermission(context, Manifest.permission.POST_NOTIFICATIONS),
                    openSettings = { openNotificationSettings(it) }
                )
            )
        }

        list.add(
            PermissionInfo(
                id = "alarms",
                title = "Exact Alarms",
                manifestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.SCHEDULE_EXACT_ALARM else null,
                rationale = "Ensures scheduled alarms trigger at the exact specified minute.",
                isGranted = canScheduleExactAlarms(context),
                isOptInOnly = true,
                openSettings = { openExactAlarmSettings(it) }
            )
        )

        return list
    }
}
