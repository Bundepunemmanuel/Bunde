package com.bunde.learning.ui.screens.revision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.screens.home.HomeViewModel
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary
import com.bunde.learning.ui.theme.BundeWarning

@Composable
fun RevisionScreen(
    viewModel: HomeViewModel,
    onStartRecallSession: () -> Unit,
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
                text = "Revision & Spaced Retrieval",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
            Text(
                text = "Review schedule: Day 1 • Day 3 • Day 7 • Day 14 intervals",
                fontSize = 13.sp,
                color = BundeTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            BundePrimaryButton(
                text = "Start Quick Recall (${state.revisionItems.size} cards ready)",
                onClick = onStartRecallSession
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(state.revisionItems) { item ->
            BundeCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.topicTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BundeAccent
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
                    fontWeight = FontWeight.Medium,
                    color = BundeTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Subtopic: ${item.subtopicTitle}",
                    fontSize = 12.sp,
                    color = BundeTextSecondary
                )
            }
        }
    }
}
