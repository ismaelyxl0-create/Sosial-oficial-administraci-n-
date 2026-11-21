package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
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
import java.util.UUID

data class RecordedAudioResult(
    val filePath: String,
    val durationMs: Long,
    val audioDataB64: String? = null
)

class VoiceNoteRecorder(private val context: Context) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMillis: Long = 0L
    private var timerJob: Job? = null

    fun startRecording(scope: CoroutineScope): Result<Unit> {
        cancelRecording()
        return try {
            val dir = File(context.filesDir, "audio_notes").apply { mkdirs() }
            val file = File(dir, "voice_${UUID.randomUUID().toString().take(8)}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(24000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            outputFile = file
            startedAtMillis = System.currentTimeMillis()
            _elapsedMs.value = 0L
            _isRecording.value = true

            timerJob = scope.launch(Dispatchers.Main) {
                while (isActive && _isRecording.value) {
                    _elapsedMs.value = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
                    delay(150L)
                }
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.w("VoiceNoteRecorder", "Missing RECORD_AUDIO permission: ${e.message}")
            cleanupInternal(deleteFile = true)
            Result.failure(IllegalStateException("Activa el permiso de micrófono para grabar notas de voz."))
        } catch (e: Exception) {
            Log.w("VoiceNoteRecorder", "Failed to start recording: ${e.message}")
            cleanupInternal(deleteFile = true)
            Result.failure(IllegalStateException("No se pudo iniciar la grabación de audio en este dispositivo."))
        }
    }

    fun stopAndFinalize(): Result<RecordedAudioResult> {
        if (!_isRecording.value) {
            return Result.failure(IllegalStateException("No hay grabación activa."))
        }
        val file = outputFile
        val elapsed = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(1000L)
        return try {
            mediaRecorder?.stop()
            cleanupRecorderOnly()
            _isRecording.value = false
            _elapsedMs.value = 0L
            if (file != null && file.exists() && file.length() > 0L) {
                val bytes = file.readBytes()
                val b64 = if (bytes.size <= 240_000) {
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else {
                    null
                }
                Result.success(RecordedAudioResult(file.absolutePath, elapsed, b64))
            } else {
                file?.delete()
                Result.failure(IllegalStateException("La nota de voz está vacía. Intenta grabar un poco más."))
            }
        } catch (e: Exception) {
            Log.w("VoiceNoteRecorder", "Stop recording failed: ${e.message}")
            cleanupInternal(deleteFile = true)
            Result.failure(IllegalStateException("La nota de voz fue muy corta. Mantén unos segundos antes de enviar."))
        }
    }

    fun cancelRecording() {
        cleanupInternal(deleteFile = true)
    }

    fun release() {
        cleanupInternal(deleteFile = true)
    }

    private fun cleanupRecorderOnly() {
        timerJob?.cancel()
        timerJob = null
        try { mediaRecorder?.reset() } catch (_: Exception) {}
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
    }

    private fun cleanupInternal(deleteFile: Boolean) {
        _isRecording.value = false
        _elapsedMs.value = 0L
        timerJob?.cancel()
        timerJob = null
        try { mediaRecorder?.stop() } catch (_: Exception) {}
        try { mediaRecorder?.reset() } catch (_: Exception) {}
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
        if (deleteFile) {
            try { outputFile?.delete() } catch (_: Exception) {}
        }
        outputFile = null
    }
}

class VoiceNotePlayer(private val context: Context? = null) {
    private val _playingMessageId = MutableStateFlow<Long?>(null)
    val playingMessageId: StateFlow<Long?> = _playingMessageId.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun togglePlayback(
        messageId: Long,
        audioPath: String?,
        audioDataB64: String? = null,
        scope: CoroutineScope,
        onError: (String) -> Unit
    ) {
        if (_playingMessageId.value == messageId) {
            stop()
            return
        }
        stop()

        val resolvedFile = resolvePlayableFile(messageId, audioPath, audioDataB64)
        if (resolvedFile == null || !resolvedFile.exists() || !resolvedFile.isFile) {
            onError("El archivo de audio aún se está sincronizando o no está disponible.")
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(resolvedFile.absolutePath)
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onError("No se pudo reproducir esta nota de voz.")
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _playingMessageId.value = messageId
            _progress.value = 0f

            progressJob = scope.launch(Dispatchers.Main) {
                while (isActive && _playingMessageId.value == messageId) {
                    val mp = mediaPlayer ?: break
                    val dur = mp.duration.takeIf { it > 0 } ?: 1
                    val pos = mp.currentPosition.coerceIn(0, dur)
                    _progress.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    delay(100L)
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceNotePlayer", "Playback error: ${e.message}")
            stop()
            onError("No se pudo reproducir el mensaje de audio.")
        }
    }

    private fun resolvePlayableFile(
        messageId: Long,
        audioPath: String?,
        audioDataB64: String?
    ): File? {
        if (!audioPath.isNullOrBlank()) {
            val localFile = File(audioPath)
            if (localFile.exists() && localFile.isFile && localFile.length() > 0L) {
                return localFile
            }
        }
        val ctx = context
        if (!audioDataB64.isNullOrBlank() && ctx != null) {
            return runCatching {
                val raw = audioDataB64.substringAfter("base64,", audioDataB64).trim()
                val decoded = Base64.decode(raw, Base64.NO_WRAP)
                if (decoded.isEmpty()) return null
                val cacheDir = File(ctx.cacheDir, "remote_voice_notes").apply { mkdirs() }
                val cached = File(cacheDir, "msg_${messageId}.m4a")
                if (!cached.exists() || cached.length() != decoded.size.toLong()) {
                    cached.writeBytes(decoded)
                }
                cached
            }.getOrNull()
        }
        return null
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        _playingMessageId.value = null
        _progress.value = 0f
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }
}
