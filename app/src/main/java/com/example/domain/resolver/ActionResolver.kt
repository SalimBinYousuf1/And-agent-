package com.example.domain.resolver

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.example.data.model.ParsedModelResponse
import com.example.domain.model.AgentAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ContactMatch(
    val name: String,
    val phoneNumber: String,
    val typeLabel: String
)

sealed class ResolutionResult {
    data class Resolved(val action: AgentAction) : ResolutionResult()
    data class DisambiguationNeeded(
        val originalResponse: ParsedModelResponse,
        val contactQuery: String,
        val matches: List<ContactMatch>,
        val isSms: Boolean,
        val messageBody: String?
    ) : ResolutionResult()
    data class Failed(val reason: String, val conversationalResponse: String? = null) : ResolutionResult()
}

class ActionResolver(private val context: Context) {

    fun resolve(
        rawResponse: ParsedModelResponse,
        directSmsOptIn: Boolean,
        directCallOptIn: Boolean,
        exactAlarmOptIn: Boolean
    ): ResolutionResult {
        return when (rawResponse.action) {
            "SEND_SMS" -> resolveSms(rawResponse, directSmsOptIn)
            "MAKE_CALL" -> resolveCall(rawResponse, directCallOptIn)
            "CREATE_CALENDAR_EVENT" -> resolveCalendar(rawResponse)
            "SET_ALARM" -> resolveAlarm(rawResponse, exactAlarmOptIn)
            "OPEN_APP" -> resolveApp(rawResponse)
            "OPEN_DEEP_LINK" -> resolveDeepLink(rawResponse)
            else -> ResolutionResult.Resolved(
                AgentAction.Unknown(
                    reason = rawResponse.reason ?: "Unrecognized action",
                    conversationalResponse = rawResponse.conversationalResponse
                )
            )
        }
    }

    private fun resolveSms(response: ParsedModelResponse, directSmsOptIn: Boolean): ResolutionResult {
        val rawMessage = response.message.orEmpty().trim()
        val rawPhone = response.phoneNumber?.trim()
        val rawRecipient = response.recipientName?.trim()

        if (rawPhone.isNullOrBlank() && rawRecipient.isNullOrBlank()) {
            return ResolutionResult.Failed("No recipient specified for text message.")
        }

        if (!rawPhone.isNullOrBlank()) {
            val normalized = normalizePhoneNumber(rawPhone)
            return ResolutionResult.Resolved(
                AgentAction.SendSms(
                    recipientName = rawRecipient,
                    phoneNumber = normalized,
                    message = rawMessage,
                    isDirectSendOptIn = directSmsOptIn
                )
            )
        }

        // Lookup contact by name
        val matches = queryContacts(rawRecipient.orEmpty())
        return when {
            matches.size > 1 -> ResolutionResult.DisambiguationNeeded(
                originalResponse = response,
                contactQuery = rawRecipient.orEmpty(),
                matches = matches,
                isSms = true,
                messageBody = rawMessage
            )
            matches.size == 1 -> ResolutionResult.Resolved(
                AgentAction.SendSms(
                    recipientName = matches[0].name,
                    phoneNumber = matches[0].phoneNumber,
                    message = rawMessage,
                    isDirectSendOptIn = directSmsOptIn
                )
            )
            else -> ResolutionResult.Resolved(
                AgentAction.SendSms(
                    recipientName = rawRecipient,
                    phoneNumber = "",
                    message = rawMessage,
                    isDirectSendOptIn = directSmsOptIn
                )
            )
        }
    }

