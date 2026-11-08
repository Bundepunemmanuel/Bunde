package com.bunde.learning.data.local

import android.content.Context
import com.bunde.learning.data.model.LearningStats
import com.bunde.learning.data.model.RevisionItem
import com.bunde.learning.data.model.SubtopicCategory
import com.bunde.learning.data.model.SubtopicNode
import com.bunde.learning.data.model.Topic
import com.bunde.learning.data.model.TopicMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Lightweight local storage manager handling JSON-based file persistence
 * without overhead of heavy database engines, optimized for 2GB RAM.
 */
class LocalStorageManager(private val context: Context) {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()

    private val _revisionItems = MutableStateFlow<List<RevisionItem>>(emptyList())
    val revisionItems: StateFlow<List<RevisionItem>> = _revisionItems.asStateFlow()

    private val _stats = MutableStateFlow(LearningStats())
    val stats: StateFlow<LearningStats> = _stats.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val topicsFile = File(context.filesDir, "bunde_topics.json")
        if (topicsFile.exists()) {
            try {
                val content = topicsFile.readText()
                val loaded: List<Topic> = json.decodeFromString(content)
                _topics.value = loaded
            } catch (_: Exception) {
                seedInitialData()
            }
        } else {
            seedInitialData()
        }

        val revisionFile = File(context.filesDir, "bunde_revision.json")
        if (revisionFile.exists()) {
            try {
                val content = revisionFile.readText()
                val loaded: List<RevisionItem> = json.decodeFromString(content)
                _revisionItems.value = loaded
            } catch (_: Exception) {
                seedRevisionData()
            }
        } else {
            seedRevisionData()
        }
    }

    private fun seedInitialData() {
        val initialTopics = listOf(
            Topic(
                id = "topic_privatization",
                subject = "Economics",
                name = "Privatization",
                classLevel = "SS2",
                progressPercent = 65,
                totalSubtopics = 7,
                completedSubtopics = 4,
                lastAccessedTimestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                isMastered = false,
                weakAreasCount = 1
            ),
            Topic(
                id = "topic_cell_division",
                subject = "Biology",
                name = "Mitosis and Meiosis",
                classLevel = "SS2",
                progressPercent = 90,
                totalSubtopics = 6,
                completedSubtopics = 5,
                lastAccessedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                isMastered = false,
                weakAreasCount = 0
            ),
            Topic(
                id = "topic_kinematics",
                subject = "Physics",
                name = "Equations of Linear Motion",
                classLevel = "SS2",
                progressPercent = 100,
                totalSubtopics = 5,
                completedSubtopics = 5,
                lastAccessedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 72,
                isMastered = true,
                weakAreasCount = 0
            )
        )
        _topics.value = initialTopics
        saveTopicsToFile(initialTopics)
    }

    private fun seedRevisionData() {
        val initialItems = listOf(
            RevisionItem(
                id = "rev_1",
                topicId = "topic_privatization",
                topicTitle = "Privatization",
                subtopicTitle = "Reasons for Privatization",
                prompt = "What is the primary economic reason governments sell state-owned enterprises?",
                expectedAnswer = "To reduce fiscal deficit and promote operational efficiency.",
                intervalDays = 3,
                dueDate = System.currentTimeMillis() + 1000 * 60 * 60 * 2,
                isWeakArea = true
            ),
            RevisionItem(
                id = "rev_2",
                topicId = "topic_cell_division",
                topicTitle = "Mitosis and Meiosis",
                subtopicTitle = "Stages of Mitosis",
                prompt = "In which stage of mitosis do sister chromatids pull apart toward opposite poles?",
                expectedAnswer = "Anaphase",
                intervalDays = 7,
                dueDate = System.currentTimeMillis() + 1000 * 60 * 60 * 24,
                isWeakArea = false
            )
        )
        _revisionItems.value = initialItems
        saveRevisionToFile(initialItems)
    }

    private fun saveTopicsToFile(list: List<Topic>) {
        try {
            val file = File(context.filesDir, "bunde_topics.json")
            file.writeText(json.encodeToString(list))
        } catch (_: Exception) {}
    }

    private fun saveRevisionToFile(list: List<RevisionItem>) {
        try {
            val file = File(context.filesDir, "bunde_revision.json")
            file.writeText(json.encodeToString(list))
        } catch (_: Exception) {}
    }

    suspend fun saveTopic(topic: Topic) = withContext(Dispatchers.IO) {
        val current = _topics.value.toMutableList()
        val index = current.indexOfFirst { it.id == topic.id }
        if (index != -1) {
            current[index] = topic
        } else {
            current.add(0, topic)
        }
        _topics.value = current
        saveTopicsToFile(current)
    }

    suspend fun saveTopicMap(topicMap: TopicMap) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "topic_map_${topicMap.topicId}.json")
            file.writeText(json.encodeToString(topicMap))
        } catch (_: Exception) {}
    }

    suspend fun loadTopicMap(topicId: String): TopicMap? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "topic_map_${topicId}.json")
            if (file.exists()) {
                return@withContext json.decodeFromString<TopicMap>(file.readText())
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun updateRevisionItem(updated: RevisionItem) = withContext(Dispatchers.IO) {
        val current = _revisionItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        _revisionItems.value = current
        saveRevisionToFile(current)
    }
}
