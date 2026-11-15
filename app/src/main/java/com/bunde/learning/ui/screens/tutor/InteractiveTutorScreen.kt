package com.bunde.learning.ui.screens.tutor

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bunde.learning.domain.ai.ConceptVisualAid
import com.bunde.learning.domain.ai.EvaluationVerdict
import com.bunde.learning.domain.ai.SessionRecap
import com.bunde.learning.domain.ai.StructuredActivity
import com.bunde.learning.domain.ai.StructuredAnswerEvaluation
import com.bunde.learning.domain.ai.StructuredConceptStep
import com.bunde.learning.domain.ai.VisualAidType
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundeOutlineButton
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.*
import com.bunde.learning.util.VisualRenderer

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
            if (state.phase == LessonPhase.LOADING_TOPIC || (state.phase == LessonPhase.EXPLANATION && state.currentStep == null) || state.phase == LessonPhase.NEXT_CONCEPT) {
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
                                text = "Preparing Part ${state.currentConceptIndex + 1}: ${state.currentSubtopic?.title ?: ""}...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = BundeTextSecondary
                            )
                        }
                    }
                }
            }

            // Phase: SESSION_RECAP (30-second "Story So Far" break card after every 3 concepts)
            if (state.phase == LessonPhase.SESSION_RECAP) {
                item {
                    SessionRecapCard(
                        recap = state.sessionRecap,
                        completedCount = state.conceptsCompletedThisSession,
                        onContinueLesson = { viewModel.continueFromRecap() },
                        onTakeBreak = onBackClick
                    )
                }
            }

            // Phase: PREDICTION_HOOK (Mini-story hook + quick guess BEFORE explanation)
            val step = state.currentStep
            if (step != null && state.phase == LessonPhase.PREDICTION_HOOK) {
                item {
                    PredictionHookCard(
                        step = step,
                        conceptIndex = state.currentConceptIndex,
                        onGuessSelected = { guess -> viewModel.submitPredictionGuess(guess) },
                        onSkipToExplanation = { viewModel.skipPredictionToExplanation() }
                    )
                }
            }

            // Phase: Active Step (Explanation + Story Anchor + Real-world Example)
            if (step != null &&
                state.phase != LessonPhase.LESSON_COMPLETED &&
                state.phase != LessonPhase.PREDICTION_HOOK &&
                state.phase != LessonPhase.SESSION_RECAP
            ) {
                item {
                    ExplanationSection(
                        step = step,
                        conceptIndex = state.currentConceptIndex,
                        selectedGuess = state.selectedPredictionGuess,
                        isVisualLoading = state.isVisualLoading,
                        visualImageBytes = state.visualImageBytes,
                        visualFallbackSvg = state.visualFallbackSvg
                    )
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
fun PredictionHookCard(
    step: StructuredConceptStep,
    conceptIndex: Int,
    onGuessSelected: (String) -> Unit,
    onSkipToExplanation: () -> Unit
) {
    BundeCard(
        backgroundColor = BundeSurface,
        borderColor = BundeAccent.copy(alpha = 0.45f)
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
                    text = "QUICK GUESS FIRST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeAccent,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = step.learningObjective,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeTextPrimary
                )
            }
        }

        if (!step.storyHook.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BundeHighlightLight)
                    .border(1.dp, BundeHighlight.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = step.storyHook,
                    fontSize = 14.sp,
                    color = BundeTextPrimary,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = step.predictionPrompt ?: "What do you think happens next?",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BundeTextPrimary,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            step.predictionOptions.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BundeBackground)
                        .border(1.dp, BundeBorder, RoundedCornerShape(8.dp))
                        .clickable { onGuessSelected(option) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BundeTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Not sure? See the explanation →",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BundeAccent,
            modifier = Modifier.clickable { onSkipToExplanation() }
        )
    }
}

@Composable
fun SessionRecapCard(
    recap: SessionRecap?,
    completedCount: Int,
    onContinueLesson: () -> Unit,
    onTakeBreak: () -> Unit
) {
    BundeCard(
        backgroundColor = BundeSurface,
        borderColor = BundeHighlight.copy(alpha = 0.4f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = "Story Recap",
                tint = BundeHighlight,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = recap?.recapTitle ?: "Story So Far ($completedCount parts done)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recap == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = BundeAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connecting the story so far...",
                    fontSize = 13.sp,
                    color = BundeTextSecondary
                )
            }
        } else {
            Text(
                text = recap.storySoFar,
                fontSize = 14.sp,
                color = BundeTextPrimary,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BundeSuccessLight)
                    .padding(10.dp)
            ) {
                Text(
                    text = "💡 ${recap.quickTip}",
                    fontSize = 12.sp,
                    color = BundeTextPrimary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BundePrimaryButton(
            text = "Continue to Next Part →",
            onClick = onContinueLesson
        )
        Spacer(modifier = Modifier.height(8.dp))
        BundeOutlineButton(
            text = "Save & Take a Short Break",
            onClick = onTakeBreak
        )
    }
}