    private fun resolveCall(response: ParsedModelResponse, directCallOptIn: Boolean): ResolutionResult {
        val rawPhone = response.phoneNumber?.trim()
        val rawContact = response.contactName ?: response.recipientName

        if (rawPhone.isNullOrBlank() && rawContact.isNullOrBlank()) {
            return ResolutionResult.Failed("No recipient specified for phone call.")
        }

        if (!rawPhone.isNullOrBlank()) {
            val normalized = normalizePhoneNumber(rawPhone)
            val isEmergency = isEmergencyNumber(normalized)
            return ResolutionResult.Resolved(
                AgentAction.MakeCall(
                    contactName = rawContact,
                    phoneNumber = normalized,
                    isEmergency = isEmergency,
                    isDirectCallOptIn = directCallOptIn && !isEmergency
                )
            )
        }

        val matches = queryContacts(rawContact.orEmpty())
        return when {
            matches.size > 1 -> ResolutionResult.DisambiguationNeeded(
                originalResponse = response,
                contactQuery = rawContact.orEmpty(),
                matches = matches,
                isSms = false,
                messageBody = null
            )
            matches.size == 1 -> {
                val num = matches[0].phoneNumber
                val isEmergency = isEmergencyNumber(num)
                ResolutionResult.Resolved(
                    AgentAction.MakeCall(
                        contactName = matches[0].name,
                        phoneNumber = num,
                        isEmergency = isEmergency,
                        isDirectCallOptIn = directCallOptIn && !isEmergency
                    )
                )
            }
            else -> ResolutionResult.Resolved(
                AgentAction.MakeCall(
                    contactName = rawContact,
                    phoneNumber = "",
                    isEmergency = false,
                    isDirectCallOptIn = directCallOptIn
                )
            )
        }
    }

    private fun resolveCalendar(response: ParsedModelResponse): ResolutionResult {
        val title = response.title?.takeIf { it.isNotBlank() } ?: "New Event"
        val startIso = response.startIso

        val startEpochMs = parseIsoToEpochMs(startIso) ?: (System.currentTimeMillis() + 3600_000L)
        val endEpochMs = parseIsoToEpochMs(response.endIso) ?: (startEpochMs + 3600_000L)

        val sdf = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val formattedTime = sdf.format(Date(startEpochMs))

        val hasConflict = checkForDuplicateCalendarEvent(title, startEpochMs, endEpochMs)

        return ResolutionResult.Resolved(
            AgentAction.CreateCalendarEvent(
                title = title,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                location = response.location,
                description = response.description,
                formattedTime = formattedTime,
                hasConflict = hasConflict
            )
        )
    }

    private fun resolveAlarm(response: ParsedModelResponse, exactAlarmOptIn: Boolean): ResolutionResult {
        val hour = response.hour ?: 7
        val minute = response.minute ?: 0
        val clampedHour = hour.coerceIn(0, 23)
        val clampedMinute = minute.coerceIn(0, 59)
        val message = response.message ?: response.title ?: "Alarm"

        return ResolutionResult.Resolved(
            AgentAction.SetAlarm(
                hour = clampedHour,
                minute = clampedMinute,
                message = message,
                isExact = exactAlarmOptIn
            )
        )
    }

    private fun resolveApp(response: ParsedModelResponse): ResolutionResult {
        val appName = response.appName.orEmpty().trim()
        if (appName.isBlank()) {
            return ResolutionResult.Failed("No app name specified to open.")
        }

        val packageManager = context.packageManager
        val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        // Try exact/fuzzy match on app name
        var resolvedPackage: String? = null
        var resolvedName: String = appName

        for (appInfo in installedPackages) {
            val label = packageManager.getApplicationLabel(appInfo).toString()
            if (label.equals(appName, ignoreCase = true)) {
                resolvedPackage = appInfo.packageName
                resolvedName = label
                break
            }
        }

        if (resolvedPackage == null) {
            // Check partial matches or well-known common packages
            for (appInfo in installedPackages) {
                val label = packageManager.getApplicationLabel(appInfo).toString()
                if (label.contains(appName, ignoreCase = true) || appName.contains(label, ignoreCase = true)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        resolvedPackage = appInfo.packageName
                        resolvedName = label
                        break
                    }
                }
            }
        }

        if (resolvedPackage == null) {
            resolvedPackage = findKnownPackageFallback(appName, packageManager)
        }

        if (resolvedPackage == null) {
            return ResolutionResult.Failed("Could not find an installed application matching '$appName'.")
        }

