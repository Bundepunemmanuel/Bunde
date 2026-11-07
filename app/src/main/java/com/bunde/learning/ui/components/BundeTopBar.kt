package com.bunde.learning.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeBorder
import com.bunde.learning.ui.theme.BundeSuccess
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary

@Composable
fun BundeTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BundeBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding Icon & Title
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BundeAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Bunde",
                    color = BundeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Low-Resource / Ready Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .border(1.dp, BundeBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BundeSuccess)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Ready",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BundeTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = BundeTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Crisp 1dp hairline border below top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BundeBorder)
        )
    }
}
