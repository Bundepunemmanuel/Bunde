package com.bunde.learning.ui.screens.tutor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.domain.ai.EvaluationVerdict
import com.bunde.learning.domain.ai.StructuredActivity
import com.bunde.learning.domain.ai.StructuredAnswerEvaluation
import com.bunde.learning.domain.ai.StructuredConceptStep
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundeOutlineButton
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.*

@Composable
fun InteractiveTutorScreen(
    topicId: String,
    viewModel: InteractiveTutorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    var studentAnswerText by remember { mutableStateOf("") }
    var tutorQueryText by remember { mutableStateOf("") }
    var isAskBundeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(topicId) {
        viewModel.loadTopicMap(topicId)
    }

    // Auto-scroll when new phase or evaluation arrives
    LaunchedEffect(state.phase, state.evaluationResult) {
        scrollState.animateScrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .statusBarsPadding()
    ) {
        // TOP APP BAR: Concept navigation & header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                    color = BundeTextPrimary,
                    maxLines = 1
                )
                val totalConcepts = state.topicMap?.subtopics?.size ?: 1
                val currentNum = (state.currentConceptIndex + 1).coerceAtMost(totalConcepts)
                val currentTitle = state.currentSubtopic?.title ?: ""
                Text(
                    text = "Concept $currentNum of $totalConcepts: $currentTitle",
                    fontSize = 12.sp,
                    color = BundeTextSecondary,
                    maxLines = 1
                )
            }
            // Ask Bunde toggle button
            IconButton(
                onClick = { isAskBundeExpanded = !isAskBundeExpanded },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isAskBundeExpanded) BundeAccentLight else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = "Ask Bunde Question",
                    tint = if (isAskBundeExpanded) BundeAccent else BundeTextSecondary
                )
            }
        }

        // ACCURATE PROGRESS BAR: Bound to currentConceptIndex / totalConcepts
        val totalSubtopics = (state.topicMap?.subtopics?.size ?: 1).coerceAtLeast(1)
        val progressFraction = when (state.phase) {
            LessonPhase.LESSON_COMPLETED -> 1.0f
            LessonPhase.CONCEPT_COMPLETED -> (state.currentConceptIndex + 1).toFloat() / totalSubtopics.toFloat()
            else -> state.currentConceptIndex.toFloat() / totalSubtopics.toFloat()
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = BundeAccent,
            trackColor = BundeBorder,
            strokeCap = StrokeCap.Round
        )

        // ASK BUNDE INLINE INTERRUPTION PANEL (Accessible anytime without advancing lesson)
        AnimatedVisibility(visible = isAskBundeExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BundeAccentLight.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Ask Bunde a Question",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeAccent
                )
                Text(
                    text = "Ask for a simpler explanation, another example, or clarification without losing your place in the lesson.",
                    fontSize = 11.sp,
                    color = BundeTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tutorQueryText,
                        onValueChange = { tutorQueryText = it },
                        placeholder = { Text("What would you like to clarify?", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (tutorQueryText.isNotBlank()) {
                                viewModel.askTutorQuestion(tutorQueryText)
                                tutorQueryText = ""
                            }
                        },
                        enabled = !state.isInterruptionLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BundeAccent)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send Question",
                            tint = BundeSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // MAIN LEARNING WORKSPACE
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interruption QA bubbles (if any student questions asked)
            if (state.interruptions.isNotEmpty()) {
                items(state.interruptions) { item ->
                    InterruptionCard(item)
                }
            }

            // Phase: LOADING_TOPIC or EXPLANATION (Skeleton / Loader)
            if (state.phase == LessonPhase.LOADING_TOPIC || (state.phase == LessonPhase.EXPLANATION && state.currentStep == null)) {
                item {
                    BundeCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BundeAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Preparing Concept ${state.currentConceptIndex + 1}: ${state.currentSubtopic?.title ?: ""}...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = BundeTextSecondary
                            )
                        }
                    }
                }
            }

            // Phase: Active Step (Explanation + Real-world Example)
            val step = state.currentStep
            if (step != null && state.phase != LessonPhase.LESSON_COMPLETED) {
                item {
                    ExplanationSection(step = step, conceptIndex = state.currentConceptIndex)
                }

                // Phase: Activity Challenge Card
                item {
                    ActivityChallengeCard(
                        activity = step.activity,
                        state = state,
                        studentAnswerText = studentAnswerText,
                        onAnswerChange = { studentAnswerText = it },
                        onSubmitAnswer = {
                            viewModel.submitAnswer(studentAnswerText)
                            studentAnswerText = ""
                        }
                    )
                }

                // Phase: Evaluation Feedback & Continue Progression Card
                if (state.evaluationResult != null) {
                    item {
                        EvaluationFeedbackCard(
                            eval = state.evaluationResult!!,
                            phase = state.phase,
                            onContinue = { viewModel.advanceToNextConcept() },
                            onRetry = { viewModel.retryCurrentActivity() }
                        )
                    }
                }
            }

            // Phase: LESSON COMPLETED!
            if (state.phase == LessonPhase.LESSON_COMPLETED) {
                item {
                    LessonCompletedCard(
                        topicName = state.topicMap?.topicName ?: "Lesson",
                        totalConcepts = state.topicMap?.subtopics?.size ?: 1,
                        onDoneClick = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
fun ExplanationSection(step: StructuredConceptStep, conceptIndex: Int) {
    BundeCard(
        backgroundColor = BundeSurface,
        borderColor = BundeBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BundeAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${conceptIndex + 1}",
                    color = BundeSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = step.learningObjective.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeAccent,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Core Concept",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Concise, readable explanation (2-3 paragraphs max)
        Text(
            text = step.conciseExplanation,
            fontSize = 14.sp,
            color = BundeTextPrimary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Concrete Example or Demonstration Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BundeHighlightLight)
                .border(1.dp, BundeHighlight.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = "Example",
                        tint = BundeHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Practical Example / Demonstration",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BundeHighlight
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.concreteExample,
                    fontSize = 13.sp,
                    color = BundeTextPrimary,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
fun ActivityChallengeCard(
    activity: StructuredActivity,
    state: TutorUiState,
    studentAnswerText: String,
    onAnswerChange: (String) -> Unit,
    onSubmitAnswer: () -> Unit
) {
    BundeCard(
        backgroundColor = BundeSurface,
        borderColor = BundeAccent.copy(alpha = 0.4f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Quiz,
                contentDescription = "Challenge",
                tint = BundeAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Targeted Challenge (${activity.type.replace('_', ' ')})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BundeAccent,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = activity.question,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BundeTextPrimary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Multiple choice options if provided
        if (activity.options.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activity.options.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (studentAnswerText == opt) BundeAccentLight else BundeBackground)
                            .border(
                                1.dp,
                                if (studentAnswerText == opt) BundeAccent else BundeBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onAnswerChange(opt) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = opt,
                            fontSize = 13.sp,
                            fontWeight = if (studentAnswerText == opt) FontWeight.Bold else FontWeight.Normal,
                            color = BundeTextPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // Open-ended input box
            OutlinedTextField(
                value = studentAnswerText,
                onValueChange = onAnswerChange,
                placeholder = { Text("Type your answer or reasoning in your own words...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape = RoundedCornerShape(8.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Submit Button (Disabled while evaluating or when already completed)
        val isEvaluating = state.phase == LessonPhase.EVALUATING_ANSWER
        val isAnswerEmpty = studentAnswerText.isBlank()
        val isAlreadyCompleted = state.phase == LessonPhase.CONCEPT_COMPLETED

        if (!isAlreadyCompleted) {
            BundePrimaryButton(
                text = if (isEvaluating) "Evaluating Understanding..." else "Submit Answer",
                onClick = onSubmitAnswer,
                enabled = !isEvaluating && !isAnswerEmpty
            )
        }
    }
}

@Composable
fun EvaluationFeedbackCard(
    eval: StructuredAnswerEvaluation,
    phase: LessonPhase,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    val isCorrect = eval.verdict == EvaluationVerdict.CORRECT || eval.conceptCompleted
    val bg = if (isCorrect) BundeSuccessLight else BundeWarningLight
    val border = if (isCorrect) BundeSuccess else BundeWarning

    BundeCard(
        backgroundColor = bg,
        borderColor = border
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isCorrect) BundeSuccess else BundeWarning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${eval.verdict.name.replace('_', ' ')} • Score: ${eval.scoreOutOfTen}/10",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Specific praise
        Text(
            text = eval.specificPraise,
            fontSize = 13.sp,
            color = BundeTextPrimary,
            lineHeight = 18.sp
        )

        // Misconception / mistake analysis
        if (!eval.explanationOfMistake.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = eval.explanationOfMistake,
                fontSize = 12.sp,
                color = BundeTextSecondary,
                lineHeight = 17.sp
            )
        }

        // Targeted hint for retry
        if (!eval.targetedHint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💡 Hint: ${eval.targetedHint}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BundeHighlight
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Explicit progression control: Continue or Retry
        if (eval.conceptCompleted || phase == LessonPhase.CONCEPT_COMPLETED) {
            BundePrimaryButton(
                text = "Continue to Next Concept →",
                onClick = onContinue
            )
        } else if (eval.allowsRetry) {
            BundeOutlineButton(
                text = "Try Again With Hint",
                onClick = onRetry
            )
        }
    }
}

@Composable
fun InterruptionCard(item: TutorInterruptionItem) {
    BundeCard(
        backgroundColor = BundeAccentLight.copy(alpha = 0.5f),
        borderColor = BundeAccent.copy(alpha = 0.3f)
    ) {
        Text(
            text = "Your Question: \"${item.studentQuery}\"",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BundeAccent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.tutorExplanation,
            fontSize = 13.sp,
            color = BundeTextPrimary,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.bridgeBack,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = BundeTextSecondary
        )
    }
}

@Composable
fun LessonCompletedCard(
    topicName: String,
    totalConcepts: Int,
    onDoneClick: () -> Unit
) {
    BundeCard(
        backgroundColor = BundeSuccessLight,
        borderColor = BundeSuccess
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BundeSuccess),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = "Completed",
                    tint = BundeSurface,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Lesson Completed!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "You mastered all $totalConcepts concepts in $topicName. Active recall retrieval cards have been saved to your Revision schedule.",
                fontSize = 13.sp,
                color = BundeTextSecondary,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            BundePrimaryButton(
                text = "Return to Dashboard",
                onClick = onDoneClick
            )
        }
    }
}
