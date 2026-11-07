package com.bunde.learning.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.data.model.Topic
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeAccentLight
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeBorder
import com.bunde.learning.ui.theme.BundeHighlight
import com.bunde.learning.ui.theme.BundeHighlightLight
import com.bunde.learning.ui.theme.BundeSuccess
import com.bunde.learning.ui.theme.BundeTextMuted
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary
import com.bunde.learning.ui.theme.BundeWarning
import com.bunde.learning.ui.theme.BundeWarningLight

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onUploadNotesClick: () -> Unit,
    onEnterTopicClick: () -> Unit,
    onContinueLearningClick: (String) -> Unit,
    onMyLearningClick: () -> Unit,
    onRevisionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Greeting Section
            Text(
                text = "Bunde",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BundeAccent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "What are we learning today?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary,
                lineHeight = 30.sp
            )
            Text(
                text = "Class: ${state.classLevel} • Interactive AI Personal Tutor",
                fontSize = 13.sp,
                color = BundeTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Gemini API Key Notice if not yet provided
        if (!state.isApiKeyConfigured) {
            item {
                BundeCard(
                    backgroundColor = BundeWarningLight,
                    borderColor = BundeWarning.copy(alpha = 0.4f),
                    onClick = onSettingsClick
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BundeWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = "Gemini API Key",
                                tint = BundeWarning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini API Key Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeTextPrimary
                            )
                            Text(
                                text = "Tap here to add your free Google AI Studio key securely on this device.",
                                fontSize = 12.sp,
                                color = BundeTextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = BundeWarning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Two Main Actions: Upload Notes & Enter a Topic
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Action 1: Upload Notes
                BundeCard(
                    onClick = onUploadNotesClick,
                    borderColor = BundeBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BundeAccentLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = "Upload Notes",
                                tint = BundeAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upload Notes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Select photos of handwritten or printed school notes for AI analysis.",
                                fontSize = 13.sp,
                                color = BundeTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = BundeTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Action 2: Enter a Topic
                BundeCard(
                    onClick = onEnterTopicClick,
                    borderColor = BundeBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BundeHighlightLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = "Enter a Topic",
                                tint = BundeHighlight,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enter a Topic",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Type your subject, topic & level (e.g. Economics: Privatization).",
                                fontSize = 13.sp,
                                color = BundeTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = BundeTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Section: Continue Learning
        item {
            val continueItem = state.continueTopic
            if (continueItem != null) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue Learning",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BundeTextPrimary
                        )
                        Text(
                            text = "Resume",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BundeAccent,
                            modifier = Modifier.clickable { onContinueLearningClick(continueItem.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    BundeCard(
                        onClick = { onContinueLearningClick(continueItem.id) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BundeAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = continueItem.subject.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeAccent,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${continueItem.progressPercent}% Completed",
                                fontSize = 12.sp,
                                color = BundeTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = continueItem.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BundeTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { continueItem.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BundeAccent,
                            trackColor = BundeBorder,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${continueItem.completedSubtopics} of ${continueItem.totalSubtopics} subtopics covered",
                                fontSize = 12.sp,
                                color = BundeTextSecondary
                            )
                            if (continueItem.weakAreasCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = BundeWarning,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${continueItem.weakAreasCount} weak area flagged",
                                        fontSize = 11.sp,
                                        color = BundeWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Revision Progress
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Revision Progress",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BundeTextPrimary
                    )
                    Text(
                        text = "Review All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BundeAccent,
                        modifier = Modifier.clickable { onRevisionClick() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Due Today Card
                    BundeCard(
                        modifier = Modifier.weight(1f),
                        onClick = onRevisionClick
                    ) {
                        Text(
                            text = "Due for Recall",
                            fontSize = 12.sp,
                            color = BundeTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.stats.dueForRevisionCount} cards",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BundeAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spaced retrieval",
                            fontSize = 11.sp,
                            color = BundeTextMuted
                        )
                    }

                    // Mastered Concepts Card
                    BundeCard(
                        modifier = Modifier.weight(1f),
                        onClick = onMyLearningClick
                    ) {
                        Text(
                            text = "Concepts Mastered",
                            fontSize = 12.sp,
                            color = BundeTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.stats.conceptsMastered}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BundeSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verified understanding",
                            fontSize = 11.sp,
                            color = BundeTextMuted
                        )
                    }
                }
            }
        }

        // Section: My Learning (Active Topics List)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Learning",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BundeTextPrimary
                    )
                    Text(
                        text = "View Library",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BundeAccent,
                        modifier = Modifier.clickable { onMyLearningClick() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                state.topics.forEach { topic ->
                    TopicRowCard(
                        topic = topic,
                        onClick = { onContinueLearningClick(topic.id) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Device & RAM Status (Infinix Smart 8 2GB Optimization)
        item {
            val dev = state.deviceProfile
            if (dev != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, BundeBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = "Device Health",
                        tint = BundeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device RAM: ${dev.totalMemoryMb}MB (${dev.availableMemoryMb}MB free) • Low-RAM mode active",
                        fontSize = 11.sp,
                        color = BundeTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TopicRowCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BundeCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${topic.subject} • ${topic.classLevel}",
                    fontSize = 11.sp,
                    color = BundeTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topic.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${topic.completedSubtopics}/${topic.totalSubtopics} concepts mastered",
                    fontSize = 12.sp,
                    color = BundeTextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (topic.isMastered) BundeAccentLight else BundeHighlightLight)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (topic.isMastered) "Mastered" else "${topic.progressPercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (topic.isMastered) BundeAccent else BundeHighlight
                )
            }
        }
    }
}
