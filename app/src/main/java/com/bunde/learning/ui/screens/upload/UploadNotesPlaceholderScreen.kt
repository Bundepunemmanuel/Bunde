package com.bunde.learning.ui.screens.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
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
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary

@Composable
fun UploadNotesPlaceholderScreen(
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
            imageVector = Icons.Outlined.DocumentScanner,
            contentDescription = null,
            tint = BundeAccent,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Upload Notes",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = BundeTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select multiple photos of handwritten or printed notes from your phone. Stage 2 will activate the image picker and multimodal Gemini analysis.",
            fontSize = 14.sp,
            color = BundeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BundeCard {
            Text(
                text = "Stage 2 Pipeline Ready:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BundeTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Multi-image selection with preview\n• Image compression for 2GB RAM\n• Unreadable text detection\n• Structured topic map extraction",
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
