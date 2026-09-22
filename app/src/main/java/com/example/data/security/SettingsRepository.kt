package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KeyStatus {
    NOT_SET,
    CONNECTED,
    INVALID,
    VALIDATING
}

data class DiagnosticsData(
    val lastTimestamp: Long = 0L,
    val lastEndpoint: String = "",
    val lastRequestRedacted: String = "",
    val lastResponseCode: Int = 0,
    val lastResponseBody: String = "",
    val lastError: String? = null
)

class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "salim_secure_settings"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_groq_api_key"
        private const val KEY_STATUS = "key_connection_status"
        private const val KEY_SELECTED_MODEL = "selected_groq_model"
        private const val KEY_DIRECT_SMS = "direct_sms_opt_in"
        private const val KEY_DIRECT_CALL = "direct_call_opt_in"
        private const val KEY_EXACT_ALARM = "exact_alarm_opt_in"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        val AVAILABLE_MODELS = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        )
        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cryptoManager = KeystoreCryptoManager()

    private val _keyStatus = MutableStateFlow(loadInitialKeyStatus())
    val keyStatus: StateFlow<KeyStatus> = _keyStatus.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _directSmsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DIRECT_SMS, false)
    )
    val directSmsEnabled: StateFlow<Boolean> = _directSmsEnabled.asStateFlow()

    private val _directCallEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DIRECT_CALL, false)
    )
    val directCallEnabled: StateFlow<Boolean> = _directCallEnabled.asStateFlow()

    private val _exactAlarmEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_EXACT_ALARM, false)
    )
    val exactAlarmEnabled: StateFlow<Boolean> = _exactAlarmEnabled.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _diagnostics = MutableStateFlow(DiagnosticsData())
    val diagnostics: StateFlow<DiagnosticsData> = _diagnostics.asStateFlow()

    private fun loadInitialKeyStatus(): KeyStatus {
        val encrypted = prefs.getString(KEY_ENCRYPTED_API_KEY, null)
        if (encrypted.isNullOrBlank()) return KeyStatus.NOT_SET
        val savedStatus = prefs.getString(KEY_STATUS, KeyStatus.NOT_SET.name)
        return try {
            KeyStatus.valueOf(savedStatus ?: KeyStatus.NOT_SET.name)
        } catch (_: Exception) {
            KeyStatus.CONNECTED
        }
    }

    fun getApiKey(): String? {
        val encrypted = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        return cryptoManager.decrypt(encrypted)
    }

    fun hasApiKey(): Boolean {
        return !prefs.getString(KEY_ENCRYPTED_API_KEY, null).isNullOrBlank()
    }

    fun saveApiKey(rawKey: String) {
        val encrypted = cryptoManager.encrypt(rawKey)
        prefs.edit()
            .putString(KEY_ENCRYPTED_API_KEY, encrypted)
            .putString(KEY_STATUS, KeyStatus.CONNECTED.name)
            .apply()
        _keyStatus.value = KeyStatus.CONNECTED
    }

    fun setKeyStatus(status: KeyStatus) {
        prefs.edit().putString(KEY_STATUS, status.name).apply()
        _keyStatus.value = status
    }

    fun removeApiKey() {
        cryptoManager.clearKey()
        prefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .putString(KEY_STATUS, KeyStatus.NOT_SET.name)
            .apply()
        _keyStatus.value = KeyStatus.NOT_SET
    }

    fun setSelectedModel(model: String) {
        if (model in AVAILABLE_MODELS) {
            prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
            _selectedModel.value = model
        }
    }

    fun setDirectSmsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIRECT_SMS, enabled).apply()
        _directSmsEnabled.value = enabled
    }

    fun setDirectCallEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIRECT_CALL, enabled).apply()
        _directCallEnabled.value = enabled
    }

    fun setExactAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXACT_ALARM, enabled).apply()
        _exactAlarmEnabled.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun recordDiagnostics(
        endpoint: String,
        requestRedacted: String,
        responseCode: Int,
        responseBody: String,
        error: String?
    ) {
        _diagnostics.value = DiagnosticsData(
            lastTimestamp = System.currentTimeMillis(),
            lastEndpoint = endpoint,
            lastRequestRedacted = requestRedacted,
            lastResponseCode = responseCode,
            lastResponseBody = responseBody,
            lastError = error
        )
    }
}
