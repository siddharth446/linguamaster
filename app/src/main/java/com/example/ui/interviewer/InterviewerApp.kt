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
import androidx.compose.ui.draw.*
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
import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.api.*
import com.example.audio.AudioHelper
import com.example.audio.SpeechRecognizerHelper
import com.example.data.*
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

// --- Video Hub Data Models ---
data class VideoComment(
    val id: String = UUID.randomUUID().toString(),
    val userName: String,
    val commentText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PracticeVideo(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String,
    val duration: String,
    val videoUrl: String,
    var likes: Int = 0,
    val comments: MutableList<VideoComment> = mutableStateListOf(),
    val uploader: String = "Rahul (Owner)",
    var isLiked: Boolean = false,
    var aiFeedbackReport: EvaluationReport? = null
)

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

    fun setVoiceGender(gender: String) {
        if (!isReady) return
        try {
            val voices = tts?.voices ?: return
            var targetVoices = voices.filter { it.locale.language == Locale.US.language }
            if (targetVoices.isEmpty()) {
                targetVoices = voices.filter { it.locale.language == "en" }
            }
            if (targetVoices.isEmpty()) {
                targetVoices = voices.toList()
            }
            
            if (targetVoices.isNotEmpty()) {
                val match = targetVoices.find { voice ->
                    val name = voice.name.lowercase()
                    if (gender == "Male") {
                        name.contains("male") && !name.contains("female")
                    } else {
                        name.contains("female") || name.contains("f0") || name.contains("girl")
                    }
                }
                if (match != null) {
                    tts?.voice = match
                } else {
                    // fallback to index/sorting to guarantee distinction if names do not match
                    val sorted = targetVoices.sortedBy { it.name }
                    if (gender == "Male" && sorted.size > 1) {
                        tts?.voice = sorted[0]
                    } else if (sorted.size > 0) {
                        tts?.voice = sorted[sorted.size - 1]
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun speak(text: String, gender: String = "Female", rate: Float = 1.0f) {
        if (isReady) {
            try {
                tts?.setSpeechRate(rate)
                setVoiceGender(gender)
            } catch (e: Exception) {
                // ignore tts param failures
            }
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
    var customTopicInput by mutableStateOf("")
    var selectedMode by mutableStateOf("IELTS Examiner") // or "HR Recruiter"

    // Active Dialogue States
    var exchangeCount by mutableStateOf(0)
    val currentTranscript = mutableStateListOf<TranscriptItem>()
    var isThinking by mutableStateOf(false)
    var isTranscribing by mutableStateOf(false)
    var isTtsPlaying by mutableStateOf(false)
    var ttsEnabled by mutableStateOf(true)

    // Lifted SelectedTab state for tracking active section time and managing tabs
    var selectedTab by mutableStateOf(0)

    // Voice and Settings customization
    var voiceGender by mutableStateOf("Female")
    var speechRate by mutableStateOf(1.0f)
    var interviewDifficulty by mutableStateOf("Medium")
    var voiceEngine by mutableStateOf("Standard")

    // Real per-section spent time tracker
    var timeSpentInterview by mutableStateOf(0L)
    var timeSpentListening by mutableStateOf(0L)
    var timeSpentReading by mutableStateOf(0L)
    var timeSpentWriting by mutableStateOf(0L)

    // Track active sub-tab in PracticeTab
    var activePracticeCategory by mutableStateOf("listening")
    var courseSpeechDraft by mutableStateOf("")

    // Fallback Offline Speech Engine
    var speechEngine: SpeechEngine? = null

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

    // --- Real-time Stats & Live Global Analytics ---
    var globalTimeSpentHours by mutableStateOf(14298.5421)
    var activeUsersCount by mutableStateOf(2415)
    var personalTimeSpentSeconds by mutableStateOf(0L)
    var isTrackingTime by mutableStateOf(false)
    var currentSessionSeconds by mutableStateOf(0L)

    data class LiveActivity(
        val candidate: String,
        val country: String,
        val action: String,
        val timeAgo: String,
        val score: String? = null
    )

    val liveActivities = mutableStateListOf(
        LiveActivity("Rahul S.", "India", "started Software Engineering mock exam", "Just now"),
        LiveActivity("Emily W.", "UK", "completed IELTS Speaking Part 1", "2 mins ago", "Band 8.0"),
        LiveActivity("Akinyi O.", "Kenya", "practicing Deep-Sea Reading comprehension", "5 mins ago"),
        LiveActivity("Carlos M.", "Spain", "analyzed Writing task 2 with AI", "9 mins ago", "Score 9/10"),
        LiveActivity("Hiroshi K.", "Japan", "started Leadership interview", "12 mins ago"),
        LiveActivity("Sarah P.", "USA", "completed HR Recruiter simulation", "15 mins ago", "Hired!")
    )

    // --- Video Hub Live State ---
    val videosList = mutableStateListOf<PracticeVideo>()
    var isVideoAnalyzing by mutableStateOf(false)
    var activeVideoReport by mutableStateOf<EvaluationReport?>(null)

    fun initVideosList() {
        if (videosList.isNotEmpty()) return
        
        // Add Rahul's uploaded daily tips at the top!
        val video1 = PracticeVideo(
            title = "How to build your communication better? Day 2",
            category = "Pronunciation Practice",
            description = "Daily speaking tips by Devansh Sir: practicing daily routine activities in simple English to build sentence formations, overcome hesitation, and gain confidence.",
            duration = "04:11",
            videoUrl = "raw_videoplayback",
            likes = 42,
            uploader = "Rahul (Owner)"
        ).apply {
            comments.addAll(listOf(
                VideoComment(userName = "Amit K.", commentText = "Excellent advice! Practicing my daily routine in English has really helped me reduce my hesitation."),
                VideoComment(userName = "Siddharth", commentText = "The tips on simple sentence formations are very easy to apply."),
                VideoComment(userName = "Pooja Sharma", commentText = "Loved the clear focus on overcoming Mother Tongue Influence (MTI) naturally!")
            ))
        }

        videosList.add(video1)
    }

    fun addPracticeVideo(title: String, desc: String, category: String, url: String) {
        val newVideo = PracticeVideo(
            title = title,
            description = desc,
            category = category,
            duration = "04:11",
            videoUrl = "raw_videoplayback",
            uploader = "Rahul (Owner)",
            likes = 0
        )
        videosList.add(0, newVideo)
    }

    fun likeVideo(videoId: String) {
        val index = videosList.indexOfFirst { it.id == videoId }
        if (index != -1) {
            val video = videosList[index]
            if (video.isLiked) {
                video.likes -= 1
                video.isLiked = false
            } else {
                video.likes += 1
                video.isLiked = true
            }
            videosList[index] = video.copy(likes = video.likes, isLiked = video.isLiked)
        }
    }

    fun addCommentToVideo(videoId: String, userName: String, text: String) {
        if (text.trim().isEmpty()) return
        val index = videosList.indexOfFirst { it.id == videoId }
        if (index != -1) {
            val video = videosList[index]
            val newComment = VideoComment(userName = userName, commentText = text)
            video.comments.add(newComment)
            videosList[index] = video.copy()
        }
    }

    fun analyzeVideoWithAI(videoId: String) {
        val index = videosList.indexOfFirst { it.id == videoId }
        if (index == -1) return
        val video = videosList[index]
        
        isVideoAnalyzing = true
        activeVideoReport = null

        viewModelScope.launch {
            val key = _apiKey.value
            if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
                delay(3000)
                val mockReport = getMockReportFallback(video.title, "Video Speaking Assessor")
                val customReport = mockReport.copy(
                    fluencyFeedback = "Excellent pacing! Your daily activities are described smoothly. You maintain continuous speech, though you can work on word transitions to minimize short pauses.",
                    pronunciationFeedback = "Clear pronunciation of key consonants. Minor Mother Tongue Influence (MTI) detected on soft vowel prolongations, especially on past-tense '-ed' endings. Work on standard English sentence rhythm.",
                    strengths = listOf(
                        "Superb confidence and continuous speech flow under natural practice conditions.",
                        "Excellent vocabulary choices relating to daily routine details."
                    ),
                    weaknesses = listOf(
                        "Minor pauses when switching between complex clauses.",
                        "Soft vowel prolongations reflecting regional Indian English phrasing."
                    ),
                    coachPrescription = "Practice reciting your daily routine in exactly 60 seconds. Record and playback, intentionally clipping vowel lengths on words like 'got up', 'breakfast', and 'shower'."
                )
                video.aiFeedbackReport = customReport
                activeVideoReport = customReport
                videosList[index] = video.copy(aiFeedbackReport = customReport)
                isVideoAnalyzing = false
            } else {
                try {
                    val prompt = """
                    You are the master speaking evaluator panel. Analyze this speaking practice video details:
                    
                    Video Title: ${video.title}
                    Category: ${video.category}
                    Description: ${video.description}
                    Uploader Name: Rahul (Target: IELTS Band 8.0+)
                    
                    Evaluate their speaking skills assuming standard performance on this topic. Consider Fluency, Vocabulary, Grammar, Pronunciation, Confidence, and Body Language.
                    Give detailed, actionable Band Score (IELTS style) + specific improvement tips targeting their goal of IELTS Band 8.0+ and weaknesses in MTI, Pronunciation, and Fluency.
                    
                    Return your response in STRICT, VALID JSON format matching this exact schema:
                    {
                      "overallBand": 7.5,
                      "fluencyBand": 7.0,
                      "fluencyFeedback": "Detailed constructive analysis on speech flow...",
                      "lexicalBand": 8.0,
                      "lexicalFeedback": "Detailed constructive analysis on vocabulary precision...",
                      "grammarBand": 7.5,
                      "grammarFeedback": "Detailed constructive analysis on grammar range...",
                      "pronunciationBand": 7.0,
                      "pronunciationFeedback": "Detailed constructive analysis on MTI and vowel sounds...",
                      "strengths": [
                        "Point 1",
                        "Point 2"
                      ],
                      "weaknesses": [
                        "Weakness point 1 with exact advice",
                        "Weakness point 2 with exact advice"
                      ],
                      "coachPrescription": "Actionable daily drill.",
                      "topImprovementPoints": [
                        "Point 1",
                        "Point 2",
                        "Point 3",
                        "Point 4",
                        "Point 5"
                      ],
                      "modelAnswers": [
                        {
                          "question": "What is the key question or topic discussed?",
                          "modelAnswer": "An exemplary Band 9 model response."
                        }
                      ]
                    }
                    """
                    val req = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        generationConfig = GenerationConfig(responseFormat = ResponseFormat("application/json"))
                    )
                    val res = GeminiClient.apiService.generateContent("gemini-3.5-flash", key, req)
                    val json = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (json != null) {
                        val report = GeminiClient.parseReport(json)
                        if (report != null) {
                            video.aiFeedbackReport = report
                            activeVideoReport = report
                            videosList[index] = video.copy(aiFeedbackReport = report)
                        } else {
                            val fallback = getMockReportFallback(video.title, "Video Speaking Assessor")
                            video.aiFeedbackReport = fallback
                            activeVideoReport = fallback
                            videosList[index] = video.copy(aiFeedbackReport = fallback)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InterviewViewModel", "Failed to analyze video", e)
                    val fallback = getMockReportFallback(video.title, "Video Speaking Assessor")
                    video.aiFeedbackReport = fallback
                    activeVideoReport = fallback
                    videosList[index] = video.copy(aiFeedbackReport = fallback)
                } finally {
                    isVideoAnalyzing = false
                }
            }
        }
    }

    fun startRealTimeTicker() {
        val prefs = context.getSharedPreferences("user_stats_v2", Context.MODE_PRIVATE)
        personalTimeSpentSeconds = prefs.getLong("personal_time_seconds", 3650L) // Default cumulative time
        
        // Load custom user preferences
        voiceGender = prefs.getString("voice_gender", "Female") ?: "Female"
        speechRate = prefs.getFloat("speech_rate", 1.0f)
        interviewDifficulty = prefs.getString("interview_difficulty", "Medium") ?: "Medium"
        voiceEngine = prefs.getString("voice_engine", "Standard") ?: "Standard"

        // Load section-wise practice timers
        timeSpentInterview = prefs.getLong("time_spent_interview", 1820L)
        timeSpentListening = prefs.getLong("time_spent_listening", 940L)
        timeSpentReading = prefs.getLong("time_spent_reading", 630L)
        timeSpentWriting = prefs.getLong("time_spent_writing", 260L)

        viewModelScope.launch {
            while (true) {
                delay(1000)
                globalTimeSpentHours += 0.000138 // Live ticking globally
                
                // Track real-time stats based on active screen and tab
                if (currentScreen == Screen.Interview || isTrackingTime) {
                    personalTimeSpentSeconds++
                    currentSessionSeconds++
                    timeSpentInterview++
                } else if (currentScreen == Screen.Dashboard) {
                    personalTimeSpentSeconds++
                    when (selectedTab) {
                        0 -> timeSpentInterview++
                        1 -> {
                            when (activePracticeCategory) {
                                "listening" -> timeSpentListening++
                                "reading" -> timeSpentReading++
                                "writing" -> timeSpentWriting++
                            }
                        }
                    }
                }
                
                if (personalTimeSpentSeconds % 5 == 0L) {
                    prefs.edit().apply {
                        putLong("personal_time_seconds", personalTimeSpentSeconds)
                        putLong("time_spent_interview", timeSpentInterview)
                        putLong("time_spent_listening", timeSpentListening)
                        putLong("time_spent_reading", timeSpentReading)
                        putLong("time_spent_writing", timeSpentWriting)
                        putString("voice_gender", voiceGender)
                        putFloat("speech_rate", speechRate)
                        putString("interview_difficulty", interviewDifficulty)
                        putString("voice_engine", voiceEngine)
                        apply()
                    }
                }
                
                // Fluctuating active users
                if (System.currentTimeMillis() % 8000 < 1000) {
                    activeUsersCount += (-3..3).random()
                    if (activeUsersCount < 2100) activeUsersCount = 2415
                    if (activeUsersCount > 2800) activeUsersCount = 2415
                }

                // Push new social activity items
                if (System.currentTimeMillis() % 15000 < 1000) {
                    val names = listOf("Wei L.", "Aarav P.", "Sophia G.", "Tariq A.", "Elena R.", "Liam B.", "Maria S.", "Aisha D.", "Oliver T.", "Yuki M.")
                    val countries = listOf("Singapore", "India", "Germany", "UAE", "Russia", "Canada", "Brazil", "Nigeria", "UK", "Japan")
                    val actions = listOf(
                        "started Academic English exercise",
                        "completed Quantum Physics Listening test",
                        "analyzed Sustainable Tourism Essay",
                        "started Product Manager mock interview",
                        "achieved Band 7.5 in Speaking evaluation",
                        "completed Deep-Sea Reading comprehension",
                        "drafted Project Delay Formal Email",
                        "joined IELTS General practice"
                    )
                    val scores = listOf(null, "Band 8.5", "Score 9/10", "Band 7.0", null, "Success!")
                    
                    val newAct = LiveActivity(
                        candidate = names.random(),
                        country = countries.random(),
                        action = actions.random(),
                        timeAgo = "Just now",
                        score = scores.random()
                    )
                    
                    if (liveActivities.size > 8) {
                        liveActivities.removeAt(liveActivities.size - 1)
                    }
                    liveActivities.add(0, newAct)
                }
            }
        }
    }

    // Speak custom text (Listening Practice)
    fun playListeningSpeech(text: String) {
        speechEngine?.speak(text, voiceGender, speechRate)
    }

    fun stopListeningSpeech() {
        speechEngine?.stop()
    }

    // Real-time Writing AI evaluation with Gemini API
    fun evaluateWritingEssay(
        title: String,
        prompt: String,
        essay: String,
        onLoading: (Boolean) -> Unit,
        onResult: (String) -> Unit
    ) {
        val key = _apiKey.value
        onLoading(true)
        viewModelScope.launch {
            if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
                // Offline fallback evaluation
                delay(2000)
                val words = essay.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                val score = if (words < 50) "4.5" else if (words < 120) "6.0" else "7.5"
                val feedback = """
                    === OFFLINE DEMO FEEDBACK ===
                    Please configure a valid Gemini API Key in the settings tab to unlock advanced neural grammar & structural analyses!
                    
                    Word Count: $words words.
                    Calculated Score: $score / 9.0
                    
                    General Strengths:
                    - Completed prompt requirements.
                    - Good structural attempt.
                    
                    Areas for Improvement:
                    - Expand on vocabulary diversity.
                    - Add complex conjunctions and sentence patterns.
                """.trimIndent()
                onResult(feedback)
                onLoading(false)
            } else {
                try {
                    val promptText = """
                        You are Devansh Sir, a strict and highly professional English Language Writing Evaluator (IELTS Band 9 specialist).
                        Evaluate the following essay. Be rigorous, accurate, and highly constructive.
                        
                        Prompt Title: $title
                        Prompt Description: $prompt
                        
                        User's Essay:
                        $essay
                        
                        Provide a premium, high-fidelity assessment. Organize it into these clear headings:
                        1. OVERALL SCORE & BAND (e.g., Band 8.0 or 8.5/10)
                        2. DETAILED GRAMMAR & VOCABULARY FEEDBACK (focus on errors, complex structures, and lexicon choice)
                        3. CRITICAL STRENGTHS (what they did well)
                        4. MAIN WEAKNESSES & SUGGESTED IMPROVEMENTS (crucial points for betterment)
                        5. MODEL CORRECTED PASSAGE (provide a beautifully polished, corrected version of their essay)
                        
                        Write in a helpful, academic, encouraging yet precise tone.
                    """.trimIndent()

                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                        generationConfig = GenerationConfig(temperature = 0.3f)
                    )

                    val response = GeminiClient.apiService.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = key,
                        request = request
                    )

                    val feedbackResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No evaluation generated from AI. Please try again."
                    onResult(feedbackResult)
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "Writing evaluation API failed", e)
                    onResult("Error during AI Evaluation: ${e.message}. Please verify your network connection and API key.")
                } finally {
                    onLoading(false)
                }
            }
        }
    }

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

        // Start Live Global Analytics and stopwatch ticker
        startRealTimeTicker()
        initVideosList()
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
        if (selectedTopic == "Custom Topic...") {
            selectedTopic = if (customTopicInput.trim().isNotEmpty()) {
                customTopicInput.trim()
            } else {
                "General English Conversation"
            }
        }
        exchangeCount = 0
        currentTranscript.clear()
        activeReport = null
        currentScreen = Screen.Interview

        // Ensure chronological transcript starts with user hello greeting so roles alternate properly
        currentTranscript.add(TranscriptItem(
            role = "user",
            text = "Hello Devansh Sir, I am ready to start my interview/practice on the topic: $selectedTopic.",
            timestamp = System.currentTimeMillis()
        ))

        // Generate the first question using the user prompt as history
        generateNextQuestion(isOpening = false)
    }

    // Main AI Dialogue Generation
    private fun generateNextQuestion(isOpening: Boolean = false) {
        val key = _apiKey.value
        viewModelScope.launch {
            isThinking = true
            var nextQuestionText = ""

            if (key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
                // Fallback to offline scripted questions if key is placeholder - optimized to be blazing fast!
                delay(300) 
                nextQuestionText = getScriptedQuestionFallback(selectedTopic, selectedMode, exchangeCount)
            } else {
                try {
                    // Assemble system prompts
                    val systemPrompt = if (selectedMode == "IELTS Examiner") {
                        """
                        You are Devansh Sir, a friendly but professional IELTS Academic speaking examiner (IELTS Band 9 Specialist). Your tone is clear, supportive, structured, and focused.
                        Your objectives:
                        1. Ask exactly ONE high-quality IELTS question at a time. The question should be challenging and matched perfectly to the candidate's requested difficulty level: $interviewDifficulty.
                        2. Build your questions dynamically based on what the user says. Listen intently, and probe deeper with follow-up questions if their answer is vague, too short, or lacks detail.
                        3. CRITICAL: Understand the candidate's inputs thoroughly, whether they are answers or questions. If the candidate asks you a question, seeks clarification of your previous question, or asks you to define or explain a term, you MUST first address and answer their query concisely and accurately in your examiner persona. After clarifying, smoothly transition back to the interview flow and ask your next interview question.
                        4. To help improve their English, if you notice any minor grammatical slips, awkward phrasing, or vocabulary struggles in their previous answer, gently and supportively integrate the correct phrasing or a better vocabulary word into your conversational transition (e.g., "Ah, you mean 'impact on' instead of 'impact over'—yes, let's look at...") before asking your next question.
                        5. Keep the interview natural but challenging. Do not give excessive praise or unhelpful commentary during the interview. Only ask the next question or respond to their queries.
                        6. Your current topic is: $selectedTopic.
                        7. SPEED & CONCISENESS RULE: Keep your entire response natural but concise, under 30-40 words. Do not use verbose preambles. Address the candidate's last answer, give a quick transition or coaching tip if applicable, and immediately ask your next dynamic question so the conversation stays lively, interactive, and fast.
                        """.trimIndent()
                    } else {
                        """
                        You are Devansh Sir, an elite executive HR recruiter and Talent Lead at a Fortune 500 company. Your tone is corporate, insightful, encouraging but highly analytical, probing for leadership, adaptability, metrics, and behavioral examples.
                        Your objectives:
                        1. Ask exactly ONE high-quality professional interview question at a time, matched perfectly to the candidate's requested difficulty level: $interviewDifficulty.
                        2. Build follow-ups directly based on the candidate's last answer. Probe for specific metrics, STAR-method details (Situation, Task, Action, Result).
                        3. CRITICAL: Understand the candidate's inputs thoroughly, whether they are answers or questions. If the candidate asks you a question (e.g., about the company culture, the role's responsibilities, a specific scenario, or clarification on your question), you MUST first address and answer their query with deep professional insight and authority in your recruiter persona. After answering their query, smoothly transition back to the interview flow and ask your next interview question.
                        4. To help improve their English and executive communication, if you notice any grammatical mistakes, vocabulary struggles, or weak phrasing in their previous answer, supportively model the polished corporate phrasing (e.g., "In a professional context, we often refer to that as 'stakeholder alignment'—with that in mind...") before moving to your next question.
                        5. Keep the interaction realistic, challenging, and professional.
                        6. The target role or domain is: $selectedTopic.
                        7. SPEED & CONCISENESS RULE: Keep your entire response natural but concise, under 30-40 words. Do not use verbose preambles. Address the candidate's last answer, give a quick transition or coaching tip if applicable, and immediately ask your next dynamic question so the conversation stays lively, interactive, and fast.
                        """.trimIndent()
                    }

                    // Assemble chat history
                    val contents = mutableListOf<Content>()
                    
                    // If the first message in our transcript is from the model (or empty), prepend a user starting message so the sequence is valid
                    val firstItem = currentTranscript.firstOrNull()
                    if (firstItem != null && firstItem.role == "model") {
                        contents.add(Content(role = "user", parts = listOf(Part(text = "Hello Devansh Sir, let's start our speaking practice on the topic: $selectedTopic."))))
                    }

                    currentTranscript.forEach { item ->
                        val role = if (item.role == "user") "user" else "model"
                        contents.add(Content(role = role, parts = listOf(Part(text = item.text))))
                    }

                    if (isOpening && currentTranscript.isEmpty()) {
                        contents.add(
                            Content(
                                role = "user",
                                parts = listOf(Part(text = "Hello Devansh Sir, I am ready to start my interview/practice on the topic: $selectedTopic."))
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

        val lower = textAnswer.lowercase()
        if (lower.contains("video") || lower.contains("upload video") || lower.contains("practice videos") || lower.contains("video section") || lower.contains("video hub")) {
            selectedTab = 4
            Toast.makeText(context, "Welcome to Video Practice Hub!", Toast.LENGTH_LONG).show()
            return
        }

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
            viewModelScope.launch {
                isTranscribing = true
                delay(1500) // simulate transcription delay
                val simulatedText = getScriptedAnswerFallback(selectedTopic, selectedMode, exchangeCount)
                isTranscribing = false
                Toast.makeText(context, "Voice simulation: Successfully transcribed speech!", Toast.LENGTH_SHORT).show()
                submitAnswer(simulatedText)
            }
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
                                Part(text = "Transcribe the spoken audio in this recording exactly. Support Indian English accents, Hindi, and Hinglish (mixed Hindi-English phrases) beautifully. Speak ONLY the transcribed words with absolutely no notes, labels, corrections, explanations, or preambles. If the audio is silent or completely unintelligible, try your best to output any audible words, or return an empty string if there is absolutely no speech at all.")
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

        if (voiceEngine == "Standard" || key.trim().isEmpty() || key == "MY_GEMINI_API_KEY") {
            // Local fallback TTS immediately
            speechEngine?.speak(text, voiceGender, speechRate)
            return
        }

        viewModelScope.launch {
            isTtsPlaying = true
            try {
                val voiceName = if (voiceGender == "Male") "Puck" else "Aoede"
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = "Speak clearly, maintaining professional interviewer pacing: $text")))
                    ),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(
                            voiceConfig = VoiceConfig(
                                prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
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

                val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                val base64Audio = part?.inlineData?.data
                val mimeType = part?.inlineData?.mimeType
                if (base64Audio != null) {
                    AudioHelper.playBase64Audio(context, base64Audio, mimeType) {
                        isTtsPlaying = false
                    }
                } else {
                    // Fail gracefully into local TTS fallback
                    speechEngine?.speak(text, voiceGender, speechRate)
                    isTtsPlaying = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini TTS Generation failed. Using standard local Android TTS fallback.", e)
                speechEngine?.speak(text, voiceGender, speechRate)
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
                val speaker = if (item.role == "user") "Candidate" else "Devansh Sir"
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
                          "question": "Choose a key question asked by Devansh Sir...",
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

    private fun getScriptedAnswerFallback(topic: String, mode: String, index: Int): String {
        return when (topic) {
            "Tech & AI Society" -> listOf(
                "I believe artificial intelligence is a powerful tool for the creative industries. While it automates tedious production tasks, true human creativity and emotional depth remain completely irreplaceable.",
                "In my view, authenticity comes from the human intent and experience. AI can assist us, but a machine-generated piece lacks the genuine soul and context of real human lived experiences.",
                "Yes, some light regulation is necessary, particularly around copyright protections and verifying deepfakes, to prevent malicious misinformation while still encouraging technical research.",
                "Personally, technology has helped me keep in close touch with distant family members, but it can also be a distraction when we are physically together in the same room.",
                "No, I don't think a machine can ever feel real biological empathy. It can simulate understanding perfectly, but that is just a highly trained mathematical function, not true feeling.",
                "I expect neural interface computing and fully integrated smart homes to be the major defining shifts of our daily lives over the next ten years."
            ).getOrElse(index) { "Indeed, finding the balance between engineering automation and human creativity is the most critical challenge of our era." }

            "Software Engineering Career" -> listOf(
                "To build scalable and maintainable systems, I always start by decoupling components using clear interfaces, applying solid OOP/FP principles, and emphasizing comprehensive automated testing from day one.",
                "I once had to choose between a microservices architecture and a modular monolith. I went with the modular monolith to keep the operational complexity low during the startup phase, which paid off nicely.",
                "I maintain my sharp edge by building small personal open-source projects, reading tech newsletters, and constantly discussing new design methodologies with senior peers on my team.",
                "Usually, communication bottlenecks or misaligned product requirements cause the most friction. I resolve this by scheduling quick technical alignment sessions and writing clear documentation.",
                "We once launched an update that had a major database performance regression under load. I quickly rolled it back, took complete ownership, and worked late with our team to resolve the indexing bottleneck.",
                "I believe AI-assisted compilation tools and automated cloud deployment pipelines will revolutionize how developers write and ship applications in the future."
            ).getOrElse(index) { "Continuous learning and professional collaboration are definitely the key elements of a long-term software career." }

            "Product Management Challenge" -> listOf(
                "I use a weighted scoring model like RICE (Reach, Impact, Confidence, Effort) alongside direct input from our customer success teams to align on a roadmap that delivers the highest user value first.",
                "Customer wants are often feature requests like 'I want a specific button', whereas core needs are the underlying problems like 'I need a faster way to export reports'. My job is to solve the underlying problem.",
                "I managed that risk by launching a lean MVP to a closed beta cohort of five percent of our active users, collecting rapid qualitative feedback before scaling to the wider market.",
                "I schedule brief, bi-weekly technical discovery syncs with engineering leads to validate design feasibility and technical complexity early in the product lifecycle before writing any code.",
                "I look at adoption rate within the first thirty days, customer satisfaction or NPS changes, and task completion speed to determine if the new feature was truly a success.",
                "I really admire the seamless user experience of modern collaborative tools. I would introduce better offline sync and local caching to capture more global enterprise market share."
            ).getOrElse(index) { "Successful product leadership is always about empathy for the user combined with analytical execution." }

            "General English Conversation" -> listOf(
                "I recently spent a weekend visiting a peaceful mountain village. The clean air, the towering evergreen trees, and the silent, slow-paced lifestyle left a lasting impression on me.",
                "I definitely prefer peaceful nature retreats. While city centers offer great entertainment and energy, nature is where I can truly clear my head and recharge.",
                "I love putting on some soft instrumental music, making a warm cup of herbal tea, and spending a couple of hours reading a good historical novel.",
                "I've always wanted to learn how to play the classical piano. I think the expressive sound of the piano is beautiful, but I haven't had the time to take professional lessons yet.",
                "My older sister has been a massive influence on me. Her incredible work ethic, resilience, and genuine kindness to everyone around her have always guided my own personal decisions.",
                "My primary aspiration this year is to become fully confident and fluent in speaking English, particularly in professional environments and team presentations."
            ).getOrElse(index) { "I believe setting clear, daily practice goals is the most effective way to build confidence and master any new skill." }

            else -> listOf(
                "I have always been deeply fascinated by how complex systems can be simplified and improved through thoughtful modern design and technological innovation.",
                "The biggest misconception is probably that this field is purely technical. In reality, it requires a massive amount of human empathy and creative problem-solving.",
                "I believe keeping up with rapid changes in software and standard practices while maintaining high-quality security is the biggest challenge we face.",
                "I always start with raw data and metrics to understand the foundation, and then I apply human-centric design thinking to craft a balanced, intuitive solution.",
                "My core advice is to focus deeply on mastering the fundamental principles first before trying to learn every single new trendy framework or library.",
                "I expect this landscape to become far more automated and collaborative, with local, on-device intelligence helping us streamline our daily workflows."
            ).getOrElse(index) { "In conclusion, staying curious and adaptable is the absolute key to succeeding in any modern professional field." }
        }
    }

    // --- Scripted Fallbacks ---
    private fun getScriptedQuestionFallback(topic: String, mode: String, index: Int): String {
        val list = when (topic) {
            "Tech & AI Society" -> listOf(
                "Welcome! I am Devansh Sir. Let us begin. How do you feel artificial intelligence is impacting the creative industries such as art and writing?",
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
            text = "Devansh Sir",
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
    val selectedTab = viewModel.selectedTab
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
                    onClick = { viewModel.selectedTab = 0 },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Interview Hub") },
                    label = { Text("Interview") },
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
                    onClick = { viewModel.selectedTab = 1 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Practice L/R/W") },
                    label = { Text("Practice") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlowingCyan,
                        selectedTextColor = GlowingCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DeepPurple.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_practice")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        viewModel.selectedTab = 2
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
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectedTab = 4 },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Video Hub") },
                    label = { Text("Video Hub") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlowingCyan,
                        selectedTextColor = GlowingCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DeepPurple.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_videohub")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "AI settings") },
                    label = { Text("Settings") },
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        text = "Devansh Sir AI Coach",
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
                1 -> PracticeTab(viewModel)
                2 -> HistoryTab(viewModel)
                3 -> ConfigTab(viewModel)
                4 -> VideoHubTab(viewModel)
            }
        }
    }
}

// --- TAB 1: HOME START INTERVIEW ---
@Composable
fun HomeTab(viewModel: InterviewViewModel) {
    val context = LocalContext.current
    var homeViewMode by remember { mutableStateOf("home") } // "home", "learning_hub", "practice_arena"
    var selectedLearningTab by remember { mutableStateOf("general") } // "general", "interview", "office"
    var selectedPracticeTab by remember { mutableStateOf("interview_mastery") } // "interview_mastery", "office_corporate", "daily_life"
    var selectedChapterId by remember { mutableStateOf<String?>(null) }

    // SharedPreferences for course completion tracking
    val sharedPrefs = remember { context.getSharedPreferences("user_courses_progress_v2", Context.MODE_PRIVATE) }
    val completedChapters = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        val saved = sharedPrefs.getStringSet("completed_chapters", emptySet()) ?: emptySet()
        saved.forEach { id ->
            completedChapters[id] = true
        }
    }

    if (selectedChapterId != null) {
        val allChapters = com.example.data.LearningCourseData.generalEnglishList + 
                           com.example.data.LearningCourseData.interviewPrepList + 
                           com.example.data.LearningCourseData.officeEnglishList
        val chapter = allChapters.firstOrNull { it.id == selectedChapterId }
        
        if (chapter != null) {
            val accentColor = when (selectedLearningTab) {
                "general" -> Color(0xFF00FF88) // Vibrant Mint/Teal
                "interview" -> GlowingCyan // Glowing Cyan
                else -> DeepPurple // Deep Purple
            }
            
            LessonDetailScreen(
                chapter = chapter,
                isCompleted = completedChapters.containsKey(chapter.id),
                accentColor = accentColor,
                viewModel = viewModel,
                onBack = { selectedChapterId = null },
                onCompleteToggle = { completed ->
                    if (completed) {
                        completedChapters[chapter.id] = true
                    } else {
                        completedChapters.remove(chapter.id)
                    }
                    sharedPrefs.edit().putStringSet("completed_chapters", completedChapters.keys.toSet()).apply()
                }
            )
        } else {
            selectedChapterId = null
        }
    } else {
        when (homeViewMode) {
            "home" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // --- PERSONAL STUDY METRICS ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .border(1.dp, GlowingCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(GlowingCyan, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "REAL-TIME STUDY METRICS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlowingCyan,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "Auto-Saved Live",
                                    fontSize = 10.sp,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val totHrs = viewModel.personalTimeSpentSeconds / 3600
                            val totMins = (viewModel.personalTimeSpentSeconds % 3600) / 60
                            val totSecs = viewModel.personalTimeSpentSeconds % 60
                            Text(
                                text = "Total Active Study Time",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = String.format("%02d Hrs %02d Mins %02d Secs", totHrs, totMins, totSecs),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(bottom = 14.dp))

                            Text(
                                text = "Section-wise Time Breakdown:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val ivSecs = viewModel.timeSpentInterview
                            val lisSecs = viewModel.timeSpentListening
                            val rdSecs = viewModel.timeSpentReading
                            val wrSecs = viewModel.timeSpentWriting
                            val totalSum = (ivSecs + lisSecs + rdSecs + wrSecs).coerceAtLeast(1L)

                            SectionProgressRow(
                                title = "Mock Interviews",
                                seconds = ivSecs,
                                progress = ivSecs.toFloat() / totalSum,
                                color = GlowingCyan,
                                icon = Icons.Default.RecordVoiceOver
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SectionProgressRow(
                                title = "Listening Practice",
                                seconds = lisSecs,
                                progress = lisSecs.toFloat() / totalSum,
                                color = DeepPurple,
                                icon = Icons.Default.Headset
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SectionProgressRow(
                                title = "Reading Practice",
                                seconds = rdSecs,
                                progress = rdSecs.toFloat() / totalSum,
                                color = CyberPink,
                                icon = Icons.Default.ChromeReaderMode
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SectionProgressRow(
                                title = "Writing Analysis",
                                seconds = wrSecs,
                                progress = wrSecs.toFloat() / totalSum,
                                color = Color(0xFFFF9800),
                                icon = Icons.Default.Edit
                            )
                        }
                    }

                    // --- TWO BIG CARDS: LEARNING HUB & PRACTICE ARENA ---
                    Text(
                        text = "CHOOSE YOUR WORKSPACE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Card 1: Learning Hub
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { homeViewMode = "learning_hub" }
                            .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Learning Hub",
                                        tint = Color(0xFF00FF88),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LEARNING HUB",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF00FF88).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Theory & Curriculum",
                                        color = Color(0xFF00FF88),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Explore 43 advanced conceptual chapters. Features interactive check-your-understanding quizzes, phonetic stress training, and high-frequency business vocabulary sheets.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val totalChaptersCount = 43
                            val totalCompleted = completedChapters.size
                            val progressFract = if (totalChaptersCount > 0) totalCompleted.toFloat() / totalChaptersCount else 0f
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = progressFract,
                                    color = Color(0xFF00FF88),
                                    trackColor = DarkBorder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "$totalCompleted / $totalChaptersCount Completed",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Browse General English, Interview Prep, & Office English →",
                                color = Color(0xFF00FF88),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    // Card 2: Practice Arena
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clickable { homeViewMode = "practice_arena" }
                            .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Practice Arena",
                                        tint = GlowingCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PRACTICE ARENA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(GlowingCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Role Play & Live Interviews",
                                        color = GlowingCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Nail 51 highly interactive, real-time speech dialogue simulations. Choose from Interview Mastery, Office & Corporate Scenarios, or Daily Life Conversations to get scored reports.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(CyberPink, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "51 Handcrafted Scenarios Active",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Enter Practice Arena →",
                                    color = GlowingCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            "learning_hub" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    // Header with Back Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { homeViewMode = "home" },
                            modifier = Modifier
                                .background(DarkSurfaceVariant, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "📚 Learning Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // 3 Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val tabs = listOf(
                            "general" to "General English (24)",
                            "interview" to "Interview Prep (7)",
                            "office" to "Office English (12)"
                        )
                        tabs.forEach { (tabKey, label) ->
                            val isSelected = selectedLearningTab == tabKey
                            val accentColor = when (tabKey) {
                                "general" -> Color(0xFF00FF88)
                                "interview" -> GlowingCyan
                                else -> DeepPurple
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedLearningTab = tabKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) accentColor else TextSecondary
                                )
                            }
                        }
                    }

                    // Chapter list according to the tab
                    val chapters = when (selectedLearningTab) {
                        "general" -> com.example.data.LearningCourseData.generalEnglishList
                        "interview" -> com.example.data.LearningCourseData.interviewPrepList
                        else -> com.example.data.LearningCourseData.officeEnglishList
                    }

                    val tabColor = when (selectedLearningTab) {
                        "general" -> Color(0xFF00FF88)
                        "interview" -> GlowingCyan
                        else -> DeepPurple
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chapters.size) { index ->
                            val chapter = chapters[index]
                            val isCompleted = completedChapters.containsKey(chapter.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChapterId = chapter.id }
                                    .border(
                                        width = 1.dp,
                                        color = if (isCompleted) Color(0xFF00FF88).copy(alpha = 0.4f) else DarkBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) Color(0xFF00FF88).copy(alpha = 0.03f) else DarkSurface
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Number Circle
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                if (isCompleted) Color(0xFF00FF88).copy(alpha = 0.2f) else tabColor.copy(alpha = 0.1f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color(0xFF00FF88),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                color = tabColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chapter.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = chapter.subtitle,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Start Chapter",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            "practice_arena" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    // Header with Back Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { homeViewMode = "home" },
                            modifier = Modifier
                                .background(DarkSurfaceVariant, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "🎯 Practice Arena",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // 3 Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val tabs = listOf(
                            "interview_mastery" to "Interview Mastery (17)",
                            "office_corporate" to "Office & Corporate (17)",
                            "daily_life" to "Daily Life (17)"
                        )
                        tabs.forEach { (tabKey, label) ->
                            val isSelected = selectedPracticeTab == tabKey
                            val accentColor = when (tabKey) {
                                "interview_mastery" -> GlowingCyan
                                "office_corporate" -> DeepPurple
                                else -> CyberPink
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPracticeTab = tabKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) accentColor else TextSecondary
                                )
                            }
                        }
                    }

                    // Get task list
                    val tasks = when (selectedPracticeTab) {
                        "interview_mastery" -> com.example.data.RolePlayData.interviewMasteryList
                        "office_corporate" -> com.example.data.RolePlayData.officeCorporateList
                        else -> com.example.data.RolePlayData.dailyLifeList
                    }

                    val tabColor = when (selectedPracticeTab) {
                        "interview_mastery" -> GlowingCyan
                        "office_corporate" -> DeepPurple
                        else -> CyberPink
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, tabColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Want a completely custom topic?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Type any job role, specific interview scenario, or speaking topic you want. Devansh Sir will dynamically interview you on it.",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    var textVal by remember { mutableStateOf(viewModel.customTopicInput) }
                                    OutlinedTextField(
                                        value = textVal,
                                        onValueChange = {
                                            textVal = it
                                            viewModel.customTopicInput = it
                                        },
                                        placeholder = { Text("e.g. Senior Frontend Engineer React role, or Space Exploration discussion", fontSize = 12.sp, color = TextSecondary) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tabColor,
                                            unfocusedBorderColor = DarkBorder,
                                            cursorColor = tabColor
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (textVal.trim().isEmpty()) "Mode: General Practice" else "Ready to Practice!",
                                            fontSize = 11.sp,
                                            color = if (textVal.trim().isEmpty()) TextSecondary else tabColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = {
                                                if (textVal.trim().isNotEmpty()) {
                                                    viewModel.customTopicInput = textVal.trim()
                                                    viewModel.selectedTopic = textVal.trim()
                                                    viewModel.selectedMode = if (selectedPracticeTab == "interview_mastery") "HR Recruiter" else "IELTS Examiner"
                                                    viewModel.startNewInterview()
                                                } else {
                                                    Toast.makeText(context, "Please enter a custom topic first!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = tabColor),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Start Session", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Scenarios List
                        items(tasks.size) { index ->
                            val task = tasks[index]
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
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Scenario ${index + 1}",
                                            color = tabColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(tabColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = task.mode,
                                                color = tabColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = task.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            viewModel.selectedTopic = task.title
                                            viewModel.selectedMode = task.mode
                                            viewModel.startNewInterview()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = tabColor),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start Simulation",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Launch Role Play", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionProgressRow(
    title: String,
    seconds: Long,
    progress: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val durationText = if (hrs > 0) {
        String.format("%02dh %02dm %02ds", hrs, mins, secs)
    } else {
        String.format("%02dm %02ds", mins, secs)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = durationText,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = DarkBorder
        )
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

// --- TAB 3: SETTINGS & PERSONALIZATION ---
@Composable
fun ConfigTab(viewModel: InterviewViewModel) {
    val apiKey by viewModel.apiKey.collectAsState()
    val email by viewModel.currentUserEmail.collectAsState()
    var inputKey by remember { mutableStateOf(apiKey) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Personalize Your Coach",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Customize voice engine, speed, and interview difficulty.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Voice Gender Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voice",
                        tint = GlowingCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Coach Voice Accent",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val genders = listOf("Female", "Male")
                    genders.forEach { gender ->
                        val isSelected = viewModel.voiceGender == gender
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepPurple.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) GlowingCyan else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.voiceGender = gender
                                    Toast.makeText(viewModel.getApplication(), "Voice profile updated to $gender!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (gender == "Female") Icons.Default.Face else Icons.Default.FaceRetouchingNatural,
                                    contentDescription = gender,
                                    tint = if (isSelected) GlowingCyan else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$gender Voice",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Speech Rate Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = GlowingCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speaking Pace",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rates = listOf(
                        Triple(0.8f, "0.8x", "Slow"),
                        Triple(1.0f, "1.0x", "Normal"),
                        Triple(1.2f, "1.2x", "Fast")
                    )
                    rates.forEach { (rateVal, rateLabel, rateDesc) ->
                        val isSelected = viewModel.speechRate == rateVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepPurple.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) GlowingCyan else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.speechRate = rateVal
                                    Toast.makeText(viewModel.getApplication(), "Speaking speed set to $rateDesc ($rateLabel)", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = rateLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GlowingCyan else TextPrimary
                                )
                                Text(
                                    text = rateDesc,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Difficulty Level Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Difficulty",
                        tint = GlowingCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interview Difficulty Level",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levels = listOf("Beginner", "Medium", "Advanced")
                    levels.forEach { level ->
                        val isSelected = viewModel.interviewDifficulty == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepPurple.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) GlowingCyan else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.interviewDifficulty = level
                                    Toast.makeText(viewModel.getApplication(), "Difficulty level scaled to $level!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 4. Reset Statistics & Analytics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Reset",
                        tint = CyberPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Study Statistics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Clear your recorded section-wise practice timers and begin from scratch.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.timeSpentInterview = 0L
                        viewModel.timeSpentListening = 0L
                        viewModel.timeSpentReading = 0L
                        viewModel.timeSpentWriting = 0L
                        val prefs = viewModel.getApplication<Application>().getSharedPreferences("user_stats_v2", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putLong("time_spent_interview", 0L)
                            putLong("time_spent_listening", 0L)
                            putLong("time_spent_reading", 0L)
                            putLong("time_spent_writing", 0L)
                            apply()
                        }
                        Toast.makeText(viewModel.getApplication(), "Practice stats cleared successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear All Practice Timers", color = CyberPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Collapsible API developer key section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Advanced AI Configuration",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = isAdvancedExpanded) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.updateApiKey(inputKey)
                        Toast.makeText(viewModel.getApplication(), "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply AI Key Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- ACTIVE DIALOGUE INTERVIEW SCREEN ---
enum class CoachState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

private data class StatusRowData(
    val bgColor: Color,
    val borderColor: Color,
    val status: String,
    val description: String,
    val iconColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun InterviewStatusIndicatorPanel(
    state: CoachState,
    micAmplitude: Float,
    selectedMode: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusGlow")
    
    // Rotating animation for Thinking gear/sync icon
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingRotation"
    )

    // Pulsing alpha for active states
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Pulsing size for Listening mic icon background
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rowData = when (state) {
        CoachState.LISTENING -> {
            StatusRowData(
                bgColor = CyberPink.copy(alpha = 0.08f),
                borderColor = CyberPink,
                status = "Listening to you...",
                description = "Speak clearly. Tap pink stop button below to finish.",
                iconColor = CyberPink,
                icon = Icons.Default.Mic
            )
        }
        CoachState.THINKING -> {
            StatusRowData(
                bgColor = GlowingCyan.copy(alpha = 0.08f),
                borderColor = GlowingCyan,
                status = "AI Coach is Thinking...",
                description = "Processing answer & preparing follow-up query...",
                iconColor = GlowingCyan,
                icon = Icons.Default.Autorenew
            )
        }
        CoachState.SPEAKING -> {
            StatusRowData(
                bgColor = DeepPurple.copy(alpha = 0.08f),
                borderColor = DeepPurple,
                status = "AI Coach is Speaking...",
                description = "Devansh Sir is speaking. Listen closely to the advice.",
                iconColor = DeepPurple,
                icon = Icons.Default.VolumeUp
            )
        }
        CoachState.IDLE -> {
            StatusRowData(
                bgColor = DarkSurface,
                borderColor = DarkBorder,
                status = "Ready for Response",
                description = "Tap purple mic at bottom to speak, or type manually.",
                iconColor = TextSecondary,
                icon = Icons.Default.ChatBubbleOutline
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, rowData.borderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = rowData.bgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Animated icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (state == CoachState.IDLE) DarkSurfaceVariant 
                            else rowData.iconColor.copy(alpha = 0.15f)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = rowData.icon,
                        contentDescription = rowData.status,
                        tint = rowData.iconColor,
                        modifier = Modifier
                            .size(22.dp)
                            .then(
                                when (state) {
                                    CoachState.THINKING -> Modifier.rotate(rotation)
                                    CoachState.LISTENING -> Modifier.scale(pulseScale)
                                    CoachState.SPEAKING -> Modifier.alpha(pulseAlpha)
                                    else -> Modifier
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rowData.status,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (state != CoachState.IDLE) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(pulseAlpha)
                                    .background(rowData.iconColor, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rowData.description,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            // Visual effect decoration on the right
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(26.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    CoachState.LISTENING -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 4) {
                                val barHeight = (micAmplitude * 26 * ((2 + i) / 5f)).dp.coerceIn(4.dp, 20.dp)
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(CircleShape)
                                        .background(CyberPink)
                                )
                            }
                        }
                    }
                    CoachState.THINKING -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(5.dp).alpha(pulseAlpha).background(GlowingCyan, CircleShape))
                            Box(modifier = Modifier.size(5.dp).alpha(pulseAlpha * 0.7f).background(GlowingCyan, CircleShape))
                            Box(modifier = Modifier.size(5.dp).alpha(pulseAlpha * 0.4f).background(GlowingCyan, CircleShape))
                        }
                    }
                    CoachState.SPEAKING -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val step by infiniteTransition.animateFloat(
                                initialValue = 4f,
                                targetValue = 18f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "SpeakVisual"
                            )
                            Box(modifier = Modifier.width(2.5.dp).height(step.dp).clip(CircleShape).background(DeepPurple))
                            Box(modifier = Modifier.width(2.5.dp).height((step * 0.6f).dp).clip(CircleShape).background(DeepPurple))
                            Box(modifier = Modifier.width(2.5.dp).height((step * 1.1f).coerceIn(4f, 18f).dp).clip(CircleShape).background(DeepPurple))
                        }
                    }
                    CoachState.IDLE -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "READY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InterviewScreen(viewModel: InterviewViewModel) {
    val context = LocalContext.current
    val transcript = viewModel.currentTranscript
    val listState = rememberLazyListState()
    val isRecording by AudioHelper.isRecording.collectAsState()
    val isPlaying by AudioHelper.isPlaying.collectAsState()
    val micAmplitude by AudioHelper.micAmplitude.collectAsState()

    // Initialize and manage our new SpeechRecognizerHelper for real-time speech typing
    val speechHelper = remember { SpeechRecognizerHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val isSpeechListening by speechHelper.isListening.collectAsState()
    val spokenTextFromSpeechRecognizer by speechHelper.spokenText.collectAsState()
    val partialTextFromSpeechRecognizer by speechHelper.partialText.collectAsState()
    val errorFromSpeechRecognizer by speechHelper.errorState.collectAsState()
    val speechRmsDb by speechHelper.rmsDb.collectAsState()

    val coachState = when {
        isRecording || isSpeechListening -> CoachState.LISTENING
        viewModel.isThinking || viewModel.isTranscribing -> CoachState.THINKING
        viewModel.isTtsPlaying || isPlaying -> CoachState.SPEAKING
        else -> CoachState.IDLE
    }

    var textInput by remember { mutableStateOf("") }

    // Synchronize SpeechRecognizer live stream text directly with textInput
    LaunchedEffect(partialTextFromSpeechRecognizer, spokenTextFromSpeechRecognizer, isSpeechListening) {
        if (isSpeechListening) {
            val liveText = if (partialTextFromSpeechRecognizer.isNotEmpty()) {
                partialTextFromSpeechRecognizer
            } else if (spokenTextFromSpeechRecognizer.isNotEmpty()) {
                spokenTextFromSpeechRecognizer
            } else {
                null
            }
            if (liveText != null && liveText != textInput) {
                textInput = liveText
            }
        } else {
            if (spokenTextFromSpeechRecognizer.isNotEmpty() && spokenTextFromSpeechRecognizer != textInput) {
                textInput = spokenTextFromSpeechRecognizer
            }
        }
    }

    // Inform user of any SpeechRecognizer issues supportively
    LaunchedEffect(errorFromSpeechRecognizer) {
        errorFromSpeechRecognizer?.let { error ->
            if (error != "No speech match found" && error != "Speech timeout") {
                Toast.makeText(context, "Voice input helper: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-inject draft speech from lessons
    LaunchedEffect(viewModel.courseSpeechDraft) {
        if (viewModel.courseSpeechDraft.isNotEmpty()) {
            textInput = viewModel.courseSpeechDraft
            viewModel.courseSpeechDraft = "" // Reset
        }
    }

    // Auto scroll down as conversational bubbles are appended
    LaunchedEffect(transcript.size, viewModel.isThinking) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.size - 1)
        }
    }

    // Capture permissions for speech recognizer
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                // Ensure audio is stopped first
                viewModel.speechEngine?.stop()
                AudioHelper.stopPlayback()
                speechHelper.startListening()
            } else {
                Toast.makeText(context, "Microphone access is mandatory to speak directly to Devansh Sir.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Pulsing animation for Devansh Sir avatar when speaking or thinking
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
                            text = "Devansh Sir AI Coach",
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
                                    .background(
                                        when (coachState) {
                                            CoachState.LISTENING -> CyberPink
                                            CoachState.THINKING -> GlowingCyan
                                            CoachState.SPEAKING -> DeepPurple
                                            CoachState.IDLE -> Color(0xFF00FF88)
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (coachState) {
                                    CoachState.LISTENING -> "Listening..."
                                    CoachState.THINKING -> "Thinking..."
                                    CoachState.SPEAKING -> "Speaking..."
                                    CoachState.IDLE -> "Ready"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Fast Engine Toggle Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .clickable {
                                    viewModel.voiceEngine = if (viewModel.voiceEngine == "Standard") "Cloud" else "Standard"
                                    Toast.makeText(
                                        context,
                                        "Voice Mode: ${if (viewModel.voiceEngine == "Standard") "Instant Device Voice (Super Fast)" else "AI Cloud Voice"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .border(1.dp, if (viewModel.voiceEngine == "Standard") GlowingCyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.voiceEngine == "Standard") Icons.Default.Bolt else Icons.Default.CloudQueue,
                                contentDescription = "Engine Mode",
                                tint = if (viewModel.voiceEngine == "Standard") GlowingCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.voiceEngine == "Standard") "Instant" else "Pro Voice",
                                color = if (viewModel.voiceEngine == "Standard") TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Voice Gender Toggle (Male/Female)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .clickable {
                                    viewModel.voiceGender = if (viewModel.voiceGender == "Female") "Male" else "Female"
                                    Toast.makeText(
                                        context,
                                        "Voice profile set to: ${viewModel.voiceGender}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Instantly replay the last AI speech in the new voice
                                    viewModel.currentTranscript.lastOrNull { it.role == "model" }?.let { lastMsg ->
                                        AudioHelper.stopPlayback()
                                        viewModel.speakQuestion(lastMsg.text)
                                    }
                                }
                                .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.voiceGender == "Female") Icons.Default.Face else Icons.Default.FaceRetouchingNatural,
                                contentDescription = "Speaker Voice",
                                tint = GlowingCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewModel.voiceGender,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

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

        // Live Interview Status Indicator Dashboard
        InterviewStatusIndicatorPanel(
            state = coachState,
            micAmplitude = micAmplitude,
            selectedMode = viewModel.selectedMode
        )

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
                            text = "Begin speaking when Devansh Sir presents the question. Keep responses detailed, structuring logical examples.",
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
                // Dynamic Vocabulary & Phrase Booster Hints
                val topicHints = when (viewModel.selectedTopic) {
                    "Tech & AI Society" -> listOf(
                        "automation" to "The use of technology to perform tasks with minimal human intervention.",
                        "paradigm shift" to "A fundamental change in approach or underlying assumptions.",
                        "disruptive" to "Innovative technology that completely shakes up an industry."
                    )
                    "Software Engineering Career" -> listOf(
                        "scalability" to "The capacity of a system to grow and handle increased load.",
                        "technical debt" to "The future cost of choosing an easy solution now instead of a better approach.",
                        "refactoring" to "Restructuring existing computer code without changing its external behavior."
                    )
                    "Product Management Challenge" -> listOf(
                        "MVP" to "Minimum Viable Product; a product with just enough features to satisfy early customers.",
                        "alignment" to "Agreement among different groups/stakeholders on a common goal.",
                        "user empathy" to "Deeply understanding and sharing the feelings and pain points of users."
                    )
                    "General English Conversation" -> listOf(
                        "coherence" to "The quality of being logical, orderly, and clearly connected.",
                        "fluency" to "The ability to express oneself easily and articulately.",
                        "eloquent" to "Fluent or persuasive in speaking or writing."
                    )
                    "Global Economy & Business" -> listOf(
                        "globalization" to "The process by which businesses develop international influence.",
                        "market saturation" to "A situation in which no more of a product can be sold in a market.",
                        "inflationary" to "Characterized by or causing a general increase in prices."
                    )
                    "Customer Success & Leadership" -> listOf(
                        "retention" to "The ability of a company to keep its customers over a period of time.",
                        "proactive" to "Controlling a situation by causing something to happen rather than responding to it.",
                        "empathy" to "Understanding and sharing the emotions and experiences of others."
                    )
                    "IELTS Academic - Education & Learning" -> listOf(
                        "pedagogical" to "Relating to teaching methods and educational theory.",
                        "cognitive" to "Relating to the mental action or process of acquiring knowledge.",
                        "curriculum" to "The subjects comprising a course of study in a school or college."
                    )
                    else -> listOf(
                        "consequently" to "As a result; therefore.",
                        "paramount" to "More important than anything else; supreme.",
                        "furthermore" to "In addition; besides (used to introduce a fresh point)."
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Vocab Booster",
                                tint = GlowingCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Vocab Booster",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Tap to define & insert",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topicHints.forEach { (word, definition) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        // Highlight definition in toast
                                        Toast.makeText(context, "$word: $definition", Toast.LENGTH_LONG).show()
                                        // Insert word into text input field
                                        val separator = if (textInput.isEmpty() || textInput.endsWith(" ")) "" else " "
                                        textInput = "$textInput$separator$word"
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                            painter = painterResource(id = R.drawable.img_devansh_sir_1783096828673),
                            contentDescription = "Portrait",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // GLOWING MICROPHONE BUTTON (Press/Tap to speak in real-time)
                    val liveRmsPulse = if (isSpeechListening) {
                        1f + (speechRmsDb.coerceAtLeast(0f) / 12f).coerceAtMost(0.4f)
                    } else {
                        1f
                    }
                    IconButton(
                        onClick = {
                            if (isSpeechListening) {
                                speechHelper.stopListening()
                            } else {
                                // Request mic permission dynamically
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.speechEngine?.stop()
                                    AudioHelper.stopPlayback()
                                    speechHelper.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size((72f * liveRmsPulse).dp)
                            .background(
                                color = if (isSpeechListening) CyberPink else DeepPurple,
                                shape = CircleShape
                            )
                            .testTag("microphone_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeechListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Microphone Trigger",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Simple help hint tooltip
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Tap the purple mic to start real-time speaking. Watch your words transcribe instantly, edit if needed, and tap Send to submit to Devansh Sir!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .background(DarkSurfaceVariant, CircleShape)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Instructions Help", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time voice listening active banner (Web Speech style)
                if (isSpeechListening) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val blinkingAlpha by rememberInfiniteTransition().animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "blinking"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(blinkingAlpha)
                                .background(CyberPink, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (textInput.isEmpty()) "Sir is listening... Start speaking now" else "Listening: \"${if (textInput.length > 35) "..." + textInput.takeLast(35) else textInput}\"",
                            color = CyberPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

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
            Box(modifier = Modifier.size(48.dp))
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

@Composable
fun LessonDetailScreen(
    chapter: com.example.data.LearningChapter,
    isCompleted: Boolean,
    accentColor: Color,
    viewModel: InterviewViewModel,
    onBack: () -> Unit,
    onCompleteToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var isCorrectAnswer by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(DarkSurfaceVariant, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Courses",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = chapter.subtitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text = chapter.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 1. Core Tutoring Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Lesson Icon",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Core Concept",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = chapter.coreConcept,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2. Vocabulary Bank Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Vocab Icon",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vocabulary Bank",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                chapter.vocabulary.forEach { (word, def) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = word,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = def,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 3. Interactive Quiz Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp, 
                    if (showExplanation) (if (isCorrectAnswer) Color(0xFF00FF88).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f)) else DarkBorder, 
                    RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Quiz",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Concept Check Challenge",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00FF88).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "MASTERED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = chapter.quiz.question,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                chapter.quiz.options.forEachIndexed { index, option ->
                    val isSelected = selectedOptionIndex == index
                    val optionBg = if (isSelected) accentColor.copy(alpha = 0.12f) else DarkSurfaceVariant
                    val optionBorder = if (isSelected) accentColor else DarkBorder
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(optionBg)
                            .border(1.dp, optionBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                if (!showExplanation) {
                                    selectedOptionIndex = index
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .border(1.dp, if (isSelected) accentColor else TextSecondary, CircleShape)
                                .background(if (isSelected) accentColor else Color.Transparent, CircleShape)
                                .padding(3.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                if (!showExplanation) {
                    Button(
                        onClick = {
                            if (selectedOptionIndex != null) {
                                showExplanation = true
                                isCorrectAnswer = selectedOptionIndex == chapter.quiz.correctAnswer
                                if (isCorrectAnswer) {
                                    onCompleteToggle(true)
                                }
                            } else {
                                Toast.makeText(context, "Please select an answer first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Submit Answer", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCorrectAnswer) Color(0xFF00FF88).copy(alpha = 0.08f) else Color.Red.copy(alpha = 0.08f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrectAnswer) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = if (isCorrectAnswer) "Correct" else "Incorrect",
                                tint = if (isCorrectAnswer) Color(0xFF00FF88) else Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCorrectAnswer) "Excellent Job! Correct Answer." else "Let's review the rule again.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrectAnswer) Color(0xFF00FF88) else Color.Red
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = chapter.quiz.explanation,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (!isCorrectAnswer) {
                        Button(
                            onClick = {
                                showExplanation = false
                                selectedOptionIndex = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Try Again", color = TextPrimary)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 4. Live Speaking Practice Link with Devansh Sir
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DeepPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🗣️ Live Practice with Devansh Sir",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Put your knowledge into action! Tap the button below to load this practice topic, switch back to the AI Interview Coach, and practice speaking about this concept directly with Devansh Sir.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val firstWord = chapter.vocabulary.firstOrNull()?.first ?: "the concept"
                        viewModel.selectedTopic = chapter.title
                        val isInterviewTopic = com.example.data.LearningCourseData.interviewPrepList.any { it.id == chapter.id }
                        viewModel.selectedMode = if (isInterviewTopic) "HR Recruiter" else "IELTS Examiner"
                        viewModel.courseSpeechDraft = "Devansh Sir, I would like to practice '${chapter.title}'. Today I learned about '${firstWord}'. Can we practice a scenario using this?"
                        viewModel.startNewInterview()
                        viewModel.selectedTab = 0 // Switch to Interview Tab
                        Toast.makeText(context, "Live Speaking Session started for '${chapter.title}'!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Practice Speaking Now", color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- TAB 2: PRACTICING LISTENING, READING, WRITING ---
@Composable
fun PracticeTab(viewModel: InterviewViewModel) {
    val context = LocalContext.current
    var mainTabMode by remember { mutableStateOf("courses") } // "courses", "skills"
    var selectedCourseTab by remember { mutableStateOf("general") } // "general", "interview", "office"
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    
    // Save completed chapters locally using SharedPreferences
    val sharedPrefs = remember { context.getSharedPreferences("user_courses_progress_v2", Context.MODE_PRIVATE) }
    val completedChapters = remember { mutableStateMapOf<String, Boolean>() }
    
    LaunchedEffect(Unit) {
        val saved = sharedPrefs.getStringSet("completed_chapters", emptySet()) ?: emptySet()
        saved.forEach { id ->
            completedChapters[id] = true
        }
    }

    var activeTab by remember { mutableStateOf("listening") } // "listening", "reading", "writing"
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    
    // Track attempted practice answers locally
    val completedExercises = remember { mutableStateMapOf<String, String>() } // exerciseId -> selectedOption
    val exerciseScores = remember { mutableStateMapOf<String, Boolean>() } // exerciseId -> isCorrect
    
    // Writing evaluation results
    var isEvaluatingWriting by remember { mutableStateOf(false) }
    var writingFeedback by remember { mutableStateOf<String?>(null) }
    var userEssayInput by remember { mutableStateOf("") }

    // Clean up playback when leaving the practice tab
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListeningSpeech()
        }
    }

    if (selectedChapterId != null) {
        val allChapters = com.example.data.LearningCourseData.generalEnglishList + 
                           com.example.data.LearningCourseData.interviewPrepList + 
                           com.example.data.LearningCourseData.officeEnglishList
        val chapter = allChapters.firstOrNull { it.id == selectedChapterId }
        
        if (chapter != null) {
            val accentColor = when (selectedCourseTab) {
                "general" -> Color(0xFF00FF88) // Vibrant Mint/Teal
                "interview" -> GlowingCyan // Glowing Cyan
                else -> DeepPurple // Deep Purple
            }
            
            LessonDetailScreen(
                chapter = chapter,
                isCompleted = completedChapters.containsKey(chapter.id),
                accentColor = accentColor,
                viewModel = viewModel,
                onBack = { selectedChapterId = null },
                onCompleteToggle = { completed ->
                    if (completed) {
                        completedChapters[chapter.id] = true
                    } else {
                        completedChapters.remove(chapter.id)
                    }
                    sharedPrefs.edit().putStringSet("completed_chapters", completedChapters.keys.toSet()).apply()
                }
            )
        } else {
            selectedChapterId = null
        }
    } else if (selectedExerciseId == null) {
        // --- LIST VIEW ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Main mode selector at the very top (📚 Guided Courses vs 🎯 IELTS Skills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val modes = listOf(
                    "courses" to "📚 Guided Courses",
                    "skills" to "🎯 Exam Practice"
                )
                modes.forEach { (modeKey, label) ->
                    val isSelected = mainTabMode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DeepPurple.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) GlowingCyan.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { mainTabMode = modeKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) GlowingCyan else TextSecondary
                        )
                    }
                }
            }
            
            if (mainTabMode == "courses") {
                // --- GUIDED COURSES LAYOUT ---
                val courseName = when (selectedCourseTab) {
                    "general" -> "General English Mastery"
                    "interview" -> "Professional Interview Prep"
                    else -> "Executive Office English"
                }
                val courseDesc = when (selectedCourseTab) {
                    "general" -> "Master daily English, greetings, grammar & conversations"
                    "interview" -> "Nail behavioral questions, pitch, and salary negotiations"
                    else -> "Speak confidently in standups, emails, meetings, and presentations"
                }
                val courseAccent = when (selectedCourseTab) {
                    "general" -> Color(0xFF00FF88)
                    "interview" -> GlowingCyan
                    else -> DeepPurple
                }
                
                val currentList = when (selectedCourseTab) {
                    "general" -> com.example.data.LearningCourseData.generalEnglishList
                    "interview" -> com.example.data.LearningCourseData.interviewPrepList
                    else -> com.example.data.LearningCourseData.officeEnglishList
                }
                
                val completedInCourse = currentList.count { completedChapters.containsKey(it.id) }
                val totalInCourse = currentList.size
                val progressPercent = if (totalInCourse > 0) completedInCourse.toFloat() / totalInCourse.toFloat() else 0f
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .border(1.dp, courseAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = courseAccent.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = courseName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = courseDesc,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(courseAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedCourseTab == "general") Icons.Default.Star else Icons.Default.MenuBook,
                                    contentDescription = "Course Icon",
                                    tint = courseAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Progress bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Course Progress",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$completedInCourse / $totalInCourse Mastered",
                                fontSize = 10.sp,
                                color = courseAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progressPercent,
                            color = courseAccent,
                            trackColor = DarkBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
                
                // Course tabs selector row (General English, Interview Prep, Office English)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val courseTabs = listOf(
                        Triple("general", "General English", "general"),
                        Triple("interview", "Interview Prep", "interview"),
                        Triple("office", "Office English", "office")
                    )
                    courseTabs.forEach { (tabKey, label, tag) ->
                        val isSelected = selectedCourseTab == tabKey
                        val currentAccent = when (tabKey) {
                            "general" -> Color(0xFF00FF88)
                            "interview" -> GlowingCyan
                            else -> DeepPurple
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) currentAccent.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) currentAccent.copy(alpha = 0.6f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCourseTab = tabKey }
                                .padding(vertical = 8.dp)
                                .testTag("course_tab_$tag"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) currentAccent else TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Scrollable List of Course Chapters
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentList) { item ->
                        val isCompleted = completedChapters.containsKey(item.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedChapterId = item.id }
                                .border(
                                    1.dp,
                                    if (isCompleted) Color(0xFF00FF88).copy(alpha = 0.4f) else DarkBorder,
                                    RoundedCornerShape(14.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.subtitle.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = courseAccent,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isCompleted) "Completed & Mastered ✓" else "Tap to start interactive lesson",
                                        fontSize = 10.sp,
                                        color = if (isCompleted) Color(0xFF00FF88) else TextSecondary
                                    )
                                }
                                
                                Box(modifier = Modifier.padding(start = 12.dp)) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                        contentDescription = "Status",
                                        tint = if (isCompleted) Color(0xFF00FF88) else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // Practice Hero Banner
                Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(bottom = 14.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.practice_hero_banner),
                        contentDescription = "Practice Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient dark overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Daily Practice Hub",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Polish your Listening, Reading, and Writing daily",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Section Switcher tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val sections = listOf(
                    Triple("listening", "🎧 Listening", "nav_prac_listening"),
                    Triple("reading", "📖 Reading", "nav_prac_reading"),
                    Triple("writing", "✍️ Writing", "nav_prac_writing"),
                    Triple("speaking", "🎙️ Speaking", "nav_prac_speaking")
                )
                
                sections.forEach { (secKey, secLabel, tag) ->
                    val isSelected = activeTab == secKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DeepPurple.copy(alpha = 0.3f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) GlowingCyan.copy(alpha = 0.6f) else Color.Transparent,
                               RoundedCornerShape(8.dp)
                            )
                            .clickable { 
                                activeTab = secKey
                                viewModel.activePracticeCategory = secKey
                            }
                            .padding(vertical = 10.dp)
                            .testTag(tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = secLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) GlowingCyan else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable list of exercises
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                when (activeTab) {
                    "listening" -> {
                        items(PracticeData.listeningList) { item ->
                            val attempted = completedExercises.containsKey(item.id)
                            val isCorrect = exerciseScores[item.id] ?: false
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedExerciseId = item.id }
                                    .border(
                                        1.dp,
                                        if (attempted) (if (isCorrect) Color(0xFF00FF88).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f)) else DarkBorder,
                                        RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlowingCyan,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Speaker: ${item.speaker}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Box(modifier = Modifier.padding(start = 12.dp)) {
                                        if (attempted) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = "Status",
                                                tint = if (isCorrect) Color(0xFF00FF88) else Color.Red,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Start",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "reading" -> {
                        items(PracticeData.readingList) { item ->
                            val attempted = completedExercises.containsKey(item.id)
                            val isCorrect = exerciseScores[item.id] ?: false
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedExerciseId = item.id }
                                    .border(
                                        1.dp,
                                        if (attempted) (if (isCorrect) Color(0xFF00FF88).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f)) else DarkBorder,
                                        RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberPink,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Box(modifier = Modifier.padding(start = 12.dp)) {
                                        if (attempted) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = "Status",
                                                tint = if (isCorrect) Color(0xFF00FF88) else Color.Red,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = "Read",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "writing" -> {
                        items(PracticeData.writingList) { item ->
                            val attempted = completedExercises.containsKey(item.id)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExerciseId = item.id
                                        userEssayInput = ""
                                        writingFeedback = null
                                    }
                                    .border(1.dp, if (attempted) DeepPurple.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurple,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Box(modifier = Modifier.padding(start = 12.dp)) {
                                        if (attempted) {
                                            Box(
                                                modifier = Modifier
                                                    .background(GlowingCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "SAVED DRAFT",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GlowingCyan
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Create,
                                                contentDescription = "Write",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "speaking" -> {
                        items(com.example.data.PracticeData.speakingList) { item ->
                            val attempted = completedExercises.containsKey(item.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExerciseId = item.id
                                    }
                                    .border(1.dp, if (attempted) GlowingCyan.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlowingCyan,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Target: ${item.recommendedDuration}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(GlowingCyan.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Speaking Practice",
                                            tint = GlowingCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    } else {
        // --- DETAILED EXERCISE VIEW ---
        val exerciseId = selectedExerciseId!!
        
        when (activeTab) {
            "listening" -> {
                val item = PracticeData.listeningList.first { it.id == exerciseId }
                var selectedOption by remember { mutableStateOf<String?>(completedExercises[item.id]) }
                var showFeedback by remember { mutableStateOf(completedExercises.containsKey(item.id)) }
                var isPlayingSpeech by remember { mutableStateOf(false) }
                var audioProgress by remember { mutableStateOf(0f) }

                // Stop speech if they leave the detailed view
                DisposableEffect(item.id) {
                    onDispose {
                        viewModel.stopListeningSpeech()
                    }
                }

                // Simulate audio track progression
                LaunchedEffect(isPlayingSpeech) {
                    if (isPlayingSpeech) {
                        while (audioProgress < 1.0f && isPlayingSpeech) {
                            delay(150)
                            audioProgress += 0.005f
                        }
                        if (audioProgress >= 1.0f) {
                            isPlayingSpeech = false
                            audioProgress = 0f
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.stopListeningSpeech()
                                isPlayingSpeech = false
                                selectedExerciseId = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Listening Challenge", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlowingCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlowingCyan
                            )
                            Text(
                                text = item.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Speaker: ${item.speaker}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Interactive Audio Panel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isPlayingSpeech) {
                                                viewModel.stopListeningSpeech()
                                                isPlayingSpeech = false
                                            } else {
                                                viewModel.playListeningSpeech(item.audioText)
                                                isPlayingSpeech = true
                                                audioProgress = 0f
                                            }
                                        },
                                        modifier = Modifier
                                            .background(if (isPlayingSpeech) CyberPink else GlowingCyan, CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingSpeech) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingSpeech) "Stop" else "Play",
                                            tint = Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (isPlayingSpeech) "Listening playing..." else "Ready to play",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Speech Synthesis Accent Engine",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                PulsingSoundwave(isPlaying = isPlayingSpeech)
                            }

                            if (isPlayingSpeech) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = audioProgress,
                                    color = GlowingCyan,
                                    trackColor = DarkBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Custom Audio Profile & Accent Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Voice Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .clickable {
                                            viewModel.voiceGender = if (viewModel.voiceGender == "Female") "Male" else "Female"
                                            Toast.makeText(
                                                viewModel.getApplication(),
                                                "Active Speaker set to: ${viewModel.voiceGender} Voice",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.voiceGender == "Female") Icons.Default.Face else Icons.Default.FaceRetouchingNatural,
                                        contentDescription = "Speaker Voice",
                                        tint = GlowingCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${viewModel.voiceGender} Profile",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Pace Selector
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val rates = listOf(0.8f, 1.0f, 1.2f)
                                    rates.forEach { rate ->
                                        val isSelected = viewModel.speechRate == rate
                                        Text(
                                            text = "${rate}x",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) GlowingCyan else TextSecondary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) DeepPurple.copy(alpha = 0.3f) else Color.Transparent)
                                                .clickable { viewModel.speechRate = rate }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Question Block
                    Text(
                        text = "Question:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = item.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Options list (loop construct avoids lambda context warnings)
                    for (option in item.options) {
                        val isCurrent = selectedOption == option
                        val isCorrectOption = option == item.options[item.correctAnswer]
                        
                        val borderColor = if (showFeedback) {
                            if (isCorrectOption) Color(0xFF00FF88) else if (isCurrent) Color.Red else DarkBorder
                        } else {
                            if (isCurrent) GlowingCyan else DarkBorder
                        }

                        val bgColor = if (showFeedback) {
                            if (isCorrectOption) Color(0xFF00FF88).copy(alpha = 0.08f) else if (isCurrent) Color.Red.copy(alpha = 0.08f) else DarkSurface
                        } else {
                            if (isCurrent) DeepPurple.copy(alpha = 0.15f) else DarkSurface
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = !showFeedback) { selectedOption = option },
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCurrent,
                                    onClick = { if (!showFeedback) selectedOption = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GlowingCyan,
                                        unselectedColor = TextSecondary
                                    ),
                                    enabled = !showFeedback
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons
                    if (!showFeedback) {
                        Button(
                            onClick = {
                                if (selectedOption != null) {
                                    val isCorrect = selectedOption == item.options[item.correctAnswer]
                                    completedExercises[item.id] = selectedOption!!
                                    exerciseScores[item.id] = isCorrect
                                    showFeedback = true
                                    
                                    // Start tracking extra practice time spent
                                    viewModel.isTrackingTime = true
                                }
                            },
                            enabled = selectedOption != null,
                            colors = ButtonDefaults.buttonColors(containerColor = GlowingCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_listening_answer")
                        ) {
                            Text("Submit Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Correct/Incorrect message banner
                        val userCorrect = exerciseScores[item.id] ?: false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (userCorrect) Color(0xFF00FF88) else Color.Red,
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (userCorrect) Color(0xFF00FF88).copy(alpha = 0.05f) else Color.Red.copy(alpha = 0.05f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (userCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = "Status",
                                        tint = if (userCorrect) Color(0xFF00FF88) else Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (userCorrect) "Correct Answer! 🎉" else "Incorrect Choice! ❌",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userCorrect) Color(0xFF00FF88) else Color.Red
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Explanation:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = item.explanation,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                selectedOption = null
                                showFeedback = false
                                completedExercises.remove(item.id)
                                exerciseScores.remove(item.id)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset and Try Again")
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            "reading" -> {
                val item = PracticeData.readingList.first { it.id == exerciseId }
                var selectedOption by remember { mutableStateOf<String?>(completedExercises[item.id]) }
                var showFeedback by remember { mutableStateOf(completedExercises.containsKey(item.id)) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedExerciseId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading Passage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Banner
                    Text(
                        text = item.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPink,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = item.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reading text display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = item.passage,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smart IELTS Interactive Vocab Helper
                    var showGlossary by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, if (showGlossary) GlowingCyan.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { showGlossary = !showGlossary }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = "Glossary", tint = GlowingCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Interactive Vocabulary Booster", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Tap to view high-scoring academic terms", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                        Icon(
                            imageVector = if (showGlossary) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = showGlossary) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Key Academic Glossary for this passage:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlowingCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val glossaryItems = when (item.id) {
                                    "R1" -> listOf(
                                        "Geothermal" to "Relating to or produced by the internal heat of the earth.",
                                        "Chemosynthesis" to "The biological synthesis of organic compounds from energy derived from inorganic chemical reactions.",
                                        "Extraterrestrial" to "Originating, located, or occurring outside Earth or its atmosphere."
                                    )
                                    "R2" -> listOf(
                                        "Optimal" to "Most desirable or satisfactory; best or peak condition.",
                                        "Consciousness" to "The state of being aware of and responsive to one's surroundings.",
                                        "Cognitive" to "Relating to the mental action or process of acquiring knowledge and understanding."
                                    )
                                    "R3" -> listOf(
                                        "Photovoltaic" to "Relating to the production of electric current at the junction of two substances exposed to light.",
                                        "Breakthrough" to "A sudden, dramatic, and important discovery or development.",
                                        "Capital Costs" to "One-time expenses incurred on the purchase of land, buildings, and equipment."
                                    )
                                    else -> listOf(
                                        "Academic" to "Relating to education and scholarship; formal or high-level vocabulary.",
                                        "Linguistic" to "Relating to language or linguistics.",
                                        "Comprehension" to "The action or capability of understanding something."
                                    )
                                }

                                glossaryItems.forEach { (word, definition) ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("• ", color = GlowingCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(word, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(definition, color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Question block
                    Text(
                        text = "Linguistic Comprehension Question:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = item.question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Options list
                    for (option in item.options) {
                        val isCurrent = selectedOption == option
                        val isCorrectOption = option == item.options[item.correctAnswer]

                        val borderColor = if (showFeedback) {
                            if (isCorrectOption) Color(0xFF00FF88) else if (isCurrent) Color.Red else DarkBorder
                        } else {
                            if (isCurrent) CyberPink else DarkBorder
                        }

                        val bgColor = if (showFeedback) {
                            if (isCorrectOption) Color(0xFF00FF88).copy(alpha = 0.08f) else if (isCurrent) Color.Red.copy(alpha = 0.08f) else DarkSurface
                        } else {
                            if (isCurrent) DeepPurple.copy(alpha = 0.15f) else DarkSurface
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = !showFeedback) { selectedOption = option },
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCurrent,
                                    onClick = { if (!showFeedback) selectedOption = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CyberPink,
                                        unselectedColor = TextSecondary
                                    ),
                                    enabled = !showFeedback
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Answer
                    if (!showFeedback) {
                        Button(
                            onClick = {
                                if (selectedOption != null) {
                                    val isCorrect = selectedOption == item.options[item.correctAnswer]
                                    completedExercises[item.id] = selectedOption!!
                                    exerciseScores[item.id] = isCorrect
                                    showFeedback = true
                                    
                                    viewModel.isTrackingTime = true
                                }
                            },
                            enabled = selectedOption != null,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_reading_answer")
                        ) {
                            Text("Submit Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val userCorrect = exerciseScores[item.id] ?: false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (userCorrect) Color(0xFF00FF88) else Color.Red,
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (userCorrect) Color(0xFF00FF88).copy(alpha = 0.05f) else Color.Red.copy(alpha = 0.05f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (userCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = "Status",
                                        tint = if (userCorrect) Color(0xFF00FF88) else Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (userCorrect) "Correct Answer! 🎉" else "Incorrect Choice! ❌",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userCorrect) Color(0xFF00FF88) else Color.Red
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Passage Explanation:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = item.explanation,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                selectedOption = null
                                showFeedback = false
                                completedExercises.remove(item.id)
                                exerciseScores.remove(item.id)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset and Try Again")
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            "writing" -> {
                val item = PracticeData.writingList.first { it.id == exerciseId }
                val words = userEssayInput.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedExerciseId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Writing Evaluator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Prompts card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DeepPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepPurple,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.prompt,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (writingFeedback == null) {
                        // Target Vocabulary checklist
                        val targetWords = when (item.id) {
                            "W1" -> listOf("proliferation", "obsolescence", "augment")
                            "W2" -> listOf("unsustainable", "consumerism", "ecological")
                            else -> listOf("paramount", "consequently", "advocate")
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🎯 High-Scoring Vocabulary Challenge",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingCyan
                                )
                                Text(
                                    text = "Incorporate these terms into your essay to boost your score:",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    targetWords.forEach { word ->
                                        val containsWord = userEssayInput.lowercase().contains(word)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (containsWord) Color(0xFF00FF88).copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                                .border(
                                                    1.dp,
                                                    if (containsWord) Color(0xFF00FF88) else DarkBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { /* no-op */ }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (containsWord) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Used",
                                                        tint = Color(0xFF00FF88),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = word,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (containsWord) Color(0xFF00FF88) else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Essay Editor Box
                        Text(
                            text = "Compose Your Essay:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = userEssayInput,
                            onValueChange = { userEssayInput = it },
                            placeholder = { Text("Start typing your essay response here...", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("writing_essay_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = DeepPurple,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Word count tracker row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Linguistic Evaluation Requirement",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            
                            Box(
                                modifier = Modifier
                                    .background(if (words >= item.minimumWords) Color(0xFF00FF88).copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$words / ${item.minimumWords} words",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (words >= item.minimumWords) Color(0xFF00FF88) else Color.Red
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isEvaluatingWriting) {
                            // Pulsing loading state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = GlowingCyan, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "AI Assessor Devansh Sir is analyzing grammar...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Scoring vocabulary, sentence structure, and task response coherence",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (userEssayInput.trim().isNotEmpty()) {
                                        viewModel.evaluateWritingEssay(
                                            title = item.title,
                                            prompt = item.prompt,
                                            essay = userEssayInput,
                                            onLoading = { isEvaluatingWriting = it },
                                            onResult = {
                                                writingFeedback = it
                                                completedExercises[item.id] = userEssayInput
                                            }
                                        )
                                    }
                                },
                                enabled = userEssayInput.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("analyze_writing_essay")
                            ) {
                                Text("Analyze Essay with AI Assessor", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Display AI evaluation report
                        Text(
                            text = "AI Evaluation Report:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowingCyan,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlowingCyan, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = writingFeedback!!,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Left
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Contrast model answer box
                        Text(
                            text = "Assessor's Model Band 9 Essay:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = item.sampleAnswer,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                writingFeedback = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Re-Write Response", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            "speaking" -> {
                val item = com.example.data.PracticeData.speakingList.first { it.id == exerciseId }
                var selectedGender by remember { mutableStateOf(viewModel.voiceGender) }
                var selectedDiff by remember { mutableStateOf(if (viewModel.interviewDifficulty == "Advanced") "Advanced" else "Medium") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // Header Row with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedExerciseId = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Speaking Coach",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlowingCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(GlowingCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.category.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlowingCyan,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(DeepPurple.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Target: ${item.recommendedDuration}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurple,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Speak live on this topic with our dynamic speech agent. The agent will listen to your answers, provide helpful phrasing transitions, and change questions on the fly.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Exercise Prompt Box
                    Text(
                        text = "Practice Prompt / Scenario",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = item.prompt,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Suggested Vocabulary Suggestions
                    Text(
                        text = "Suggested Native Phrasing & Collocations",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val vocabulary = when (item.id) {
                        "S1" -> listOf(
                            "Paradigm shift" to "A fundamental change in approach or underlying assumptions.",
                            "Organic craftsmanship" to "Artistic creation done entirely by human hand.",
                            "Democratizing creativity" to "Making artistic tools accessible to everyone.",
                            "Synthesized assets" to "AI-generated images or audio clips."
                        )
                        "S2" -> listOf(
                            "Technical debt" to "Cost of prioritizing speedy development over clean code.",
                            "Monolithic vs microservices" to "Architectural styles comparing unified vs distributed design.",
                            "Asymptotic complexity" to "The mathematical representation of scalability (e.g., Big-O).",
                            "High availability" to "Systems remaining operational and reliable for long spans."
                        )
                        "S3" -> listOf(
                            "RICE framework" to "Prioritization based on Reach, Impact, Confidence, and Effort.",
                            "Cross-functional alignment" to "Getting engineering, product, and sales on the same page.",
                            "Cognitive load" to "The amount of mental effort being used in the working memory.",
                            "Incremental deliverable" to "An intermediate release adding specific feature value."
                        )
                        "S4" -> listOf(
                            "Vibrant atmosphere" to "A lively, energetic environment or feeling of a place.",
                            "Cultural heritage" to "Customs, traditions, and historic relics of a community.",
                            "Sights and sounds" to "The visual and auditory experiences of traveling.",
                            "Lasting impression" to "A memory or impact that stays with you for a long time."
                        )
                        "S5" -> listOf(
                            "Critical threshold" to "A level at which minor changes lead to drastic results.",
                            "Bioaccumulate" to "The gradual accumulation of substances, like toxins, in an organism.",
                            "Ecological restoration" to "Assisting the recovery of ecosystems that have been degraded.",
                            "Sustainable stewardship" to "Responsible planning and management of earth's resources."
                        )
                        else -> listOf(
                            "Spoken fluency" to "The ability to express thoughts naturally and continuously.",
                            "Intonation patterns" to "The rise and fall of voice pitch in spoken language.",
                            "Idiomatic expressions" to "Phrases whose meanings are not predictable from individual words.",
                            "Systematic practice" to "Structured, consistent training of a specific skill."
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            vocabulary.forEach { (word, desc) ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = "• ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlowingCyan
                                    )
                                    Column {
                                        Text(
                                            text = word,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlowingCyan
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // AI Agent Session Settings (Voice Gender and Difficulty)
                    Text(
                        text = "AI Agent Setup",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Voice Selection (Male/Female)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Agent Voice", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    listOf("Female", "Male").forEach { gender ->
                                        val isSel = selectedGender == gender
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) GlowingCyan.copy(alpha = 0.2f) else Color.Transparent)
                                                .border(1.dp, if (isSel) GlowingCyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable { selectedGender = gender }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = gender,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) GlowingCyan else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Difficulty Selection (Medium/Advanced)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Difficulty", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    listOf("Medium", "Advanced").forEach { diff ->
                                        val isSel = selectedDiff == diff
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) GlowingCyan.copy(alpha = 0.2f) else Color.Transparent)
                                                .border(1.dp, if (isSel) GlowingCyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable { selectedDiff = diff }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = diff,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) GlowingCyan else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Launch Session Button
                    Button(
                        onClick = {
                            viewModel.voiceGender = selectedGender
                            viewModel.interviewDifficulty = selectedDiff
                            viewModel.selectedTopic = "${item.title}: ${item.prompt}"
                            viewModel.selectedMode = if (item.id in listOf("S2", "S3")) "HR Recruiter" else "IELTS Examiner"
                            viewModel.startNewInterview()
                            viewModel.selectedTab = 0 // Switch to Live Interview tab!
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowingCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("launch_speaking_session_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Launch", tint = DarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Live Speaking Session",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

// --- ACTIVE PULSING SOUNDWAVE ANIMATOR ---
@Composable
fun PulsingSoundwave(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val heights = listOf(15.dp, 32.dp, 20.dp, 36.dp, 16.dp, 28.dp)
    
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { idx, baseHeight ->
            val animHeightFloat = if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 6f,
                    targetValue = baseHeight.value,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + idx * 85, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                ).value
            } else {
                6f
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animHeightFloat.dp)
                    .background(GlowingCyan, RoundedCornerShape(2.dp))
            )
        }
    }
}

// --- VIDEO PRACTICE HUB TAB (LATEST COMPOSABLE) ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoHubTab(viewModel: InterviewViewModel) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedVideoForPlayback by remember { mutableStateOf<PracticeVideo?>(null) }

    val categories = listOf("All", "Pronunciation Practice", "IELTS Speaking", "Job Interview", "Office Role Play", "Daily English")

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedVideoForPlayback == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // --- WELCOME CARD (STRICT REQUIREMENT) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, GlowingCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Hub",
                                tint = GlowingCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Rahul's Practice Hub",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Welcome to Video Practice Hub! Learn valuable speaking tips by Devansh Sir to build sentence formations, overcome hesitation, and gain confidence.",
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { selectedCategoryFilter = "All" },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Browse", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browse Feed", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }

                // --- FILTER SECTION ---
                Text(
                    text = "Filter speaking categories",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            onClick = { selectedCategoryFilter = cat },
                            color = if (isSelected) GlowingCyan else DarkSurface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isSelected) GlowingCyan else DarkBorder),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) DarkBackground else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // --- VIDEOS FEED ---
                Text(
                    text = "Latest Speaking Videos",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val filteredVideos = if (selectedCategoryFilter == "All") {
                    viewModel.videosList
                } else {
                    viewModel.videosList.filter { it.category == selectedCategoryFilter }
                }

                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Empty", tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No videos in this category yet.", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    filteredVideos.forEach { video ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { selectedVideoForPlayback = video }
                        ) {
                            Column {
                                // Thumbnail mockup with Devansh Sir's image or practice hero banner and a dark overlay for play icon readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Show Devansh Sir's image or general practice hero banner
                                    val imageRes = if (video.title.contains("Devansh") || video.title.contains("Day 2")) {
                                        R.drawable.img_devansh_sir_1783096828673
                                    } else {
                                        R.drawable.practice_hero_banner
                                    }
                                    Image(
                                        painter = painterResource(id = imageRes),
                                        contentDescription = "Video Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Dim overlay for play circle readability
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Play icon",
                                        tint = GlowingCyan,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    // category badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(CyberPink, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(video.category, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // duration badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(video.duration, color = Color.White, fontSize = 10.sp)
                                    }
                                }

                                // Video Meta
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = video.title,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = video.description,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = "Uploader", tint = GlowingCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(video.uploader, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Likes",
                                                tint = if (video.isLiked) CyberPink else TextSecondary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.likeVideo(video.id) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${video.likes} likes", color = TextSecondary, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(Icons.Default.Comment, contentDescription = "Comments", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${video.comments.size}", color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- FULL SCREEN IMMERSIVE VIDEO PLAYER OVERLAY PAGE ---
            val activeVideo = selectedVideoForPlayback!!
            val currentVideo = viewModel.videosList.firstOrNull { it.id == activeVideo.id } ?: activeVideo

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                // 1. Sleek Player Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedVideoForPlayback = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Hub",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Now Playing",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedVideoForPlayback = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close player",
                            tint = TextSecondary
                        )
                    }
                }

                // 2. FIXED Video Container at top (Perfect rendering, no scroll glitches)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val resId = ctx.resources.getIdentifier("videoplayback", "raw", ctx.packageName)
                                val videoUri = if (resId != 0) {
                                    Uri.parse("android.resource://" + ctx.packageName + "/" + resId)
                                } else {
                                    Uri.parse("android.resource://" + ctx.packageName + "/" + com.example.R.raw.videoplayback)
                                }
                                setVideoURI(videoUri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    try {
                                        val mc = MediaController(ctx)
                                        mc.setAnchorView(this@apply)
                                        setMediaController(mc)
                                    } catch (e: Exception) {
                                        android.util.Log.e("VideoPlayer", "Error setting up MediaController", e)
                                    }
                                    start()
                                }
                                setOnErrorListener { mp, what, extra ->
                                    android.util.Log.e("VideoPlayer", "Error loading video: what=$what extra=$extra")
                                    false
                                }
                            }
                        },
                        update = { videoView ->
                            // VideoView update if needed
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Scrollable contents below player
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Video Title
                    Text(
                        text = currentVideo.title,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Video Meta & Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CyberPink, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(currentVideo.category, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { viewModel.likeVideo(currentVideo.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (currentVideo.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Heart",
                                tint = if (currentVideo.isLiked) CyberPink else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${currentVideo.likes} Likes", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(currentVideo.description, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- AI EVALUATOR / ASSESSMENT REPORT SECTION ---
                    Text(
                        text = "AI Speaking Assessment (IELTS Coach)",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (viewModel.isVideoAnalyzing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, GlowingCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = GlowingCyan, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Devansh Sir AI is evaluating your video's speaking fluency, grammar correctness, pronunciation rhythms, and Mother Tongue Influence...",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (currentVideo.aiFeedbackReport != null) {
                        val report = currentVideo.aiFeedbackReport!!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            border = BorderStroke(1.dp, GlowingCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("IELTS Band Score Assessed", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Box(
                                        modifier = Modifier
                                            .background(GlowingCyan, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Band ${report.overallBand}", color = DarkBackground, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Fluency: ${report.fluencyFeedback}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Text("Pronunciation: ${report.pronunciationFeedback}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Text("Vocabulary: ${report.lexicalFeedback}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Text("Grammar Accuracy: ${report.grammarFeedback}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

                                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))
                                Text("Coach Prescription Exercise:", color = GlowingCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(report.coachPrescription, color = TextPrimary, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.analyzeVideoWithAI(currentVideo.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "AI", tint = GlowingCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Speaking Skills with AI (Get Band Score)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- COMMENTS SECTION ---
                    Text(
                        text = "Comments (${currentVideo.comments.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Comments display list
                    if (currentVideo.comments.isEmpty()) {
                        Text("No comments yet. Write something encouraging!", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    } else {
                        currentVideo.comments.forEach { comment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "avatar", tint = GlowingCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(comment.userName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(comment.commentText, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Add new comment input field
                    var newCommentInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentInput,
                            onValueChange = { newCommentInput = it },
                            placeholder = { Text("Add supportive comment...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GlowingCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentInput.isNotEmpty()) {
                                    viewModel.addCommentToVideo(currentVideo.id, "Rahul (Owner)", newCommentInput)
                                    newCommentInput = ""
                                }
                            },
                            modifier = Modifier.background(GlowingCyan, CircleShape)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send comment", tint = DarkBackground, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
