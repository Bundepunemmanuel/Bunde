package com.bunde.learning.ui.screens.mylearning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.screens.home.HomeViewModel
import com.bunde.learning.ui.screens.home.TopicRowCard
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary

@Composable
fun MyLearningScreen(
    viewModel: HomeViewModel,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "My Learning",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
            Text(
                text = "Curriculum topics, completed lessons, and mastered concepts",
                fontSize = 13.sp,
                color = BundeTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(state.topics) { topic ->
            TopicRowCard(
                topic = topic,
                onClick = { onTopicClick(topic.id) }
            )
        }
    }
}
