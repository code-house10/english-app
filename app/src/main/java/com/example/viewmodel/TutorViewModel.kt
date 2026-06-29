package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.CoachPersona
import com.example.data.TutorRepository
import com.example.data.VocabularyWord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TutorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TutorRepository(db.chatDao())

    // Active coach persona selected by the user
    private val _selectedCoach = MutableStateFlow(CoachPersona.EMMA)
    val selectedCoach: StateFlow<CoachPersona> = _selectedCoach.asStateFlow()

    // Is the coach currently thinking / writing?
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Flow of messages for the selected coach
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = _selectedCoach
        .flatMapLatest { coach -> repository.getMessagesForCoach(coach.id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All learned vocabulary words
    val vocabularyList: StateFlow<List<VocabularyWord>> = repository.getAllVocabulary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Seed welcome message if the conversation is fresh
        viewModelScope.launch {
            CoachPersona.entries.forEach { coach ->
                repository.getMessagesForCoach(coach.id).collect { msgs ->
                    if (msgs.isEmpty()) {
                        val welcomeText = when (coach) {
                            CoachPersona.EMMA -> "Hello! I am Emma, your English speaking buddy. Let's chat about your day or any topic you like! How are you doing today?"
                            CoachPersona.JAMES -> "Good day. I am James, your professional English coach. Let's practice business dialogues, interview prep, or advanced work vocabulary. What is your career goal?"
                            CoachPersona.SOPHIA -> "Hello. I am Sophia, your academic IELTS/TOEFL tutor. I will help you master pristine grammar, complex syntax, and advanced collocations. Shall we begin a practice prompt?"
                        }
                        repository.insertMessage(
                            ChatMessage(
                                coachId = coach.id,
                                sender = "coach",
                                text = welcomeText
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectCoach(coach: CoachPersona) {
        _selectedCoach.value = coach
    }

    fun sendMessage(userText: String) {
        if (userText.trim().isEmpty() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            val coach = _selectedCoach.value
            // Capture a snapshot of current messages to pass as context
            val currentHistory = chatMessages.value

            // Call repository which inserts user message and retrieves/saves coach reply
            repository.getCoachResponse(
                coachPersona = coach,
                userMessageText = userText,
                history = currentHistory
            )
            _isGenerating.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            val coachId = _selectedCoach.value.id
            repository.clearHistory(coachId)
            
            // Re-insert welcome message
            val welcomeText = when (_selectedCoach.value) {
                CoachPersona.EMMA -> "Hello! I am Emma, your English speaking buddy. Let's chat about your day or any topic you like! How are you doing today?"
                CoachPersona.JAMES -> "Good day. I am James, your professional English coach. Let's practice business dialogues, interview prep, or advanced work vocabulary. What is your career goal?"
                CoachPersona.SOPHIA -> "Hello. I am Sophia, your academic IELTS/TOEFL tutor. I will help you master pristine grammar, complex syntax, and advanced collocations. Shall we begin a practice prompt?"
            }
            repository.insertMessage(
                ChatMessage(
                    coachId = coachId,
                    sender = "coach",
                    text = welcomeText
                )
            )
        }
    }

    fun deleteVocabularyWord(id: Int) {
        viewModelScope.launch {
            repository.deleteVocabulary(id)
        }
    }
}