        return ResolutionResult.Resolved(
            AgentAction.OpenApp(
                appName = resolvedName,
                packageName = resolvedPackage
            )
        )
    }

    private fun findKnownPackageFallback(appName: String, pm: PackageManager): String? {
        val lower = appName.lowercase(Locale.ROOT)
        val candidates = when {
            "youtube" in lower -> listOf("com.google.android.youtube")
            "spotify" in lower -> listOf("com.spotify.music")
            "chrome" in lower -> listOf("com.android.chrome")
            "camera" in lower -> listOf("com.google.android.GoogleCamera", "com.android.camera")
            "maps" in lower -> listOf("com.google.android.apps.maps")
            "clock" in lower -> listOf("com.google.android.deskclock", "com.android.deskclock")
            "calculator" in lower -> listOf("com.google.android.calculator", "com.android.calculator2")
            "whatsapp" in lower -> listOf("com.whatsapp")
            "settings" in lower -> listOf("com.android.settings")
            "gmail" in lower -> listOf("com.google.android.gm")
            else -> emptyList()
        }

        for (pkg in candidates) {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                return pkg
            }
        }
        return null
    }

    private fun resolveDeepLink(response: ParsedModelResponse): ResolutionResult {
        val linkType = response.linkType?.uppercase(Locale.ROOT) ?: "BROWSER_SEARCH"
        val query = response.queryOrTarget.orEmpty().trim()

        val (label, uriString) = when (linkType) {
            "MAPS" -> {
                val encoded = Uri.encode(query.ifBlank { "directions" })
                "Directions for $query" to "geo:0,0?q=$encoded"
            }
            "EMAIL_COMPOSE" -> {
                val encoded = Uri.encode(query)
                "Compose Email to $query" to "mailto:$encoded"
            }
            "CAMERA" -> {
                "Open Camera" to "content://camera"
            }
            "BROWSER_SEARCH" -> {
                val encoded = Uri.encode(query)
                "Search web for \"$query\"" to "https://www.google.com/search?q=$encoded"
            }
            else -> {
                return ResolutionResult.Failed("Unsupported deep link type: $linkType")
            }
        }

        return ResolutionResult.Resolved(
            AgentAction.OpenDeepLink(
                linkType = linkType,
                label = label,
                uriString = uriString
            )
        )
    }

    private fun queryContacts(nameQuery: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        if (nameQuery.isBlank()) return matches

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$nameQuery%")

            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (it.moveToNext() && matches.size < 6) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: nameQuery else nameQuery
                    val number = if (numberIdx >= 0) it.getString(numberIdx).orEmpty() else ""
                    val type = if (typeIdx >= 0) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    val customLabel = if (labelIdx >= 0) it.getString(labelIdx) else null

                    val typeLabel = customLabel ?: when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                        else -> "Other"
                    }

                    if (number.isNotBlank()) {
                        matches.add(ContactMatch(name = name, phoneNumber = normalizePhoneNumber(number), typeLabel = typeLabel))
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted
        } catch (_: Exception) {
            // Graceful degradation
        }
        return matches
    }

    private fun checkForDuplicateCalendarEvent(title: String, startEpochMs: Long, endEpochMs: Long): Boolean {
        try {
            val projection = arrayOf(CalendarContract.Events.TITLE)
            val selection = "(${CalendarContract.Events.TITLE} = ?) AND (${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
            val buffer = 30 * 60 * 1000L // 30 minutes window
            val selectionArgs = arrayOf(
                title,
                (startEpochMs - buffer).toString(),
                (startEpochMs + buffer).toString()
            )

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                return it.count > 0
            }
        } catch (_: Exception) {}
        return false
    }

    private fun parseIsoToEpochMs(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getDefault()
                val date = sdf.parse(isoString)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun normalizePhoneNumber(raw: String): String {
        return raw.replace(Regex("[^0-9+]"), "")
    }

    private fun isEmergencyNumber(phone: String): Boolean {
        val normalized = normalizePhoneNumber(phone)
        val emergencyNumbers = setOf("911", "112", "999", "000", "110", "119", "100", "101", "102")
        return normalized in emergencyNumbers || PhoneNumberUtils.isEmergencyNumber(normalized)
    }
}
