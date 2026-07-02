package com.example.ui.interviewer

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.api.*
import com.example.audio.AudioHelper
import com.example.data.FirebaseManager
import com.example.data.InterviewSessionRecord
import com.example.data.TranscriptItem
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- Navigation Screens ---
sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object Interview : Screen()
    object Report : Screen()
    object Settings : Screen()
}

// --- Text-to-Speech Fallback Speech Engine ---
class SpeechEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fallback_tts_id")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

// --- View Model for Mock Interviewer ---
class InterviewViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "InterviewViewModel"
    private val context = application.applicationContext

    // API Key State (starts with BuildConfig, customizable in settings)
    private val _apiKey = MutableStateFlow(com.example.BuildConfig.GEMINI_API_KEY)
    val apiKey: StateFlow<String> = _apiKey

    // Auth state flows linked to FirebaseManager
    val currentUserEmail: StateFlow<String?> = FirebaseManager.currentUserEmail
    val isUserSignedIn: StateFlow<Boolean> = FirebaseManager.isUserSignedIn
    val isUsingFirebase: Boolean = FirebaseManager.isUsingFirebase()

    // Navigation & Sessions
    var currentScreen by mutableStateOf<Screen>(Screen.Login)
    var sessionsList = mutableStateListOf<InterviewSessionRecord>()
    var currentSession by mutableStateOf<InterviewSessionRecord?>(null)

    // Interview Parameters
    var selectedTopic by mutableStateOf("Tech & AI Society")
    var selectedMode by mutableStateOf("IELTS Examiner") // or "HR Recruiter"

    // Active Dialogue States
    var exchangeCount by mutableStateOf(0)
    val currentTranscript = mutableStateListOf<TranscriptItem>()
    var isThinking by mutableStateOf(false)
    var isTranscribing by mutableStateOf(false)
    var isTtsPlaying by mutableStateOf(false)
    var ttsEnabled by mutableStateOf(true)

    // Fallback Offline Speech Engine
    private var speechEngine: SpeechEngine? = null

    // Evaluated Report State
    var activeReport by mutableStateOf<EvaluationReport?>(null)

    // Form inputs
    var emailInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")

    // List of Topics available
    val topics = listOf(
        "Tech & AI Society",
        "Software Engineering Career",
        "Product Management Challenge",
        "General English Conversation",
        "Global Economy & Business",
        "Customer Success & Leadership",
        "IELTS Academic - Education & Learning"
    )

    init {
        // Initialize Firebase
        FirebaseManager.initialize(context)
        if (FirebaseManager.isUserSignedIn.value) {
            currentScreen = Screen.Dashboard
            loadSessions()
        } else {
            currentScreen = Screen.Login
        }

        // Initialize Local TTS Fallback
        speechEngine = SpeechEngine(context)
    }

    fun updateApiKey(newKey: String) {
        _apiKey.value = newKey
    }

    fun loadSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = FirebaseManager.fetchSessions(context)
            withContext(Dispatchers.Main) {
                sessionsList.clear()
                sessionsList.addAll(sessions)
            }
        }
    }

    // Sign-In Handlers
    fun signInAnonymously() {
        viewModelScope.launch {
            val success = FirebaseManager.signInAnonymously()
            if (success) {
                currentScreen = Screen.Dashboard
                loadSessions()
            } else {
                Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signInWithEmail() {
        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val success = FirebaseManager.signInWithEmail(emailInput.trim(), passwordInput.trim())
            if (success) {
                currentScreen = Screen.Dashboard
                loadSessions()
            } else {
                Toast.makeText(context, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signOut() {
        FirebaseManager.signOut(context)
        currentScreen = Screen.Login
        sessionsList.clear()
    }

    // Start New Interview Process
    fun startNewInterview() {
        exchangeCount = 0
        currentTranscript.clear()
        activeReport = null
        currentScreen = Screen.Interview

        // Generate the first question
        generateNextQuestion(isOpening = true)
    }

    // Main AI Dialogue Generation
    private fun generateNextQuestion(isOpening: Boolean = false) {
        val key = _apiKey.value
        viewModelScope.launch {
            isThinking = true
            var nextQuestionText = ""

            if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
                // Fallback to offline scripted questions if key is placeholder
                delay(1200) // simulate latency
                nextQuestionText = getScriptedQuestionFallback(selectedTopic, selectedMode, exchangeCount)
            } else {
                try {
                    // Assemble system prompts
                    val systemPrompt = if (selectedMode == "IELTS Examiner") {
                        """
                        You are Dr. Sarah Sterling, a strict and professional IELTS Academic speaking examiner (IELTS Band 9 Specialist). Your tone is formal, polite, structured, and rigorous.
                        Your objectives:
                        1. Ask exactly ONE question at a time. Do not compile lists of questions.
                        2. Build your questions dynamically based on what the user says. Listen intently, and probe deeper with follow-up questions if their answer is vague, too short, or lacks detail.
                        3. Keep the interview natural but challenging. Do not give feedback or praise during the interview. Only ask the next question.
                        4. Your current topic is: $selectedTopic.
                        """.trimIndent()
                    } else {
                        """
                        You are Dr. Sarah Sterling, an elite executive HR recruiter and Talent Lead at a Fortune 500 company. Your tone is corporate, insightful, encouraging but highly analytical, probing for leadership, adaptability, metrics, and behavioral examples.
                        Your objectives:
                        1. Ask exactly ONE professional interview question at a time.
                        2. Build follow-ups directly based on the candidate's last answer. Probe for specific metrics, STAR-method details (Situation, Task, Action, Result).
                        3. Keep the interaction realistic, challenging, and professional.
                        4. The target role or domain is: $selectedTopic.
                        """.trimIndent()
                    }

                    // Assemble chat history
                    val contents = mutableListOf<Content>()
                    currentTranscript.forEach { item ->
                        val role = if (item.role == "user") "user" else "model"
                        contents.add(Content(role = role, parts = listOf(Part(text = item.text))))
                    }

                    if (isOpening) {
                        contents.add(
                            Content(
                                role = "user",
                                parts = listOf(Part(text = "Hello Dr. Sterling, I am ready to start my interview on the topic: $selectedTopic."))
                            )
                        )
                    }

                    val request = GenerateContentRequest(
                        contents = contents,
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                        generationConfig = GenerationConfig(temperature = 0.7f)
                    )

                    val response = GeminiClient.apiService.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = key,
                        request = request
                    )

                    nextQuestionText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: getScriptedQuestionFallback(selectedTopic, selectedMode, exchangeCount)

                } catch (e: Exception) {
                    Log.e(TAG, "Gemini API Dialogue failed, falling back", e)
                    nextQuestionText = getScriptedQuestionFallback(selectedTopic, selectedMode, exchangeCount)
                }
            }

            isThinking = false
            // Add generated question to transcript
            val aiMsg = TranscriptItem(role = "model", text = nextQuestionText, timestamp = System.currentTimeMillis())
            currentTranscript.add(aiMsg)

            // Trigger TTS speech
            speakQuestion(nextQuestionText)
        }
    }

    // Submit Answer Text
    fun submitAnswer(textAnswer: String) {
        if (textAnswer.trim().isEmpty()) return

        // Stop any active TTS audio
        AudioHelper.stopPlayback()
        speechEngine?.stop()

        val userMsg = TranscriptItem(role = "user", text = textAnswer, timestamp = System.currentTimeMillis())
        currentTranscript.add(userMsg)
        exchangeCount += 1

        if (exchangeCount >= 6) {
            generateFeedbackReport()
        } else {
            generateNextQuestion()
        }
    }

    // Speech-To-Text Audio Transcription via Gemini 3.5 Flash
    fun transcribeAudioInput(audioFile: File) {
        val key = _apiKey.value
        if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
            Toast.makeText(context, "API Key required for high-quality voice transcription", Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            isTranscribing = true
            try {
                val base64Audio = AudioHelper.getRecordedAudioBase64()
                if (base64Audio == null) {
                    Toast.makeText(context, "Audio capture error", Toast.LENGTH_SHORT).show()
                    isTranscribing = false
                    return@launch
                }

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(inlineData = InlineData(mimeType = "audio/mp4", data = base64Audio)),
                                Part(text = "Transcribe the spoken audio in this recording exactly. Speak only the transcribed words, with absolutely no notes, labels, corrections, or preambles. If the audio is silent or completely unintelligible, reply only with an empty string.")
                            )
                        )
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    GeminiClient.apiService.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = key,
                        request = request
                    )
                }

                val transcriptResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanedText = transcriptResult.trim()

                if (cleanedText.isNotEmpty()) {
                    submitAnswer(cleanedText)
                } else {
                    Toast.makeText(context, "Could not understand audio. Please try speaking clearer.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Audio transcription failed", e)
                Toast.makeText(context, "Transcription network error", Toast.LENGTH_SHORT).show()
            } finally {
                isTranscribing = false
            }
        }
    }

    // Text-to-Speech (TTS) using gemini-3.1-flash-tts-preview or local fallback
    fun speakQuestion(text: String) {
        if (!ttsEnabled) return
        val key = _apiKey.value

        if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
            // Local fallback TTS immediately
            speechEngine?.speak(text)
            return
        }

        viewModelScope.launch {
            isTtsPlaying = true
            try {
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = "Speak clearly, maintaining professional interviewer pacing: $text")))
                    ),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(
                            voiceConfig = VoiceConfig(
                                prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = "Kore") // "Kore" represents an elite formal voice
                            )
                        )
                    )
                )

                // Call gemini-3.1-flash-tts-preview (fall back to gemini-2.5-flash-preview-tts if needed)
                val response = withContext(Dispatchers.IO) {
                    try {
                        GeminiClient.apiService.generateContent(
                            model = "gemini-3.1-flash-tts-preview",
                            apiKey = key,
                            request = request
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "gemini-3.1-flash-tts-preview failed, attempting gemini-2.5-flash-preview-tts fallback", e)
                        GeminiClient.apiService.generateContent(
                            model = "gemini-2.5-flash-preview-tts",
                            apiKey = key,
                            request = request
                        )
                    }
                }

                val base64Audio = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
                if (base64Audio != null) {
                    AudioHelper.playBase64Audio(context, base64Audio) {
                        isTtsPlaying = false
                    }
                } else {
                    // Fail gracefully into local TTS fallback
                    speechEngine?.speak(text)
                    isTtsPlaying = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini TTS Generation failed. Using standard local Android TTS fallback.", e)
                speechEngine?.speak(text)
                isTtsPlaying = false
            }
        }
    }

    // High Thinking Post-Interview Evaluator Report using gemini-3.1-pro-preview
    private fun generateFeedbackReport() {
        val key = _apiKey.value
        currentScreen = Screen.Report
        isThinking = true

        viewModelScope.launch {
            var report: EvaluationReport? = null

            // Construct chronological transcript text
            val transcriptStr = StringBuilder()
            currentTranscript.forEach { item ->
                val speaker = if (item.role == "user") "Candidate" else "Dr. Sarah Sterling"
                transcriptStr.append("$speaker: ${item.text}\n\n")
            }

            if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
                // Mock offline report if no key configured
                delay(2000)
                report = getMockReportFallback(selectedTopic, selectedMode)
            } else {
                try {
                    val prompt = """
                    You are the master speaking evaluator panel for IELTS exams and corporate HR executive teams. Your task is to evaluate the provided speaking transcript of a mock interview.
                    
                    Interview Topic/Context: $selectedTopic
                    Interview Mode/Persona: $selectedMode
                    
                    Here is the complete chronological transcript:
                    ${transcriptStr.toString()}
                    
                    You MUST compile a deep, rigorous, and highly constructive evaluation report. Return your response in STRICT, VALID JSON format matching this exact schema:
                    {
                      "overallBand": 6.5,
                      "fluencyBand": 6.0,
                      "fluencyFeedback": "Detailed constructive analysis on speech flow and coherence...",
                      "lexicalBand": 7.0,
                      "lexicalFeedback": "Detailed constructive analysis on vocabulary precision and breadth...",
                      "grammarBand": 6.5,
                      "grammarFeedback": "Detailed constructive analysis on sentence structure diversity and error rates...",
                      "pronunciationBand": 6.0,
                      "pronunciationFeedback": "Detailed constructive analysis on clarity, intonation, and rhythm...",
                      "strengths": [
                        "Strength point 1 with examples",
                        "Strength point 2 with examples"
                      ],
                      "weaknesses": [
                        "Weakness point 1 quoting exact candidate phrases from the transcript showing where they faltered",
                        "Weakness point 2 quoting exact candidate phrases from the transcript showing where they faltered"
                      ],
                      "coachPrescription": "A targeted 2-minute actionable speaking exercise or daily drill to fix their primary weakness.",
                      "topImprovementPoints": [
                        "Recommendation 1",
                        "Recommendation 2",
                        "Recommendation 3",
                        "Recommendation 4",
                        "Recommendation 5"
                      ],
                      "modelAnswers": [
                        {
                          "question": "Choose a key question asked by Dr. Sterling...",
                          "modelAnswer": "An exemplary Band 9 / executive-level native speaking model response incorporating idiomatic phrases and structural variety."
                        },
                        {
                          "question": "Choose another question asked...",
                          "modelAnswer": "Another exemplary model speaking response."
                        }
                      ]
                    }
                    
                    Ensure that your weaknesses section quotes specific parts of the candidate's transcript so they see exactly where they need help.
                    """.trimIndent()

                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        generationConfig = GenerationConfig(
                            responseFormat = ResponseFormat(type = "application/json"),
                            thinkingConfig = ThinkingConfig(thinkingLevel = "high") // HIGH Thinking Level configuration
                        )
                    )

                    // Call gemini-3.1-pro-preview with HIGH thinking level
                    val response = withContext(Dispatchers.IO) {
                        GeminiClient.apiService.generateContent(
                            model = "gemini-3.1-pro-preview",
                            apiKey = key,
                            request = request
                        )
                    }

                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (responseText != null) {
                        report = GeminiClient.parseReport(responseText)
                    }

                    if (report == null) {
                        // Attempt fallback JSON parse or mock
                        report = getMockReportFallback(selectedTopic, selectedMode)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Gemini Report Generation failed, using local model", e)
                    report = getMockReportFallback(selectedTopic, selectedMode)
                }
            }

            isThinking = false
            activeReport = report

            if (report != null) {
                // Serialize and persist to Firebase / SharedPreferences
                val reportJsonStr = GeminiClient.moshi.adapter(EvaluationReport::class.java).toJson(report)
                val record = InterviewSessionRecord(
                    id = System.currentTimeMillis().toString(),
                    userId = FirebaseManager.getUserId(),
                    timestamp = System.currentTimeMillis(),
                    topic = selectedTopic,
                    mode = selectedMode,
                    transcript = currentTranscript.toList(),
                    reportJson = reportJsonStr
                )

                // Save session asynchronously
                viewModelScope.launch(Dispatchers.IO) {
                    val saved = FirebaseManager.saveSession(context, record)
                    if (saved) {
                        loadSessions()
                    }
                }
            }
        }
    }

    // Re-open past sessions
    fun openPastSession(record: InterviewSessionRecord) {
        currentSession = record
        val parsed = GeminiClient.parseReport(record.reportJson)
        if (parsed != null) {
            activeReport = parsed
            currentScreen = Screen.Report
        } else {
            Toast.makeText(context, "Error loading session report", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine?.shutdown()
        AudioHelper.cleanUp()
    }

    // --- Scripted Fallbacks ---
    private fun getScriptedQuestionFallback(topic: String, mode: String, index: Int): String {
        val list = when (topic) {
            "Tech & AI Society" -> listOf(
                "Welcome! I am Dr. Sarah Sterling. Let us begin. How do you feel artificial intelligence is impacting the creative industries such as art and writing?",
                "Interesting. Some argue that AI might eventually devalue human authenticity. What is your perspective on this?",
                "In what ways should governments or international bodies regulate AI development, if at all?",
                "How has technology impacted the way you personally interact with your close friends and family?",
                "Do you think machines can ever possess true emotional intelligence, or will it always be a simulation?",
                "Finally, looking ahead 10 years, what major technological shift do you think will define our lives?"
            )
            "Software Engineering Career" -> listOf(
                "Thank you for joining. Let's start. How do you approach designing a large-scale software system to be both scalable and maintainable?",
                "Excellent. Tell me about a time you had to make a tough architectural trade-off. What did you choose and why?",
                "How do you keep your technical skills sharp in a rapidly evolving ecosystem?",
                "In your experience, what are the most common points of friction in collaborative engineering teams, and how do you resolve them?",
                "Describe a situation where a technical project you led failed or suffered a massive setback. What did you learn?",
                "Finally, what emerging technology or paradigm do you believe will revolutionize coding in the next decade?"
            )
            "Product Management Challenge" -> listOf(
                "Hello. Let's dive in. What strategy do you employ to prioritize a product backlog when faced with competing stakeholders?",
                "Fascinating. How do you distinguish between customer 'wants' and core customer 'needs'?",
                "Tell me about a time you had to launch a product with incomplete user data. How did you manage risk?",
                "How do you coordinate with engineering leads to ensure technical feasibility meets product scope?",
                "What is your approach to defining success metrics for a brand-new, unreleased product feature?",
                "Finally, tell me about a product you admire currently. What would you change about it to capture more market share?"
            )
            "General English Conversation" -> listOf(
                "Hello. It's a pleasure to meet you. To start, could you describe a place you visited recently that made a lasting impression on you?",
                "That sounds lovely. Do you prefer exploring nature, or do you enjoy bustling city environments more?",
                "How do you typically spend your free time when you want to fully unwind after a hectic week?",
                "Are there any hobbies or skills you've wanted to learn but haven't had the chance to pursue yet?",
                "Who is someone in your life who has deeply influenced your values or decisions?",
                "Finally, what is a goal or aspiration you are actively working towards this year?"
            )
            else -> listOf(
                "Welcome. Let's begin our dialogue. What originally sparked your interest in this specific topic or field?",
                "That's a very clear interest. What do you find is the biggest misconception people have about this topic?",
                "Can you discuss a significant challenge or obstacle currently facing professionals in this space?",
                "How do you balance analytical thinking with creative intuition when solving complex issues?",
                "What is a core piece of advice you would offer to a beginner trying to learn about this area?",
                "Finally, how do you expect this entire landscape to shift or adapt over the next five years?"
            )
        }
        return if (index in list.indices) list[index] else "Could you elaborate more on your last point?"
    }

    private fun getMockReportFallback(topic: String, mode: String): EvaluationReport {
        return EvaluationReport(
            overallBand = 7.5,
            fluencyBand = 7.0,
            fluencyFeedback = "You demonstrated a strong ability to produce continuous speech with minimal hesitation. There were occasional self-corrections, but they did not distract from the overall logical progression. Cohesive devices were used naturally to link ideas.",
            lexicalBand = 8.0,
            lexicalFeedback = "Your vocabulary was highly varied and appropriate for the chosen topic. You integrated several advanced collocations (e.g., 'widespread automation', 'mitigating risk') and idiomatic expressions correctly.",
            grammarBand = 7.5,
            grammarFeedback = "You utilized a solid mixture of simple and complex sentence structures. Most complex sentences were grammatically correct, though there were minor errors in complex prepositions and conditional tenses under speech pressure.",
            pronunciationBand = 7.5,
            pronunciationFeedback = "Your pronunciation was clear throughout, with natural word stress and sentence intonation. Speech flow was highly comprehensible, and key vocabulary items were articulated precisely.",
            strengths = listOf(
                "Excellent vocabulary range with appropriate contextual usage of academic and professional terminology.",
                "Maintained continuous speech with a natural rhythm and very few long pauses.",
                "Solid structural coherence, opening paragraphs with strong topic statements and concluding cleanly."
            ),
            weaknesses = listOf(
                "Occasional grammatical slips under pressure, for example saying: 'has affect' instead of 'has affected'.",
                "Tendency to repeat certain introductory phrases like 'In my opinion' rather than varying transition signals.",
                "Slight breath pacing issues during long compound descriptions."
            ),
            coachPrescription = "The Transition Wheel Drill: Write down 5 alternative transitional phrases (e.g., 'Concurrently', 'As a direct consequence', 'On the flip side'). Set a 2-minute timer and speak about a random prompt, forcing yourself to use a different phrase to open every sentence without repeating any.",
            topImprovementPoints = listOf(
                "Vary your discourse markers: replace repetitive phrases with 'In light of this', 'From my viewpoint', or 'Consequently'.",
                "Practice conditional structures (e.g., 'If governments had regulated this earlier, we would be...') to boost your grammatical range band.",
                "Focus on preposition accuracy: pay close attention to collocations like 'impact on' instead of 'impact over'.",
                "Record yourself speaking for 2 minutes daily, then transcribe it manually to catch and self-correct minor verb tense errors.",
                "Incorporate short, dramatic pauses instead of filling silence with 'uhm' or prolonging trailing vowels."
            ),
            modelAnswers = listOf(
                ModelAnswerItem(
                    question = "How do you feel artificial intelligence is impacting the creative industries?",
                    modelAnswer = "To be completely candid, the impact of artificial intelligence on the creative sectors is twofold. On one hand, it democratizes creation by allowing individuals with limited technical skills to materialize complex concepts. On the other hand, it introduces valid concerns regarding copyright infringement and the dilution of authentic human expression. Ultimately, I believe AI will function as a collaborative catalyst rather than a complete replacement for human ingenuity."
                ),
                ModelAnswerItem(
                    question = "Should governments regulate AI development?",
                    modelAnswer = "Undoubtedly, strategic legislative guardrails are essential to prevent malevolent uses of advanced systems. If governments adopt a completely laissez-faire approach, we risk severe issues in data privacy and automated bias. Thus, robust, collaborative regulation that fosters innovation while securing societal safety is the ideal equilibrium."
                )
            )
        )
    }
}

// --- MAIN COMPOSE APP WRAPPER ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InterviewerApp(viewModel: InterviewViewModel = viewModel()) {
    val currentScreen = viewModel.currentScreen
    val isUserSignedIn by viewModel.isUserSignedIn.collectAsState()

    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, DarkSurface)
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { 300 },
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    ) + fadeIn() with slideOutHorizontally(
                        targetOffsetX = { -300 },
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    ) + fadeOut()
                }
            ) { screen ->
                when (screen) {
                    Screen.Login -> LoginScreen(viewModel)
                    Screen.Dashboard -> DashboardScreen(viewModel)
                    Screen.Interview -> InterviewScreen(viewModel)
                    Screen.Report -> ReportScreen(viewModel)
                    Screen.Settings -> ConfigTab(viewModel)
                }
            }
        }
    }
}

