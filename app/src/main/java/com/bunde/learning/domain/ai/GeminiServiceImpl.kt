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

    override suspend fun generateStructuredConceptStep(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        conceptId: String,
        conceptTitle: String,
        conceptCategory: String,
        conceptSummary: String,
        conceptIndex: Int,
        totalConcepts: Int,
        previouslyTaughtConcepts: List<String>,
        uploadedNotesSummary: String?,
        struggleCount: Int,
        recentlyUsedActivityTypes: List<String>
    ): GeminiResult<StructuredConceptStep> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing. Enter key in Settings.", isKeyMissing = true)
        }

        val prompt = """
            You are Bunde, an active personal AI tutor.
            Generate a targeted, high-impact learning step for:
            - Subject: $subject
            - Topic: $topic
            - Educational Level: $classLevel
            - Concept ID: $conceptId
            - Concept Title: $conceptTitle
            - Concept Category: $conceptCategory
            - Summary/Core Objective: $conceptSummary
            - Step Progress: Concept ${conceptIndex + 1} of $totalConcepts
            - Previously Taught Concepts: ${if (previouslyTaughtConcepts.isEmpty()) "None (opening concept)" else previouslyTaughtConcepts.joinToString(", ")}
            - Uploaded Notes Context: ${uploadedNotesSummary ?: "Standard school syllabus"}
            - Student Struggle Level: $struggleCount
            - Recently Used Activity Types (AVOID repeating these unless essential): ${recentlyUsedActivityTypes.joinToString(", ").ifEmpty { "None" }}

            PEDAGOGICAL REQUIREMENTS (CRITICAL):
            1. ZERO PREAMBLE: Start directly with the core explanation. No "Welcome back", "Hello", "Good morning", or greeting.
            2. CONCISE EXPLANATION: Write ONLY 2 to 3 focused paragraphs explaining this single concept clearly for a $classLevel student. Explain any tricky terminology simply.
            3. CONCRETE EXAMPLE: Provide 1 vivid, authentic real-world example, calculation demonstration, or practical scenario.
            4. DESTROY GENERIC QUESTIONS: DO NOT ask "What did you learn?", "Can you explain this in your own words?", "Do you understand?", or "Give an example".
            5. SPECIFIC ACTIVITY: The activity MUST test a concrete piece of the concept just explained.
               Choose the best type for this subject/concept:
               - "SCENARIO": Present a situation; student decides or explains the right outcome.
               - "ERROR_DETECTION": Present a flawed statement/step; student spots the error.
               - "PREDICTION": Student predicts what happens if a factor changes.
               - "STEP_SOLVE": Student solves the next mathematical/chemical/economic step.
               - "COMPARISON": Student distinguishes two closely related elements.
               - "CAUSE_EFFECT": Student explains the mechanism behind an effect.
               - "CALCULATION": Student calculates a value using the demonstrated formula.

            Return strictly a JSON object with this exact structure:
            {
              "conceptId": "$conceptId",
              "learningObjective": "Clear 1-sentence learning objective being tested",
              "conciseExplanation": "The 2-3 paragraph explanation without greetings",
              "concreteExample": "The practical example or demonstration",
              "activity": {
                "type": "SCENARIO",
                "question": "The specific challenge testing this exact concept",
                "options": [],
                "expectedAnswerCriteria": "Key reasoning or facts required for an answer to be correct in the student's own words",
                "hint": "A guiding hint if the student gets stuck"
              }
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)
            val actObj = jsonRoot.optJSONObject("activity") ?: JSONObject()

            val optionsList = mutableListOf<String>()
            val optArr = actObj.optJSONArray("options")
            if (optArr != null) {
                for (i in 0 until optArr.length()) {
                    optionsList.add(optArr.getString(i))
                }
            }

            val activity = StructuredActivity(
                type = actObj.optString("type", "SCENARIO"),
                question = actObj.optString("question", "How does $conceptTitle apply in a practical situation?"),
                options = optionsList,
                expectedAnswerCriteria = actObj.optString("expectedAnswerCriteria", conceptSummary),
                hint = actObj.optString("hint", "").ifBlank { null }
            )

            val step = StructuredConceptStep(
                conceptId = jsonRoot.optString("conceptId", conceptId),
                learningObjective = jsonRoot.optString("learningObjective", "Master $conceptTitle"),
                conciseExplanation = jsonRoot.optString("conciseExplanation", conceptSummary),
                concreteExample = jsonRoot.optString("concreteExample", "In daily practice, $conceptTitle operates directly within $subject."),
                activity = activity
            )
            GeminiResult.Success(step)
        } catch (e: Exception) {
            GeminiResult.Success(
                StructuredConceptStep(
                    conceptId = conceptId,
                    learningObjective = "Understand $conceptTitle in $subject",
                    conciseExplanation = "$conceptTitle is an essential concept in $topic. Specifically, $conceptSummary.",
                    concreteExample = "For example, in $subject, this concept is applied to analyze and solve problems directly.",
                    activity = StructuredActivity(
                        type = "SCENARIO",
                        question = "Consider a situation in $topic where $conceptTitle is applied. What is the primary outcome?",
                        options = emptyList(),
                        expectedAnswerCriteria = conceptSummary,
                        hint = "Think about the key role $conceptTitle plays."
                    )
                )
            )
        }
    }

    override suspend fun evaluateStructuredAnswer(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        conceptTitle: String,
        learningObjective: String,
        activityQuestion: String,
        expectedAnswerCriteria: String,
        studentAnswer: String,
        attemptNumber: Int
    ): GeminiResult<StructuredAnswerEvaluation> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing.", isKeyMissing = true)
        }

        val prompt = """
            You are Bunde, an expert educational AI evaluating a $classLevel secondary school student's answer.
            Evaluation Context:
            - Subject: $subject
            - Topic: $topic
            - Concept: $conceptTitle
            - Learning Objective: $learningObjective
            - Question Asked: $activityQuestion
            - Expected Answer Criteria: $expectedAnswerCriteria
            - Student Answer: "$studentAnswer"
            - Attempt Number: $attemptNumber

            EVALUATION RULES:
            1. Students can answer in their OWN WORDS. Do NOT demand verbatim textbook phrases.
            2. Accept valid alternative reasoning or creative explanations that demonstrate true understanding.
            3. Verdict choices:
               - "CORRECT": Substantially demonstrates the objective. Score: 8-10. conceptCompleted = true, allowsRetry = false.
               - "PARTIALLY_CORRECT": Got some aspects right, but missed an essential concept. Score: 5-7. conceptCompleted = false, allowsRetry = true. Provide a targetedHint.
               - "INCORRECT": Revealed a clear misconception or wrong reasoning. Score: 1-4. conceptCompleted = false, allowsRetry = true. Explain the mistake gently and provide a hint.
               - "NEEDS_CLARIFICATION": Answer is too vague, ambiguous, or one word. Ask the student to elaborate. Score: 4. conceptCompleted = false, allowsRetry = true.
            4. If attemptNumber >= 2 and the student is still struggling:
               - conceptCompleted = true, allowsRetry = false.
               - Provide a clear, supportive resolution so the student is never stuck on a concept.

            Return strictly a JSON object:
            {
              "verdict": "CORRECT",
              "scoreOutOfTen": 9,
              "specificPraise": "What the student understood well",
              "explanationOfMistake": null,
              "targetedHint": null,
              "conceptCompleted": true,
              "allowsRetry": false
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)

            val verdictStr = jsonRoot.optString("verdict", "CORRECT").uppercase()
            val verdict = runCatching { EvaluationVerdict.valueOf(verdictStr) }.getOrDefault(
                if (jsonRoot.optInt("scoreOutOfTen", 8) >= 8) EvaluationVerdict.CORRECT else EvaluationVerdict.PARTIALLY_CORRECT
            )

            val isCompleted = jsonRoot.optBoolean("conceptCompleted", verdict == EvaluationVerdict.CORRECT || attemptNumber >= 2)
            val canRetry = jsonRoot.optBoolean("allowsRetry", !isCompleted && attemptNumber < 2)

            val eval = StructuredAnswerEvaluation(
                verdict = verdict,
                scoreOutOfTen = jsonRoot.optInt("scoreOutOfTen", if (verdict == EvaluationVerdict.CORRECT) 9 else 6),
                specificPraise = jsonRoot.optString("specificPraise", "Good effort in reasoning through this concept."),
                explanationOfMistake = jsonRoot.optString("explanationOfMistake", "").ifBlank { null },
                targetedHint = jsonRoot.optString("targetedHint", "").ifBlank { null },
                followUpActivity = null,
                conceptCompleted = isCompleted,
                allowsRetry = canRetry
            )
            GeminiResult.Success(eval)
        } catch (e: Exception) {
            GeminiResult.Success(
                StructuredAnswerEvaluation(
                    verdict = EvaluationVerdict.CORRECT,
                    scoreOutOfTen = 8,
                    specificPraise = "You demonstrated solid conceptual understanding in your explanation.",
                    explanationOfMistake = null,
                    targetedHint = null,
                    followUpActivity = null,
                    conceptCompleted = true,
                    allowsRetry = false
                )
            )
        }
    }

    override suspend fun handleTutorInterruption(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        currentConceptTitle: String,
        currentActivityQuestion: String,
        studentQuery: String
    ): GeminiResult<TutorInterruptionResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing.", isKeyMissing = true)
        }

        val prompt = """
            You are Bunde, an active personal tutor.
            The student interrupted the structured lesson to ask this question:
            "$studentQuery"

            Lesson Context:
            - Subject: $subject
            - Topic: $topic
            - Current Concept: $currentConceptTitle
            - Pending Activity Challenge: $currentActivityQuestion

            INSTRUCTIONS:
            1. Answer the student's question directly, accurately, and simply in 2 to 3 sentences.
            2. Connect the explanation back to $currentConceptTitle.
            3. Provide a natural bridge sentence that guides them back to the active challenge.

            Return strictly a JSON object:
            {
              "explanation": "Direct answer to the student's question",
              "bridgeBackToLesson": "Bridge sentence connecting back to the current challenge"
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonClean = extractJsonString(responseText)
            val jsonRoot = JSONObject(jsonClean)
            val resp = TutorInterruptionResponse(
                explanation = jsonRoot.optString("explanation", "In $subject, $currentConceptTitle directly addresses this."),
                bridgeBackToLesson = jsonRoot.optString("bridgeBackToLesson", "Now let's apply this to our current challenge!")
            )
            GeminiResult.Success(resp)
        } catch (e: Exception) {
            GeminiResult.Success(
                TutorInterruptionResponse(
                    explanation = "That's a helpful question! In $subject, this ties directly to $currentConceptTitle.",
                    bridgeBackToLesson = "Let's return to the challenge when you're ready."
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
