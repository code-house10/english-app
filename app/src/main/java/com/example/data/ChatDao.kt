package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE coachId = :coachId ORDER BY timestamp ASC")
    fun getMessagesForCoach(coachId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE coachId = :coachId")
    suspend fun clearHistoryForCoach(coachId: String)

    @Query("SELECT * FROM vocabulary_words ORDER BY timestamp DESC")
    fun getAllVocabulary(): Flow<List<VocabularyWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(word: VocabularyWord): Long

    @Query("DELETE FROM vocabulary_words WHERE id = :id")
    suspend fun deleteVocabulary(id: Int)
}
