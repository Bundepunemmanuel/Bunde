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

    suspend fun generateStructuredConceptStep(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        conceptId: String,
        conceptTitle: String,
        conceptCategory: String,
        conceptSummary: String,
        conceptIndex: Int,
        totalConcepts: Int,
        previouslyTaughtConcepts: List<String>,
        uploadedNotesSummary: String? = null,
        struggleCount: Int = 0,
        recentlyUsedActivityTypes: List<String> = emptyList()
    ): GeminiResult<StructuredConceptStep>

    suspend fun evaluateStructuredAnswer(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        conceptTitle: String,
        learningObjective: String,
        activityQuestion: String,
        expectedAnswerCriteria: String,
        studentAnswer: String,
        attemptNumber: Int = 1
    ): GeminiResult<StructuredAnswerEvaluation>

    suspend fun handleTutorInterruption(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        currentConceptTitle: String,
        currentActivityQuestion: String,
        studentQuery: String
    ): GeminiResult<TutorInterruptionResponse>
}

data class StructuredActivity(
    val type: String, // e.g. "SCENARIO", "ERROR_DETECTION", "PREDICTION", "STEP_SOLVE", "COMPARISON", "CALCULATION", "CAUSE_EFFECT", "REASONING"
    val question: String,
    val options: List<String> = emptyList(),
    val expectedAnswerCriteria: String,
    val hint: String? = null
)

data class StructuredConceptStep(
    val conceptId: String,
    val learningObjective: String,
    val conciseExplanation: String,
    val concreteExample: String,
    val activity: StructuredActivity
)

enum class EvaluationVerdict {
    CORRECT,
    PARTIALLY_CORRECT,
    INCORRECT,
    NEEDS_CLARIFICATION
}

data class StructuredAnswerEvaluation(
    val verdict: EvaluationVerdict,
    val scoreOutOfTen: Int,
    val specificPraise: String,
    val explanationOfMistake: String? = null,
    val targetedHint: String? = null,
    val followUpActivity: StructuredActivity? = null,
    val conceptCompleted: Boolean,
    val allowsRetry: Boolean
)

data class TutorInterruptionResponse(
    val explanation: String,
    val bridgeBackToLesson: String
)
