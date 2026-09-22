package com.example.data.network

import com.example.data.model.ParsedModelResponse
import com.example.data.security.KeyStatus
import com.example.data.security.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class GroqApiClient(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1"
        private const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
        private const val MODELS_ENDPOINT = "$BASE_URL/models"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty"))
        }

        try {
            val request = Request.Builder()
                .url(MODELS_ENDPOINT)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("User-Agent", "SalimAgent-Android/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                settingsRepository.recordDiagnostics(
                    endpoint = MODELS_ENDPOINT,
                    requestRedacted = "GET $MODELS_ENDPOINT (Authorization: Bearer [REDACTED])",
                    responseCode = response.code,
                    responseBody = responseBody.take(1000),
                    error = if (!response.isSuccessful) "HTTP ${response.code}: ${response.message}" else null
                )

                when (response.code) {
                    200 -> Result.success(true)
                    401, 403 -> Result.failure(Exception("Invalid API key or unauthorized (HTTP ${response.code})"))
                    429 -> Result.failure(Exception("Rate limit reached on Groq free tier. Please retry in a moment."))
                    else -> Result.failure(Exception("Server returned HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            settingsRepository.recordDiagnostics(
                endpoint = MODELS_ENDPOINT,
                requestRedacted = "GET $MODELS_ENDPOINT (Authorization: Bearer [REDACTED])",
                responseCode = 0,
                responseBody = "",
                error = e.localizedMessage ?: e.toString()
            )
            Result.failure(e)
        }
    }

    suspend fun processCommand(
        apiKey: String,
        model: String,
        userCommand: String
    ): Result<ParsedModelResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Groq API key configured"))
        }

        val nowIso = getNowIsoString()
        val systemPrompt = buildSystemPrompt(nowIso)

        val chatRequest = GroqChatRequest(
            model = model,
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = userCommand)
            ),
            responseFormat = GroqResponseFormat(type = "json_object"),
            temperature = 0.1
        )

        val requestJsonString = json.encodeToString(GroqChatRequest.serializer(), chatRequest)
        val requestBody = requestJsonString.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(CHAT_ENDPOINT)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("User-Agent", "SalimAgent-Android/1.0")
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string().orEmpty()

                settingsRepository.recordDiagnostics(
                    endpoint = CHAT_ENDPOINT,
                    requestRedacted = "POST $CHAT_ENDPOINT (Key: [REDACTED], Model: $model, Prompt: \"$userCommand\")",
                    responseCode = response.code,
                    responseBody = responseBodyString.take(1500),
                    error = if (!response.isSuccessful) "HTTP ${response.code}: ${response.message}" else null
                )

                if (response.code == 401 || response.code == 403) {
                    settingsRepository.setKeyStatus(KeyStatus.INVALID)
                    return@withContext Result.failure(
                        Exception("Groq API key authentication failed (HTTP ${response.code}). Please update your key in Settings.")
                    )
                }

                if (response.code == 429) {
                    return@withContext Result.failure(
                        Exception("Groq API rate limit exceeded. Please wait a moment and retry.")
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Groq API error HTTP ${response.code}: ${response.message}")
                    )
                }

                val chatResponse = json.decodeFromString(GroqChatResponse.serializer(), responseBodyString)
                val rawContent = chatResponse.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.success(
                        ParsedModelResponse(
                            action = "UNKNOWN",
                            reason = "Empty response received from AI model",
                            conversationalResponse = "I couldn't understand that command."
                        )
                    )

                try {
                    val parsed = json.decodeFromString(ParsedModelResponse.serializer(), rawContent)
                    val validated = sanitizeActionResponse(parsed)
                    Result.success(validated)
                } catch (pe: Exception) {
                    Result.success(
                        ParsedModelResponse(
                            action = "UNKNOWN",
                            reason = "Malformed structured output: ${pe.localizedMessage}",
                            conversationalResponse = "I received an unreadable response format from the AI model."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            settingsRepository.recordDiagnostics(
                endpoint = CHAT_ENDPOINT,
                requestRedacted = "POST $CHAT_ENDPOINT (Key: [REDACTED], Model: $model, Prompt: \"$userCommand\")",
                responseCode = 0,
                responseBody = "",
                error = e.localizedMessage ?: e.toString()
            )
            Result.failure(e)
        }
    }

    private fun sanitizeActionResponse(response: ParsedModelResponse): ParsedModelResponse {
        val allowedActions = setOf(
            "SEND_SMS",
            "MAKE_CALL",
            "CREATE_CALENDAR_EVENT",
            "SET_ALARM",
            "OPEN_APP",
            "OPEN_DEEP_LINK",
            "UNKNOWN"
        )
        val normalizedAction = response.action.uppercase().trim()
        if (normalizedAction !in allowedActions) {
            return ParsedModelResponse(
                action = "UNKNOWN",
                reason = "Action '$normalizedAction' is outside the permitted allowlist",
                conversationalResponse = "I cannot execute that type of action safely."
            )
        }
        return response.copy(action = normalizedAction)
    }

    private fun getNowIsoString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    private fun buildSystemPrompt(nowIso: String): String {
        return """
You are salim, a security-first Android personal agent.
Current device date and time: $nowIso (${TimeZone.getDefault().id}).

Your ONLY job is to interpret the user's natural language command into structured JSON.
You MUST output ONLY valid JSON matching this exact schema:

{
  "action": "SEND_SMS" | "MAKE_CALL" | "CREATE_CALENDAR_EVENT" | "SET_ALARM" | "OPEN_APP" | "OPEN_DEEP_LINK" | "UNKNOWN",
  "confidence": 0.0 to 1.0,
  "recipient_name": string or null,
  "phone_number": string or null,
  "message": string or null,
  "contact_name": string or null,
  "title": string or null,
  "start_iso": string or null (ISO-8601 format e.g. 2026-09-23T15:00:00),
  "end_iso": string or null,
  "location": string or null,
  "description": string or null,
  "hour": integer 0-23 or null,
  "minute": integer 0-59 or null,
  "app_name": string or null,
  "link_type": "MAPS" | "EMAIL_COMPOSE" | "CAMERA" | "BROWSER_SEARCH" or null,
  "query_or_target": string or null,
  "reason": string or null,
  "conversational_response": string or null
}

STRICT RULES:
1. Closed allowlist of actions ONLY: SEND_SMS, MAKE_CALL, CREATE_CALENDAR_EVENT, SET_ALARM, OPEN_APP, OPEN_DEEP_LINK, UNKNOWN.
2. For SEND_SMS: Extract recipient_name (e.g. "Mom") or phone_number, and the exact message body.
3. For MAKE_CALL: Extract contact_name or phone_number.
4. For CREATE_CALENDAR_EVENT: Compute exact start_iso based on the current device time ($nowIso). For relative times ("tomorrow at 3pm", "in 2 hours"), calculate the exact target ISO-8601 string. Default duration is 1 hour if unspecified.
5. For SET_ALARM: Extract 24-hour hour (0-23), minute (0-59), and message label.
6. For OPEN_APP: Extract app_name (e.g. "YouTube", "Spotify", "WhatsApp", "Chrome", "Camera", "Maps", "Settings", "Clock", "Calculator"). Do NOT invent package names.
7. For OPEN_DEEP_LINK: link_type must be one of:
   - "MAPS": with query_or_target (e.g. "coffee near me", "Times Square")
   - "EMAIL_COMPOSE": with query_or_target (e.g. recipient email)
   - "CAMERA"
   - "BROWSER_SEARCH": with query_or_target
8. If the command is vague, ambiguous, purely conversational ("hello", "who are you"), or unsupported, set action to "UNKNOWN" with a helpful conversational_response. NEVER guess dangerous actions.
""".trimIndent()
    }
}
