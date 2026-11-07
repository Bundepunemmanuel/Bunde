package com.bunde.learning.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val code: String,
    val activeTopicsCount: Int = 0
)

@Serializable
data class Topic(
    val id: String,
    val subject: String,
    val name: String,
    val classLevel: String, // e.g. "SS2", "SS3", "Grade 11"
    val progressPercent: Int = 0,
    val totalSubtopics: Int = 0,
    val completedSubtopics: Int = 0,
    val lastAccessedTimestamp: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false,
    val weakAreasCount: Int = 0
)

@Serializable
enum class SubtopicCategory {
    DEFINITION,
    FEATURE,
    CHARACTERISTIC,
    TYPE,
    OBJECTIVE,
    REASON,
    CAUSE,
    EFFECT,
    FUNCTION,
    IMPORTANCE,
    ADVANTAGE,
    DISADVANTAGE,
    CHALLENGE,
    PROCESS,
    STAGE,
    EXAMPLE,
    APPLICATION,
    FORMULA,
    COMPARISON,
    GENERAL
}

@Serializable
data class SubtopicNode(
    val id: String,
    val title: String,
    val category: SubtopicCategory = SubtopicCategory.DEFINITION,
    val shortSummary: String = "",
    val isFromUploadedNotes: Boolean = false,
    val isMastered: Boolean = false,
    val isWeakArea: Boolean = false,
    val orderIndex: Int = 0
)

@Serializable
data class TopicMap(
    val topicId: String,
    val topicName: String,
    val subject: String,
    val classLevel: String,
    val summary: String,
    val subtopics: List<SubtopicNode> = emptyList(),
    val unreadableNoteNotice: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class RevisionItem(
    val id: String,
    val topicId: String,
    val topicTitle: String,
    val subtopicTitle: String,
    val prompt: String,
    val expectedAnswer: String,
    val intervalDays: Int = 1,
    val repetitionLevel: Int = 0,
    val dueDate: Long = System.currentTimeMillis(),
    val isWeakArea: Boolean = false
)

@Serializable
data class LearningStats(
    val streakDays: Int = 4,
    val topicsCompleted: Int = 8,
    val conceptsMastered: Int = 34,
    val dueForRevisionCount: Int = 6,
    val weakAreasCount: Int = 2
)

data class DevicePerformanceProfile(
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val isLowRamDevice: Boolean,
    val maxImageUploadDim: Int = 1024,
    val compressionQuality: Int = 75
)
