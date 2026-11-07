package com.bunde.learning.ui.screens.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.BundeApplication
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeTextPrimary
import com.bunde.learning.ui.theme.BundeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BundeApplication
    val prefs = app.preferencesRepository
    val scope = rememberCoroutineScope()

    val currentKey by prefs.geminiApiKeyFlow.collectAsState(initial = "")
    val classLevel by prefs.studentClassLevelFlow.collectAsState(initial = "SS2")
    val isLowData by prefs.isLowDataModeFlow.collectAsState(initial = true)

    var inputKey by remember(currentKey) { mutableStateOf(currentKey) }
    var keySavedMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Settings & Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
            Text(
                text = "Device configuration, AI key, and learning level",
                fontSize = 13.sp,
                color = BundeTextSecondary
            )
        }

        // Gemini API Key Card
        item {
            BundeCard {
                Text(
                    text = "Google Gemini API Key",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your API key is stored securely in private local device storage and is never exposed.",
                    fontSize = 12.sp,
                    color = BundeTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                BundePrimaryButton(
                    text = "Save Key",
                    onClick = {
                        scope.launch {
                            prefs.saveGeminiApiKey(inputKey)
                            keySavedMessage = "Key securely saved on device!"
                        }
                    }
                )
                if (keySavedMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = keySavedMessage ?: "",
                        fontSize = 12.sp,
                        color = BundeAccent
                    )
                }
            }
        }

        // Device Optimization Card
        item {
            BundeCard {
                Text(
                    text = "Low-RAM Device Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optimized for Infinix Smart 8 (2GB RAM). Downsamples images and disables memory-heavy animations.",
                    fontSize = 12.sp,
                    color = BundeTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Memory Saver Active",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BundeTextPrimary
                    )
                    Switch(
                        checked = isLowData,
                        onCheckedChange = { scope.launch { prefs.setLowDataMode(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = BundeAccent)
                    )
                }
            }
        }

        // Architecture Note
        item {
            BundeCard {
                Text(
                    text = "About Bunde",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BundeTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 1.0.0 (Stage 1 Native Android Build)\nEngine: Jetpack Compose + Kotlin + Android DataStore\nTarget SDK: Android 14 (API 34) • Min SDK: Android 8.0 (API 26)",
                    fontSize = 12.sp,
                    color = BundeTextSecondary,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
