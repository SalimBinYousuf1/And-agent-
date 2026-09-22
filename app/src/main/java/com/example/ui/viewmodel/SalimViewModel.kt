package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActionHistoryEntity
import com.example.data.local.ActionHistoryRepository
import com.example.data.local.SalimDatabase
import com.example.data.model.ParsedModelResponse
import com.example.data.network.GroqApiClient
import com.example.data.security.DiagnosticsData
import com.example.data.security.KeyStatus
import com.example.data.security.SettingsRepository
import com.example.domain.executor.ActionExecutor
import com.example.domain.executor.ExecutionResult
import com.example.domain.model.AgentAction
import com.example.domain.resolver.ActionResolver
import com.example.domain.resolver.ContactMatch
import com.example.domain.resolver.ResolutionResult
import com.example.ui.speech.SpeechManager
import com.example.ui.speech.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ActionSheetState {
    object Hidden : ActionSheetState()
    data class ConfirmAction(val action: AgentAction, val originalPrompt: String) : ActionSheetState()
    data class Disambiguate(
        val originalResponse: ParsedModelResponse,
        val contactQuery: String,
        val matches: List<ContactMatch>,
        val isSms: Boolean,
        val messageBody: String?,
        val originalPrompt: String
    ) : ActionSheetState()
}

data class ExecutionNotification(
    val isSuccess: Boolean,
    val message: String,
    val details: String? = null
)

class SalimViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalimDatabase.getDatabase(application)
    private val historyRepository = ActionHistoryRepository(db.actionHistoryDao())
    val settingsRepository = SettingsRepository(application)
    private val groqClient = GroqApiClient(settingsRepository)
    private val actionResolver = ActionResolver(application)
    private val actionExecutor = ActionExecutor(application)
    val speechManager = SpeechManager(application)

    val historyList: StateFlow<List<ActionHistoryEntity>> = historyRepository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val keyStatus: StateFlow<KeyStatus> = settingsRepository.keyStatus
    val selectedModel: StateFlow<String> = settingsRepository.selectedModel
    val directSmsEnabled: StateFlow<Boolean> = settingsRepository.directSmsEnabled
    val directCallEnabled: StateFlow<Boolean> = settingsRepository.directCallEnabled
    val exactAlarmEnabled: StateFlow<Boolean> = settingsRepository.exactAlarmEnabled
    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.onboardingCompleted
    val diagnostics: StateFlow<DiagnosticsData> = settingsRepository.diagnostics
    val speechState: StateFlow<SpeechState> = speechManager.speechState

    private val _inputCommand = MutableStateFlow("")
    val inputCommand: StateFlow<String> = _inputCommand.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _sheetState = MutableStateFlow<ActionSheetState>(ActionSheetState.Hidden)
    val sheetState: StateFlow<ActionSheetState> = _sheetState.asStateFlow()

    private val _notification = MutableStateFlow<ExecutionNotification?>(null)
    val notification: StateFlow<ExecutionNotification?> = _notification.asStateFlow()

    private val _keyValidationInProgress = MutableStateFlow(false)
    val keyValidationInProgress: StateFlow<Boolean> = _keyValidationInProgress.asStateFlow()

    private val _keyValidationResult = MutableStateFlow<String?>(null)
    val keyValidationResult: StateFlow<String?> = _keyValidationResult.asStateFlow()

    fun updateInputCommand(text: String) {
        _inputCommand.value = text
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun dismissSheet() {
        _sheetState.value = ActionSheetState.Hidden
    }

    fun submitCommand(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return

        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _notification.value = ExecutionNotification(
                isSuccess = false,
                message = "Groq API key is missing. Please add your key in Settings to activate salim.",
                details = "Missing API key"
            )
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _notification.value = null

            val result = groqClient.processCommand(
                apiKey = apiKey,
                model = settingsRepository.selectedModel.value,
                userCommand = trimmed
            )

            _isProcessing.value = false

            result.fold(
                onSuccess = { parsedResponse ->
                    handleParsedResponse(parsedResponse, trimmed)
                },
                onFailure = { error ->
                    val errorMsg = error.localizedMessage ?: "Network or API failure"
                    _notification.value = ExecutionNotification(
                        isSuccess = false,
                        message = errorMsg,
                        details = "Command processing failed"
                    )
                    historyRepository.recordAction(
                        originalPrompt = trimmed,
                        actionType = "ERROR",
                        summary = "Failed to process command",
                        parametersJson = "{}",
                        status = "FAILED",
                        resultMessage = errorMsg,
                        rawModelOutput = error.stackTraceToString().take(500)
                    )
                }
            )
        }
    }

    private fun handleParsedResponse(response: ParsedModelResponse, originalPrompt: String) {
        val resolution = actionResolver.resolve(
            rawResponse = response,
            directSmsOptIn = settingsRepository.directSmsEnabled.value,
            directCallOptIn = settingsRepository.directCallEnabled.value,
            exactAlarmOptIn = settingsRepository.exactAlarmEnabled.value
        )

        when (resolution) {
            is ResolutionResult.Resolved -> {
                if (resolution.action is AgentAction.Unknown) {
                    val message = resolution.action.conversationalResponse
                        ?: "I couldn't understand that command. Please try rephrasing."
                    _notification.value = ExecutionNotification(
                        isSuccess = true,
                        message = message
                    )
                    viewModelScope.launch {
                        historyRepository.recordAction(
                            originalPrompt = originalPrompt,
                            actionType = "UNKNOWN",
                            summary = "Conversational or unsupported query",
                            parametersJson = "{}",
                            status = "SUCCESS",
                            resultMessage = message,
                            rawModelOutput = response.toString()
                        )
                    }
                } else {
                    _sheetState.value = ActionSheetState.ConfirmAction(
                        action = resolution.action,
                        originalPrompt = originalPrompt
                    )
                }
            }
            is ResolutionResult.DisambiguationNeeded -> {
                _sheetState.value = ActionSheetState.Disambiguate(
                    originalResponse = resolution.originalResponse,
                    contactQuery = resolution.contactQuery,
                    matches = resolution.matches,
                    isSms = resolution.isSms,
                    messageBody = resolution.messageBody,
                    originalPrompt = originalPrompt
                )
            }
            is ResolutionResult.Failed -> {
                _notification.value = ExecutionNotification(
                    isSuccess = false,
                    message = resolution.reason,
                    details = resolution.conversationalResponse
                )
                viewModelScope.launch {
                    historyRepository.recordAction(
                        originalPrompt = originalPrompt,
                        actionType = response.action,
                        summary = "Resolution failed",
                        parametersJson = "{}",
                        status = "FAILED",
                        resultMessage = resolution.reason,
                        rawModelOutput = response.toString()
                    )
                }
            }
        }
    }

    fun selectDisambiguatedContact(
        match: ContactMatch,
        disambiguateState: ActionSheetState.Disambiguate
    ) {
        val action = if (disambiguateState.isSms) {
            AgentAction.SendSms(
                recipientName = match.name,
                phoneNumber = match.phoneNumber,
                message = disambiguateState.messageBody.orEmpty(),
                isDirectSendOptIn = settingsRepository.directSmsEnabled.value
            )
        } else {
            AgentAction.MakeCall(
                contactName = match.name,
                phoneNumber = match.phoneNumber,
                isEmergency = false,
                isDirectCallOptIn = settingsRepository.directCallEnabled.value
            )
        }
        _sheetState.value = ActionSheetState.ConfirmAction(
            action = action,
            originalPrompt = disambiguateState.originalPrompt
        )
    }

    fun executeConfirmedAction(action: AgentAction, originalPrompt: String) {
        viewModelScope.launch {
            _sheetState.value = ActionSheetState.Hidden
            val result = actionExecutor.execute(action)

            when (result) {
                is ExecutionResult.Success -> {
                    _notification.value = ExecutionNotification(
                        isSuccess = true,
                        message = result.message
                    )
                    historyRepository.recordAction(
                        originalPrompt = originalPrompt,
                        actionType = getActionTypeString(action),
                        summary = result.message,
                        parametersJson = result.detailsJson,
                        status = "SUCCESS",
                        resultMessage = result.message,
                        rawModelOutput = action.toString()
                    )
                }
                is ExecutionResult.Failure -> {
                    _notification.value = ExecutionNotification(
                        isSuccess = false,
                        message = result.reason,
                        details = result.recoverySuggestion
                    )
                    historyRepository.recordAction(
                        originalPrompt = originalPrompt,
                        actionType = getActionTypeString(action),
                        summary = "Action execution failed",
                        parametersJson = "{}",
                        status = "FAILED",
                        resultMessage = "${result.reason} (${result.recoverySuggestion ?: ""})",
                        rawModelOutput = action.toString()
                    )
                }
            }
        }
    }

    fun cancelAction(action: AgentAction, originalPrompt: String) {
        _sheetState.value = ActionSheetState.Hidden
        viewModelScope.launch {
            historyRepository.recordAction(
                originalPrompt = originalPrompt,
                actionType = getActionTypeString(action),
                summary = "Action cancelled by user",
                parametersJson = "{}",
                status = "CANCELLED",
                resultMessage = "User declined to execute the action",
                rawModelOutput = action.toString()
            )
        }
    }

    private fun getActionTypeString(action: AgentAction): String {
        return when (action) {
            is AgentAction.SendSms -> "SEND_SMS"
            is AgentAction.MakeCall -> "MAKE_CALL"
            is AgentAction.CreateCalendarEvent -> "CREATE_CALENDAR_EVENT"
            is AgentAction.SetAlarm -> "SET_ALARM"
            is AgentAction.OpenApp -> "OPEN_APP"
            is AgentAction.OpenDeepLink -> "OPEN_DEEP_LINK"
            is AgentAction.Unknown -> "UNKNOWN"
        }
    }

    fun validateAndSaveApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isBlank()) {
            _keyValidationResult.value = "API key cannot be empty"
            return
        }

        viewModelScope.launch {
            _keyValidationInProgress.value = true
            _keyValidationResult.value = null

            val result = groqClient.validateApiKey(trimmed)
            _keyValidationInProgress.value = false

            result.fold(
                onSuccess = {
                    settingsRepository.saveApiKey(trimmed)
                    _keyValidationResult.value = "SUCCESS"
                },
                onFailure = { error ->
                    settingsRepository.setKeyStatus(KeyStatus.INVALID)
                    _keyValidationResult.value = error.localizedMessage ?: "Invalid API key"
                }
            )
        }
    }

    fun clearKeyValidationResult() {
        _keyValidationResult.value = null
    }

    fun removeApiKey() {
        settingsRepository.removeApiKey()
    }

    fun setModel(model: String) {
        settingsRepository.setSelectedModel(model)
    }

    fun setDirectSms(enabled: Boolean) {
        settingsRepository.setDirectSmsEnabled(enabled)
    }

    fun setDirectCall(enabled: Boolean) {
        settingsRepository.setDirectCallEnabled(enabled)
    }

    fun setExactAlarm(enabled: Boolean) {
        settingsRepository.setExactAlarmEnabled(enabled)
    }

    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _notification.value = ExecutionNotification(
                isSuccess = true,
                message = "Action history cleared."
            )
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    fun exportHistory(context: Context, history: List<ActionHistoryEntity>) {
        try {
            val sb = StringBuilder()
            sb.append("Timestamp,Action,Status,Prompt,Summary,Result\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (item in history) {
                val dateStr = sdf.format(Date(item.timestamp))
                sb.append("\"$dateStr\",\"${item.actionType}\",\"${item.status}\",\"${item.originalPrompt.replace("\"", "\"\"")}\",\"${item.summary.replace("\"", "\"\"")}\",\"${item.resultMessage.replace("\"", "\"\"")}\"\n")
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                putExtra(Intent.EXTRA_TITLE, "Salim Action History Export")
                type = "text/csv"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export History").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
