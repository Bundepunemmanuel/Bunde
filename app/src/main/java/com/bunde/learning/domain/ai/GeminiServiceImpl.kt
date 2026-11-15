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

    companion object {
        private val GLOBAL_TUTOR_SYSTEM_INSTRUCTION = """
            You are Bunde, a patient, warm, intelligent secondary school teacher sitting right beside a student.
            Your primary target is an SS2 secondary school student (unless another class level is specified).

            PERMANENT TEACHING & LANGUAGE RULES:
            1. SIMPLE ENGLISH IS MANDATORY:
               - Use common, everyday words and short, natural sentences.
               - Write short paragraphs (1 to 3 short sentences each). Explain one small idea at a time.
               - NEVER write like a university lecturer, academic researcher, formal textbook author, or generic AI chatbot.
               - Avoid unnecessary big words, stiff phrases (like "thereby", "pivotal", "operational efficiency", "implications"), and long complex sentences.
               - Avoid repeating the same idea just to sound detailed.
               - ZERO filler: never open with "Welcome back", "Hello!", or "Good day", and never end with "Let me know if you want to learn more" or "Do you understand?".

            2. EXPLAIN DIFFICULT WORDS IMMEDIATELY:
               - Keep important subject and examination terms so the student learns what they need for exams.
               - Whenever you use a hard or unfamiliar word, explain it right away in plain everyday English.
               - Example: "Pharaoh adopted Moses. Adopted means he accepted Moses as his own child and raised him."
               - Never pack several difficult words into one sentence without explaining them.

            3. PLAIN, CLEAR HEADINGS & QUESTIONS:
               - Concept titles, headings, instructions, examples, and questions must be easy to read on the first try.
               - Bad heading: "ANALYZE THE DIVINE PRESERVATION OF INFANT MOSES AND HIS FLIGHT TO MIDIAN."
               - Good heading: "How Moses Was Saved and Grew Up in the Palace"
               - Bad question: "Analyze the implications of Moses' decision in the context of his conflicting cultural identities."
               - Good question: "Why did Moses decide to help the Hebrew man?"

            4. PRESERVE ACADEMIC ACCURACY:
               - Simple English must never mean wrong or incomplete facts.
               - Teach accurate definitions, dates, names, events, formulas, and calculation steps.
               - When an exam definition matters, give the accurate point and then explain it in everyday English.
               - Distinguish facts from opinions. Follow uploaded student notes faithfully when notes are provided.
               - Never confuse the topic name with the student's name, and never invent personal details about the student.

            5. RELATABLE EXAMPLES & SUBJECT ADAPTATION:
               - Use clear everyday, school, family, community, or realistic examples (including Nigerian examples where natural, without forcing them into every lesson).
               - Examples must clarify the topic, never replace real facts.
               - Adapt naturally to each subject:
                 * Mathematics: show calculations step by step.
                 * Science: explain clearly how and why things happen.
                 * History & CRS: tell events in the order they happened, one event at a time.
                 * Economics & Commerce: use familiar buying, selling, saving, and business examples.
                 * English Language: explain grammar and words with short, clear examples.

            6. ADAPTIVE LANGUAGE DIFFICULTY:
               - If a student is confused or asks what a word means, explain even more simply using a fresh everyday example. Never repeat the same complicated explanation.

            FINAL SELF-CHECK BEFORE ANSWERING:
            Ask yourself: "Would an SS2 student understand this clearly the very first time they read it?"
            If not, make the words simpler and the sentences shorter before replying.
        """.trimIndent()
    }

    override suspend fun testConnection(apiKey: String): GeminiResult<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API Key is missing. Please enter your Gemini API key in Settings.", isKeyMissing = true)
        }
        try {
            executeGeminiPrompt(apiKey, "Reply with 'OK'", emptyList(), expectJson = false)
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
            Create a clear, step-by-step lesson plan (topic map) for a $classLevel student:
            - Subject: $subject
            - Topic: $topic
            - Class Level: $classLevel

            RULES FOR THIS TOPIC MAP:
            1. Break the topic into 4 to 8 small, easy-to-follow subtopics in logical teaching order (from basic idea to deeper parts).
            2. Teach ONE small part per subtopic. For example, in History/CRS, put birth/saving, growing up, and later events as separate subtopics—never lump everything into one big heading.
            3. Use simple, friendly headings in Title Case (like "What Privatization Means" or "How Moses Was Saved and Grew Up in the Palace"). NEVER use ALL-CAPS or research-paper titles.
            4. Keep "summary" to 2 short, clear sentences telling the student what this topic is about in everyday English.
            5. Keep each subtopic's "shortSummary" to 1 plain-English sentence explaining the main fact or exam point.
            6. Include only subtopics that genuinely belong to $subject and $topic.
            7. Never confuse the topic name "$topic" with the student's name.

            Allowed "category" values:
            DEFINITION, FEATURE, CHARACTERISTIC, TYPE, OBJECTIVE, REASON, CAUSE, EFFECT, FUNCTION, IMPORTANCE, ADVANTAGE, DISADVANTAGE, CHALLENGE, PROCESS, STAGE, EXAMPLE, APPLICATION, FORMULA, COMPARISON, GENERAL

            Return strictly a JSON object with this exact structure:
            {
              "topicName": "$topic",
              "subject": "$subject",
              "classLevel": "$classLevel",
              "summary": "2 short, clear sentences in everyday English about what the student will learn.",
              "subtopics": [
                {
                  "id": "sub_1",
                  "title": "Simple, Clear Subtopic Heading",
                  "category": "DEFINITION",
                  "shortSummary": "One plain-English sentence covering the main idea and key fact.",
                  "orderIndex": 0
                }
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonRoot = parseJsonWithRegeneration(
                apiKey = apiKey,
                rawResponse = responseText,
                schemaHint = """{"topicName": String, "subject": String, "classLevel": String, "summary": String, "subtopics": [{"id": String, "title": String, "category": String, "shortSummary": String, "orderIndex": Int}]}"""
            )

            val subtopicsList = mutableListOf<SubtopicNode>()
            val subArray = jsonRoot.optJSONArray("subtopics") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val item = subArray.getJSONObject(i)
                val catStr = item.optString("category", "GENERAL").uppercase()
                val category = runCatching { SubtopicCategory.valueOf(catStr) }.getOrDefault(SubtopicCategory.GENERAL)
                subtopicsList.add(
                    SubtopicNode(
                        id = item.optString("id", "sub_${i + 1}"),
                        title = cleanHeading(item.optString("title", "Part ${i + 1}")),
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
                summary = jsonRoot.optString("summary", "A clear, step-by-step guide to $topic in $subject."),
                subtopics = subtopicsList
            )
            GeminiResult.Success(topicMap)
        } catch (e: Exception) {
            GeminiResult.Error("Could not build the lesson plan right now: ${e.localizedMessage}")
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
            Read the student's uploaded school note images carefully.
            Subject hint from student: ${userSubject ?: "Not specified"}

            RULES FOR ANALYZING UPLOADED NOTES:
            1. Follow the facts, definitions, formulas, dates, and points written in the student's notes faithfully. Do NOT invent missing facts.
            2. If any writing in the photos is blurry or hard to read, say so plainly in "unreadableNoteNotice" instead of guessing.
            3. Turn the note sections into small, clear subtopics with easy-to-understand Title Case headings (never ALL-CAPS or academic jargon).
            4. Explain the "summary" and each subtopic's "shortSummary" in simple, everyday English suitable for an SS2 student while keeping all key exam facts from the notes.

            Allowed "category" values:
            DEFINITION, FEATURE, CHARACTERISTIC, TYPE, OBJECTIVE, REASON, CAUSE, EFFECT, FUNCTION, IMPORTANCE, ADVANTAGE, DISADVANTAGE, CHALLENGE, PROCESS, STAGE, EXAMPLE, APPLICATION, FORMULA, COMPARISON, GENERAL

            Return strictly a JSON object with this exact structure:
            {
              "topicName": "Clear Main Topic Name from Notes",
              "subject": "Extracted Subject",
              "classLevel": "SS2",
              "summary": "2 short, simple sentences summarizing what these notes cover.",
              "unreadableNoteNotice": null,
              "subtopics": [
                {
                  "id": "sub_1",
                  "title": "Simple Heading from Notes",
                  "category": "DEFINITION",
                  "shortSummary": "Simple explanation of the exact fact or point in the notes.",
                  "orderIndex": 0
                }
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, imageBytesList)
            val jsonRoot = parseJsonWithRegeneration(
                apiKey = apiKey,
                rawResponse = responseText,
                schemaHint = """{"topicName": String, "subject": String, "classLevel": String, "summary": String, "unreadableNoteNotice": String?, "subtopics": [{"id": String, "title": String, "category": String, "shortSummary": String, "orderIndex": Int}]}"""
            )

            val subtopicsList = mutableListOf<SubtopicNode>()
            val subArray = jsonRoot.optJSONArray("subtopics") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val item = subArray.getJSONObject(i)
                val catStr = item.optString("category", "GENERAL").uppercase()
                val category = runCatching { SubtopicCategory.valueOf(catStr) }.getOrDefault(SubtopicCategory.GENERAL)
                subtopicsList.add(
                    SubtopicNode(
                        id = item.optString("id", "sub_${i + 1}"),
                        title = cleanHeading(item.optString("title", "Note Part ${i + 1}")),
                        category = category,
                        shortSummary = item.optString("shortSummary", ""),
                        isFromUploadedNotes = true,
                        orderIndex = i
                    )
                )
            }

            val rawNotice = jsonRoot.optString("unreadableNoteNotice", "").trim()
            val notice = if (rawNotice.isBlank() || rawNotice.equals("null", ignoreCase = true)) null else rawNotice
            val topicMap = TopicMap(
                topicId = "notes_topic_${System.currentTimeMillis()}",
                topicName = jsonRoot.optString("topicName", "Scanned Notes Topic"),
                subject = jsonRoot.optString("subject", userSubject ?: "General Studies"),
                classLevel = jsonRoot.optString("classLevel", "SS2"),
                summary = jsonRoot.optString("summary", "Key points taken from your uploaded class notes."),
                subtopics = subtopicsList,
                unreadableNoteNotice = notice
            )
            GeminiResult.Success(topicMap)
        } catch (e: Exception) {
            GeminiResult.Error("Could not read your notes clearly right now: ${e.localizedMessage}")
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
        recentlyUsedActivityTypes: List<String>,
        previousStoryThread: String?
    ): GeminiResult<StructuredConceptStep> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing. Enter key in Settings.", isKeyMissing = true)
        }

        val adaptiveNote = if (struggleCount > 0) {
            "ADAPTIVE ALERT: The student found earlier parts tricky (struggle count: $struggleCount). Use EVEN SIMPLER everyday words, shorter sentences, and break this idea down gently step by step."
        } else {
            "Keep explanations clear, natural, and easy for a $classLevel student to grasp on the first read."
        }

        val threadContext = if (previousStoryThread.isNullOrBlank()) {
            "This is the opening episode—introduce a fresh, memorable real-life mini-story or character if helpful."
        } else {
            "Continue smoothly from the previous episode/thread so the topic feels like one connected story: $previousStoryThread"
        }

        val prompt = """
            Teach ONE small concept to a $classLevel student so it sticks in memory like a movie scene:
            - Subject: $subject
            - Topic: $topic
            - Class Level: $classLevel
            - Concept (${conceptIndex + 1} of $totalConcepts): $conceptTitle
            - Focus Area: $conceptCategory
            - Core Fact/Point to Teach: $conceptSummary
            - Already Taught Earlier: ${if (previouslyTaughtConcepts.isEmpty()) "Nothing yet (this is the first concept)" else previouslyTaughtConcepts.joinToString(", ")}
            - Story Continuity: $threadContext
            - Student Notes Context: ${uploadedNotesSummary ?: "Standard school syllabus"}
            - Recently Used Question Types (try to pick a fresh one): ${recentlyUsedActivityTypes.joinToString(", ").ifEmpty { "None" }}
            - $adaptiveNote

            HOW TO MAKE THIS CONCEPT UNFORGETTABLE (FOLLOW EVERY RULE):
            1. ZERO GREETINGS OR FILLER: Start straight with the teaching. No "Hello", "Welcome", or "Today we will learn".
            2. LEARNING OBJECTIVE ("learningObjective"): 1 short, plain-English line spoken directly TO the student (e.g. "Learn why a company is treated like its own legal person", no ALL-CAPS).
            3. MINI-STORY HOOK ("storyHook"):
               - Write 1 to 2 vivid, curious sentences putting real people in a concrete situation BEFORE giving the textbook term.
               - Example: "Chidi and Musa register a bread bakery together. One day, the bakery owes 300,000 Naira for flour—can the supplier take Musa's personal phone to pay the debt?"
            4. GUESS FIRST ("predictionPrompt" & "predictionOptions"):
               - If guessing before reading wakes up curiosity (great for concepts, decisions, causes, science predictions, or scenarios), write a short 1-sentence guess question and 2 or 3 very short guess choices (e.g. ["Yes, Musa must pay from his pocket", "No, only the company pays", "Only if Chidi agrees"]).
               - If this step is a pure math formula or step-by-step calculation where guessing doesn't fit, set "predictionPrompt": null and "predictionOptions": [].
            5. SIMPLE EXPLANATION ("conciseExplanation"):
               - Write 2 to 3 very short, friendly paragraphs (2 to 3 short sentences per paragraph) that directly answer the hook and teach the exam point.
               - Explain any hard or exam word immediately right after using it in everyday English.
               - Keep accurate facts, dates, names, and formulas. Adapt naturally to $subject.
            6. MEANING OF KEY WORDS ("keyWordMeanings"):
               - List 0 to 3 important exam or hard words from this step with their simple 1-sentence meanings.
            7. MOVIE-SCENE EXAMPLE ("concreteExample"):
               - Write 1 vivid mini-scene with clear characters, action, and outcome (or a clear step-by-step worked example for Math/Science) that makes the idea easy to picture.
            8. PICTURE THIS MEMORY ANCHOR ("memoryPicture"):
               - Write 1 memorable, easy-to-picture mental image tying the term to the scene (e.g., "Picture a registered company like a separate invisible person holding its own wallet—separate from the owners' pockets.").
            9. NEXT-EPISODE THREAD ("storyThreadSummary"):
               - Write 1 short sentence summarizing the story or key idea so the next concept can pick up cleanly from here.
            10. EASY-TO-UNDERSTAND CHECK QUESTION ("activity"):
               - Ask ONE clear, direct question in simple English that tests this exact concept.
               - NEVER ask generic questions like "What did you learn?", "Do you understand?", or "Explain this in your own words."
               - Choose from: "SCENARIO", "ERROR_DETECTION", "PREDICTION", "STEP_SOLVE", "COMPARISON", "CAUSE_EFFECT", or "CALCULATION".

            Return strictly a JSON object with this exact structure:
            {
              "conceptId": "$conceptId",
              "learningObjective": "Short plain-English goal for this step",
              "storyHook": "1-2 sentence relatable mini-scene or curiosity dilemma.",
              "predictionPrompt": "Short 1-sentence question asking the student to guess before reading (or null)",
              "predictionOptions": ["Short guess A", "Short guess B"],
              "conciseExplanation": "2-3 short paragraphs in everyday English explaining one idea clearly.",
              "keyWordMeanings": [
                {
                  "word": "Important Term",
                  "meaning": "Simple everyday meaning of this word."
                }
              ],
              "concreteExample": "One vivid, relatable mini-scene or step-by-step worked example.",
              "memoryPicture": "One short mental picture so the student never forgets this concept.",
              "storyThreadSummary": "One short sentence connecting this episode to the next concept.",
              "activity": {
                "type": "SCENARIO",
                "question": "One short, clear question testing this concept",
                "options": [],
                "expectedAnswerCriteria": "Simple summary of the key idea the student should give",
                "hint": "A gentle, plain-English clue if the student gets stuck"
              }
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonRoot = parseJsonWithRegeneration(
                apiKey = apiKey,
                rawResponse = responseText,
                schemaHint = """{"conceptId": String, "learningObjective": String, "storyHook": String?, "predictionPrompt": String?, "predictionOptions": [String], "conciseExplanation": String, "keyWordMeanings": [{"word": String, "meaning": String}], "concreteExample": String, "memoryPicture": String?, "storyThreadSummary": String?, "activity": {"type": String, "question": String, "options": [String], "expectedAnswerCriteria": String, "hint": String}}"""
            )
            val actObj = jsonRoot.optJSONObject("activity") ?: JSONObject()

            val optionsList = mutableListOf<String>()
            val optArr = actObj.optJSONArray("options")
            if (optArr != null) {
                for (i in 0 until optArr.length()) {
                    val optText = optArr.optString(i, "").trim()
                    if (optText.isNotBlank()) optionsList.add(optText)
                }
            }

            val predOptionsList = mutableListOf<String>()
            val predOptArr = jsonRoot.optJSONArray("predictionOptions")
            if (predOptArr != null) {
                for (i in 0 until predOptArr.length()) {
                    val pOpt = predOptArr.optString(i, "").trim()
                    if (pOpt.isNotBlank()) predOptionsList.add(pOpt)
                }
            }

            val wordMeanings = mutableListOf<WordMeaning>()
            val wordsArr = jsonRoot.optJSONArray("keyWordMeanings")
            if (wordsArr != null) {
                for (i in 0 until wordsArr.length()) {
                    val wObj = wordsArr.optJSONObject(i) ?: continue
                    val word = wObj.optString("word", "").trim()
                    val meaning = wObj.optString("meaning", "").trim()
                    if (word.isNotBlank() && meaning.isNotBlank()) {
                        wordMeanings.add(WordMeaning(word = word, meaning = meaning))
                    }
                }
            }

            val activity = StructuredActivity(
                type = actObj.optString("type", "SCENARIO"),
                question = actObj.optString("question", "How would you use $conceptTitle in a real situation?"),
                options = optionsList,
                expectedAnswerCriteria = actObj.optString("expectedAnswerCriteria", conceptSummary),
                hint = actObj.optString("hint", "").ifBlank { null }
            )

            val rawHook = jsonRoot.optString("storyHook", "").trim()
            val rawPredPrompt = jsonRoot.optString("predictionPrompt", "").trim()
            val rawMemPic = jsonRoot.optString("memoryPicture", "").trim()
            val rawThread = jsonRoot.optString("storyThreadSummary", "").trim()

            val step = StructuredConceptStep(
                conceptId = jsonRoot.optString("conceptId", conceptId),
                learningObjective = cleanHeading(jsonRoot.optString("learningObjective", "Understand $conceptTitle")),
                conciseExplanation = jsonRoot.optString("conciseExplanation", conceptSummary),
                concreteExample = jsonRoot.optString("concreteExample", "Think of how $conceptTitle shows up in everyday life around you."),
                activity = activity,
                keyWordMeanings = wordMeanings,
                storyHook = if (rawHook.isBlank() || rawHook.equals("null", ignoreCase = true)) null else rawHook,
                predictionPrompt = if (rawPredPrompt.isBlank() || rawPredPrompt.equals("null", ignoreCase = true) || predOptionsList.isEmpty()) null else rawPredPrompt,
                predictionOptions = predOptionsList,
                memoryPicture = if (rawMemPic.isBlank() || rawMemPic.equals("null", ignoreCase = true)) null else rawMemPic,
                storyThreadSummary = if (rawThread.isBlank() || rawThread.equals("null", ignoreCase = true)) null else rawThread
            )
            GeminiResult.Success(step)
        } catch (e: Exception) {
            // Simple-English local recovery if network or API fails
            GeminiResult.Success(
                StructuredConceptStep(
                    conceptId = conceptId,
                    learningObjective = "Understand ${cleanHeading(conceptTitle)}",
                    conciseExplanation = "Let's look at ${cleanHeading(conceptTitle)} in $topic.\n\nIn simple words: $conceptSummary",
                    concreteExample = "Think about a real-life situation at school or at home where $conceptTitle plays a part. That everyday link helps you remember the main point for your exam.",
                    activity = StructuredActivity(
                        type = "SCENARIO",
                        question = "In your own words, why does ${cleanHeading(conceptTitle)} matter in $topic?",
                        options = emptyList(),
                        expectedAnswerCriteria = conceptSummary,
                        hint = "Think back to the main idea we just read above."
                    ),
                    keyWordMeanings = emptyList(),
                    storyHook = null,
                    predictionPrompt = null,
                    predictionOptions = emptyList(),
                    memoryPicture = "Picture one real-life scene where ${cleanHeading(conceptTitle)} makes a clear difference.",
                    storyThreadSummary = "We just covered ${cleanHeading(conceptTitle)}."
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
            Check a $classLevel student's answer like a warm, patient secondary school teacher.
            Context:
            - Subject: $subject
            - Topic: $topic
            - Concept: $conceptTitle
            - Goal: $learningObjective
            - Question Asked: $activityQuestion
            - Key Idea Expected: $expectedAnswerCriteria
            - Student's Answer: "$studentAnswer"
            - Attempt Number: $attemptNumber

            HOW TO RESPOND (SIMPLE ENGLISH + ADAPTIVE TEACHING):
            1. Write ALL feedback ("specificPraise", "explanationOfMistake", "targetedHint") in short, natural everyday English. Never sound like a textbook.
            2. Let the student answer in their OWN simple words. Never demand textbook wording.
            3. Handle what the student actually wrote:
               - If the answer is right or shows clear understanding -> "CORRECT" (score 8-10, conceptCompleted = true, allowsRetry = false, explanationOfMistake = null, targetedHint = null). Say clearly what they got right in "specificPraise".
               - If the student says "I don't understand", asks what a word means, or writes something too short/unclear -> "NEEDS_CLARIFICATION" (score 4, conceptCompleted = false, allowsRetry = true). Explain the confusing idea or word in MUCH simpler everyday words inside "explanationOfMistake", and give a very easy clue in "targetedHint".
               - If the answer got part of it right but missed an important point -> "PARTIALLY_CORRECT" (score 5-7, conceptCompleted = false, allowsRetry = true). Praise what they got right, explain what is missing in 1-2 short simple sentences, and give a helpful "targetedHint".
               - If the answer is wrong -> "INCORRECT" (score 1-4, conceptCompleted = false, allowsRetry = true). Gently explain the mix-up in simple English and give an easy clue in "targetedHint".
            4. If attemptNumber >= 2 and the student is still struggling:
               - Set conceptCompleted = true, allowsRetry = false.
               - Give the clear, simple explanation of the right answer in "explanationOfMistake" so the student understands before moving on.

            Allowed "verdict" values (MUST be one of these exact 4 strings):
            "CORRECT", "PARTIALLY_CORRECT", "INCORRECT", "NEEDS_CLARIFICATION"

            Return strictly a JSON object (always use null for "explanationOfMistake" and "targetedHint" when verdict is "CORRECT"):
            {
              "verdict": "CORRECT",
              "scoreOutOfTen": 9,
              "specificPraise": "Short, warm praise telling them what they got right in simple words",
              "explanationOfMistake": null,
              "targetedHint": null,
              "conceptCompleted": true,
              "allowsRetry": false
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonRoot = parseJsonWithRegeneration(
                apiKey = apiKey,
                rawResponse = responseText,
                schemaHint = """{"verdict": String, "scoreOutOfTen": Int, "specificPraise": String, "explanationOfMistake": String?, "targetedHint": String?, "conceptCompleted": Boolean, "allowsRetry": Boolean}"""
            )

            val verdictStr = jsonRoot.optString("verdict", "CORRECT").trim().uppercase().replace(" ", "_")
            val score = jsonRoot.optInt("scoreOutOfTen", 8)
            val verdict = runCatching { EvaluationVerdict.valueOf(verdictStr) }.getOrElse {
                when {
                    "CLARIF" in verdictStr || "PRACTICE" in verdictStr || "HELP" in verdictStr -> EvaluationVerdict.NEEDS_CLARIFICATION
                    "PARTIAL" in verdictStr -> EvaluationVerdict.PARTIALLY_CORRECT
                    "INCORRECT" in verdictStr || "WRONG" in verdictStr -> EvaluationVerdict.INCORRECT
                    score >= 8 -> EvaluationVerdict.CORRECT
                    score <= 4 -> EvaluationVerdict.NEEDS_CLARIFICATION
                    else -> EvaluationVerdict.PARTIALLY_CORRECT
                }
            }

            val isCompleted = jsonRoot.optBoolean("conceptCompleted", verdict == EvaluationVerdict.CORRECT || attemptNumber >= 2)
            val canRetry = jsonRoot.optBoolean("allowsRetry", !isCompleted && attemptNumber < 2)

            val rawMistake = jsonRoot.optString("explanationOfMistake", "").trim()
            val rawHint = jsonRoot.optString("targetedHint", "").trim()
            val cleanMistake = if (verdict == EvaluationVerdict.CORRECT || rawMistake.isBlank() || rawMistake.equals("null", ignoreCase = true)) null else rawMistake
            val cleanHint = if (verdict == EvaluationVerdict.CORRECT || rawHint.isBlank() || rawHint.equals("null", ignoreCase = true)) null else rawHint

            val eval = StructuredAnswerEvaluation(
                verdict = verdict,
                scoreOutOfTen = jsonRoot.optInt("scoreOutOfTen", if (verdict == EvaluationVerdict.CORRECT) 9 else 6),
                specificPraise = jsonRoot.optString("specificPraise", "Good effort! You shared a clear thought here."),
                explanationOfMistake = cleanMistake,
                targetedHint = cleanHint,
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
                    specificPraise = "Nice try! You explained your answer clearly in your own words.",
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
        studentQuery: String,
        currentConceptExplanation: String?,
        conversationHistory: List<Pair<String, String>>
    ): GeminiResult<TutorInterruptionResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API key missing.", isKeyMissing = true)
        }

        val historyBlock = if (conversationHistory.isEmpty()) {
            "None yet."
        } else {
            conversationHistory.takeLast(3).joinToString("\n") { (q, a) ->
                "- Student asked: \"$q\"\n  Bunde replied: \"$a\""
            }
        }

        val prompt = """
            The $classLevel student paused the lesson to ask you a follow-up question:
            Student Question: "$studentQuery"

            Lesson Context:
            - Subject: $subject
            - Topic: $topic
            - Current Concept: $currentConceptTitle
            - What You Already Explained: ${currentConceptExplanation ?: "Teaching $currentConceptTitle in $topic"}
            - Active Question Waiting for Student: $currentActivityQuestion
            - Earlier Questions in This Step:
            $historyBlock

            HOW TO ANSWER FOLLOW-UP QUESTIONS (CRITICAL):
            1. Keep the EXACT SAME simple, everyday SS2 teacher English. Never switch back to textbook language.
            2. Adapt directly to what the student needs:
               - If they say they don't understand or are confused -> explain it in MUCH simpler everyday words with a fresh, relatable example. NEVER repeat the same complicated sentences from earlier.
               - If they ask what a word means -> give the clear everyday meaning of that exact word first, plus a short example sentence.
               - If they ask for more details or "why?" -> give a deeper explanation while keeping words simple and sentences short.
            3. Keep "explanation" short and clear (2 to 4 short sentences). Keep all exam facts accurate.
            4. Write a warm, natural 1-sentence "bridgeBackToLesson" guiding them smoothly back to the question waiting for them.

            Return strictly a JSON object:
            {
              "explanation": "Simple, clear answer in everyday English that directly helps the student",
              "bridgeBackToLesson": "One short, friendly sentence guiding them back to the question"
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonRoot = parseJsonWithRegeneration(
                apiKey = apiKey,
                rawResponse = responseText,
                schemaHint = """{"explanation": String, "bridgeBackToLesson": String}"""
            )
            val resp = TutorInterruptionResponse(
                explanation = jsonRoot.optString("explanation", "In $subject, $currentConceptTitle simply means how this idea works in everyday life."),
                bridgeBackToLesson = jsonRoot.optString("bridgeBackToLesson", "Now try the question below when you feel ready!")
            )
            GeminiResult.Success(resp)
        } catch (e: Exception) {
            GeminiResult.Success(
                TutorInterruptionResponse(
                    explanation = "Good question! In $topic, ${cleanHeading(currentConceptTitle)} is about the main idea we just looked at—try picturing it in a real everyday situation.",
                    bridgeBackToLesson = "When you feel ready, go ahead and try the question below."
                )
            )
        }
    }

    override suspend fun generateSessionRecap(
        apiKey: String,
        subject: String,
        topic: String,
        classLevel: String,
        completedConcepts: List<String>,
        latestStoryThread: String?
    ): GeminiResult<SessionRecap> = withContext(Dispatchers.IO) {
        val fallback = SessionRecap(
            recapTitle = "Quick Brain Lock-In",
            storySoFar = "You just finished ${completedConcepts.takeLast(3).joinToString(", ")}. ${latestStoryThread ?: "Take a quick breath so your brain saves what you just learned before we move on."}",
            quickTip = "Resting for 1–2 minutes after a few concepts helps your brain lock them into long-term memory."
        )
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Success(fallback)
        }

        val prompt = """
            Give a 30-second "Story So Far" memory recap for a $classLevel student who just completed 3 concepts in $subject ($topic):
            - Concepts Covered: ${completedConcepts.takeLast(3).joinToString(", ")}
            - Latest Story Thread: ${latestStoryThread ?: "Connected lessons in $topic"}

            RULES:
            1. Write 2 to 3 short, friendly sentences in simple English connecting what they learned like a quick "Previously on..." movie recap.
            2. Add 1 short memory tip reminding them why pausing or picturing the scene helps them remember for exams.

            Return strictly JSON:
            {
              "recapTitle": "Short friendly title like 'Story So Far'",
              "storySoFar": "2-3 short connected sentences summarizing the ideas covered.",
              "quickTip": "1 short encouraging sentence about pausing to lock the ideas in."
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(apiKey, prompt, emptyList())
            val jsonRoot = parseJsonWithRegeneration(
                apiKey = apiKey,
                rawResponse = responseText,
                schemaHint = """{"recapTitle": String, "storySoFar": String, "quickTip": String}"""
            )
            GeminiResult.Success(
                SessionRecap(
                    recapTitle = cleanHeading(jsonRoot.optString("recapTitle", fallback.recapTitle)),
                    storySoFar = jsonRoot.optString("storySoFar", fallback.storySoFar),
                    quickTip = jsonRoot.optString("quickTip", fallback.quickTip)
                )
            )
        } catch (_: Exception) {
            GeminiResult.Success(fallback)
        }
    }

    /**
     * Parses Gemini JSON robustly and runs one regeneration/recovery prompt if formatting broke.
     */
    private fun parseJsonWithRegeneration(
        apiKey: String,
        rawResponse: String,
        schemaHint: String
    ): JSONObject {
        val cleaned = extractJsonString(rawResponse)
        return try {
            JSONObject(cleaned)
        } catch (_: Exception) {
            val recoveryPrompt = """
                Fix the text below into a valid JSON object matching this schema: $schemaHint
                Keep all explanations in very simple, everyday English for an SS2 secondary school student.
                Return ONLY valid JSON with no extra text.

                Text to fix:
                $rawResponse
            """.trimIndent()
            val regenerated = executeGeminiPrompt(apiKey, recoveryPrompt, emptyList())
            JSONObject(extractJsonString(regenerated))
        }
    }

    private fun executeGeminiPrompt(
        apiKey: String,
        prompt: String,
        imageBytesList: List<ByteArray>,
        expectJson: Boolean = true
    ): String {
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
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", GLOBAL_TUTOR_SYSTEM_INSTRUCTION)
                            })
                        })
                    })
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", partsArray)
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.35)
                        if (expectJson) {
                            put("responseMimeType", "application/json")
                        }
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
                    if (response.code == 404 || response.code == 400) {
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
                if (e.message?.contains("404") != true && e.message?.contains("400") != true) {
                    throw e
                }
            }
        }

        throw lastException ?: IOException("Failed to reach Gemini API. Please check your key or internet connection.")
    }

    private fun cleanHeading(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val letters = trimmed.filter { it.isLetter() }
        val isAllCaps = letters.isNotEmpty() && letters.all { it.isUpperCase() }
        return if (isAllCaps) {
            trimmed.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            trimmed
        }
    }

    private fun extractJsonString(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json").substringBeforeLast("```").trim()
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```").substringBeforeLast("```").trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace > firstBrace) {
            trimmed.substring(firstBrace, lastBrace + 1)
        } else {
            trimmed
        }
    }
}
