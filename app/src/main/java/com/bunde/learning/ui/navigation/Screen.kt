package com.bunde.learning.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MyLearning : Screen("my_learning")
    object Revision : Screen("revision")
    object Profile : Screen("profile")
    object UploadNotes : Screen("upload_notes")
    object EnterTopic : Screen("enter_topic")
    object TopicReview : Screen("topic_review/{topicId}") {
        fun createRoute(topicId: String) = "topic_review/$topicId"
    }
    object InteractiveTutor : Screen("tutor/{topicId}") {
        fun createRoute(topicId: String) = "tutor/$topicId"
    }
    object WeakAreas : Screen("weak_areas")
    object Settings : Screen("settings")
}
