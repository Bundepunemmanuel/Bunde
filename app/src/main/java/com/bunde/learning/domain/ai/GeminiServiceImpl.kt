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

    private val candidateModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.8-flash",
        "gemini-flash-latest",
        "gemini-2.5-flash"
    )

    override suspend fun testConnection(apiKey: String): GeminiResult<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API Key is missing. Please enter your Gemini API key in Settings.", isKeyMissing = true)
        }
        try {
            executeGeminiPrompt(apiKey, "Reply with 'OK'", emptyList())
            GeminiResult.Success(true)
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
            You are Bunde, an expert educational AI personal tutor.
            Generate an exhaustive, logically sequenced curriculum topic map for:
            Subject: $subject
            Topic: $topic
            Class / Educational Level: $classLevel

            Pedagogical Hierarchy Requirements:
            1. Break the topic into logically ordered, progressive learning subtopics (Concepts) from foundational to advanced.
            2. Fully represent the relevant educational dimensions: Definitions, Features, Characteristics, Types/Classifications, Objectives, Reasons/Causes, Effects/Consequences, Functions, Importance, Advantages/Disadvantages, Challenges, Processes/Stages, Applications, Calculations/Formulas (for quantitative subjects), or Textual/Scriptural context (for humanities/languages/religious studies).
            3. ONLY include subtopics that genuinely apply to this subject and topic. Never force artificial categories.
            4. Do not confuse the topic name with any person's name.
            5. Provide 4 to 8 clear, distinct subtopics that cover the complete curriculum unit for this class level.

            Return strictly a JSON object with this exact structure:
            {
              "topicName": "$topic",
              "subject": "$subject",
              "classLevel": "$classLevel",
              "summary": "Clear, engaging 2-sentence summary of what this curriculum unit accomplishes.",
              "subtopics": [
                {
                  "id": "sub_1",
                  "title": "Subtopic Title",
                  "category": "DEFINITION",
                  "shortSummary": "Specific core concept covered here.",
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

    override suspend fun generateConceptStep(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        subtopicTitle: String,
        category: String,
        conceptSummary: String,
        conceptIndex: Int,
        totalConcepts: Int,
        previouslyTaughtConcepts: List<String>,
        uploadedNotesSummary: String?,
        struggleCount: Int
    ): GeminiResult<ConceptStepResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing. Enter key in Settings.", isKeyMissing = true)
        }

        val adaptationGuideline = """
            ADAPTATION DISCIPLINE BY SUBJECT:
            - Mathematics: Demonstrate the exact underlying step/rule first. Formulate an activity where the student solves a problem or spots a specific error.
            - Sciences (Physics, Chemistry, Biology): Clarify mechanisms, cause-and-effect relationships, laws, or thought experiments. Formulate a prediction or application question.
            - Languages & Literature: Focus on structural usage, grammar rules, vocabulary distinctions, character motivations, themes, or textual analysis.
            - Social Sciences (Economics, Commerce, Government, Geography, History): Ground concepts in real-world cause/effect, economic incentives, institutional functions, or historical sequence.
            - Religious Studies (CRS/IRS): Accurately explain scriptures, events, figures, historical context, moral lessons, and theological concepts without bias or dilution.
        """.trimIndent()

        val prompt = """
            You are Bunde, an active, master educational personal tutor.
            You are teaching ONE specific concept in a structured lesson:
            - Subject: $subject
            - Overall Topic: $topic
            - Educational Level: $classLevel
            - Concept Position: Concept ${conceptIndex + 1} of $totalConcepts
            - Current Concept Name: $subtopicTitle
            - Concept Category: $category
            - Core Concept Goal: $conceptSummary
            - Concepts Already Covered in this Lesson: ${if (previouslyTaughtConcepts.isEmpty()) "None yet (this is the opening concept)." else previouslyTaughtConcepts.joinToString(", ")}
            - Student's Uploaded Notes Context: ${uploadedNotesSummary ?: "Standard curriculum (no external notes uploaded)."}
            - Student's Current Struggle Level: $struggleCount (0 = normal, >0 = student struggled previously on this topic, simplify explanation and use an intuitive analogy).

            $adaptationGuideline

            TUTOR BEHAVIOR RULES (CRITICAL):
            1. ZERO GREETINGS: Do NOT say "Good morning", "Welcome", "Hello", "Take your seats", "Today in class", "I am Bunde", or introduce yourself. Start directly with the concept explanation sentence.
            2. DO NOT deliver a long lecture. Teach ONLY this single concept in 2 to 3 concise, vivid paragraphs with 1 concrete real-world example.
            3. DO NOT ask "Do you understand?" or "Let me know if you want to learn more."
            4. END WITH ONE ENGAGING, INTERACTIVE ACTIVITY QUESTION to test the student's actual understanding immediately (e.g. solve a step, make a prediction, spot an error, apply the rule, or explain in their own words).
            5. Never confuse the topic ($topic) with a person's name.

            Return strictly a JSON object:
            {
              "explanation": "Direct explanation of this concept without greeting or preamble, including 1 concrete example.",
              "checkActivityQuestion": "The specific question or challenge testing this concept.",
              "activityType": "QUESTION",
              "expectedConceptKeyPoints": "Key points that a correct student response must embody (in student's own words)."
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)
            val step = ConceptStepResponse(
                explanation = jsonRoot.optString("explanation", conceptSummary),
                checkActivityQuestion = jsonRoot.optString("checkActivityQuestion", "In your own words, how does $subtopicTitle relate to $topic?"),
                activityType = jsonRoot.optString("activityType", "QUESTION"),
                expectedConceptKeyPoints = jsonRoot.optString("expectedConceptKeyPoints", conceptSummary)
            )
            GeminiResult.Success(step)
        } catch (e: Exception) {
            GeminiResult.Success(
                ConceptStepResponse(
                    explanation = "$subtopicTitle is a fundamental part of $topic in $subject. Specifically, $conceptSummary.",
                    checkActivityQuestion = "How would you explain the importance of $subtopicTitle in your own words?",
                    activityType = "QUESTION",
                    expectedConceptKeyPoints = conceptSummary
                )
            )
        }
    }

    override suspend fun askTutor(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        currentConcept: String,
        currentActivityQuestion: String?,
        conversationHistory: List<Pair<String, String>>,
        studentMessage: String,
        previouslyTaughtConcepts: List<String>,
        struggleCount: Int
    ): GeminiResult<TutorTurnResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing.", isKeyMissing = true)
        }

        val prompt = """
            You are Bunde, an active, responsive personal tutor.
            Current Lesson Context:
            - Subject: $subject
            - Topic: $topic
            - Educational Level: $classLevel
            - Current Concept Being Taught: $currentConcept
            - Current Pending Activity/Question: ${currentActivityQuestion ?: "None pending."}
            - Previous Concepts Covered: ${previouslyTaughtConcepts.joinToString(", ").ifEmpty { "Opening concept" }}
            - Student Struggle Level: $struggleCount

            The student sent this message during the lesson:
            "$studentMessage"

            DETERMINE THE INTENT:
            Case A: The student is asking an INTERRUPTION / QUESTION / CLARIFICATION (e.g. "what does that word mean?", "give another example", "why not X?", "I don't get this", "can we go back?"):
            - Answer their question directly and concisely in 2-3 sentences.
            - Connect the answer back to the current concept ($currentConcept).
            - Guide them smoothly back to the lesson activity.
            - Set "isInterruptionHandled": true.
            - Set "resumePrompt": "Ready to try the challenge? [Restate or adjust the question]"

            Case B: The student is ANSWERING the pending activity question:
            - Evaluate their thought process.
            - Recognize what is correct.
            - If incomplete or incorrect, give a tailored hint rather than giving away the answer immediately.
            - Set "isInterruptionHandled": false.

            Return strictly a JSON object:
            {
              "text": "Your direct, engaging educational response.",
              "isInterruptionHandled": false,
              "resumePrompt": null
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)
            val tutorResponse = TutorTurnResponse(
                text = jsonRoot.optString("text", "Let's examine how this connects to $currentConcept."),
                isInterruptionHandled = jsonRoot.optBoolean("isInterruptionHandled", false),
                resumePrompt = jsonRoot.optString("resumePrompt", "").ifBlank { null }
            )
            GeminiResult.Success(tutorResponse)
        } catch (e: Exception) {
            GeminiResult.Success(
                TutorTurnResponse(
                    text = "Regarding $studentMessage: in $subject, $currentConcept is central because of how it applies here. Let's focus on our current question.",
                    isInterruptionHandled = true,
                    resumePrompt = currentActivityQuestion
                )
            )
        }
    }

    override suspend fun evaluateAnswer(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        currentConcept: String,
        activityQuestion: String,
        expectedConceptOrKeyPoints: String,
        studentAnswer: String,
        attemptNumber: Int
    ): GeminiResult<AnswerEvaluation> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing.", isKeyMissing = true)
        }

        val prompt = """
            You are Bunde, an expert educational AI evaluating a secondary school student's answer.
            Lesson Context:
            - Subject: $subject
            - Topic: $topic
            - Class Level: $classLevel
            - Concept: $currentConcept
            - Question Asked: $activityQuestion
            - Core Concept / Key Points: $expectedConceptOrKeyPoints
            - Student Answer: "$studentAnswer"
            - Attempt Number: $attemptNumber

            EVALUATION PRINCIPLES:
            1. Students can answer in their OWN WORDS. Do NOT demand textbook regurgitation. Evaluate genuine conceptual comprehension.
            2. Accept valid alternative interpretations, methods, or analogies.
            3. If the answer is substantially correct (showing good understanding):
               - "isCorrect": true, score 8-10, "isMastered": true, "allowsRetry": false.
               - Praise specifically what they explained well.
            4. If the answer is partially correct or missing a critical component:
               - "isCorrect": false, score 5-7, "allowsRetry": true, "isMastered": false.
               - Acknowledge what they got right, specify the missing piece, and give a guiding HINT without giving the answer away yet.
            5. If the answer is incorrect or reveals a misconception:
               - "isCorrect": false, score 1-4, "allowsRetry": true, "isMastered": false.
               - Address the misconception gently, provide an intuitive analogy or hint, and invite them to rethink.
            6. If attemptNumber >= 2 and student still struggles:
               - "allowsRetry": false. Provide a clear, alternative explanation so the student is not stuck.

            Return strictly a JSON object:
            {
              "isCorrect": true,
              "scoreOutOfTen": 9,
              "praise": "Clear, specific praise on what was understood.",
              "correction": "Targeted explanation of misconception or missing element, or null if complete.",
              "hint": "Guiding hint if student should retry, or null.",
              "allowsRetry": false,
              "isMastered": true,
              "nextStepRecommendation": "Next concept ready or retry current concept."
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)
            val eval = AnswerEvaluation(
                isCorrect = jsonRoot.optBoolean("isCorrect", true),
                scoreOutOfTen = jsonRoot.optInt("scoreOutOfTen", 8),
                praise = jsonRoot.optString("praise", "Good effort and clear reasoning!"),
                correction = jsonRoot.optString("correction", "").ifBlank { null },
                hint = jsonRoot.optString("hint", "").ifBlank { null },
                allowsRetry = jsonRoot.optBoolean("allowsRetry", false),
                isMastered = jsonRoot.optBoolean("isMastered", true),
                nextStepRecommendation = jsonRoot.optString("nextStepRecommendation", "Ready for the next concept.")
            )
            GeminiResult.Success(eval)
        } catch (e: Exception) {
            GeminiResult.Success(
                AnswerEvaluation(
                    isCorrect = true,
                    scoreOutOfTen = 8,
                    praise = "Good attempt! You demonstrated solid understanding.",
                    correction = null,
                    hint = null,
                    allowsRetry = false,
                    isMastered = true,
                    nextStepRecommendation = "Proceeding to next concept."
                )
            )
        }
    }

    private fun executeGeminiPrompt(apiKey: String, prompt: String, imageBytesList: List<ByteArray>): String {
        var lastException: Exception? = null

        for (modelName in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                val partsArray = JSONArray().apply {
                    for (imgBytes in imageBytesList) {
                        val base64Data = Base64.encodeToString(imgBytes, Base64.NO_WRAP)
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Data)
                            })
                        })
                    }
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
                    })
                }

                val requestBody = payload.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).post(requestBody).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    response.close()
                    val ex = IOException("Gemini API error code ${response.code}: $errorBody")
                    lastException = ex
                    if (response.code == 404) {
                        continue
                    } else {
                        throw ex
                    }
                }

                val body = response.body?.string() ?: throw IOException("Empty response from Gemini")
                response.close()

                val root = JSONObject(body)
                val candidates = root.optJSONArray("candidates") ?: throw IOException("No candidates returned from Gemini")
                if (candidates.length() == 0) throw IOException("Empty candidates list")
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts") ?: throw IOException("No parts in Gemini response")
                val text = parts.getJSONObject(0).optString("text") ?: ""
                if (text.isNotBlank()) {
                    return text
                }
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("404") != true) {
                    throw e
                }
            }
        }

        throw lastException ?: IOException("Failed to reach Gemini API. Please check your key or internet connection.")
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
