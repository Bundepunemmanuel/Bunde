package com.bunde.learning.ui.screens.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeHighlight
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary

@Composable
fun EnterTopicPlaceholderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = null,
            tint = BundeHighlight,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enter a Topic",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = BundeTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your Subject, Topic, and Class level (e.g. Economics • Privatization • SS2). Stage 2 generates the complete curriculum map via Gemini.",
            fontSize = 14.sp,
            color = BundeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BundeCard {
            Text(
                text = "Coverage Categories Supported:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BundeTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Definitions • Features • Types • Objectives • Reasons • Causes & Effects • Advantages & Disadvantages • Challenges • Processes • Formulas",
                fontSize = 12.sp,
                color = BundeTextSecondary,
                lineHeight = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        BundePrimaryButton(
            text = "Back to Home",
            onClick = onBackClick
        )
    }
}
