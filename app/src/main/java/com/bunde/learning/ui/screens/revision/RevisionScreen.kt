package com.bunde.learning.ui.screens.revision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.data.model.RevisionItem
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundeOutlineButton
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.screens.home.HomeViewModel
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeAccentLight
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeBorder
import com.bunde.learning.ui.theme.BundeSuccess
import com.bunde.learning.ui.theme.BundeSuccessLight
import com.bunde.learning.ui.theme.BundeTextMuted
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary
import com.bunde.learning.ui.theme.BundeWarning

@Composable
fun RevisionScreen(
    viewModel: HomeViewModel,
    onStartRecallSession: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var isPracticeActive by remember { mutableStateOf(false) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isAnswerRevealed by remember { mutableStateOf(false) }

    val revisionList = state.revisionItems

    if (isPracticeActive && revisionList.isNotEmpty()) {
        if (currentCardIndex < revisionList.size) {
            val currentCard = revisionList[currentCardIndex]

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BundeBackground)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Active Session Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Active Recall Practice",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BundeAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Card ${currentCardIndex + 1} of ${revisionList.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BundeTextPrimary
                        )
                    }
                    Text(
                        text = "Exit",
                        fontSize = 13.sp,
                        color = BundeTextSecondary,
                        modifier = Modifier.clickable { isPracticeActive = false }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Linear Progress
                LinearProgressIndicator(
                    progress = { (currentCardIndex + 1).toFloat() / revisionList.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BundeAccent,
                    trackColor = BundeBorder,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Flashcard Box
                BundeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentCard.topicTitle.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BundeAccent,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentCard.isWeakArea) {
                            Text(
                                text = "Weak Area Focus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Subtopic: ${currentCard.subtopicTitle}",
                        fontSize = 12.sp,
                        color = BundeTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Question Prompt
                    Text(
                        text = currentCard.prompt,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BundeTextPrimary,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reveal Answer Toggle
                    if (!isAnswerRevealed) {
                        BundeOutlineButton(
                            text = "Reveal Answer",
                            onClick = { isAnswerRevealed = true }
                        )
                    } else {
                        // Answer View
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BundeSuccessLight)
                                .border(1.dp, BundeSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Expected Answer:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BundeSuccess
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentCard.expectedAnswer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BundeTextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "How well did you recall this?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BundeTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Spaced Retrieval Rating Action Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingButton(
                                label = "Hard (1d)",
                                color = BundeWarning,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.processRecallResult(currentCard, 1)
                                    isAnswerRevealed = false
                                    currentCardIndex++
                                }
                            )
                            RatingButton(
                                label = "Ok (3d)",
                                color = BundeTextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.processRecallResult(currentCard, 2)
                                    isAnswerRevealed = false
                                    currentCardIndex++
                                }
                            )
                            RatingButton(
                                label = "Good (7d)",
                                color = BundeAccent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.processRecallResult(currentCard, 3)
                                    isAnswerRevealed = false
                                    currentCardIndex++
                                }
                            )
                            RatingButton(
                                label = "Easy (14d)",
                                color = BundeSuccess,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.processRecallResult(currentCard, 4)
                                    isAnswerRevealed = false
                                    currentCardIndex++
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Practice Finished View
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BundeBackground)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BundeSuccessLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Completed",
                        tint = BundeSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recall Session Completed!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You reviewed all ${revisionList.size} cards. Spaced repetition schedules have been updated.",
                    fontSize = 13.sp,
                    color = BundeTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                BundePrimaryButton(
                    text = "Back to Schedule",
                    onClick = {
                        isPracticeActive = false
                        currentCardIndex = 0
                        isAnswerRevealed = false
                    }
                )
            }
        }
    } else {
        // Main Revision Schedule Overview
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(BundeBackground)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Revision & Spaced Retrieval",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BundeTextPrimary
                )
                Text(
                    text = "Review schedule: Day 1 • Day 3 • Day 7 • Day 14 intervals",
                    fontSize = 13.sp,
                    color = BundeTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (revisionList.isNotEmpty()) {
                    BundePrimaryButton(
                        text = "Start Quick Recall (${revisionList.size} cards ready)",
                        onClick = {
                            currentCardIndex = 0
                            isAnswerRevealed = false
                            isPracticeActive = true
                        }
                    )
                } else {
                    BundeCard {
                        Text(
                            text = "No cards due for revision today!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BundeTextPrimary
                        )
                        Text(
                            text = "Complete interactive tutor lessons to automatically generate revision cards.",
                            fontSize = 12.sp,
                            color = BundeTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(revisionList) { item ->
                RevisionCardRow(item)
            }
        }
    }
}

@Composable
fun RevisionCardRow(item: RevisionItem) {
    BundeCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.topicTitle.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BundeAccent,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            if (item.isWeakArea) {
                Text(
                    text = "Weak Area",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeWarning
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.prompt,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BundeTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Subtopic: ${item.subtopicTitle}",
                fontSize = 12.sp,
                color = BundeTextSecondary
            )
            Text(
                text = "Interval: ${item.intervalDays}d",
                fontSize = 11.sp,
                color = BundeTextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RatingButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
