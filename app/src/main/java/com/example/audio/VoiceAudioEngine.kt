package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Process
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min

/**
 * Full-duplex multi-speaker voice engine designed for smooth global real-time audio rooms:
 * - Captures microphone at 8000 Hz mono PCM16, enables hardware AcousticEchoCanceler /
 *   NoiseSuppressor / AutomaticGainControl, and sets AudioManager.MODE_IN_COMMUNICATION.
 * - Compresses 120 ms frames (960 samples) into compact 8-bit G.711 mu-law (~1.3 KB Base64)
 *   for low-latency transmission over Cloud Firestore ring slots.
 * - Plays back simultaneous remote speakers through an adaptive per-speaker jitter queue +
 *   high-priority AudioTrack PCM mixer without blocking or stuttering.
 */
class VoiceAudioEngine(private val context: Context? = null) {

    companion object {
        const val SAMPLE_RATE = 8000
        const val CHUNK_SAMPLES = 160 // 20 ms at 8 kHz
        const val FRAME_CHUNKS = 6    // 120 ms packet (960 samples)
        const val FRAME_SAMPLES = CHUNK_SAMPLES * FRAME_CHUNKS
        private const val MAX_JITTER_CHUNKS_PER_SPEAKER = 18 // 360 ms cap
        private const val SILENCE_EXPIRE_MS = 1800L
    }

    private val _amplitudeLevel = MutableStateFlow(0f)
    val amplitudeLevel: StateFlow<Float> = _amplitudeLevel.asStateFlow()

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive.asStateFlow()

    private val _remoteSpeakingLevels = MutableStateFlow<Map<String, Float>>(emptyMap())
    val remoteSpeakingLevels: StateFlow<Map<String, Float>> = _remoteSpeakingLevels.asStateFlow()

    private var recordJob: Job? = null
    private var playbackJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var aec: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null
    private var previousAudioMode: Int? = null
    private var previousSpeakerphone: Boolean? = null

    private class SpeakerQueue {
        val lock = Any()
        val chunks = ArrayDeque<ShortArray>()
        var lastSeq: Long = -1L
        var lastRms: Float = 0f
        var lastUpdatedAtMs: Long = 0L
        var primed: Boolean = false
    }

    private val speakerQueues = ConcurrentHashMap<String, SpeakerQueue>()
    private var txSeq = 0L

