package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val originalPrompt: String,
    val actionType: String,
    val summary: String,
    val parametersJson: String,
    val status: String, // SUCCESS, FAILED, CANCELLED
    val resultMessage: String,
    val rawModelOutput: String
)
