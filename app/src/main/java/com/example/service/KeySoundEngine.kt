package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.model.KeyType
import com.example.model.SoundProfile
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class KeySoundEngine(context: Context) {

    private val audioManager: AudioManager? =
        runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()

    private val executor = Executors.newSingleThreadExecutor()

    private val pcmCache: Map<Pair<SoundProfile, String>, ShortArray> by lazy {
        mapOf(
            (SoundProfile.MECHANICAL to "std") to synthesizeMechanical(freq = 1650.0, decay = 48.0, gain = 0.72),
            (SoundProfile.MECHANICAL to "space") to synthesizeMechanical(freq = 920.0, decay = 36.0, gain = 0.80),
            (SoundProfile.TYPEWRITER to "std") to synthesizeTypewriterClack(isReturn = false),
            (SoundProfile.TYPEWRITER to "enter") to synthesizeTypewriterClack(isReturn = true),
            (SoundProfile.THOCK to "std") to synthesizeThock(baseFreq = 260.0),
            (SoundProfile.THOCK to "space") to synthesizeThock(baseFreq = 195.0),
            (SoundProfile.POPPY to "std") to synthesizePop(startFreq = 860.0, endFreq = 320.0),
            (SoundProfile.POPPY to "space") to synthesizePop(startFreq = 640.0, endFreq = 240.0)
        )
    }

    fun play(profile: SoundProfile, keyType: KeyType) {
        if (profile == SoundProfile.SYSTEM) {
            playSystemEffect(profile, keyType)
            return
        }

        val variant = when {
            profile == SoundProfile.TYPEWRITER && keyType == KeyType.ENTER -> "enter"
            keyType == KeyType.SPACEBAR || keyType == KeyType.ENTER || keyType == KeyType.BACKSPACE ->
                if (profile == SoundProfile.TYPEWRITER) "std" else "space"
            else -> "std"
        }

        val samples = pcmCache[profile to variant] ?: pcmCache[profile to "std"]
        if (samples == null) {
            playSystemEffect(profile, keyType)
            return
        }

        executor.execute {
            val played = runCatching {
                playPcmSamples(samples, profile.volume)
            }.getOrDefault(false)
            if (!played) {
                playSystemEffect(profile, keyType)
            }
        }
    }

    private fun playSystemEffect(profile: SoundProfile, keyType: KeyType) {
        val effect = when (keyType) {
            KeyType.SPACEBAR -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyType.BACKSPACE -> AudioManager.FX_KEYPRESS_DELETE
            KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        runCatching { audioManager?.playSoundEffect(effect, profile.volume) }
    }

    private fun playPcmSamples(samples: ShortArray, volume: Float): Boolean {
        val sampleRate = 22050
        val bufferSize = (samples.size * 2).coerceAtLeast(
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
            .build()

        return try {
            track.write(samples, 0, samples.size)
            track.setVolume(volume.coerceIn(0.15f, 1.0f))
            track.play()
            Thread.sleep((samples.size * 1000L / sampleRate) + 12L)
            true
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    private fun synthesizeMechanical(freq: Double, decay: Double, gain: Double): ShortArray {
        val sampleRate = 22050
        val count = (sampleRate * 0.028).toInt() // 28ms crisp switch snap
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val click1 = sin(2.0 * PI * freq * t) * exp(-decay * t * 3.0)
            val click2 = if (i > count / 3) {
                val t2 = (i - count / 3).toDouble() / sampleRate
                sin(2.0 * PI * (freq * 1.28) * t2) * exp(-decay * t2 * 4.0) * 0.75
            } else 0.0
            val body = sin(2.0 * PI * 320.0 * t) * exp(-55.0 * t) * 0.45
            val mixed = ((click1 + click2 + body) * gain).coerceIn(-1.0, 1.0)
            (mixed * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun synthesizeTypewriterClack(isReturn: Boolean): ShortArray {
        val sampleRate = 22050
        val duration = if (isReturn) 0.095 else 0.032
        val count = (sampleRate * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val clack = (sin(2.0 * PI * 1950.0 * t) + 0.5 * sin(2.0 * PI * 3100.0 * t)) * exp(-120.0 * t) * 0.55
            val bell = if (isReturn) sin(2.0 * PI * 2780.0 * t) * exp(-24.0 * t) * 0.65 else 0.0
            val mixed = (clack + bell).coerceIn(-1.0, 1.0)
            (mixed * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun synthesizeThock(baseFreq: Double): ShortArray {
        val sampleRate = 22050
        val count = (sampleRate * 0.036).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val thud = sin(2.0 * PI * baseFreq * t) * exp(-78.0 * t) * 0.85
            val overtone = sin(2.0 * PI * (baseFreq * 1.9) * t) * exp(-130.0 * t) * 0.30
            val mixed = (thud + overtone).coerceIn(-1.0, 1.0)
            (mixed * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun synthesizePop(startFreq: Double, endFreq: Double): ShortArray {
        val sampleRate = 22050
        val count = (sampleRate * 0.024).toInt()
        var phase = 0.0
        return ShortArray(count) { i ->
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-4.0 * progress) * kotlin.math.sin(PI * progress.coerceIn(0.02, 0.98))
            val mixed = (sin(phase) * env * 0.88).coerceIn(-1.0, 1.0)
            (mixed * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun release() {
        runCatching { executor.shutdownNow() }
    }

    companion object {
        fun preview(context: Context, profile: SoundProfile) {
            val engine = KeySoundEngine(context.applicationContext)
            engine.play(profile, KeyType.CHARACTER)
        }
    }
}