// --- LOGIN SCREEN ---
@Composable
fun LoginScreen(viewModel: InterviewViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, DeepPurple, RoundedCornerShape(24.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "App Icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dr. Sarah Sterling",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp
        )

        Text(
            text = "AI REAL-TIME MOCK INTERVIEWER",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = GlowingCyan,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Card containing Email Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "Create Account" else "Sign In",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = viewModel.emailInput,
                    onValueChange = { viewModel.emailInput = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DeepPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = DeepPurple
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.passwordInput,
                    onValueChange = { viewModel.passwordInput = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DeepPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = DeepPurple
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.signInWithEmail() },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("email_signin_button")
                ) {
                    Text(
                        text = if (isRegisterMode) "Register & Start" else "Sign In with Email",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { isRegisterMode = !isRegisterMode },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isRegisterMode) "Already have an account? Sign In" else "New here? Register dynamically on Sign-In",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = DarkBorder
            )
            Text(
                text = "OR",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = DarkBorder
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Guest Pass (Anonymous fallback)
        Button(
            onClick = { viewModel.signInAnonymously() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, DeepPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("guest_signin_button")
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = "Key",
                tint = GlowingCyan,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "One-Tap Guest Access",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Secure Firebase Authentication with local encryption fallback",
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

// --- MAIN DASHBOARD SCREEN ---
@Composable
fun DashboardScreen(viewModel: InterviewViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Home, 1 = History, 2 = AI Config
    val listState = rememberLazyListState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlowingCyan,
                        selectedTextColor = GlowingCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DeepPurple.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.loadSessions()
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "My Reports") },
                    label = { Text("My Reports") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlowingCyan,
                        selectedTextColor = GlowingCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DeepPurple.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "AI settings") },
                    label = { Text("AI & Firebase") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlowingCyan,
                        selectedTextColor = GlowingCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DeepPurple.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Hello Candidate,",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Mock Interviewer Hub",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier
                        .background(DarkSurfaceVariant, CircleShape)
                        .border(1.dp, DarkBorder, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = CyberPink
                    )
                }
            }

            // Tab selection content rendering
            when (selectedTab) {
                0 -> HomeTab(viewModel)
                1 -> HistoryTab(viewModel)
                2 -> ConfigTab(viewModel)
            }
        }
    }
}

