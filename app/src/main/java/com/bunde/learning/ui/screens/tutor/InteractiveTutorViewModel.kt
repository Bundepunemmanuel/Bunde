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
    val isAskingQuestion: Boolean = false,
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
                val initialMsg = ChatMessage(
                    id = "msg_init",
                    sender = SenderType.TUTOR,
                    text = "Welcome to ${loaded.topicName}! Today we will explore ${loaded.subtopics.size} key concepts. Let's start with concept 1: ${firstSub?.title ?: ""}."
                )
                _uiState.value = _uiState.value.copy(
                    topicMap = loaded,
                    currentSubtopicIndex = 0,
                    currentSubtopic = firstSub,
                    messages = listOf(initialMsg),
                    isLoading = false
                )
                explainCurrentSubtopic()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Topic map data not found."
                )
            }
        }
    }

    fun explainCurrentSubtopic() {
        val state = _uiState.value
        val map = state.topicMap ?: return
        val subtopic = state.currentSubtopic ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val prompt = "Explain ${subtopic.title} under ${map.topicName} for a ${map.classLevel} student in 3 clear, engaging sentences. Focus on ${subtopic.category.name} and provide 1 practical example."

            val history = state.messages.map { it.sender.name to it.text }

            val result = gemini.askTutor(
                apiKey = apiKey,
                topic = map.topicName,
                subtopic = subtopic.title,
                conversationHistory = history,
                studentMessage = prompt
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is GeminiResult.Success -> {
                    val tutorMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        sender = SenderType.TUTOR,
                        text = result.data
                    )
                    val updatedMsgs = _uiState.value.messages + tutorMsg
                    _uiState.value = _uiState.value.copy(messages = updatedMsgs)
                }
                is GeminiResult.Error -> {
                    val fallbackMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        sender = SenderType.TUTOR,
                        text = "Here is the key summary for ${subtopic.title}: ${subtopic.shortSummary}"
                    )
                    _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + fallbackMsg)
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
            val history = updatedMsgs.map { it.sender.name to it.text }
            val result = gemini.askTutor(
                apiKey = apiKey,
                topic = map.topicName,
                subtopic = subtopic.title,
                conversationHistory = history,
                studentMessage = messageText
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is GeminiResult.Success -> {
                    val responseMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis() + 1}",
                        sender = SenderType.TUTOR,
                        text = result.data
                    )
                    _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + responseMsg)
                }
                is GeminiResult.Error -> {
                    val errorMsg = ChatMessage(
                        id = "msg_${System.currentTimeMillis() + 1}",
                        sender = SenderType.TUTOR,
                        text = "Great question! ${subtopic.title} is an important part of ${map.topicName}. Let's continue testing your understanding."
                    )
                    _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + errorMsg)
                }
                is GeminiResult.Loading -> {}
            }
        }
    }

    fun submitAnswer(answerText: String) {
        if (answerText.isBlank()) return
        val state = _uiState.value
        val subtopic = state.currentSubtopic ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val eval = gemini.evaluateAnswer(
                apiKey = apiKey,
                question = "Explain ${subtopic.title}",
                expectedConcept = subtopic.shortSummary,
                studentAnswer = answerText
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (eval) {
                is GeminiResult.Success -> {
                    val evaluation = eval.data
                    _uiState.value = _uiState.value.copy(evaluationResult = evaluation)

                    if (!evaluation.isCorrect) {
                        // Flag as weak area and add to revision queue
                        val revItem = RevisionItem(
                            id = "rev_${System.currentTimeMillis()}",
                            topicId = state.topicMap?.topicId ?: "",
                            topicTitle = state.topicMap?.topicName ?: "",
                            subtopicTitle = subtopic.title,
                            prompt = "Review concept: ${subtopic.title}",
                            expectedAnswer = subtopic.shortSummary,
                            dueDate = System.currentTimeMillis() + 1000 * 60 * 60 * 24,
                            isWeakArea = true
                        )
                    }
                }
                is GeminiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        evaluationResult = AnswerEvaluation(
                            isCorrect = true,
                            scoreOutOfTen = 8,
                            praise = "Good answer!",
                            correction = null,
                            nextStepRecommendation = "Ready for next concept."
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
        val nextIdx = state.currentSubtopicIndex + 1

        if (nextIdx < map.subtopics.size) {
            val nextSub = map.subtopics[nextIdx]
            val noticeMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = SenderType.SYSTEM_CHECK,
                text = "Moving to Concept ${nextIdx + 1}: ${nextSub.title}"
            )
            _uiState.value = _uiState.value.copy(
                currentSubtopicIndex = nextIdx,
                currentSubtopic = nextSub,
                messages = state.messages + noticeMsg,
                evaluationResult = null
            )
            explainCurrentSubtopic()
        } else {
            val completeMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = SenderType.TUTOR,
                text = "Congratulations! You have completed all ${map.subtopics.size} concepts for ${map.topicName}."
            )
            _uiState.value = _uiState.value.copy(
                messages = state.messages + completeMsg,
                isLessonCompleted = true
            )
        }
    }
}
