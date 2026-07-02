package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

object AudioHelper {
    private const val TAG = "AudioHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude

    private var amplitudeJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Start recording audio from the microphone
    fun startRecording(context: Context): Boolean {
        if (_isRecording.value) return false
        try {
            recordFile = File(context.cacheDir, "recording_temp.m4a")
            if (recordFile?.exists() == true) {
                recordFile?.delete()
            }

            // MediaRecorder initialization depending on API version
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordFile?.absolutePath)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                prepare()
                start()
            }

            _isRecording.value = true
            Log.d(TAG, "Audio recording started. Path: ${recordFile?.absolutePath}")

            // Start monitoring amplitude for UI waveform
            startAmplitudeMonitoring()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            stopRecording()
            return false
        }
    }

    // Stop recording and get the recorded file
    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        try {
            amplitudeJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            _micAmplitude.value = 0f
            Log.d(TAG, "Audio recording stopped.")
        }
        return recordFile
    }

    // Convert File content to Base64 String for sending to Gemini API
    fun getRecordedAudioBase64(): String? {
        val file = recordFile ?: return null
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read audio file bytes", e)
            null
        }
    }

    // Start monitoring recording amplitude
    private fun startAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = audioScope.launch {
            while (isActive && _isRecording.value) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize to a 0.0 to 1.0 scale
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    _micAmplitude.value = normalized
                } catch (e: Exception) {
                    // Ignore errors during stops
                }
                delay(80) // update every 80ms
            }
        }
    }

    // Write base64 audio string to temporary file and play it back
    fun playBase64Audio(context: Context, base64Audio: String, onComplete: () -> Unit = {}) {
        stopPlayback()
        try {
            val decodedBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            Log.d(TAG, "Decoded base64 audio size: ${decodedBytes.size} bytes")
            if (decodedBytes.isEmpty()) {
                throw Exception("Decoded audio byte array is empty")
            }

            // Using .aac since Gemini audio response modalities return AAC format
            val tempPlayFile = File(context.cacheDir, "tts_play_temp.aac")
            if (tempPlayFile.exists()) {
                tempPlayFile.delete()
            }

            FileOutputStream(tempPlayFile).use { fos ->
                fos.write(decodedBytes)
                fos.flush()
            }
            Log.d(TAG, "Written to temp audio file. Path: ${tempPlayFile.absolutePath}, Size: ${tempPlayFile.length()} bytes")

            val fis = java.io.FileInputStream(tempPlayFile)
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(fis.fd)
                    prepare()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        stopPlayback()
                        onComplete()
                    }
                    start()
                }
            } finally {
                try {
                    fis.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to close temporary FileInputStream", e)
                }
            }

            _isPlaying.value = true
            Log.d(TAG, "Playing TTS generated audio response successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play base64 audio", e)
            _isPlaying.value = false
            onComplete()
        }
    }

    // Stop active audio playback
    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
        }
    }

    fun cleanUp() {
        stopRecording()
        stopPlayback()
    }
}
