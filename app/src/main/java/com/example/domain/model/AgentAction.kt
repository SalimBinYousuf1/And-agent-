package com.example.domain.model

sealed class AgentAction {
    data class SendSms(
        val recipientName: String?,
        val phoneNumber: String,
        val message: String,
        val isDirectSendOptIn: Boolean = false
    ) : AgentAction()

    data class MakeCall(
        val contactName: String?,
        val phoneNumber: String,
        val isEmergency: Boolean = false,
        val isDirectCallOptIn: Boolean = false
    ) : AgentAction()

    data class CreateCalendarEvent(
        val title: String,
        val startEpochMs: Long,
        val endEpochMs: Long,
        val location: String?,
        val description: String?,
        val formattedTime: String,
        val hasConflict: Boolean = false
    ) : AgentAction()

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val message: String,
        val isExact: Boolean = false
    ) : AgentAction()

    data class OpenApp(
        val appName: String,
        val packageName: String
    ) : AgentAction()

    data class OpenDeepLink(
        val linkType: String,
        val label: String,
        val uriString: String
    ) : AgentAction()

    data class Unknown(
        val reason: String,
        val conversationalResponse: String? = null
    ) : AgentAction()
}
