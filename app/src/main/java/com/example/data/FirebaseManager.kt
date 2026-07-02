package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.GeminiClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class InterviewSessionRecord(
    val id: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val topic: String = "",
    val mode: String = "",
    val transcript: List<TranscriptItem> = emptyList(),
    val reportJson: String = ""
)

data class TranscriptItem(
    val role: String = "", // "user" or "model"
    val text: String = "",
    val timestamp: Long = 0L
)

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val LOCAL_PREFS_NAME = "mock_interview_local_db"
    private const val LOCAL_KEY_SESSIONS = "saved_sessions"

    private var isFirebaseAvailable = false
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail

    private val _isUserSignedIn = MutableStateFlow(false)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn

    // Initialize Firebase and Auth
    fun initialize(context: Context) {
        try {
            // Check if Firebase is initialized
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase initialized successfully.")

                // Listen to Auth State Changes
                auth?.addAuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    _currentUserEmail.value = user?.email ?: if (user != null) "Guest User" else null
                    _isUserSignedIn.value = user != null
                }
            } else {
                Log.w(TAG, "Firebase is not configured (No apps initialized). Falling back to local database mode.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error. Using local database mode.", e)
            isFirebaseAvailable = false
        }
    }

    fun isUsingFirebase(): Boolean = isFirebaseAvailable

    fun getUserId(): String {
        return if (isFirebaseAvailable) {
            auth?.currentUser?.uid ?: "local_guest_user"
        } else {
            "local_guest_user"
        }
    }

    // Sign in Anonymously (Guest Pass)
    suspend fun signInAnonymously(): Boolean {
        if (!isFirebaseAvailable || auth == null) {
            // Local fallback
            _currentUserEmail.value = "Guest User"
            _isUserSignedIn.value = true
            return true
        }
        return try {
            auth!!.signInAnonymously().await()
            Log.d(TAG, "Signed in anonymously with Firebase")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Anonymous Sign-In failed, fallback to offline guest session", e)
            _currentUserEmail.value = "Guest User"
            _isUserSignedIn.value = true
            true
        }
    }

    // Sign in with Email & Password
    suspend fun signInWithEmail(email: String, password: String): Boolean {
        if (!isFirebaseAvailable || auth == null) {
            _currentUserEmail.value = email
            _isUserSignedIn.value = true
            return true
        }
        return try {
            auth!!.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Signed in successfully with email: $email")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed, checking if we need to auto-create account...", e)
            // Auto-create to make user experience seamless and zero-friction!
            try {
                auth!!.createUserWithEmailAndPassword(email, password).await()
                Log.d(TAG, "Auto-created account and signed in: $email")
                true
            } catch (createEx: Exception) {
                Log.e(TAG, "Account creation failed", createEx)
                false
            }
        }
    }

    // Sign Out
    fun signOut(context: Context) {
        if (isFirebaseAvailable && auth != null) {
            auth!!.signOut()
        }
        _currentUserEmail.value = null
        _isUserSignedIn.value = false
    }

    // Save Interview Session Record
    suspend fun saveSession(context: Context, session: InterviewSessionRecord): Boolean {
        val finalSession = session.copy(
            userId = getUserId(),
            id = session.id.ifEmpty { System.currentTimeMillis().toString() },
            timestamp = if (session.timestamp == 0L) System.currentTimeMillis() else session.timestamp
        )

        // Try Firestore first if available
        if (isFirebaseAvailable && db != null && auth?.currentUser != null) {
            try {
                db!!.collection("interviews")
                    .document(finalSession.id)
                    .set(finalSession)
                    .await()
                Log.d(TAG, "Session saved to Cloud Firestore: ${finalSession.id}")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Firestore save failed, caching to local database", e)
            }
        }

        // Local SharedPreferences fallback
        return saveSessionLocally(context, finalSession)
    }

    // Fetch All Sessions for user
    suspend fun fetchSessions(context: Context): List<InterviewSessionRecord> {
        val currentUserId = getUserId()

        // Try Firestore first if available
        if (isFirebaseAvailable && db != null && auth?.currentUser != null) {
            try {
                val snapshot = db!!.collection("interviews")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .await()

                val sessions = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Manual mapping to handle standard Firestore structures safely
                        val id = doc.id
                        val uId = doc.getString("userId") ?: ""
                        val ts = doc.getLong("timestamp") ?: 0L
                        val topic = doc.getString("topic") ?: ""
                        val mode = doc.getString("mode") ?: ""
                        val reportJson = doc.getString("reportJson") ?: ""
                        
                        val transcriptList = mutableListOf<TranscriptItem>()
                        val rawTranscript = doc.get("transcript") as? List<*>
                        rawTranscript?.forEach { item ->
                            val map = item as? Map<*, *>
                            if (map != null) {
                                transcriptList.add(
                                    TranscriptItem(
                                        role = map["role"] as? String ?: "",
                                        text = map["text"] as? String ?: "",
                                        timestamp = (map["timestamp"] as? Long) ?: 0L
                                    )
                                )
                            }
                        }

                        InterviewSessionRecord(
                            id = id,
                            userId = uId,
                            timestamp = ts,
                            topic = topic,
                            mode = mode,
                            transcript = transcriptList,
                            reportJson = reportJson
                        )
                    } catch (parseEx: Exception) {
                        Log.e(TAG, "Failed to parse document ${doc.id}", parseEx)
                        null
                    }
                }
                Log.d(TAG, "Fetched ${sessions.size} sessions from Firestore")
                if (sessions.isNotEmpty()) {
                    return sessions.sortedByDescending { it.timestamp }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore fetch failed, querying local database instead", e)
            }
        }

        // Local fallback
        return fetchSessionsLocally(context).sortedByDescending { it.timestamp }
    }

    // --- Local Helpers ---
    private fun saveSessionLocally(context: Context, session: InterviewSessionRecord): Boolean {
        return try {
            val prefs = context.getSharedPreferences(LOCAL_PREFS_NAME, Context.MODE_PRIVATE)
            val currentList = fetchSessionsLocally(context).toMutableList()
            // Remove if duplicate id
            currentList.removeAll { it.id == session.id }
            currentList.add(session)

            val array = JSONArray()
            currentList.forEach { record ->
                val obj = JSONObject()
                obj.put("id", record.id)
                obj.put("userId", record.userId)
                obj.put("timestamp", record.timestamp)
                obj.put("topic", record.topic)
                obj.put("mode", record.mode)
                obj.put("reportJson", record.reportJson)

                val transcriptArray = JSONArray()
                record.transcript.forEach { item ->
                    val tObj = JSONObject()
                    tObj.put("role", item.role)
                    tObj.put("text", item.text)
                    tObj.put("timestamp", item.timestamp)
                    transcriptArray.put(tObj)
                }
                obj.put("transcript", transcriptArray)
                array.put(obj)
            }

            prefs.edit().putString(LOCAL_KEY_SESSIONS, array.toString()).apply()
            Log.d(TAG, "Session saved locally: ${session.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Local save failed", e)
            false
        }
    }

    private fun fetchSessionsLocally(context: Context): List<InterviewSessionRecord> {
        val list = mutableListOf<InterviewSessionRecord>()
        try {
            val prefs = context.getSharedPreferences(LOCAL_PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(LOCAL_KEY_SESSIONS, null) ?: return emptyList()
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val userId = obj.optString("userId", "")
                val timestamp = obj.optLong("timestamp", 0L)
                val topic = obj.optString("topic", "")
                val mode = obj.optString("mode", "")
                val reportJson = obj.optString("reportJson", "")

                val transcriptList = mutableListOf<TranscriptItem>()
                val transcriptArray = obj.optJSONArray("transcript")
                if (transcriptArray != null) {
                    for (j in 0 until transcriptArray.length()) {
                        val tObj = transcriptArray.getJSONObject(j)
                        transcriptList.add(
                            TranscriptItem(
                                role = tObj.optString("role", ""),
                                text = tObj.optString("text", ""),
                                timestamp = tObj.optLong("timestamp", 0L)
                            )
                        )
                    }
                }

                list.add(
                    InterviewSessionRecord(
                        id = id,
                        userId = userId,
                        timestamp = timestamp,
                        topic = topic,
                        mode = mode,
                        transcript = transcriptList,
                        reportJson = reportJson
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Local fetch failed", e)
        }
        return list
    }
}
