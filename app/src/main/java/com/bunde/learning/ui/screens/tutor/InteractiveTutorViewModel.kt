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
import com.bunde.learning.domain.ai.GeneratedVisualPayload
import com.bunde.learning.domain.ai.StructuredActivity
import com.bunde.learning.domain.ai.StructuredAnswerEvaluation
import com.bunde.learning.domain.ai.StructuredConceptStep
import com.bunde.learning.domain.ai.VisualAidType
import com.bunde.learning.util.VisualRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Explicit Finite State Machine for Lesson Progression.
 * Controller strictly governs transitions; AI only generates learning content and evaluations.
 */
enum class LessonPhase {
    LOADING_TOPIC,
    EXPLANATION,          // Concise progressive concept explanation & real-world example
    PREDICTION_HOOK,      // Mini-story hook + quick guess BEFORE reading the explanation
    ACTIVITY,             // Specific activity presentation (scenario, calculation, prediction, etc.)
    WAITING_FOR_ANSWER,   // Awaiting student's input (answer button active)
    EVALUATING_ANSWER,    // Gemini is evaluating the submission against the objective criteria
    FEEDBACK,             // Evaluated feedback displayed (verdict, praise, misconception analysis, hint)
    CONCEPT_COMPLETED,    // Objective demonstrated or attempt limit reached; explicit Continue enabled
    SESSION_RECAP,        // 30-second "Story So Far" break card after every 3 concepts
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
    val selectedPredictionGuess: String? = null,
    val attemptNumber: Int = 1,
    val struggleCount: Int = 0,
    val conceptsCompletedThisSession: Int = 0,
    val previousStoryThread: String? = null,
    val sessionRecap: com.bunde.learning.domain.ai.SessionRecap? = null,
    val previouslyTaughtConcepts: List<String> = emptyList(),
    val recentlyUsedActivityTypes: List<String> = emptyList(),
    val interruptions: List<TutorInterruptionItem> = emptyList(),
    val isInterruptionLoading: Boolean = false,
    val isVisualLoading: Boolean = false,
    val visualImageBytes: ByteArray? = null,
    val visualFallbackSvg: String? = null,
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
    private var visualFetchJob: Job? = null

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
                visualFetchJob?.cancel()
                val firstConcept = loaded.subtopics.first()
                _uiState.value = _uiState.value.copy(
                    topicMap = loaded,
                    currentConceptIndex = 0,
                    currentSubtopic = firstConcept,
                    previouslyTaughtConcepts = emptyList(),
                    recentlyUsedActivityTypes = emptyList(),
                    interruptions = emptyList(),
                    selectedPredictionGuess = null,
                    previousStoryThread = null,
                    sessionRecap = null,
                    conceptsCompletedThisSession = 0,
                    isVisualLoading = false,
                    visualImageBytes = null,
                    visualFallbackSvg = null,
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
        visualFetchJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = LessonPhase.EXPLANATION,
                currentConceptIndex = index,
                currentSubtopic = concept,
                evaluationResult = null,
                studentAnswerInput = "",
                selectedPredictionGuess = null,
                isVisualLoading = false,
                visualImageBytes = null,
                visualFallbackSvg = null,
                attemptNumber = 1,
                errorMessage = null
            )

