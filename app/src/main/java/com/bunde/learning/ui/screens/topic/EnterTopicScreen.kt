package com.bunde.learning.ui.screens.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.BundeApplication
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EnterTopicScreen(
    onBackClick: () -> Unit,
    onTopicMapGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BundeApplication
    val prefs = app.preferencesRepository
    val storage = app.localStorage
    val gemini = app.geminiService
    val scope = rememberCoroutineScope()

    val apiKey by prefs.geminiApiKeyFlow.collectAsState(initial = "")
    val defaultClassLevel by prefs.studentClassLevelFlow.collectAsState(initial = "SS2")

    var subject by remember { mutableStateOf("") }
    var topicName by remember { mutableStateOf("") }
    var classLevel by remember { mutableStateOf(defaultClassLevel) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Back",
                    tint = BundeTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enter a Topic",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter any school subject and topic. Gemini will generate a structured curriculum map.",
            fontSize = 13.sp,
            color = BundeTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject (e.g. Economics, Biology, Physics)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = topicName,
            onValueChange = { topicName = it },
            label = { Text("Topic (e.g. Privatization, Mitosis, Kinematics)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = classLevel,
            onValueChange = { classLevel = it },
            label = { Text("Class / Educational Level (e.g. SS2, Grade 11)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                fontSize = 12.sp,
                color = BundeAccent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BundePrimaryButton(
            text = if (isLoading) "Generating Topic Map..." else "Generate Structured Topic Map",
            onClick = {
                if (subject.isBlank() || topicName.isBlank()) {
                    errorMessage = "Please enter both subject and topic name."
                    return@BundePrimaryButton
                }
                if (apiKey.isBlank()) {
                    errorMessage = "Gemini API Key is missing. Set it in Profile / Settings."
                    return@BundePrimaryButton
                }

                isLoading = true
                errorMessage = null

                scope.launch {
                    val result = gemini.generateTopicMap(
                        apiKey = apiKey,
                        subject = subject.trim(),
                        topic = topicName.trim(),
                        classLevel = classLevel.trim()
                    )

                    isLoading = false

                    when (result) {
                        is com.bunde.learning.domain.ai.GeminiResult.Success -> {
                            val topicMap = result.data
                            storage.saveTopicMap(topicMap)
                            val newTopic = com.bunde.learning.data.model.Topic(
                                id = topicMap.topicId,
                                subject = topicMap.subject,
                                name = topicMap.topicName,
                                classLevel = topicMap.classLevel,
                                progressPercent = 0,
                                totalSubtopics = topicMap.subtopics.size,
                                completedSubtopics = 0,
                                lastAccessedTimestamp = System.currentTimeMillis(),
                                isMastered = false,
                                weakAreasCount = 0
                            )
                            storage.saveTopic(newTopic)
                            onTopicMapGenerated(topicMap.topicId)
                        }
                        is com.bunde.learning.domain.ai.GeminiResult.Error -> {
                            errorMessage = result.message
                        }
                        is com.bunde.learning.domain.ai.GeminiResult.Loading -> {}
                    }
                }
            },
            enabled = !isLoading
        )
    }
}