    fun startPlaybackMixer(scope: CoroutineScope) {
        if (playbackJob?.isActive == true) return
        configureCommunicationAudioMode(enable = true)
        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Throwable) {
            }
            val minOut = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FRAME_SAMPLES * 4)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minOut)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.w("VoiceAudioEngine", "AudioTrack init unavailable: ${e.message}")
                null
            } ?: return@launch

            audioTrack = track
            try {
                track.play()
                val mixAcc = IntArray(CHUNK_SAMPLES)
                val outBuf = ShortArray(CHUNK_SAMPLES)

                while (isActive) {
                    val now = System.currentTimeMillis()
                    mixAcc.fill(0)
                    var anyMixed = false
                    val activeLevels = mutableMapOf<String, Float>()

                    val iterator = speakerQueues.entries.iterator()
                    while (iterator.hasNext()) {
                        val (senderId, queue) = iterator.next()
                        val chunk: ShortArray? = synchronized(queue.lock) {
                            if (now - queue.lastUpdatedAtMs > SILENCE_EXPIRE_MS && queue.chunks.isEmpty()) {
                                null
                            } else {
                                if (!queue.primed && queue.chunks.size >= 3) {
                                    queue.primed = true
                                }
                                if (queue.primed && queue.chunks.isNotEmpty()) {
                                    queue.chunks.removeFirst()
                                } else {
                                    if (queue.chunks.isEmpty()) queue.primed = false
                                    null
                                }
                            }
                        }

                        if (chunk == null && now - queue.lastUpdatedAtMs > SILENCE_EXPIRE_MS) {
                            iterator.remove()
                            continue
                        }

                        if (now - queue.lastUpdatedAtMs <= 900L && queue.lastRms > 0.03f) {
                            activeLevels[senderId] = queue.lastRms
                        }

                        if (chunk != null) {
                            anyMixed = true
                            val n = min(CHUNK_SAMPLES, chunk.size)
                            for (i in 0 until n) {
                                mixAcc[i] += chunk[i].toInt()
                            }
                        }
                    }

                    if (_remoteSpeakingLevels.value != activeLevels) {
                        _remoteSpeakingLevels.value = activeLevels
                    }

                    if (anyMixed) {
                        for (i in 0 until CHUNK_SAMPLES) {
                            outBuf[i] = mixAcc[i].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                        track.write(outBuf, 0, CHUNK_SAMPLES, AudioTrack.WRITE_BLOCKING)
                    } else {
                        delay(20L)
                    }
                }
            } catch (e: Exception) {
                Log.w("VoiceAudioEngine", "Playback loop ended: ${e.message}")
            } finally {
                releasePlayback()
            }
        }
    }

    fun stopPlaybackMixer() {
        playbackJob?.cancel()
        playbackJob = null
        speakerQueues.clear()
        _remoteSpeakingLevels.value = emptyMap()
        releasePlayback()
        if (!_isMicActive.value) {
            configureCommunicationAudioMode(enable = false)
        }
    }

    fun enqueueRemoteFrame(senderId: String, seq: Long, payloadB64: String, rms: Float) {
        if (payloadB64.isBlank()) return
        val bytes = try {
            Base64.decode(payloadB64, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            try {
                Base64.decode(payloadB64, Base64.DEFAULT)
            } catch (_: Exception) {
                return
            }
        }
        if (bytes.isEmpty()) return

        val queue = speakerQueues.getOrPut(senderId) { SpeakerQueue() }
        synchronized(queue.lock) {
            if (seq <= queue.lastSeq && queue.lastSeq - seq < 5000L) return
            queue.lastSeq = seq
            queue.lastRms = rms.coerceIn(0f, 1f)
            queue.lastUpdatedAtMs = System.currentTimeMillis()

            val pcm = decodeMuLaw(bytes)
            var offset = 0
            while (offset < pcm.size) {
                val end = min(offset + CHUNK_SAMPLES, pcm.size)
                queue.chunks.addLast(pcm.copyOfRange(offset, end))
                offset = end
            }
            while (queue.chunks.size > MAX_JITTER_CHUNKS_PER_SPEAKER) {
                queue.chunks.removeFirst()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startCapture(
        scope: CoroutineScope,
        hasPermission: Boolean,
        onEncodedVoiceFrame: ((seq: Long, payloadB64: String, rms: Float) -> Unit)? = null
    ) {
        stopCapture()
        _isMicActive.value = true
        if (!hasPermission) {
            _amplitudeLevel.value = 0f
            return
        }

        configureCommunicationAudioMode(enable = true)
        recordJob = scope.launch(Dispatchers.IO) {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Throwable) {
            }
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FRAME_SAMPLES * 2)

            try {
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    return@launch
                }
                attachAudioEffects(recorder.audioSessionId)
                audioRecord = recorder
                recorder.startRecording()

                val packet = ShortArray(FRAME_SAMPLES)
                var filled = 0
                var lastNoiseSentMs = 0L

                while (isActive) {
                    val remaining = FRAME_SAMPLES - filled
                    val read = recorder.read(packet, filled, remaining)
                    if (read > 0) {
                        var peak = 0
                        for (i in filled until filled + read) {
                            val v = abs(packet[i].toInt())
                            if (v > peak) peak = v
                        }
                        val normalized = (peak / 9500f).coerceIn(0f, 1f)
                        _amplitudeLevel.value = normalized
                        filled += read

                        if (filled >= FRAME_SAMPLES) {
                            filled = 0
                            val now = System.currentTimeMillis()
                            // Transmit speech actively + heartbeat comfort noise every 700ms so listeners stay synced
                            if (normalized >= 0.025f || now - lastNoiseSentMs >= 700L) {
                                lastNoiseSentMs = now
                                val encoded = encodeMuLaw(packet)
                                val b64 = Base64.encodeToString(encoded, Base64.NO_WRAP)
                                txSeq += 1L
                                onEncodedVoiceFrame?.invoke(txSeq, b64, normalized)
                            }
                        }
                    } else {
                        delay(15L)
                    }
                }
            } catch (e: SecurityException) {
                Log.w("VoiceAudioEngine", "RECORD_AUDIO permission missing: ${e.message}")
            } catch (e: Exception) {
                Log.w("VoiceAudioEngine", "AudioRecord unavailable on device: ${e.message}")
            } finally {
                releaseRecorder()
            }
        }
    }

    fun stopCapture() {
        _isMicActive.value = false
        _amplitudeLevel.value = 0f
        recordJob?.cancel()
        recordJob = null
        releaseRecorder()
        if (playbackJob?.isActive != true) {
            configureCommunicationAudioMode(enable = false)
        }
    }

    private fun attachAudioEffects(sessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
            }
        } catch (_: Exception) {
        }
        try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
            }
        } catch (_: Exception) {
        }
        try {
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
            }
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun configureCommunicationAudioMode(enable: Boolean) {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (enable) {
                if (previousAudioMode == null) previousAudioMode = am.mode
                if (previousSpeakerphone == null) previousSpeakerphone = am.isSpeakerphoneOn
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = true
            } else {
                previousAudioMode?.let { am.mode = it }
                previousSpeakerphone?.let { am.isSpeakerphoneOn = it }
                previousAudioMode = null
                previousSpeakerphone = null
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseRecorder() {
        try { aec?.release() } catch (_: Exception) {}
        aec = null
        try { noiseSuppressor?.release() } catch (_: Exception) {}
        noiseSuppressor = null
        try { gainControl?.release() } catch (_: Exception) {}
        gainControl = null
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }

    private fun releasePlayback() {
        try { audioTrack?.pause() } catch (_: Exception) {}
        try { audioTrack?.flush() } catch (_: Exception) {}
        try { audioTrack?.release() } catch (_: Exception) {}
        audioTrack = null
    }

    // --- ITU-T G.711 mu-law codec (8-bit <-> 16-bit linear PCM) ---

    fun encodeMuLaw(pcm: ShortArray): ByteArray {
        val out = ByteArray(pcm.size)
        for (i in pcm.indices) {
            out[i] = linearToMuLawByte(pcm[i].toInt())
        }
        return out
    }

    fun decodeMuLaw(ulaw: ByteArray): ShortArray {
        val out = ShortArray(ulaw.size)
        for (i in ulaw.indices) {
            out[i] = muLawByteToLinear(ulaw[i])
        }
        return out
    }

    private fun linearToMuLawByte(sample: Int): Byte {
        var pcm = sample
        val sign = if (pcm < 0) {
            pcm = -pcm
            0x80
        } else {
            0x00
        }
        if (pcm > 32635) pcm = 32635
        pcm += 0x84

        var exponent = 7
        var expMask = 0x4000
        while ((pcm and expMask) == 0 && exponent > 0) {
            exponent--
            expMask = expMask shr 1
        }
        val mantissa = (pcm shr (exponent + 3)) and 0x0F
        val ulawByte = (sign or (exponent shl 4) or mantissa).inv() and 0xFF
        return ulawByte.toByte()
    }

    private fun muLawByteToLinear(ulawByte: Byte): Short {
        val u = ulawByte.toInt().inv() and 0xFF
        val sign = u and 0x80
        val exponent = (u shr 4) and 0x07
        val mantissa = u and 0x0F
        var magnitude = ((mantissa shl 3) + 0x84) shl exponent
        magnitude -= 0x84
        return (if (sign != 0) -magnitude else magnitude).toShort()
    }
}
