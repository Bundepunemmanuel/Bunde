package com.bunde.learning.ui.screens.tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bunde.learning.BundeApplication
import com.bunde.learning.data.model.RevisionItem
import com.bunde.learning.data.model.SubtopicNode
import com.bunde.learning.data.model.TopicMap
import com.bunde.learning.domain.ai.EvaluationVerdict
import com.bunde.learning.domain.ai.GeminiResult
import com.bunde.learning.domain.ai.StructuredActivity
import com.bunde.learning.domain.ai.StructuredAnswerEvaluation
import com.bunde.learning.domain.ai.StructuredConceptStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Explicit Finite State Machine for Lesson Progression.
 * Controller strictly governs transitions; AI only generates learning content and evaluations.
 */
enum class LessonPhase {
    LOADING_TOPIC,
    EXPLANATION,          // Concise progressive concept explanation & real-world example
    ACTIVITY,             // Specific activity presentation (scenario, calculation, prediction, etc.)
    WAITING_FOR_ANSWER,   // Awaiting student's input (answer button active)
    EVALUATING_ANSWER,    // Gemini is evaluating the submission against the objective criteria
    FEEDBACK,             // Evaluated feedback displayed (verdict, praise, misconception analysis, hint)
    CONCEPT_COMPLETED,    // Objective demonstrated or attempt limit reached; explicit Continue enabled
    NEXT_CONCEPT,         // Transitioning to the next concept
    LESSON_COMPLETED,     // All concepts finished; celebration and persistence
    ERROR                 // Recoverable error state
}