            val hasUploadedNotes = map.subtopics.any { it.isFromUploadedNotes }
            val notesContext = when {
                hasUploadedNotes && !map.unreadableNoteNotice.isNullOrBlank() ->
                    "${map.summary} (Note issue: ${map.unreadableNoteNotice})"
                hasUploadedNotes -> map.summary
                else -> null
            }

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
                uploadedNotesSummary = notesContext,
                struggleCount = _uiState.value.struggleCount,
                recentlyUsedActivityTypes = _uiState.value.recentlyUsedActivityTypes,
                previousStoryThread = _uiState.value.previousStoryThread
            )

            when (result) {
                is GeminiResult.Success -> {
                    val step = result.data
                    val updatedRecentTypes = (_uiState.value.recentlyUsedActivityTypes + step.activity.type).takeLast(4)
                    val shouldPredictFirst = !step.predictionPrompt.isNullOrBlank() && step.predictionOptions.isNotEmpty()
                    _uiState.value = _uiState.value.copy(
                        phase = if (shouldPredictFirst) LessonPhase.PREDICTION_HOOK else LessonPhase.WAITING_FOR_ANSWER,
                        currentStep = step,
                        currentActivity = step.activity,
                        recentlyUsedActivityTypes = updatedRecentTypes
                    )

                    if (step.visualAid.type == VisualAidType.GENERATED_IMAGE && !step.visualAid.imagePrompt.isNullOrBlank()) {
                        fetchConceptVisualAsync(map, concept, step)
                    }
                }
                is GeminiResult.Error -> {
                    // Safe, simple-English local recovery
                    val fallbackActivity = StructuredActivity(
                        type = "SCENARIO",
                        question = "In your own words, why is ${concept.title} important in ${map.topicName}?",
                        options = emptyList(),
                        expectedAnswerCriteria = concept.shortSummary,
                        hint = "Think back to the main idea about ${concept.title} we just read."
                    )
                    val fallbackStep = StructuredConceptStep(
                        conceptId = concept.id,
                        learningObjective = "Understand ${concept.title}",
                        conciseExplanation = "Let's look at ${concept.title} in ${map.topicName}.\n\nIn simple words: ${concept.shortSummary}",
                        concreteExample = "Think of an everyday example at school or at home where ${concept.title} is used. That helps make the main idea easy to remember.",
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

    private fun fetchConceptVisualAsync(
        map: TopicMap,
        concept: SubtopicNode,
        step: StructuredConceptStep
    ) {
        val imagePrompt = step.visualAid.imagePrompt ?: return
        val cacheKey = "${map.topicId}_${concept.id}_${imagePrompt.hashCode()}"
        visualFetchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVisualLoading = true)

            val cachedBytes = withContext(Dispatchers.IO) {
                VisualRenderer.loadCachedImageBytes(app, cacheKey)
            }
            if (cachedBytes != null) {
                if (_uiState.value.currentStep?.conceptId == step.conceptId) {
                    _uiState.value = _uiState.value.copy(
                        isVisualLoading = false,
                        visualImageBytes = cachedBytes
                    )
                }
                return@launch
            }

            val cachedSvg = withContext(Dispatchers.IO) {
                VisualRenderer.loadCachedSvg(app, cacheKey)
            }
            if (cachedSvg != null) {
                if (_uiState.value.currentStep?.conceptId == step.conceptId) {
                    _uiState.value = _uiState.value.copy(
                        isVisualLoading = false,
                        visualFallbackSvg = cachedSvg
                    )
                }
                return@launch
            }

            val result = gemini.generateConceptVisualImage(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                conceptTitle = concept.title,
                imagePrompt = imagePrompt
            )

            if (_uiState.value.currentStep?.conceptId != step.conceptId) return@launch

            when (result) {
                is GeminiResult.Success -> {
                    when (val payload = result.data) {
                        is GeneratedVisualPayload.RasterImage -> {
                            withContext(Dispatchers.IO) {
                                VisualRenderer.saveCachedImageBytes(app, cacheKey, payload.bytes)
                            }
                            _uiState.value = _uiState.value.copy(
                                isVisualLoading = false,
                                visualImageBytes = payload.bytes
                            )
                        }
                        is GeneratedVisualPayload.FallbackSvg -> {
                            withContext(Dispatchers.IO) {
                                VisualRenderer.saveCachedSvg(app, cacheKey, payload.svgMarkup)
                            }
                            _uiState.value = _uiState.value.copy(
                                isVisualLoading = false,
                                visualFallbackSvg = payload.svgMarkup
                            )
                        }
                    }
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isVisualLoading = false)
                }
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

                    // Spaced Retrieval Integration: update memory queue with story cue
                    if (evaluation.conceptCompleted) {
                        val revItem = RevisionItem(
                            id = "rev_${concept.id}",
                            topicId = map.topicId,
                            topicTitle = map.topicName,
                            subtopicTitle = concept.title,
                            prompt = activity.question,
                            expectedAnswer = activity.expectedAnswerCriteria,
                            memoryCue = listOfNotNull(
                                step.memoryHook?.let { "Memory Key: $it" },
                                step.memoryPicture ?: step.concreteExample
                            ).joinToString("\n").ifBlank { step.concreteExample },
                            intervalDays = if (evaluation.scoreOutOfTen >= 8) 3 else 1,
                            dueDate = System.currentTimeMillis() + (if (evaluation.scoreOutOfTen >= 8) 3 else 1) * 24 * 60 * 60 * 1000L,
                            isWeakArea = evaluation.scoreOutOfTen < 8
                        )
                        storage.updateRevisionItem(revItem)
                    }
                }
                is GeminiResult.Error -> {
                    // Safe recovery: accept answer with warm, simple feedback
                    val fallbackEval = StructuredAnswerEvaluation(
                        verdict = EvaluationVerdict.CORRECT,
                        scoreOutOfTen = 8,
                        specificPraise = "Nice try! You explained the main idea clearly in your own words.",
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

    fun submitPredictionGuess(guess: String) {
        if (_uiState.value.phase != LessonPhase.PREDICTION_HOOK) return
        _uiState.value = _uiState.value.copy(
            selectedPredictionGuess = guess,
            phase = LessonPhase.WAITING_FOR_ANSWER
        )
    }

    fun skipPredictionToExplanation() {
        if (_uiState.value.phase != LessonPhase.PREDICTION_HOOK) return
        _uiState.value = _uiState.value.copy(
            selectedPredictionGuess = null,
            phase = LessonPhase.WAITING_FOR_ANSWER
        )
    }

    fun continueFromRecap() {
        val state = _uiState.value
        val map = state.topicMap ?: return
        val nextConcept = state.currentSubtopic ?: return
        _uiState.value = state.copy(sessionRecap = null)
        startConcept(nextConcept, state.currentConceptIndex, map)
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
        val updatedStoryThread = state.currentStep?.storyThreadSummary ?: state.previousStoryThread
        val completedInSession = state.conceptsCompletedThisSession + 1

        if (nextIndex < map.subtopics.size) {
            visualFetchJob?.cancel()
            val nextConcept = map.subtopics[nextIndex]
            val shouldShowRecapBreak = completedInSession % 3 == 0

            _uiState.value = state.copy(
                phase = if (shouldShowRecapBreak) LessonPhase.SESSION_RECAP else LessonPhase.NEXT_CONCEPT,
                currentConceptIndex = nextIndex,
                currentSubtopic = nextConcept,
                previouslyTaughtConcepts = updatedPreviouslyTaught,
                previousStoryThread = updatedStoryThread,
                conceptsCompletedThisSession = completedInSession,
                sessionRecap = null,
                evaluationResult = null,
                studentAnswerInput = "",
                selectedPredictionGuess = null,
                isVisualLoading = false,
                visualImageBytes = null,
                visualFallbackSvg = null,
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

            if (shouldShowRecapBreak) {
                viewModelScope.launch {
                    val recapRes = gemini.generateSessionRecap(
                        apiKey = apiKey,
                        subject = map.subject,
                        topic = map.topicName,
                        classLevel = map.classLevel,
                        completedConcepts = updatedPreviouslyTaught,
                        latestStoryThread = updatedStoryThread
                    )
                    if (recapRes is GeminiResult.Success) {
                        _uiState.value = _uiState.value.copy(sessionRecap = recapRes.data)
                    }
                }
            } else {
                startConcept(nextConcept, nextIndex, map)
            }
        } else {
            // All concepts completed!
            _uiState.value = state.copy(
                phase = LessonPhase.LESSON_COMPLETED,
                previouslyTaughtConcepts = updatedPreviouslyTaught,
                previousStoryThread = updatedStoryThread,
                conceptsCompletedThisSession = completedInSession
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

            val currentExplanation = state.currentStep?.conciseExplanation
            val history = state.interruptions.map { it.studentQuery to it.tutorExplanation }

            val result = gemini.handleTutorInterruption(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                currentConceptTitle = currentConcept,
                currentActivityQuestion = currentQuestion,
                studentQuery = trimmed,
                currentConceptExplanation = currentExplanation,
                conversationHistory = history
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
                    tutorExplanation = "Good question! Think about \"$trimmed\" as a simple part of $currentConcept—try picturing a real everyday example of how it works.",
                    bridgeBack = "When you feel ready, go ahead and try the question below."
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
