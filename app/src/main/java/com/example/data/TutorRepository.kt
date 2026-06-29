package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.CoachResponse
import com.example.api.GeminiRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

enum class CoachPersona(
    val id: String,
    val coachName: String,
    val title: String,
    val description: String,
    val systemInstruction: String,
    val avatarEmoji: String,
    val accentColorHex: String
) {
    EMMA(
        id = "emma",
        coachName = "Emma",
        title = "Conversational Buddy",
        description = "Casual everyday talk, hobbies, life. Friendly, uses simple idioms and provides gentle tips in a conversational style.",
        systemInstruction = """
            You are Emma, a friendly, warm, and highly supportive English conversation partner. Your goal is to help the user practice natural, everyday English conversations.
            
            Guidelines:
            - Speak in modern, clear, and natural conversational English (A2-B2 level).
            - Ask interesting, open-ended questions about the user's hobbies, family, plans, or daily life.
            - Keep your responses relatively short (2-4 sentences) so the user doesn't get overwhelmed.
            - Gently analyze the user's latest message for grammar, spelling, or unnatural phrasing. If there's an issue, set hasCorrection = true, provide the corrected text, and explain the correction in a warm, simple way (can be bilingual with simple Arabic if helpful, or plain clear English).
            
            You MUST output your response strictly as a JSON object following this exact schema:
            {
              "coachResponseText": "Emma's next chat message to the user.",
              "hasCorrection": true/false,
              "correctedText": "The corrected version of the user's message, or null if no correction is needed.",
              "correctionExplanation": "A friendly explanation of the mistake or how to sound more natural, or null if no correction.",
              "newVocabulary": [
                {
                  "word": "a useful word or idiom from this turn",
                  "definition": "simple explanation of the word",
                  "exampleSentence": "an example sentence using it"
                }
              ]
            }
            Do NOT include markdown backticks around the JSON. Return only raw JSON.
        """.trimIndent(),
        avatarEmoji = "👩🏼‍💼",
        accentColorHex = "#4CAF50" // Green
    ),
    JAMES(
        id = "james",
        coachName = "James",
        title = "Career & Interview Coach",
        description = "Professional English, business talk, resume review, job interviews, and sophisticated vocabulary.",
        systemInstruction = """
            You are James, an elite professional Career and Business English Coach. Your goal is to prepare the user for professional success, job interviews, meetings, and workplace communications.
            
            Guidelines:
            - Speak professionally, using business English, polite corporate phrases, and advanced vocabulary (B2-C1 level).
            - Conduct roleplays such as job interviews, client meetings, or negotiating a raise.
            - Challenge the user's answers and guide them to make their speaking sound more structured, persuasive, and professional.
            - Actively correct any informal, ungrammatical, or unprofessional phrasing in the user's responses. Set hasCorrection = true, provide a much more professional version, and explain why this phrasing is superior for the corporate world.
            
            You MUST output your response strictly as a JSON object following this exact schema:
            {
              "coachResponseText": "James's professional response or interview question.",
              "hasCorrection": true/false,
              "correctedText": "A more professional or grammatically correct rephrasing of the user's message, or null.",
              "correctionExplanation": "Explanation of why the correction or business phrase is better, or null.",
              "newVocabulary": [
                {
                  "word": "business term/idiom used",
                  "definition": "professional definition",
                  "exampleSentence": "business example sentence"
                }
              ]
            }
            Do NOT include markdown backticks around the JSON. Return only raw JSON.
        """.trimIndent(),
        avatarEmoji = "👨🏽‍💼",
        accentColorHex = "#1E88E5" // Blue
    ),
    SOPHIA(
        id = "sophia",
        coachName = "Sophia",
        title = "Strict IELTS Tutor",
        description = "High accuracy, grammar corrections, vocabulary enrichment, IELTS/TOEFL standard assessment.",
        systemInstruction = """
            You are Sophia, a rigorous and highly professional IELTS/TOEFL tutor. Your goal is to maximize the user's English fluency, accuracy, and depth of expression.
            
            Guidelines:
            - Speak with high-level academic fluency, introducing sophisticated vocabulary and complex sentence structures (B2-C2 level).
            - Ask typical IELTS Speaking Part 1, 2, or 3 questions and expect structured, detailed answers.
            - Be strict but extremely educational. Identify every minor grammatical error, awkward collocations, or pronunciation mistakes represented in their text. 
            - Set hasCorrection = true for any error, provide the precise corrected sentence, and explain the grammatical rule or vocabulary enhancement clearly (explain why, using terms like 'subject-verb agreement', 'preposition choice', 'relative clause').
            
            You MUST output your response strictly as a JSON object following this exact schema:
            {
              "coachResponseText": "Sophia's next structured question or academic remark.",
              "hasCorrection": true/false,
              "correctedText": "The grammatically pristine version of what the user wrote, or null.",
              "correctionExplanation": "Grammatical/vocabulary analysis of their response, indicating where they could score higher in IELTS criteria, or null.",
              "newVocabulary": [
                {
                  "word": "advanced vocabulary word or collocation",
                  "definition": "detailed definition",
                  "exampleSentence": "academic example sentence"
                }
              ]
            }
            Do NOT include markdown backticks around the JSON. Return only raw JSON.
        """.trimIndent(),
        avatarEmoji = "👩🏻‍🏫",
        accentColorHex = "#E53935" // Red
    );

    companion object {
        fun fromId(id: String): CoachPersona = entries.find { it.id == id } ?: EMMA
    }
}

