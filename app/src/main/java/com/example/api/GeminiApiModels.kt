package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// The structured model we parse from the JSON string inside candidates[0].content.parts[0].text
@JsonClass(generateAdapter = true)
data class CoachResponse(
    @Json(name = "coachResponseText") val coachResponseText: String,
    @Json(name = "hasCorrection") val hasCorrection: Boolean,
    @Json(name = "correctedText") val correctedText: String? = null,
    @Json(name = "correctionExplanation") val correctionExplanation: String? = null,
    @Json(name = "newVocabulary") val newVocabulary: List<NewVocabularyItem>? = null
)

@JsonClass(generateAdapter = true)
data class NewVocabularyItem(
    @Json(name = "word") val word: String,
    @Json(name = "definition") val definition: String,
    @Json(name = "exampleSentence") val exampleSentence: String
)
