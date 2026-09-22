package com.example.data.local

import kotlinx.coroutines.flow.Flow

class ActionHistoryRepository(private val dao: ActionHistoryDao) {
    val history: Flow<List<ActionHistoryEntity>> = dao.getAllHistory()

    suspend fun getById(id: Long): ActionHistoryEntity? = dao.getById(id)

    suspend fun recordAction(
        originalPrompt: String,
        actionType: String,
        summary: String,
        parametersJson: String,
        status: String,
        resultMessage: String,
        rawModelOutput: String
    ): Long {
        return dao.insert(
            ActionHistoryEntity(
                originalPrompt = originalPrompt,
                actionType = actionType,
                summary = summary,
                parametersJson = parametersJson,
                status = status,
                resultMessage = resultMessage,
                rawModelOutput = rawModelOutput
            )
        )
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
