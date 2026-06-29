package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coachId: String,
    val sender: String, // "user" or "coach"
    val text: String,
    val grammarCorrection: String? = null,
    val grammarExplanation: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vocabulary_words")
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val definition: String,
    val exampleSentence: String,
    val coachId: String,
    val timestamp: Long = System.currentTimeMillis()
)
