package com.bunde.learning.ui.screens.upload

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.bunde.learning.BundeApplication
import com.bunde.learning.ui.components.BundeCard
import com.bunde.learning.ui.components.BundePrimaryButton
import com.bunde.learning.ui.theme.*
import com.bunde.learning.util.DeviceOptimization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UploadNotesScreen(
    onBackClick: () -> Unit,
    onTopicMapGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BundeApplication
    val prefs = app.preferencesRepository
    val storage = app.localStorage
    val gemini = app.geminiService
    val scope = rememberCoroutineScope()

    val apiKey by prefs.geminiApiKeyFlow.collectAsState(initial = "")
    val deviceProfile = remember { DeviceOptimization.getDeviceProfile(context) }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var userSubject by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = (selectedImageUris + uris).distinct()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BundeBackground)
            .padding(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Back",
                    tint = BundeTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upload School Notes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BundeTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select handwritten or printed note photos. Gemini will extract headings, subtopics, definitions, and verify clarity.",
            fontSize = 13.sp,
            color = BundeTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subject optional hint
        OutlinedTextField(
            value = userSubject,
            onValueChange = { userSubject = it },
            label = { Text("Subject (Optional, e.g. Biology, Economics)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Select Images Button
        BundePrimaryButton(
            text = if (selectedImageUris.isEmpty()) "Select Note Photos" else "Add More Photos (${selectedImageUris.size} selected)",
            onClick = { imagePickerLauncher.launch("image/*") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image Thumbnails Grid
        if (selectedImageUris.isNotEmpty()) {
            Text(
                text = "Selected Images Preview",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BundeTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(selectedImageUris) { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BundeBorder, RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Note preview $index",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                selectedImageUris = selectedImageUris.filterIndexed { i, _ -> i != index }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(BundeAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Remove",
                                tint = BundeSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, BundeBorder, RoundedCornerShape(12.dp), true)
                    .background(BundeSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        tint = BundeTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No images selected yet",
                        fontSize = 14.sp,
                        color = BundeTextSecondary
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "",
                fontSize = 12.sp,
                color = BundeAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analyze Button
        BundePrimaryButton(
            text = if (isLoading) "Analyzing Notes with Gemini..." else "Analyze Notes & Build Topic Map",
            onClick = {
                if (selectedImageUris.isEmpty()) {
                    errorMessage = "Please select at least one note image."
                    return@BundePrimaryButton
                }
                if (apiKey.isBlank()) {
                    errorMessage = "Gemini API Key is missing. Please set it in Profile / Settings."
                    return@BundePrimaryButton
                }

                isLoading = true
                errorMessage = null

                scope.launch {
                    val compressedBytesList = mutableListOf<ByteArray>()
                    withContext(Dispatchers.IO) {
                        for (uri in selectedImageUris) {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    val compressed = DeviceOptimization.compressImageForGemini(
                                        inputStream = inputStream,
                                        maxDimension = deviceProfile.maxImageUploadDim,
                                        quality = deviceProfile.compressionQuality
                                    )
                                    compressedBytesList.add(compressed)
                                    inputStream.close()
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (compressedBytesList.isEmpty()) {
                        isLoading = false
                        errorMessage = "Failed to process selected note images."
                        return@launch
                    }

                    val result = gemini.analyzeUploadedNotes(
                        apiKey = apiKey,
                        imageBytesList = compressedBytesList,
                        userSubject = userSubject.ifBlank { null }
                    )

                    isLoading = false

                    when (result) {
                        is com.bunde.learning.domain.ai.GeminiResult.Success -> {
                            val topicMap = result.data
                            storage.saveTopicMap(topicMap)
                            val newTopic = com.bunde.learning.data.model.Topic(
                                id = topicMap.topicId,
                                subject = topicMap.subject,
                                name = topicMap.topicName,
                                classLevel = topicMap.classLevel,
                                progressPercent = 10,
                                totalSubtopics = topicMap.subtopics.size,
                                completedSubtopics = 0,
                                lastAccessedTimestamp = System.currentTimeMillis(),
                                isMastered = false,
                                weakAreasCount = 0
                            )
                            storage.saveTopic(newTopic)
                            onTopicMapGenerated(topicMap.topicId)
                        }
                        is com.bunde.learning.domain.ai.GeminiResult.Error -> {
                            errorMessage = result.message
                        }
                        is com.bunde.learning.domain.ai.GeminiResult.Loading -> {}
                    }
                }
            },
            enabled = !isLoading && selectedImageUris.isNotEmpty()
        )
    }
}