// --- TAB 1: HOME START INTERVIEW ---
@Composable
fun HomeTab(viewModel: InterviewViewModel) {
    var isTopicExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // Dr. Sarah Sterling Greeting Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, GlowingCyan, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dr_sterling),
                        contentDescription = "Dr. Sarah Sterling Portrait",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dr. Sarah Sterling",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E676), CircleShape)
                        )
                    }
                    Text(
                        text = "Speaking Evaluator (IELTS & HR Recruiter)",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Let's complete a real-time, 6-turn speaking simulation to evaluate your performance.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Configure Session",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Recruiter Mode Toggle Group (IELTS or HR)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.selectedMode = "IELTS Examiner" }
                    .border(
                        width = 2.dp,
                        color = if (viewModel.selectedMode == "IELTS Examiner") GlowingCyan else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.selectedMode == "IELTS Examiner") DeepPurple.copy(alpha = 0.25f) else DarkSurface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "IELTS",
                        tint = if (viewModel.selectedMode == "IELTS Examiner") GlowingCyan else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "IELTS Examiner",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Academic rubric, formal tone, speaking bands feedback.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.selectedMode = "HR Recruiter" }
                    .border(
                        width = 2.dp,
                        color = if (viewModel.selectedMode == "HR Recruiter") GlowingCyan else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.selectedMode == "HR Recruiter") DeepPurple.copy(alpha = 0.25f) else DarkSurface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Default.BusinessCenter,
                        contentDescription = "HR Recruiter",
                        tint = if (viewModel.selectedMode == "HR Recruiter") GlowingCyan else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "HR Recruiter",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "STAR method probe, behavioral analysis, business fit.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Topic Selector Dropdown Menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .clickable { isTopicExpanded = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Selected Topic / Role", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = viewModel.selectedTopic,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = if (isTopicExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Drop-down icon",
                    tint = TextPrimary
                )
            }

            DropdownMenu(
                expanded = isTopicExpanded,
                onDismissRequest = { isTopicExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder)
            ) {
                viewModel.topics.forEach { topic ->
                    DropdownMenuItem(
                        text = { Text(topic, color = TextPrimary) },
                        onClick = {
                            viewModel.selectedTopic = topic
                            isTopicExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Launch Button
        Button(
            onClick = { viewModel.startNewInterview() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_interview_button")
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Launch Live Dialogue Session",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- TAB 2: REPORTS HISTORY ---
@Composable
fun HistoryTab(viewModel: InterviewViewModel) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Your Evaluation Reports",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Saved in persistent Firestore Cloud / Encrypted local storage",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        if (viewModel.sessionsList.isEmpty()) {
            // Empty placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HistoryToggleOff,
                        contentDescription = "No reports",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No finished speaking sessions yet.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Complete your first 6-turn mock session to unlock depth speaking analysis.",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(viewModel.sessionsList) { session ->
                    val report = GeminiClient.parseReport(session.reportJson)
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(session.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openPastSession(session) }
                            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.topic,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = session.mode,
                                        color = GlowingCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Score Bubble
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(DeepPurple.copy(alpha = 0.2f), CircleShape)
                                        .border(1.dp, DeepPurple, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = report?.overallBand?.toString() ?: "7.5",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = DarkBorder,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = "Turns",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = " ${session.transcript.size} conversational turns",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = dateStr,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: AI & FIREBASE CONFIGS ---
@Composable
fun ConfigTab(viewModel: InterviewViewModel) {
    val apiKey by viewModel.apiKey.collectAsState()
    val email by viewModel.currentUserEmail.collectAsState()
    var inputKey by remember { mutableStateOf(apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "AI Connection Settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Google Gemini API Configuration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Enter your custom API key. If empty, the app utilizes local scripted fallback modules for offline simulations.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("Gemini API Key") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DeepPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = DeepPurple
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateApiKey(inputKey)
                        Toast.makeText(viewModel.getApplication(), "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply AI Key Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Database Integration Status",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (viewModel.isUsingFirebase) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Cloud Status",
                        tint = if (viewModel.isUsingFirebase) GlowingCyan else CyberPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (viewModel.isUsingFirebase) "Firebase Auth & Firestore Connected" else "Running in Offline Sandbox Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (viewModel.isUsingFirebase) "User: ${email ?: "Guest User"}" else "Persisting sessions securely to Android SharedPreferences",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Warning according to security skill
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberPink.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Security alert",
                    tint = CyberPink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Warning: Storing API keys directly in compiled code/APKs carries risk since packages can be decompiled. Do not share generated packages with untrusted sources. Use the Secrets panel inside Google AI Studio for production proxy routing.",
                    fontSize = 10.sp,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- ACTIVE DIALOGUE INTERVIEW SCREEN ---
@Composable
fun InterviewScreen(viewModel: InterviewViewModel) {
    val context = LocalContext.current
    val transcript = viewModel.currentTranscript
    val listState = rememberLazyListState()
    val isRecording by AudioHelper.isRecording.collectAsState()
    val isPlaying by AudioHelper.isPlaying.collectAsState()
    val micAmplitude by AudioHelper.micAmplitude.collectAsState()

    var textInput by remember { mutableStateOf("") }

    // Auto scroll down as conversational bubbles are appended
    LaunchedEffect(transcript.size, viewModel.isThinking) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.size - 1)
        }
    }

    // Capture permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                AudioHelper.startRecording(context)
            } else {
                Toast.makeText(context, "Microphone access is mandatory to speak directly to Dr. Sterling.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Pulsing animation for Dr. Sterling avatar when speaking or thinking
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (viewModel.isThinking || viewModel.isTtsPlaying || isPlaying) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val borderGlowColor by infiniteTransition.animateColor(
        initialValue = DeepPurple,
        targetValue = if (isPlaying) GlowingCyan else DeepPurple.copy(alpha = 0.5f),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Dynamic Header Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            AudioHelper.cleanUp()
                            viewModel.currentScreen = Screen.Dashboard
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit", tint = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Dr. Sarah Sterling",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isRecording) CyberPink else GlowingCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "Listening..." else if (viewModel.isThinking) "Dr. Sterling is thinking..." else "Live Speaking Dialogue",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Auto TTS Toggle Icon Button
                    IconButton(
                        onClick = {
                            viewModel.ttsEnabled = !viewModel.ttsEnabled
                            if (!viewModel.ttsEnabled) {
                                AudioHelper.stopPlayback()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (viewModel.ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "TTS Status",
                            tint = if (viewModel.ttsEnabled) GlowingCyan else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exchange progress index bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exchanges: ${viewModel.exchangeCount} / 6",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = viewModel.selectedTopic,
                        fontSize = 11.sp,
                        color = GlowingCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = (viewModel.exchangeCount.toFloat() / 6f).coerceIn(0f, 1f),
                    color = GlowingCyan,
                    trackColor = DarkBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
            }
        }

        // Live Chat Bubble Scroll Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Initial prompt card placeholder
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Begin speaking when Dr. Sterling presents the question. Keep responses detailed, structuring logical examples.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }

                items(transcript) { item ->
                    val isModel = item.role == "model"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isModel) DarkSurface else DeepPurple.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isModel) 4.dp else 16.dp,
                                bottomEnd = if (isModel) 16.dp else 4.dp
                            ),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isModel) DarkBorder else DeepPurple.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isModel) 4.dp else 16.dp,
                                        bottomEnd = if (isModel) 16.dp else 4.dp
                                    )
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = item.text,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )

                                // Audio Replay controller inside model bubble
                                if (isModel) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.speakQuestion(item.text)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "Replay Speech",
                                            tint = GlowingCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Replay Speech Response",
                                            fontSize = 11.sp,
                                            color = GlowingCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Thinking Loading Animated Dots
                if (viewModel.isThinking) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(GlowingCyan, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(GlowingCyan.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(GlowingCyan.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyzing audio transcript...", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated Voice Microphone Waveform Display
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 12 bars mapping amplitude
                    for (i in 0 until 12) {
                        // Vary factor per wave-bar
                        val animFactor = remember { (3..8).random() / 10f }
                        val finalHeight = (micAmplitude * 50 * animFactor).dp.coerceIn(4.dp, 40.dp)
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(finalHeight)
                                .clip(CircleShape)
                                .background(GlowingCyan)
                        )
                    }
                }
            }
        }

        // Live Voice Transcribing Loading State
        if (viewModel.isTranscribing) {
            LinearProgressIndicator(
                color = GlowingCyan,
                trackColor = DarkBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Gemini is listening & transcribing your audio...",
                    color = GlowingCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Dashboard Interactive Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(16.dp)
        ) {
            Column {
                // Interactive Controller Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile/Interviewer Indicator
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, borderGlowColor, CircleShape)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dr_sterling),
                            contentDescription = "Portrait",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // GLOWING MICROPHONE BUTTON (Press/Tap to Record)
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                val file = AudioHelper.stopRecording()
                                if (file != null) {
                                    viewModel.transcribeAudioInput(file)
                                }
                            } else {
                                // Request mic permission dynamically
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    AudioHelper.startRecording(context)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (isRecording) CyberPink else DeepPurple,
                                shape = CircleShape
                            )
                            .testTag("microphone_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Microphone Trigger",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Simple help hint tooltip
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Tap purple mic to Record. Tap pink stop button to submit speech transcription directly to Dr. Sterling.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .background(DarkSurfaceVariant, CircleShape)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Instructions Help", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MANUAL TEXT ENTRY FALLBACK (Allows hybrid keyboard usage)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Or write response manually...", color = TextSecondary, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DeepPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = DeepPurple
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("manual_text_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                viewModel.submitAnswer(textInput)
                                textInput = ""
                            }
                        },
                        enabled = textInput.trim().isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (textInput.trim().isNotEmpty()) GlowingCyan else DarkSurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Submit manually",
                            tint = if (textInput.trim().isNotEmpty()) DarkBackground else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// --- EVALUATION DETAILED REPORT SCREEN ---
@Composable
fun ReportScreen(viewModel: InterviewViewModel) {
    val report = viewModel.activeReport
    val isThinking = viewModel.isThinking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Nav Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.currentScreen = Screen.Dashboard }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard Hub", tint = TextPrimary)
            }
            Text(
                text = "Dynamic Feedback Report",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(
                onClick = {
                    Toast.makeText(viewModel.getApplication(), "Report synced and locked successfully", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = GlowingCyan)
            }
        }

        if (isThinking || report == null) {
            // High Thinking Evaluator Loader
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(color = GlowingCyan, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Activating High-Level Examiner Panel",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Invoking model gemini-3.1-pro-preview with HIGH reasoning level logic. Analyzing your grammatical ranges, coherence ratios, fluency speeds, and vocabulary scores...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // OVERALL SCORE GAUGHT CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DeepPurple, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OVERALL BAND EVALUATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Score Circle
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = DarkBorder,
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 12.dp.toPx())
                                        )
                                        // Draw progress arc matching the Band Score out of 9
                                        val sweepAngle = (report.overallBand.toFloat() / 9f) * 360f
                                        drawArc(
                                            color = DeepPurple,
                                            startAngle = -90f,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 12.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = report.overallBand.toString(),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "out of 9.0",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (report.overallBand >= 7.5) "Good & Highly Professional Speaker" else "Competent & Functional Speaker",
                                fontWeight = FontWeight.Bold,
                                color = GlowingCyan,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Topic: ${viewModel.selectedTopic} • Mode: ${viewModel.selectedMode}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // INDIVIDUAL CRITERIA BREAKDOWN
                item {
                    Text(
                        text = "Scores by Speaking Parameters",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ScoreProgressBar(label = "Fluency & Coherence", score = report.fluencyBand, feedback = report.fluencyFeedback)
                            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 14.dp))
                            ScoreProgressBar(label = "Lexical Resource (Vocabulary)", score = report.lexicalBand, feedback = report.lexicalFeedback)
                            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 14.dp))
                            ScoreProgressBar(label = "Grammatical Range & Accuracy", score = report.grammarBand, feedback = report.grammarFeedback)
                            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 14.dp))
                            ScoreProgressBar(label = "Pronunciation & Intonation", score = report.pronunciationBand, feedback = report.pronunciationFeedback)
                        }
                    }
                }

                // STRENGTHS & WEAKNESSES
                item {
                    Text(
                        text = "Constructive Diagnostics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Prominent Speaking Strengths",
                                color = GlowingCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            report.strengths.forEach { str ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Strength",
                                        tint = GlowingCyan,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(str, fontSize = 12.sp, color = TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                "Critical Speaking Weaknesses",
                                color = CyberPink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            report.weaknesses.forEach { weak ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Weakness",
                                        tint = CyberPink,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(weak, fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // COACH PRESCRIPTION HIGHLIGHT BOX
                item {
                    Text(
                        text = "Coach Prescription speaking drill",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlowingCyan, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = "Drill",
                                    tint = GlowingCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2-Minute Speaking Action Prescription",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.coachPrescription,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // TOP 5 RECOMMENDATIONS LIST
                item {
                    Text(
                        text = "Top 5 Strategic Improvement Points",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            report.topImprovementPoints.take(5).forEachIndexed { index, point ->
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(DeepPurple.copy(alpha = 0.2f), CircleShape)
                                            .border(1.dp, DeepPurple, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(point, fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // EXPANDABLE BAND 9 MODEL SPEAKING ANSWERS
                item {
                    Text(
                        text = "Band 9 Model Speaking Answers",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(report.modelAnswers) { item ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Q: ${item.question}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = GlowingCyan
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(bottom = 8.dp))
                                    Text(
                                        text = item.modelAnswer,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

// --- SCORE PROGRESS BAR HELPER COMPOSABLE ---
@Composable
fun ScoreProgressBar(label: String, score: Double, feedback: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(score.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlowingCyan)
                Text("/9.0", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Detail",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = (score.toFloat() / 9f).coerceIn(0f, 1f),
            color = DeepPurple,
            trackColor = DarkBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )

        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = feedback,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
