package com.bunde.learning.ui.screens.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.BundeApplication
import com.bunde.learning.data.model.TopicMap
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TopicReviewScreen(
    topicId: String,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BundeApplication
    val storage = app.localStorage
    val scope = rememberCoroutineScope()

    var topicMap by remember { mutableStateOf<TopicMap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(topicId) {
        val loaded = storage.loadTopicMap(topicId)
        topicMap = loaded
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToHome) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = BundeTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Topic Map Review",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BundeAccent)
            }
        } else {
            val map = topicMap
            if (map == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Topic map not found.", color = BundeTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BundeCard {
                            Text(
                                text = "${map.subject} • ${map.classLevel}".uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BundeAccent,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = map.topicName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = BundeTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = map.summary,
                                fontSize = 14.sp,
                                color = BundeTextSecondary,
                                lineHeight = 20.sp
                            )

                            if (map.unreadableNoteNotice != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BundeWarningLight)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = BundeWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = map.unreadableNoteNotice ?: "",
                                        fontSize = 12.sp,
                                        color = BundeTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Curriculum Components (${map.subtopics.size} subtopics)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BundeTextPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    items(map.subtopics) { sub ->
                        BundeCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sub.category.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BundeHighlight
                                )
                                if (sub.isFromUploadedNotes) {
                                    Text(
                                        text = "From Notes",
                                        fontSize = 10.sp,
                                        color = BundeAccent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sub.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BundeTextPrimary
                            )
                            if (sub.shortSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sub.shortSummary,
                                    fontSize = 13.sp,
                                    color = BundeTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BundePrimaryButton(
                    text = "Start Interactive Lesson",
                    onClick = onBackToHome
                )
            }
        }
    }
}
