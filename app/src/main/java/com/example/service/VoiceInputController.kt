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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class VoiceInputController(private val context: Context) {

    var isListening by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("Speak now")
        private set

    var permissionMissing by mutableStateOf(false)
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun toggleListening(
        languageCode: String,
        onPermissionRequired: () -> Unit = {},
        onTextRecognized: (String) -> Unit
    ) {
        if (isListening) {
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
        mainHandler.post {
            if (!hasAudioPermission()) {
                permissionMissing = true
                isListening = false
                statusText = "Allow Microphone Permission"
                onPermissionRequired()
                return@post
            }
            permissionMissing = false

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                statusText = "Voice input unavailable on device"
                isListening = false
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            statusText = "Speak now"
                        }

                        override fun onBeginningOfSpeech() {
                            statusText = "Listening..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            statusText = "Processing..."
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            statusText = when (error) {
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                    permissionMissing = true
                                    "Allow Microphone Permission"
                                }
                                SpeechRecognizer.ERROR_NO_MATCH -> "Tap mic to speak again"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speak now"
                                else -> "Speak now"
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val text = matches[0]
                                if (text.isNotBlank()) {
                                    onTextRecognized(text)
                                }
                            }
                            isListening = false
                            statusText = "Speak now"
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                                statusText = matches[0]
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val langTag = when {
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
                        else -> "en-US"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                statusText = "Speak now"
            } catch (_: SecurityException) {
                permissionMissing = true
                isListening = false
                statusText = "Allow Microphone Permission"
                onPermissionRequired()
            } catch (_: Exception) {
                isListening = false
                statusText = "Speak now"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            isListening = false
            statusText = "Speak now"
        }
    }

    fun destroy() {
        stopListening()
    }
}
