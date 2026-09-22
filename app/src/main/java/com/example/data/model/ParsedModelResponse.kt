package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParsedModelResponse(
    @SerialName("action")
    val action: String = "UNKNOWN",
    @SerialName("confidence")
    val confidence: Double = 0.0,
    @SerialName("recipient_name")
    val recipientName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("contact_name")
    val contactName: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("start_iso")
    val startIso: String? = null,
    @SerialName("end_iso")
    val endIso: String? = null,
    @SerialName("location")
    val location: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("hour")
    val hour: Int? = null,
    @SerialName("minute")
    val minute: Int? = null,
    @SerialName("app_name")
    val appName: String? = null,
    @SerialName("link_type")
    val linkType: String? = null,
    @SerialName("query_or_target")
    val queryOrTarget: String? = null,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("conversational_response")
    val conversationalResponse: String? = null
)
