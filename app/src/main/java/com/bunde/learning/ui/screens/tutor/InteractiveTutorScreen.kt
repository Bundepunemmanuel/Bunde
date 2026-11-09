package com.bunde.learning.ui.screens.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun InteractiveTutorScreen(
    topicId: String,
    viewModel: InteractiveTutorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }

    LaunchedEffect(topicId) {
        viewModel.loadTopicMap(topicId)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .statusBarsPadding()
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = BundeTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.topicMap?.topicName ?: "Interactive AI Lesson",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeTextPrimary
                )
                Text(
                    text = "Concept ${(state.currentSubtopicIndex + 1)} of ${(state.topicMap?.subtopics?.size ?: 1)}: ${state.currentSubtopic?.title ?: ""}",
                    fontSize = 12.sp,
                    color = BundeTextSecondary
                )
            }
        }

        // Lesson Progress Bar
        val progress = if (state.topicMap != null && state.topicMap!!.subtopics.isNotEmpty()) {
            (state.currentSubtopicIndex + 1).toFloat() / state.topicMap!!.subtopics.size.toFloat()
        } else 0f

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = BundeAccent,
            trackColor = BundeBorder,
            strokeCap = StrokeCap.Round
        )

        // Chat Message History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.messages) { msg ->
                when (msg.sender) {
                    SenderType.TUTOR -> TutorChatBubble(msg.text)
                    SenderType.STUDENT -> StudentChatBubble(msg.text)
                    SenderType.SYSTEM_CHECK -> SystemNoticeBubble(msg.text)
                }
            }

            if (state.evaluationResult != null) {
                item {
                    val eval = state.evaluationResult!!
                    BundeCard(
                        backgroundColor = if (eval.isCorrect) BundeSuccessLight else BundeWarningLight,
                        borderColor = if (eval.isCorrect) BundeSuccess else BundeWarning
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (eval.isCorrect) BundeSuccess else BundeWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Score: ${eval.scoreOutOfTen}/10 • ${eval.praise}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BundeTextPrimary
                            )
                        }
                        if (eval.correction != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = eval.correction!!,
                                fontSize = 13.sp,
                                color = BundeTextSecondary
                            )
                        }
                        if (eval.hint != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 Hint: ${eval.hint!!}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeHighlight
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (eval.isCorrect || !eval.allowsRetry) {
                            BundePrimaryButton(
                                text = if (state.isLessonCompleted) "Finish Lesson" else "Next Concept",
                                onClick = {
                                    if (state.isLessonCompleted) {
                                        onBackClick()
                                    } else {
                                        viewModel.nextSubtopic()
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "Try typing another answer below with the hint, or ask a question anytime!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BundeTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Student Question / Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BundeSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask Bunde anything about this lesson...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.sendStudentMessage(inputQuery)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BundeAccent)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Send",
                    tint = BundeSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TutorChatBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BundeAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "B", color = BundeSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
            color = BundeSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BundeBorder),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = BundeTextPrimary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun StudentChatBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
            color = BundeAccent,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = BundeSurface,
                lineHeight = 20.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun SystemNoticeBubble(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BundeHighlightLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, BundeHighlight.copy(alpha = 0.3f))
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BundeHighlight,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
