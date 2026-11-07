package com.bunde.learning.domain.ai

import android.util.Base64
import com.bunde.learning.data.model.SubtopicCategory
import com.bunde.learning.data.model.SubtopicNode
import com.bunde.learning.data.model.TopicMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiServiceImpl : GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(apiKey: String): GeminiResult<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API Key is missing. Please enter your Gemini API key in Settings.", isKeyMissing = true)
        }
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val requestBody = """
                {
                    "contents": [{
                        "parts": [{"text": "Reply with 'OK'"}]
                    }]
                }
            """.trimIndent().toRequestBody(jsonMediaType)

            val request = Request.Builder().url(url).post(requestBody).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    GeminiResult.Success(true)
                } else {
                    GeminiResult.Error("Gemini error (${response.code}): ${response.message}")
                }
            }
        } catch (e: IOException) {
            GeminiResult.Error("Network failure: check your internet connection.", isNetworkIssue = true)
        } catch (e: Exception) {
            GeminiResult.Error("Connection test failed: ${e.localizedMessage}")
        }
    }

    override suspend fun generateTopicMap(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String
    ): GeminiResult<TopicMap> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("Please set your Gemini API Key in Settings.", isKeyMissing = true)
        }

        val prompt = """
            You are Bunde, an expert educational AI tutor.
            Generate a comprehensive structured topic map for:
            Subject: $subject
            Topic: $topic
            Class / Educational Level: $classLevel

            Identify all relevant parts of the topic without forcing irrelevant categories.
            Categories must be one of: DEFINITION, FEATURE, CHARACTERISTIC, TYPE, OBJECTIVE, REASON, CAUSE, EFFECT, FUNCTION, IMPORTANCE, ADVANTAGE, DISADVANTAGE, CHALLENGE, PROCESS, STAGE, EXAMPLE, APPLICATION, FORMULA, COMPARISON, GENERAL.

            Return strictly a JSON object with this exact structure:
            {
              "topicName": "$topic",
              "subject": "$subject",
              "classLevel": "$classLevel",
              "summary": "Clear, engaging 2-sentence summary of the core concept.",
              "subtopics": [
                {
                  "id": "sub_1",
                  "title": "Subtopic Title",
                  "category": "DEFINITION",
                  "shortSummary": "Concise explanation of this component",
                  "orderIndex": 0
                }
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)

            val subtopicsList = mutableListOf<SubtopicNode>()
            val subArray = jsonRoot.optJSONArray("subtopics") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val item = subArray.getJSONObject(i)
                val catStr = item.optString("category", "GENERAL").uppercase()
                val category = runCatching { SubtopicCategory.valueOf(catStr) }.getOrDefault(SubtopicCategory.GENERAL)
                subtopicsList.add(
                    SubtopicNode(
                        id = item.optString("id", "sub_${i + 1}"),
                        title = item.optString("title", "Concept ${i + 1}"),
                        category = category,
                        shortSummary = item.optString("shortSummary", ""),
                        isFromUploadedNotes = false,
                        orderIndex = i
                    )
                )
            }

            val topicMap = TopicMap(
                topicId = "topic_${System.currentTimeMillis()}",
                topicName = jsonRoot.optString("topicName", topic),
                subject = jsonRoot.optString("subject", subject),
                classLevel = jsonRoot.optString("classLevel", classLevel),
                summary = jsonRoot.optString("summary", "Structured curriculum map generated by Gemini."),
                subtopics = subtopicsList
            )
            GeminiResult.Success(topicMap)
        } catch (e: Exception) {
            GeminiResult.Error("Failed to generate topic map: ${e.localizedMessage}")
        }
    }

    override suspend fun analyzeUploadedNotes(
        apiKey: String,
        imageBytesList: List<ByteArray>,
        userSubject: String?
    ): GeminiResult<TopicMap> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing. Enter key in Settings.", isKeyMissing = true)
        }
        if (imageBytesList.isEmpty()) {
            return@withContext GeminiResult.Error("No note images provided for analysis.")
        }

        val prompt = """
            You are Bunde, an expert educational AI tutor.
            Analyze the provided student school note images carefully.
            Extract the subject, main topic, educational level (e.g. SS2, Grade 11), headings, subtopics, definitions, and key concepts.
            If any text in the note images is unreadable or blurry, explicitly note it in the unreadableNoteNotice instead of inventing facts.
            Subject hint provided by student: ${userSubject ?: "General"}

            Return strictly a JSON object with this exact structure:
            {
              "topicName": "Extracted Main Topic Name",
              "subject": "Extracted Subject",
              "classLevel": "SS2",
              "summary": "Clear summary of the uploaded notes content.",
              "unreadableNoteNotice": null,
              "subtopics": [
                {
                  "id": "sub_1",
                  "title": "Extracted Heading / Subtopic",
                  "category": "DEFINITION",
                  "shortSummary": "Explanation extracted from notes",
                  "orderIndex": 0
                }
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, imageBytesList)
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)

            val subtopicsList = mutableListOf<SubtopicNode>()
            val subArray = jsonRoot.optJSONArray("subtopics") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val item = subArray.getJSONObject(i)
                val catStr = item.optString("category", "GENERAL").uppercase()
                val category = runCatching { SubtopicCategory.valueOf(catStr) }.getOrDefault(SubtopicCategory.GENERAL)
                subtopicsList.add(
                    SubtopicNode(
                        id = item.optString("id", "sub_${i + 1}"),
                        title = item.optString("title", "Note Concept ${i + 1}"),
                        category = category,
                        shortSummary = item.optString("shortSummary", ""),
                        isFromUploadedNotes = true,
                        orderIndex = i
                    )
                )
            }

            val notice = jsonRoot.optString("unreadableNoteNotice", "").ifBlank { null }
            val topicMap = TopicMap(
                topicId = "notes_topic_${System.currentTimeMillis()}",
                topicName = jsonRoot.optString("topicName", "Scanned Notes Topic"),
                subject = jsonRoot.optString("subject", userSubject ?: "General Studies"),
                classLevel = jsonRoot.optString("classLevel", "SS2"),
                summary = jsonRoot.optString("summary", "Analysis extracted from student notes."),
                subtopics = subtopicsList,
                unreadableNoteNotice = notice
            )
            GeminiResult.Success(topicMap)
        } catch (e: Exception) {
            GeminiResult.Error("Failed to analyze notes: ${e.localizedMessage}")
        }
    }

    override suspend fun askTutor(
        apiKey: String,
        topic: String,
        subtopic: String,
        conversationHistory: List<Pair<String, String>>,
        studentMessage: String
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        GeminiResult.Success("Interactive tutor response for $subtopic.")
    }

    override suspend fun evaluateAnswer(
        apiKey: String,
        question: String,
        expectedConcept: String,
        studentAnswer: String
    ): GeminiResult<AnswerEvaluation> = withContext(Dispatchers.IO) {
        GeminiResult.Success(
            AnswerEvaluation(
                isCorrect = true,
                scoreOutOfTen = 9,
                praise = "Excellent answer!",
                correction = null,
                nextStepRecommendation = "Proceeding to next concept."
            )
        )
    }

    private fun executeGeminiPrompt(apiKey: String, prompt: String, imageBytesList: List<ByteArray>): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        
        val partsArray = JSONArray().apply {
            // Add image parts if present
            for (imgBytes in imageBytesList) {
                val base64Data = Base64.encodeToString(imgBytes, Base64.NO_WRAP)
                put(JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Data)
                    })
                })
            }
            // Add text prompt
            put(JSONObject().apply {
                put("text", prompt)
            })
        }

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            throw IOException("Gemini API error code ${response.code}: $errorBody")
        }

        val body = response.body?.string() ?: throw IOException("Empty response from Gemini")
        val root = JSONObject(body)
        val candidates = root.optJSONArray("candidates") ?: throw IOException("No candidates returned from Gemini")
        if (candidates.length() == 0) throw IOException("Empty candidates list")
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: throw IOException("No parts in Gemini response")
        val text = parts.getJSONObject(0).optString("text") ?: ""
        return text
    }

    private fun extractJsonString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            return trimmed.removePrefix("```json").substringBeforeLast("```").trim()
        }
        if (trimmed.startsWith("```")) {
            return trimmed.removePrefix("```").substringBeforeLast("```").trim()
        }
        return trimmed
    }
}