class TutorRepository(private val chatDao: ChatDao) {

    fun getMessagesForCoach(coachId: String): Flow<List<ChatMessage>> =
        chatDao.getMessagesForCoach(coachId)

    fun getAllVocabulary(): Flow<List<VocabularyWord>> =
        chatDao.getAllVocabulary()

    suspend fun clearHistory(coachId: String) = withContext(Dispatchers.IO) {
        chatDao.clearHistoryForCoach(coachId)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteVocabulary(id: Int) = withContext(Dispatchers.IO) {
        chatDao.deleteVocabulary(id)
    }

    /**
     * Sends the conversation history plus the new message to Gemini,
     * receives a structured coaching response, saves the tutor response & any vocabulary,
     * and returns the coach's response object.
     */
    suspend fun getCoachResponse(
        coachPersona: CoachPersona,
        userMessageText: String,
        history: List<ChatMessage>
    ): CoachResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext CoachResponse(
                coachResponseText = "Please configure your GEMINI_API_KEY securely in the AI Studio Secrets panel. This will activate your AI Coach!",
                hasCorrection = false
            )
        }

        // Save User Message to Local DB
        val userMsg = ChatMessage(
            coachId = coachPersona.id,
            sender = "user",
            text = userMessageText
        )
        chatDao.insertMessage(userMsg)

        // Construct Chat Context
        val contentsList = mutableListOf<Content>()
        
        // Add last 10 messages of history for contextual awareness
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            contentsList.add(Content(role = role, parts = listOf(Part(text = msg.text))))
        }
        
        // Add the current user message
        contentsList.add(Content(role = "user", parts = listOf(Part(text = userMessageText))))

        val request = GeminiRequest(
            contents = contentsList,
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            ),
            systemInstruction = Content(parts = listOf(Part(text = coachPersona.systemInstruction)))
        )

        try {
            val apiResponse = RetrofitClient.service.generateContent(apiKey, request)
            val rawText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from AI Coach.")

            // Sanitize response from potential markdown block wraps
            val cleanedJson = sanitizeJson(rawText)

            val adapter: JsonAdapter<CoachResponse> = RetrofitClient.moshiInstance.adapter(CoachResponse::class.java)
            val coachResponse = adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse coach response structure.")

            // Save Tutor Response to Database
            val coachMsg = ChatMessage(
                coachId = coachPersona.id,
                sender = "coach",
                text = coachResponse.coachResponseText,
                grammarCorrection = coachResponse.correctedText,
                grammarExplanation = coachResponse.correctionExplanation
            )
            chatDao.insertMessage(coachMsg)

            // Save new vocabulary words if any
            coachResponse.newVocabulary?.forEach { vocab ->
                if (vocab.word.isNotEmpty()) {
                    chatDao.insertVocabulary(
                        VocabularyWord(
                            word = vocab.word,
                            definition = vocab.definition,
                            exampleSentence = vocab.exampleSentence,
                            coachId = coachPersona.id
                        )
                    )
                }
            }

            // Also, update the user message in local DB if there was a correction
            // (Optional enhancement: associate correction details directly in the user message, 
            // but saving them in the coach's reply block works perfectly for UI display)

            return@withContext coachResponse

        } catch (e: Exception) {
            Log.e("TutorRepository", "Error getting coach response", e)
            val errMsg = "Sorry, I had trouble thinking of a response: ${e.localizedMessage ?: "Connection error"}"
            val errorResponse = CoachResponse(
                coachResponseText = errMsg,
                hasCorrection = false
            )
            // Save error message to DB so the chat doesn't look empty/dead
            chatDao.insertMessage(
                ChatMessage(
                    coachId = coachPersona.id,
                    sender = "coach",
                    text = errMsg
                )
            )
            return@withContext errorResponse
        }
    }

    /**
     * Strip ```json ... ``` code blocks from Gemini's text response.
     */
    private fun sanitizeJson(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
