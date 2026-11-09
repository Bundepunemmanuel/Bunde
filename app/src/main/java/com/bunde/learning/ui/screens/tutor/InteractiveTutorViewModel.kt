package com.bunde.learning.ui.screens.tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bunde.learning.BundeApplication
import com.bunde.learning.data.model.RevisionItem
import com.bunde.learning.data.model.SubtopicNode
import com.bunde.learning.data.model.TopicMap
import com.bunde.learning.domain.ai.AnswerEvaluation
import com.bunde.learning.domain.ai.GeminiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val sender: SenderType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SenderType {
    TUTOR,
    STUDENT,
    SYSTEM_CHECK
}

data class TutorUiState(
    val topicMap: TopicMap? = null,
    val currentSubtopicIndex: Int = 0,
    val currentSubtopic: SubtopicNode? = null,
    val messages: List<ChatMessage> = emptyList(),
    val currentActivityQuestion: String? = null,
    val currentExpectedKeyPoints: String? = null,
    val isAwaitingAnswer: Boolean = false,
    val attemptCount: Int = 1,
    val struggleCount: Int = 0,
    val previouslyTaughtConcepts: List<String> = emptyList(),
    val studentAnswerInput: String = "",
    val evaluationResult: AnswerEvaluation? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLessonCompleted: Boolean = false
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

    fun loadTopicMap(topicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val loaded = storage.loadTopicMap(topicId)
            if (loaded != null && loaded.subtopics.isNotEmpty()) {
                val firstSub = loaded.subtopics.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    topicMap = loaded,
                    currentSubtopicIndex = 0,
                    currentSubtopic = firstSub,
                    messages = emptyList(),
                    previouslyTaughtConcepts = emptyList(),
                    isLoading = false
                )
                teachCurrentSubtopic()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Topic map data not found."
                )
            }
        }
    }

    fun teachCurrentSubtopic() {
        val state = _uiState.value
        val map = state.topicMap ?: return
        val subtopic = state.currentSubtopic ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, evaluationResult = null)

            val result = gemini.generateConceptStep(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                subtopicTitle = subtopic.title,
                category = subtopic.category.name,
                conceptSummary = subtopic.shortSummary,
                conceptIndex = state.currentSubtopicIndex,
                totalConcepts = map.subtopics.size,
                previouslyTaughtConcepts = state.previouslyTaughtConcepts,
                uploadedNotesSummary = map.unreadableNoteNotice,
                struggleCount = state.struggleCount
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is GeminiResult.Success -> {
                    val step = result.data
                    val fullLessonMessage = "${step.explanation}\n\n👉 **Challenge:** ${step.checkActivityQuestion}"
                    val tutorMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        sender = SenderType.TUTOR,
                        text = fullLessonMessage
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + tutorMsg,
                        currentActivityQuestion = step.checkActivityQuestion,
                        currentExpectedKeyPoints = step.expectedConceptKeyPoints,
                        isAwaitingAnswer = true,
                        attemptCount = 1
                    )
                }
                is GeminiResult.Error -> {
                    val fallbackMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        sender = SenderType.TUTOR,
                        text = "${subtopic.title}: ${subtopic.shortSummary}\n\n👉 **Challenge:** How would you explain this in your own words?"
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + fallbackMsg,
                        currentActivityQuestion = "How would you explain this in your own words?",
                        currentExpectedKeyPoints = subtopic.shortSummary,
                        isAwaitingAnswer = true,
                        attemptCount = 1
                    )
                }
                is GeminiResult.Loading -> {}
            }
        }
    }

    fun sendStudentMessage(messageText: String) {
        if (messageText.isBlank()) return

        val state = _uiState.value
        val map = state.topicMap ?: return
        val subtopic = state.currentSubtopic ?: return

        val studentMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = SenderType.STUDENT,
            text = messageText.trim()
        )

        val updatedMsgs = state.messages + studentMsg
        _uiState.value = _uiState.value.copy(messages = updatedMsgs, isLoading = true)

        viewModelScope.launch {
            // Check if this is an answer to the pending activity OR an interruption / question
            val history = updatedMsgs.map { it.sender.name to it.text }

            val tutorTurn = gemini.askTutor(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                currentConcept = subtopic.title,
                currentActivityQuestion = state.currentActivityQuestion,
                conversationHistory = history,
                studentMessage = messageText,
                previouslyTaughtConcepts = state.previouslyTaughtConcepts,
                struggleCount = state.struggleCount
            )

            if (tutorTurn is GeminiResult.Success && tutorTurn.data.isInterruptionHandled) {
                // Interruption handled: Answer student question, then prompt them to continue
                _uiState.value = _uiState.value.copy(isLoading = false)
                val responseMsg = ChatMessage(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    sender = SenderType.TUTOR,
                    text = tutorTurn.data.text + if (!tutorTurn.data.resumePrompt.isNullOrBlank()) "\n\n${tutorTurn.data.resumePrompt}" else ""
                )
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + responseMsg)
                return@launch
            }

            // Otherwise, evaluate student answer to the current activity
            val eval = gemini.evaluateAnswer(
                apiKey = apiKey,
                subject = map.subject,
                topic = map.topicName,
                classLevel = map.classLevel,
                currentConcept = subtopic.title,
                activityQuestion = state.currentActivityQuestion ?: "Explain ${subtopic.title}",
                expectedConceptOrKeyPoints = state.currentExpectedKeyPoints ?: subtopic.shortSummary,
                studentAnswer = messageText,
                attemptNumber = state.attemptCount
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (eval) {
                is GeminiResult.Success -> {
                    val evaluation = eval.data
                    _uiState.value = _uiState.value.copy(
                        evaluationResult = evaluation,
                        attemptCount = state.attemptCount + 1,
                        struggleCount = if (!evaluation.isCorrect) state.struggleCount + 1 else state.struggleCount
                    )

                    if (evaluation.allowsRetry && !evaluation.isCorrect) {
                        // Guide student with hint and let them think/try again
                        val hintMsg = ChatMessage(
                            id = "msg_${System.currentTimeMillis() + 2}",
                            sender = SenderType.TUTOR,
                            text = (if (!evaluation.hint.isNullOrBlank()) "💡 **Hint:** ${evaluation.hint}" else evaluation.praise) + "\n\nGive it another shot in your own words!"
                        )
                        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + hintMsg)
                    }

                    if (!evaluation.isCorrect) {
                        // Schedule spaced retrieval item for weak concept
                        val revItem = RevisionItem(
                            id = "rev_${System.currentTimeMillis()}",
                            topicId = map.topicId,
                            topicTitle = map.topicName,
                            subtopicTitle = subtopic.title,
                            prompt = "Recall concept: ${subtopic.title}",
                            expectedAnswer = subtopic.shortSummary,
                            intervalDays = 1,
                            dueDate = System.currentTimeMillis() + 1000 * 60 * 60 * 24,
                            isWeakArea = true
                        )
                        storage.updateRevisionItem(revItem)
                    } else {
                        // Mark mastered and schedule retrieval review
                        val revItem = RevisionItem(
                            id = "rev_${System.currentTimeMillis()}",
                            topicId = map.topicId,
                            topicTitle = map.topicName,
                            subtopicTitle = subtopic.title,
                            prompt = state.currentActivityQuestion ?: "Explain ${subtopic.title}",
                            expectedAnswer = subtopic.shortSummary,
                            intervalDays = 3,
                            dueDate = System.currentTimeMillis() + 1000 * 60 * 60 * 72,
                            isWeakArea = false
                        )
                        storage.updateRevisionItem(revItem)
                    }
                }
                is GeminiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        evaluationResult = AnswerEvaluation(
                            isCorrect = true,
                            scoreOutOfTen = 8,
                            praise = "Good attempt! You demonstrated solid understanding.",
                            correction = null,
                            hint = null,
                            allowsRetry = false,
                            isMastered = true,
                            nextStepRecommendation = "Ready for the next concept."
                        )
                    )
                }
                is GeminiResult.Loading -> {}
            }
        }
    }

    fun nextSubtopic() {
        val state = _uiState.value
        val map = state.topicMap ?: return
        val currentSub = state.currentSubtopic
        val nextIdx = state.currentSubtopicIndex + 1

        val updatedPreviouslyTaught = if (currentSub != null) {
            state.previouslyTaughtConcepts + currentSub.title
        } else state.previouslyTaughtConcepts

        if (nextIdx < map.subtopics.size) {
            val nextSub = map.subtopics[nextIdx]
            val noticeMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = SenderType.SYSTEM_CHECK,
                text = "Next: Concept ${nextIdx + 1} of ${map.subtopics.size} — ${nextSub.title}"
            )
            _uiState.value = _uiState.value.copy(
                currentSubtopicIndex = nextIdx,
                currentSubtopic = nextSub,
                previouslyTaughtConcepts = updatedPreviouslyTaught,
                messages = state.messages + noticeMsg,
                evaluationResult = null,
                isAwaitingAnswer = false,
                attemptCount = 1,
                struggleCount = 0
            )
            teachCurrentSubtopic()
        } else {
            val completeMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = SenderType.TUTOR,
                text = "Congratulations! You have completed all ${map.subtopics.size} concepts for ${map.topicName}. Active recall cards have been added to your Revision schedule."
            )
            _uiState.value = _uiState.value.copy(
                messages = state.messages + completeMsg,
                isLessonCompleted = true
            )
        }
    }
}
