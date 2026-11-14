package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import com.example.model.KeyType
import com.example.model.SoundProfile
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

class KeySoundEngine private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager: AudioManager? =
        runCatching { appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()

    private val executor = Executors.newSingleThreadExecutor()
    private val loadedSampleIds = ConcurrentHashMap.newKeySet<Int>()
    private val soundIdMap = ConcurrentHashMap<Pair<SoundProfile, String>, Int>()
    private val pcmCache: Map<Pair<SoundProfile, String>, ShortArray> by lazy {
        buildAllProfilesPcmMap()
    }

    private var soundPool: SoundPool? = null

    init {
        initSoundPoolAsync()
    }

    private fun initSoundPoolAsync() {
        executor.execute {
            runCatching {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(attrs)
                    .build()

                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSampleIds.add(sampleId)
                    }
                }

                val soundDir = File(appContext.cacheDir, "key_sounds").apply {
                    if (!exists()) mkdirs()
                }

                pcmCache.forEach { (key, samples) ->
                    val fileName = "${key.first.id}_${key.second}_v2.wav"
                    val wavFile = File(soundDir, fileName)
                    if (!wavFile.exists() || wavFile.length() < 64L) {
                        FileOutputStream(wavFile).use { fos ->
                            fos.write(buildWavBytes(samples, SAMPLE_RATE))
                        }
                    }
                    val sampleId = pool.load(wavFile.absolutePath, 1)
                    if (sampleId > 0) {
                        soundIdMap[key] = sampleId
                    }
                }
                soundPool = pool
            }
        }
    }

    fun play(profile: SoundProfile, keyType: KeyType, volumeScale: Float = 1.0f) {
        val clampedScale = volumeScale.coerceIn(0f, 1f)
        if (clampedScale <= 0.01f) return
        val variant = resolveVariant(profile, keyType)
        val pool = soundPool
        val sampleId = soundIdMap[profile to variant] ?: soundIdMap[profile to "std"]

        val rate = when (keyType) {
            KeyType.SPACEBAR -> 0.90f
            KeyType.BACKSPACE -> 0.94f
            KeyType.ENTER -> 1.05f
            else -> 1.0f
        }

        val gain = (profile.volume * 1.15f * clampedScale).coerceIn(0.08f, 1.0f)

        if (pool != null && sampleId != null && loadedSampleIds.contains(sampleId)) {
            val streamId = runCatching {
                pool.play(sampleId, gain, gain, 1, 0, rate)
            }.getOrDefault(0)
            if (streamId != 0) return
        }

        val samples = pcmCache[profile to variant] ?: pcmCache[profile to "std"]
        if (samples != null) {
            executor.execute {
                val played = runCatching { playPcmSamplesFallback(samples, gain) }.getOrDefault(false)
                if (!played && profile == SoundProfile.SYSTEM) {
                    playSystemEffect(profile, keyType)
                }
            }
        } else {
            playSystemEffect(profile, keyType)
        }
    }

    private fun resolveVariant(profile: SoundProfile, keyType: KeyType): String {
        return when {
            profile == SoundProfile.TYPEWRITER && keyType == KeyType.ENTER -> "enter"
            keyType == KeyType.SPACEBAR || keyType == KeyType.ENTER || keyType == KeyType.BACKSPACE ->
                if (profile == SoundProfile.TYPEWRITER) "std" else "space"
            else -> "std"
        }
    }

    private fun playSystemEffect(profile: SoundProfile, keyType: KeyType) {
        val effect = when (keyType) {
            KeyType.SPACEBAR -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyType.BACKSPACE -> AudioManager.FX_KEYPRESS_DELETE
            KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        runCatching { audioManager?.playSoundEffect(effect, profile.volume.coerceIn(0.4f, 1.0f)) }
    }

    private fun playPcmSamplesFallback(samples: ShortArray, volume: Float): Boolean {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(SAMPLE_RATE / 10)
        val bufferSize = (samples.size * 2).coerceAtLeast(minBuf)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        return try {
            track.write(samples, 0, samples.size)
            track.setVolume(volume.coerceIn(0.30f, 1.0f))
            track.play()
            val durationMs = (samples.size * 1000L) / SAMPLE_RATE
            Thread.sleep((durationMs + 180L).coerceAtLeast(210L))
            true
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    fun release() {
        // Singleton manages shared SoundPool across IME restarts; no premature shutdown needed.
    }

    companion object {
        const val SAMPLE_RATE = 22050

        @Volatile
        private var instance: KeySoundEngine? = null

        fun getInstance(context: Context): KeySoundEngine {
            return instance ?: synchronized(this) {
                instance ?: KeySoundEngine(context.applicationContext).also { instance = it }
            }
        }

        fun preview(context: Context, profile: SoundProfile) {
            getInstance(context).play(profile, KeyType.CHARACTER)
        }

        fun buildAllProfilesPcmMap(): Map<Pair<SoundProfile, String>, ShortArray> {
            return mapOf(
                (SoundProfile.SYSTEM to "std") to synthesizeCrispTap(baseFreq = 1280.0, gain = 0.85),
                (SoundProfile.SYSTEM to "space") to synthesizeCrispTap(baseFreq = 860.0, gain = 0.90),
                (SoundProfile.MECHANICAL to "std") to synthesizeMechanical(freq = 1650.0, decay = 48.0, gain = 0.85),
                (SoundProfile.MECHANICAL to "space") to synthesizeMechanical(freq = 920.0, decay = 36.0, gain = 0.92),
                (SoundProfile.TYPEWRITER to "std") to synthesizeTypewriterClack(isReturn = false),
                (SoundProfile.TYPEWRITER to "enter") to synthesizeTypewriterClack(isReturn = true),
                (SoundProfile.THOCK to "std") to synthesizeThock(baseFreq = 260.0),
                (SoundProfile.THOCK to "space") to synthesizeThock(baseFreq = 195.0),
                (SoundProfile.POPPY to "std") to synthesizePop(startFreq = 860.0, endFreq = 320.0),
                (SoundProfile.POPPY to "space") to synthesizePop(startFreq = 640.0, endFreq = 240.0)
            )
        }

        fun synthesizeCrispTap(baseFreq: Double, gain: Double): ShortArray {
            val count = (SAMPLE_RATE * 0.022).toInt()
            return ShortArray(count) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val impulse = sin(2.0 * PI * baseFreq * t) * exp(-185.0 * t)
                val warmBody = sin(2.0 * PI * 360.0 * t) * exp(-110.0 * t) * 0.55
                val mixed = ((impulse + warmBody) * gain).coerceIn(-1.0, 1.0)
                (mixed * Short.MAX_VALUE).toInt().toShort()
            }
        }

        fun synthesizeMechanical(freq: Double, decay: Double, gain: Double): ShortArray {
            val count = (SAMPLE_RATE * 0.028).toInt()
            return ShortArray(count) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val click1 = sin(2.0 * PI * freq * t) * exp(-decay * t * 3.0)
                val click2 = if (i > count / 3) {
                    val t2 = (i - count / 3).toDouble() / SAMPLE_RATE
                    sin(2.0 * PI * (freq * 1.28) * t2) * exp(-decay * t2 * 4.0) * 0.78
                } else 0.0
                val body = sin(2.0 * PI * 320.0 * t) * exp(-55.0 * t) * 0.50
                val mixed = ((click1 + click2 + body) * gain).coerceIn(-1.0, 1.0)
                (mixed * Short.MAX_VALUE).toInt().toShort()
            }
        }

        fun synthesizeTypewriterClack(isReturn: Boolean): ShortArray {
            val duration = if (isReturn) 0.095 else 0.032
            val count = (SAMPLE_RATE * duration).toInt()
            return ShortArray(count) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val clack = (sin(2.0 * PI * 1950.0 * t) + 0.55 * sin(2.0 * PI * 3100.0 * t)) * exp(-110.0 * t) * 0.72
                val bell = if (isReturn) sin(2.0 * PI * 2780.0 * t) * exp(-22.0 * t) * 0.75 else 0.0
                val mixed = (clack + bell).coerceIn(-1.0, 1.0)
                (mixed * Short.MAX_VALUE).toInt().toShort()
            }
        }

        fun synthesizeThock(baseFreq: Double): ShortArray {
            val count = (SAMPLE_RATE * 0.036).toInt()
            return ShortArray(count) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val thud = sin(2.0 * PI * baseFreq * t) * exp(-72.0 * t) * 0.92
                val overtone = sin(2.0 * PI * (baseFreq * 1.9) * t) * exp(-120.0 * t) * 0.36
                val mixed = (thud + overtone).coerceIn(-1.0, 1.0)
                (mixed * Short.MAX_VALUE).toInt().toShort()
            }
        }

        fun synthesizePop(startFreq: Double, endFreq: Double): ShortArray {
            val count = (SAMPLE_RATE * 0.024).toInt()
            var phase = 0.0
            return ShortArray(count) { i ->
                val progress = i.toDouble() / count
                val freq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val env = exp(-3.5 * progress) * sin(PI * progress.coerceIn(0.02, 0.98))
                val mixed = (sin(phase) * env * 0.94).coerceIn(-1.0, 1.0)
                (mixed * Short.MAX_VALUE).toInt().toShort()
            }
        }

        fun buildWavBytes(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
            val dataSize = samples.size * 2
            val totalSize = 36 + dataSize
            val bytes = ByteArray(44 + dataSize)

            // RIFF header
            bytes[0] = 'R'.code.toByte()
            bytes[1] = 'I'.code.toByte()
            bytes[2] = 'F'.code.toByte()
            bytes[3] = 'F'.code.toByte()
            writeIntLe(bytes, 4, totalSize)
            bytes[8] = 'W'.code.toByte()
            bytes[9] = 'A'.code.toByte()
            bytes[10] = 'V'.code.toByte()
            bytes[11] = 'E'.code.toByte()

            // fmt chunk
            bytes[12] = 'f'.code.toByte()
            bytes[13] = 'm'.code.toByte()
            bytes[14] = 't'.code.toByte()
            bytes[15] = ' '.code.toByte()
            writeIntLe(bytes, 16, 16) // PCM chunk size
            writeShortLe(bytes, 20, 1) // AudioFormat = 1 (PCM)
            writeShortLe(bytes, 22, 1) // NumChannels = 1 (Mono)
            writeIntLe(bytes, 24, sampleRate)
            writeIntLe(bytes, 28, sampleRate * 2) // ByteRate
            writeShortLe(bytes, 32, 2) // BlockAlign
            writeShortLe(bytes, 34, 16) // BitsPerSample

            // data chunk
            bytes[36] = 'd'.code.toByte()
            bytes[37] = 'a'.code.toByte()
            bytes[38] = 't'.code.toByte()
            bytes[39] = 'a'.code.toByte()
            writeIntLe(bytes, 40, dataSize)

            var offset = 44
            for (sample in samples) {
                val s = sample.toInt()
                bytes[offset++] = (s and 0xFF).toByte()
                bytes[offset++] = ((s ushr 8) and 0xFF).toByte()
            }
            return bytes
        }

        fun hasAudibleEnergy(samples: ShortArray): Boolean {
            return samples.any { abs(it.toInt()) > 500 }
        }

        private fun writeIntLe(buf: ByteArray, offset: Int, value: Int) {
            buf[offset] = (value and 0xFF).toByte()
            buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
            buf[offset + 2] = ((value ushr 16) and 0xFF).toByte()
            buf[offset + 3] = ((value ushr 24) and 0xFF).toByte()
        }

        private fun writeShortLe(buf: ByteArray, offset: Int, value: Int) {
            buf[offset] = (value and 0xFF).toByte()
            buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        }
    }
}
