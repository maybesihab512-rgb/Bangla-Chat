package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AudioPlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentPlayingMessageId = MutableStateFlow<String?>(null)
    val currentPlayingMessageId: StateFlow<String?> = _currentPlayingMessageId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    fun playOrToggle(messageId: String, audioUrlOrPath: String) {
        if (_currentPlayingMessageId.value == messageId) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        play(messageId, audioUrlOrPath)
    }

    fun play(messageId: String, audioUrlOrPath: String) {
        if (audioUrlOrPath.isBlank()) {
            Log.w("AudioPlaybackManager", "Audio path or URL is empty")
            return
        }

        stop()

        try {
            _currentPlayingMessageId.value = messageId
            _progress.value = 0f
            _currentPositionMs.value = 0

            val resolvedUri = resolveAudioUri(audioUrlOrPath) ?: run {
                Log.e("AudioPlaybackManager", "Could not resolve audio source: $audioUrlOrPath")
                resetState()
                return
            }

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (resolvedUri.scheme == "file" || resolvedUri.scheme == "content") {
                    setDataSource(context, resolvedUri)
                } else {
                    setDataSource(resolvedUri.toString())
                }

                setOnPreparedListener { mp ->
                    try {
                        val duration = mp.duration.coerceAtLeast(1)
                        _durationMs.value = duration
                        mp.start()
                        _isPlaying.value = true
                        startProgressTracker()
                        Log.d("AudioPlaybackManager", "Playback started for message: $messageId (duration: $duration ms)")
                    } catch (e: Exception) {
                        Log.e("AudioPlaybackManager", "Error starting playback after prep", e)
                        resetState()
                    }
                }

                setOnCompletionListener {
                    Log.d("AudioPlaybackManager", "Playback completed for message: $messageId")
                    resetState()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlaybackManager", "MediaPlayer error: what=$what, extra=$extra")
                    resetState()
                    true
                }

                prepareAsync()
            }

            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Failed to initialize audio playback", e)
            resetState()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    progressJob?.cancel()
                }
            }
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error pausing player", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error resuming player", e)
        }
    }

    fun seekTo(fraction: Float) {
        try {
            mediaPlayer?.let {
                val dur = it.duration
                if (dur > 0) {
                    val targetMs = (dur * fraction.coerceIn(0f, 1f)).toInt()
                    it.seekTo(targetMs)
                    _currentPositionMs.value = targetMs
                    _progress.value = fraction
                }
            }
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error seeking player", e)
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            resetState()
        }
    }

    fun release() {
        stop()
    }

    private fun resetState() {
        _isPlaying.value = false
        _currentPlayingMessageId.value = null
        _progress.value = 0f
        _currentPositionMs.value = 0
        _durationMs.value = 0
        progressJob?.cancel()
        progressJob = null
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            val current = mp.currentPosition
                            val total = mp.duration.coerceAtLeast(1)
                            _currentPositionMs.value = current
                            _progress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }

    /**
     * Resolves an audio URI from a URL, local path, or base64 data string.
     */
    private fun resolveAudioUri(raw: String): Uri? {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                Uri.parse(trimmed)
            }
            trimmed.startsWith("data:audio") -> {
                decodeBase64ToCacheFile(trimmed)
            }
            trimmed.startsWith("content://") -> {
                Uri.parse(trimmed)
            }
            else -> {
                // Check if local file exists
                val file = File(trimmed)
                if (file.exists()) Uri.fromFile(file) else null
            }
        }
    }

    private fun decodeBase64ToCacheFile(dataUri: String): Uri? {
        return try {
            val base64Data = dataUri.substringAfter("base64,")
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "decoded_audio_${System.currentTimeMillis()}.m4a")
            FileOutputStream(tempFile).use { it.write(decodedBytes) }
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Failed to decode base64 audio", e)
            null
        }
    }
}