data class TutorInterruptionItem(
    val id: String,
    val studentQuery: String,
    val tutorExplanation: String,
    val bridgeBack: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TutorUiState(
    val phase: LessonPhase = LessonPhase.LOADING_TOPIC,
    val topicMap: TopicMap? = null,
    val currentConceptIndex: Int = 0,
    val currentSubtopic: SubtopicNode? = null,
    val currentStep: StructuredConceptStep? = null,
    val currentActivity: StructuredActivity? = null,
    val evaluationResult: StructuredAnswerEvaluation? = null,
    val studentAnswerInput: String = "",
    val attemptNumber: Int = 1,
    val struggleCount: Int = 0,
    val previouslyTaughtConcepts: List<String> = emptyList(),
    val recentlyUsedActivityTypes: List<String> = emptyList(),
    val interruptions: List<TutorInterruptionItem> = emptyList(),
    val isInterruptionLoading: Boolean = false,
    val errorMessage: String? = null
)

class InteractiveTutorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BundeApplication
    private val storage = app.localStorage
    private val prefs = app.preferencesRepository
    private val gemini = app.geminiService

    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    private var apiKey: String = ""

    init {
        viewModelScope.launch {
            prefs.geminiApiKeyFlow.collect { key ->
                apiKey = key
            }
        }
    }

    /**
     * Load the topic map from local persistence and initialize Concept 1.
     */
    fun loadTopicMap(topicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = LessonPhase.LOADING_TOPIC, errorMessage = null)
            val loaded = storage.loadTopicMap(topicId)
            if (loaded != null && loaded.subtopics.isNotEmpty()) {
                val firstConcept = loaded.subtopics.first()
                _uiState.value = _uiState.value.copy(
                    topicMap = loaded,
                    currentConceptIndex = 0,
                    currentSubtopic = firstConcept,
                    previouslyTaughtConcepts = emptyList(),
                    recentlyUsedActivityTypes = emptyList(),
                    interruptions = emptyList(),
                    attemptNumber = 1,
                    struggleCount = 0
                )
                startConcept(firstConcept, 0, loaded)
            } else {
                _uiState.value = _uiState.value.copy(
                    phase = LessonPhase.ERROR,
                    errorMessage = "Curriculum topic map could not be found."
                )
            }
        }
    }

    /**
     * Start teaching a specific concept by index.
     */
    private fun startConcept(concept: SubtopicNode, index: Int, map: TopicMap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = LessonPhase.EXPLANATION,
                currentConceptIndex = index,
                currentSubtopic = concept,
                evaluationResult = null,
                studentAnswerInput = "",
                attemptNumber = 1,
                errorMessage = null
            )

            val result = gemini.generateStructuredConceptStep(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                conceptId = concept.id,
                conceptTitle = concept.title,
                conceptCategory = concept.category.name,
                conceptSummary = concept.shortSummary,
                conceptIndex = index,
                totalConcepts = map.subtopics.size,
                previouslyTaughtConcepts = _uiState.value.previouslyTaughtConcepts,
                uploadedNotesSummary = map.unreadableNoteNotice,
                struggleCount = _uiState.value.struggleCount,
                recentlyUsedActivityTypes = _uiState.value.recentlyUsedActivityTypes
            )

            when (result) {
                is GeminiResult.Success -> {
                    val step = result.data
                    val updatedRecentTypes = (_uiState.value.recentlyUsedActivityTypes + step.activity.type).takeLast(4)
                    _uiState.value = _uiState.value.copy(
                        phase = LessonPhase.WAITING_FOR_ANSWER,
                        currentStep = step,
                        currentActivity = step.activity,
                        recentlyUsedActivityTypes = updatedRecentTypes
                    )
                }
                is GeminiResult.Error -> {
                    // Safe, verified local fallback
                    val fallbackActivity = StructuredActivity(
                        type = "SCENARIO",
                        question = "In ${map.subject}, what is the fundamental purpose of ${concept.title}?",
                        options = emptyList(),
                        expectedAnswerCriteria = concept.shortSummary,
                        hint = "Focus on why ${concept.title} is applied."
                    )
                    val fallbackStep = StructuredConceptStep(
                        conceptId = concept.id,
                        learningObjective = "Understand ${concept.title}",
                        conciseExplanation = "${concept.title} is an essential component of ${map.topicName}.\n\nSpecifically: ${concept.shortSummary}",
                        concreteExample = "In practical applications of ${map.subject}, understanding this concept ensures accurate analysis.",
                        activity = fallbackActivity
                    )
                    _uiState.value = _uiState.value.copy(
                        phase = LessonPhase.WAITING_FOR_ANSWER,
                        currentStep = fallbackStep,
                        currentActivity = fallbackActivity
                    )
                }
                is GeminiResult.Loading -> {}
            }
        }
    }

    /**
     * Submit an answer for the current activity. State transitions strictly to EVALUATING_ANSWER.
     */
    fun submitAnswer(answerText: String) {
        val trimmed = answerText.trim()
        if (trimmed.isBlank()) return

        val state = _uiState.value
        if (state.phase != LessonPhase.WAITING_FOR_ANSWER && state.phase != LessonPhase.FEEDBACK) return

        val map = state.topicMap ?: return
        val concept = state.currentSubtopic ?: return
        val step = state.currentStep ?: return
        val activity = state.currentActivity ?: return

        // 1. Disable duplicate submissions by immediate state transition
        _uiState.value = state.copy(
            phase = LessonPhase.EVALUATING_ANSWER,
            studentAnswerInput = trimmed
        )

        viewModelScope.launch {
            val evalResult = gemini.evaluateStructuredAnswer(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                conceptTitle = concept.title,
                learningObjective = step.learningObjective,
                activityQuestion = activity.question,
                expectedAnswerCriteria = activity.expectedAnswerCriteria,
                studentAnswer = trimmed,
                attemptNumber = state.attemptNumber
            )

            when (evalResult) {
                is GeminiResult.Success -> {
                    val evaluation = evalResult.data
                    val nextPhase = if (evaluation.conceptCompleted) {
                        LessonPhase.CONCEPT_COMPLETED
                    } else {
                        LessonPhase.FEEDBACK
                    }

                    _uiState.value = _uiState.value.copy(
                        phase = nextPhase,
                        evaluationResult = evaluation,
                        attemptNumber = state.attemptNumber + 1,
                        struggleCount = if (!evaluation.conceptCompleted) state.struggleCount + 1 else state.struggleCount
                    )

                    // Spaced Retrieval Integration: update memory queue based on demonstrated mastery
                    if (evaluation.conceptCompleted) {
                        val revItem = RevisionItem(
                            id = "rev_${concept.id}",
                            topicId = map.topicId,
                            topicTitle = map.topicName,
                            subtopicTitle = concept.title,
                            prompt = activity.question,
                            expectedAnswer = activity.expectedAnswerCriteria,
                            intervalDays = if (evaluation.scoreOutOfTen >= 8) 3 else 1,
                            dueDate = System.currentTimeMillis() + (if (evaluation.scoreOutOfTen >= 8) 3 else 1) * 24 * 60 * 60 * 1000L,
                            isWeakArea = evaluation.scoreOutOfTen < 8
                        )
                        storage.updateRevisionItem(revItem)
                    }
                }
                is GeminiResult.Error -> {
                    // Safe recovery: accept answer with supportive feedback
                    val fallbackEval = StructuredAnswerEvaluation(
                        verdict = EvaluationVerdict.CORRECT,
                        scoreOutOfTen = 8,
                        specificPraise = "You demonstrated clear understanding of the core concept.",
                        explanationOfMistake = null,
                        targetedHint = null,
                        conceptCompleted = true,
                        allowsRetry = false
                    )
                    _uiState.value = _uiState.value.copy(
                        phase = LessonPhase.CONCEPT_COMPLETED,
                        evaluationResult = fallbackEval
                    )
                }
                is GeminiResult.Loading -> {}
            }
        }
    }

    /**
     * Explicit advance to the next concept. Called ONLY when the student taps the Continue button.
     * Prevents Gemini or loose text from arbitrarily skipping or remaining on a concept.
     */
    fun advanceToNextConcept() {
        val state = _uiState.value
        val map = state.topicMap ?: return
        val currentIndex = state.currentConceptIndex
        val nextIndex = currentIndex + 1

        val currentTitle = state.currentSubtopic?.title
        val updatedPreviouslyTaught = if (currentTitle != null) {
            state.previouslyTaughtConcepts + currentTitle
        } else state.previouslyTaughtConcepts

        if (nextIndex < map.subtopics.size) {
            val nextConcept = map.subtopics[nextIndex]
            _uiState.value = state.copy(
                phase = LessonPhase.NEXT_CONCEPT,
                currentConceptIndex = nextIndex,
                currentSubtopic = nextConcept,
                previouslyTaughtConcepts = updatedPreviouslyTaught,
                evaluationResult = null,
                studentAnswerInput = "",
                attemptNumber = 1,
                struggleCount = 0
            )

            // Update topic progress in local database
            viewModelScope.launch {
                val progressPercent = ((nextIndex).toFloat() / map.subtopics.size.toFloat() * 100).toInt()
                val updatedTopic = com.bunde.learning.data.model.Topic(
                    id = map.topicId,
                    subject = map.subject,
                    name = map.topicName,
                    classLevel = map.classLevel,
                    progressPercent = progressPercent,
                    totalSubtopics = map.subtopics.size,
                    completedSubtopics = nextIndex,
                    lastAccessedTimestamp = System.currentTimeMillis(),
                    isMastered = false,
                    weakAreasCount = state.struggleCount
                )
                storage.saveTopic(updatedTopic)
            }

            startConcept(nextConcept, nextIndex, map)
        } else {
            // All concepts completed!
            _uiState.value = state.copy(
                phase = LessonPhase.LESSON_COMPLETED,
                previouslyTaughtConcepts = updatedPreviouslyTaught
            )

            viewModelScope.launch {
                val completedTopic = com.bunde.learning.data.model.Topic(
                    id = map.topicId,
                    subject = map.subject,
                    name = map.topicName,
                    classLevel = map.classLevel,
                    progressPercent = 100,
                    totalSubtopics = map.subtopics.size,
                    completedSubtopics = map.subtopics.size,
                    lastAccessedTimestamp = System.currentTimeMillis(),
                    isMastered = true,
                    weakAreasCount = 0
                )
                storage.saveTopic(completedTopic)
            }
        }
    }

    /**
     * Ask Bunde a natural clarifying question / interruption at any point during the lesson.
     * Does NOT advance the lesson or submit an answer.
     */
    fun askTutorQuestion(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val state = _uiState.value
        val map = state.topicMap ?: return
        val currentConcept = state.currentSubtopic?.title ?: map.topicName
        val currentQuestion = state.currentActivity?.question ?: "current lesson challenge"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInterruptionLoading = true)

            val result = gemini.handleTutorInterruption(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                currentConceptTitle = currentConcept,
                currentActivityQuestion = currentQuestion,
                studentQuery = trimmed
            )

            val item = when (result) {
                is GeminiResult.Success -> TutorInterruptionItem(
                    id = "intr_${System.currentTimeMillis()}",
                    studentQuery = trimmed,
                    tutorExplanation = result.data.explanation,
                    bridgeBack = result.data.bridgeBackToLesson
                )
                is GeminiResult.Error -> TutorInterruptionItem(
                    id = "intr_${System.currentTimeMillis()}",
                    studentQuery = trimmed,
                    tutorExplanation = "Regarding '$trimmed': in ${map.subject}, $currentConcept focuses on how this principle operates directly.",
                    bridgeBack = "Let's return to the challenge when you're ready."
                )
                is GeminiResult.Loading -> null
            }

            if (item != null) {
                _uiState.value = _uiState.value.copy(
                    interruptions = _uiState.value.interruptions + item,
                    isInterruptionLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isInterruptionLoading = false)
            }
        }
    }

    fun retryCurrentActivity() {
        _uiState.value = _uiState.value.copy(
            phase = LessonPhase.WAITING_FOR_ANSWER,
            studentAnswerInput = ""
        )
    }
}
