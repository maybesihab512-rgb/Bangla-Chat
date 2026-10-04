package com.example.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecordManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    /**
     * Starts recording audio to a new cache file in AAC/MPEG_4 format.
     * Returns the output file path or failure result.
     */
    fun startRecording(): Result<File> {
        return try {
            release()

            val dir = File(context.cacheDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            recordStartTime = System.currentTimeMillis()
            Log.d("AudioRecordManager", "Started recording to: ${outputFile.absolutePath}")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Failed to start recording", e)
            release()
            Result.failure(e)
        }
    }

    /**
     * Stops recording and validates that the audio file was written and contains audible bytes.
     * Returns the recorded file and its actual duration in seconds.
     */
    fun stopRecording(): Result<Pair<File, Int>> {
        val activeRecorder = recorder ?: return Result.failure(IllegalStateException("Recorder not active"))
        val file = currentOutputFile ?: return Result.failure(IllegalStateException("No output file"))
        val durationMs = System.currentTimeMillis() - recordStartTime
        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)

        return try {
            activeRecorder.stop()
            activeRecorder.release()
            recorder = null

            // Validate that audio was recorded
            if (!file.exists() || file.length() < 300) {
                file.delete()
                currentOutputFile = null
                return Result.failure(IOException("Recorded audio file is empty or corrupted (${file.length()} bytes)"))
            }

            Log.d("AudioRecordManager", "Successfully recorded ${file.length()} bytes ($durationSec sec) to ${file.absolutePath}")
            currentOutputFile = null
            Result.success(Pair(file, durationSec))
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error stopping recorder", e)
            try { file.delete() } catch (_: Exception) {}
            recorder = null
            currentOutputFile = null
            Result.failure(e)
        }
    }

    /**
     * Cancels recording and cleans up temporary file.
     */
    fun cancelRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w("AudioRecordManager", "Error stopping recorder during cancel", e)
        } finally {
            try {
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            currentOutputFile?.let {
                try { it.delete() } catch (_: Exception) {}
            }
            currentOutputFile = null
        }
    }

    fun release() {
        cancelRecording()
    }
}
