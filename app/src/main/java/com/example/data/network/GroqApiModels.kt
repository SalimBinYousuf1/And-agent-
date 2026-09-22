package com.example.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroqChatRequest(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<GroqMessage>,
    @SerialName("response_format")
    val responseFormat: GroqResponseFormat? = GroqResponseFormat(type = "json_object"),
    @SerialName("temperature")
    val temperature: Double = 0.1
)

@Serializable
data class GroqMessage(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class GroqResponseFormat(
    @SerialName("type")
    val type: String = "json_object"
)

@Serializable
data class GroqChatResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("choices")
    val choices: List<GroqChoice> = emptyList(),
    @SerialName("error")
    val error: GroqErrorDetail? = null
)

@Serializable
data class GroqChoice(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("message")
    val message: GroqMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class GroqErrorDetail(
    @SerialName("message")
    val message: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("code")
    val code: String? = null
)
