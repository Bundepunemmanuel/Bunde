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

    suspend fun generateConceptStep(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        subtopicTitle: String,
        category: String,
        conceptSummary: String,
        conceptIndex: Int,
        totalConcepts: Int,
        previouslyTaughtConcepts: List<String>,
        uploadedNotesSummary: String? = null,
        struggleCount: Int = 0
    ): GeminiResult<ConceptStepResponse>

    suspend fun askTutor(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        currentConcept: String,
        currentActivityQuestion: String?,
        conversationHistory: List<Pair<String, String>>,
        studentMessage: String,
        previouslyTaughtConcepts: List<String> = emptyList(),
        struggleCount: Int = 0
    ): GeminiResult<TutorTurnResponse>

    suspend fun evaluateAnswer(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        currentConcept: String,
        activityQuestion: String,
        expectedConceptOrKeyPoints: String,
        studentAnswer: String,
        attemptNumber: Int = 1
    ): GeminiResult<AnswerEvaluation>
}

data class ConceptStepResponse(
    val explanation: String,
    val checkActivityQuestion: String,
    val activityType: String = "QUESTION", // e.g., "QUESTION", "CHALLENGE", "PREDICTION", "STEP_BY_STEP", "COMPARISON"
    val expectedConceptKeyPoints: String
)

data class TutorTurnResponse(
    val text: String,
    val isInterruptionHandled: Boolean = false,
    val resumePrompt: String? = null
)

data class AnswerEvaluation(
    val isCorrect: Boolean,
    val scoreOutOfTen: Int,
    val praise: String,
    val correction: String?,
    val hint: String? = null,
    val allowsRetry: Boolean = false,
    val isMastered: Boolean = false,
    val nextStepRecommendation: String
)
