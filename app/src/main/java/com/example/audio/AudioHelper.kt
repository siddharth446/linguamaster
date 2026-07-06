package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
    private var audioTrack: AudioTrack? = null
    private var pcmJob: Job? = null
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
    fun playBase64Audio(
        context: Context,
        base64Audio: String,
        mimeType: String? = null,
        onComplete: () -> Unit = {}
    ) {
        stopPlayback()
        try {
            val decodedBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            Log.d(TAG, "Decoded base64 audio size: ${decodedBytes.size} bytes, mimeType: $mimeType")
            if (decodedBytes.isEmpty()) {
                throw Exception("Decoded audio byte array is empty")
            }

            // Print first 20 bytes signature for debugging format
            val hexSig = decodedBytes.take(20).joinToString(" ") { String.format("%02X", it) }
            Log.d(TAG, "Audio data hex signature: $hexSig")

            // Determine if it is explicitly PCM or lacks container headers
            val isExplicitPcm = mimeType != null && (
                mimeType.contains("pcm", ignoreCase = true) || 
                mimeType.contains("l16", ignoreCase = true) || 
                mimeType.contains("raw", ignoreCase = true)
            )

            // WAV starts with "RIFF" (52 49 46 46)
            val isWav = decodedBytes.size >= 4 && 
                decodedBytes[0] == 0x52.toByte() && 
                decodedBytes[1] == 0x49.toByte() && 
                decodedBytes[2] == 0x46.toByte() && 
                decodedBytes[3] == 0x46.toByte()

            // MP3 starts with ID3 (49 44 33) or standard sync frames (FF FB / FF F3 / FF F2)
            val isMp3 = (decodedBytes.size >= 3 && decodedBytes[0] == 0x49.toByte() && decodedBytes[1] == 0x44.toByte() && decodedBytes[2] == 0x33.toByte()) ||
                (decodedBytes.size >= 2 && decodedBytes[0] == 0xFF.toByte() && (decodedBytes[1].toInt() and 0xE0) == 0xE0)

            // If explicitly PCM, bypass MediaPlayer completely and play via AudioTrack
            if (isExplicitPcm) {
                Log.d(TAG, "Explicit PCM/L16 detected. Playing via AudioTrack.")
                playRawPcm(decodedBytes, 24000, onComplete)
                return
            }

            // Try playing via MediaPlayer first
            try {
                // Using .aac as default extension, or .mp3 / .wav if matched
                val ext = when {
                    isWav -> "wav"
                    isMp3 -> "mp3"
                    else -> "aac"
                }
                val tempPlayFile = File(context.cacheDir, "tts_play_temp.$ext")
                if (tempPlayFile.exists()) {
                    tempPlayFile.delete()
                }

                FileOutputStream(tempPlayFile).use { fos ->
                    fos.write(decodedBytes)
                    fos.flush()
                }
                Log.d(TAG, "Written to temp file: ${tempPlayFile.absolutePath} (${tempPlayFile.length()} bytes)")

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
                    _isPlaying.value = true
                    Log.d(TAG, "MediaPlayer playing successfully with extension .$ext")
                } finally {
                    try {
                        fis.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to close temporary FileInputStream", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer failed to play/prepare audio (e.g. raw PCM without headers). Attempting fallback to raw PCM playing via AudioTrack.", e)
                // Fallback to playing as raw 24kHz Mono 16-bit PCM (standard Gemini TTS output)
                playRawPcm(decodedBytes, 24000, onComplete)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play base64 audio", e)
            _isPlaying.value = false
            onComplete()
        }
    }

    // Play raw PCM audio data at specified sample rate
    private fun playRawPcm(pcmData: ByteArray, sampleRate: Int = 24000, onComplete: () -> Unit) {
        stopPlayback()
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, pcmData.size)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build().apply {
                    val bytesWritten = write(pcmData, 0, pcmData.size)
                    Log.d(TAG, "AudioTrack wrote $bytesWritten bytes of raw PCM")
                }

            _isPlaying.value = true

            pcmJob = audioScope.launch {
                try {
                    audioTrack?.play()
                    val samples = pcmData.size / 2
                    val durationMs = ((samples.toDouble() / sampleRate) * 1000).toLong()
                    Log.d(TAG, "PCM duration: ${durationMs}ms. Waiting...")
                    delay(durationMs + 150)
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.e(TAG, "Error during AudioTrack playback", e)
                    }
                } finally {
                    _isPlaying.value = false
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize or play raw PCM via AudioTrack", e)
            _isPlaying.value = false
            onComplete()
        }
    }

    // Stop active audio playback
    fun stopPlayback() {
        try {
            pcmJob?.cancel()
            pcmJob = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling PCM playback job", e)
        }

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

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }

    fun cleanUp() {
        stopRecording()
        stopPlayback()
    }
}