@Composable
fun ExplanationSection(
    step: StructuredConceptStep,
    conceptIndex: Int,
    selectedGuess: String? = null,
    isVisualLoading: Boolean = false,
    visualImageBytes: ByteArray? = null,
    visualFallbackSvg: String? = null
) {
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
                    text = step.learningObjective,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeAccent
                )
                Text(
                    text = "Part ${conceptIndex + 1}",
                    fontSize = 11.sp,
                    color = BundeTextSecondary
                )
            }
        }

        // Mini-story hook + student's prediction guess if they made one
        if (!step.storyHook.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BundeBackground)
                    .border(1.dp, BundeBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = step.storyHook,
                        fontSize = 13.sp,
                        color = BundeTextPrimary,
                        lineHeight = 19.sp
                    )
                    if (!selectedGuess.isNullOrBlank()) {
                        Text(
                            text = "Your guess: \"$selectedGuess\"",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BundeHighlight
                        )
                    }
                    if (!step.predictionReveal.isNullOrBlank()) {
                        Text(
                            text = step.predictionReveal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BundeTextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Concise, readable explanation (2-3 short paragraphs)
        Text(
            text = step.conciseExplanation,
            fontSize = 14.sp,
            color = BundeTextPrimary,
            lineHeight = 22.sp
        )

        // Labeled Technical Diagram / Hybrid Visual Aid
        if (step.visualAid.type != VisualAidType.NONE) {
            ConceptVisualCard(
                visualAid = step.visualAid,
                isVisualLoading = isVisualLoading,
                visualImageBytes = visualImageBytes,
                visualFallbackSvg = visualFallbackSvg
            )
        }

        // Small "Meaning of key words" helper when exam/hard words are introduced
        if (step.keyWordMeanings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BundeBackground)
                    .border(1.dp, BundeBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Meaning of Key Words",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BundeAccent
                    )
                    step.keyWordMeanings.forEach { item ->
                        Text(
                            text = "• ${item.word}: ${item.meaning}",
                            fontSize = 13.sp,
                            color = BundeTextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

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
                        text = "Story Example",
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

        // Memory Picture & Storage Key Anchor
        if (!step.memoryPicture.isNullOrBlank() || !step.memoryHook.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BundeAccentLight.copy(alpha = 0.55f))
                    .border(1.dp, BundeAccent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Lock It In Memory",
                            tint = BundeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lock It In Memory (Picture This)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BundeAccent
                        )
                    }
                    if (!step.memoryPicture.isNullOrBlank()) {
                        Text(
                            text = step.memoryPicture,
                            fontSize = 13.sp,
                            color = BundeTextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                    if (!step.memoryHook.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(BundeSurface)
                                .border(1.dp, BundeAccent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Memory Key: ${step.memoryHook}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BundeAccent,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConceptVisualCard(
    visualAid: ConceptVisualAid,
    isVisualLoading: Boolean,
    visualImageBytes: ByteArray?,
    visualFallbackSvg: String?
) {
    val bitmap: ImageBitmap? = remember(
        visualAid.type,
        visualAid.svgMarkup,
        visualImageBytes,
        visualFallbackSvg
    ) {
        when {
            visualAid.type == VisualAidType.SVG_DIAGRAM && !visualAid.svgMarkup.isNullOrBlank() ->
                VisualRenderer.renderSvgToBitmap(visualAid.svgMarkup)?.asImageBitmap()
            visualImageBytes != null ->
                VisualRenderer.decodeDownsampledBitmap(visualImageBytes)?.asImageBitmap()
            !visualFallbackSvg.isNullOrBlank() ->
                VisualRenderer.renderSvgToBitmap(visualFallbackSvg)?.asImageBitmap()
            else -> null
        }
    }

    if (bitmap == null && !isVisualLoading) return

    var isZoomed by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BundeBackground)
            .border(1.dp, BundeBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = visualAid.title.ifBlank { "Labeled Diagram" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeHighlight,
                    modifier = Modifier.weight(1f)
                )
                if (bitmap != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isZoomed = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ZoomIn,
                            contentDescription = "Enlarge Diagram",
                            tint = BundeTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tap to enlarge",
                            fontSize = 11.sp,
                            color = BundeTextSecondary
                        )
                    }
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = visualAid.title.ifBlank { "Concept diagram" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, BundeBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { isZoomed = true }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, BundeBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BundeAccent,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Drawing illustration while you read...",
                        fontSize = 12.sp,
                        color = BundeTextSecondary
                    )
                }
            }

            if (visualAid.caption.isNotBlank()) {
                Text(
                    text = visualAid.caption,
                    fontSize = 12.sp,
                    color = BundeTextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }

    if (isZoomed && bitmap != null) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { isZoomed = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = visualAid.title.ifBlank { "Labeled Diagram" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isZoomed = false }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 3.5f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = visualAid.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }

                    if (visualAid.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = visualAid.caption,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 19.sp
                        )
                    }
                }
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
