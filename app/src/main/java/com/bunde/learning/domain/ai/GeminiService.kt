package com.bunde.learning.domain.ai

import com.bunde.learning.data.model.TopicMap

sealed class GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String, val isNetworkIssue: Boolean = false, val isKeyMissing: Boolean = false) : GeminiResult<Nothing>()
    object Loading : GeminiResult<Nothing>()
}

interface GeminiService {
    suspend fun testConnection(apiKey: String): GeminiResult<Boolean>

    suspend fun analyzeUploadedNotes(
        apiKey: String,
        imageBytesList: List<ByteArray>,
        userSubject: String? = null
    ): GeminiResult<TopicMap>

    suspend fun generateTopicMap(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String
    ): GeminiResult<TopicMap>

    suspend fun askTutor(
        apiKey: String,
        topic: String,
        subtopic: String,
        conversationHistory: List<Pair<String, String>>,
        studentMessage: String
    ): GeminiResult<String>

    suspend fun evaluateAnswer(
        apiKey: String,
        question: String,
        expectedConcept: String,
        studentAnswer: String
    ): GeminiResult<AnswerEvaluation>
}

data class AnswerEvaluation(
    val isCorrect: Boolean,
    val scoreOutOfTen: Int,
    val praise: String,
    val correction: String?,
    val nextStepRecommendation: String
)
