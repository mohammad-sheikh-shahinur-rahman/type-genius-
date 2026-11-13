package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class VoiceInputController(private val context: Context) {

    var continuousMode by mutableStateOf(true)
        private set

    var isListening by mutableStateOf(false)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("Speak now")
        private set

    var partialText by mutableStateOf("")
        private set

    var rmsLevel by mutableFloatStateOf(0f)
        private set

    var permissionMissing by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isSessionDesired = false
    private var silentCycleCount = 0
    private var activeLanguageCode = "bn"
    private var activeOnPermissionRequired: () -> Unit = {}
    private var activeOnTextRecognized: (String) -> Unit = {}

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun toggleContinuousMode() {
        continuousMode = !continuousMode
    }

    fun toggleListening(
        languageCode: String,
        onPermissionRequired: () -> Unit = {},
        onTextRecognized: (String) -> Unit
    ) {
        if (isListening || isSessionDesired) {
            stopListening()
        } else {
            startListening(languageCode, onPermissionRequired, onTextRecognized)
        }
    }

    fun startListening(
        languageCode: String,
        onPermissionRequired: () -> Unit = {},
        onTextRecognized: (String) -> Unit
    ) {
        activeLanguageCode = languageCode
        activeOnPermissionRequired = onPermissionRequired
        activeOnTextRecognized = onTextRecognized
        isSessionDesired = true
        silentCycleCount = 0
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post { launchRecognizerCycle() }
    }

    private fun scheduleNextCycle(delayMs: Long) {
        if (!isSessionDesired || !continuousMode) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isSessionDesired && continuousMode) {
                launchRecognizerCycle()
            }
        }, delayMs)
    }

    private fun launchRecognizerCycle() {
        if (!isSessionDesired) return

        if (!hasAudioPermission()) {
            isSessionDesired = false
            permissionMissing = true
            isListening = false
            isProcessing = false
            hasError = true
            rmsLevel = 0f
            partialText = ""
            statusText = "Allow Microphone Permission"
            activeOnPermissionRequired()
            return
        }
        permissionMissing = false
        hasError = false
        partialText = ""
        rmsLevel = 0f

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            isSessionDesired = false
            statusText = "Voice input unavailable on device"
            isListening = false
            isProcessing = false
            hasError = true
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        isProcessing = false
                        hasError = false
                        partialText = ""
                        statusText = if (continuousMode) "Listening continuously..." else "Speak now"
                    }

                    override fun onBeginningOfSpeech() {
                        silentCycleCount = 0
                        isProcessing = false
                        statusText = "Listening..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        rmsLevel = (rmsLevel * 0.40f) + (normalized * 0.60f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isProcessing = true
                        rmsLevel = 0.18f
                        statusText = "Processing..."
                    }

                    override fun onError(error: Int) {
                        val isPermissionError = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                        val isTransientEmpty = error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == SpeechRecognizer.ERROR_CLIENT

                        if (!isPermissionError && isSessionDesired && continuousMode && isTransientEmpty && silentCycleCount < 12) {
                            silentCycleCount++
                            isListening = true
                            isProcessing = false
                            hasError = false
                            rmsLevel = 0f
                            partialText = ""
                            statusText = "Listening continuously..."
                            scheduleNextCycle(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 320L else 180L)
                            return
                        }

                        isSessionDesired = false
                        isListening = false
                        isProcessing = false
                        rmsLevel = 0f
                        hasError = true
                        partialText = ""
                        statusText = when (error) {
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                permissionMissing = true
                                "Allow Microphone Permission"
                            }
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — tap mic"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard — tap mic"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Check connection or offline pack"
                            else -> "Tap mic to speak again"
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val committed = matches?.firstOrNull()?.trim().orEmpty()
                        if (committed.isNotEmpty()) {
                            silentCycleCount = 0
                            activeOnTextRecognized(committed)
                        }
                        isProcessing = false
                        hasError = false
                        rmsLevel = 0f
                        partialText = ""

                        if (isSessionDesired && continuousMode) {
                            isListening = true
                            statusText = "Listening continuously..."
                            scheduleNextCycle(180L)
                        } else {
                            isSessionDesired = false
                            isListening = false
                            statusText = if (committed.isNotEmpty()) "Tap mic to speak more" else "Tap mic to speak"
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotEmpty()) {
                            silentCycleCount = 0
                            partialText = partial
                            statusText = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val langTag = resolveLanguageTag(activeLanguageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            isProcessing = false
            statusText = if (continuousMode) "Listening continuously..." else "Speak now"
        } catch (_: SecurityException) {
            isSessionDesired = false
            permissionMissing = true
            isListening = false
            isProcessing = false
            hasError = true
            rmsLevel = 0f
            statusText = "Allow Microphone Permission"
            activeOnPermissionRequired()
        } catch (_: Exception) {
            isSessionDesired = false
            isListening = false
            isProcessing = false
            hasError = true
            rmsLevel = 0f
            statusText = "Tap mic to speak again"
        }
    }

    private fun resolveLanguageTag(languageCode: String): String {
        return when {
            languageCode == "banglish" || languageCode.startsWith("bn") || languageCode == "syl" || languageCode == "as" -> "bn-BD"
            languageCode.startsWith("hi") || languageCode == "ne" -> "hi-IN"
            languageCode.startsWith("fa") -> "fa-IR"
            languageCode.startsWith("ur") -> "ur-PK"
            languageCode.startsWith("ar") -> "ar"
            languageCode.startsWith("zh") -> "zh-CN"
            languageCode.startsWith("ja") -> "ja-JP"
            languageCode.startsWith("ko") -> "ko-KR"
            languageCode.startsWith("es") -> "es-ES"
            languageCode.startsWith("fr") -> "fr-FR"
            languageCode.startsWith("de") -> "de-DE"
            languageCode.startsWith("ru") -> "ru-RU"
            languageCode.startsWith("tr") -> "tr-TR"
            languageCode == "en-GB" -> "en-GB"
            else -> "en-US"
        }
    }

    fun stopListening() {
        isSessionDesired = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            isListening = false
            isProcessing = false
            hasError = false
            rmsLevel = 0f
            partialText = ""
            statusText = "Speak now"
        }
    }

    fun destroy() {
        stopListening()
    }
}
