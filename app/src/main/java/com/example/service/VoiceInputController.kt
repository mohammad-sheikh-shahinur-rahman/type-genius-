package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class VoiceInputController(private val context: Context) {

    var isListening by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("Speak now")
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun toggleListening(languageCode: String, onTextRecognized: (String) -> Unit) {
        if (isListening) {
            stopListening()
        } else {
            startListening(languageCode, onTextRecognized)
        }
    }

    fun startListening(languageCode: String, onTextRecognized: (String) -> Unit) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                statusText = "Tap mic to speak"
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
                            statusText = "Processing speech..."
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            statusText = "Speak now"
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val text = matches[0]
                                onTextRecognized(text)
                            }
                            isListening = false
                            statusText = "Speak now"
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                statusText = matches[0]
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val langTag = when {
                        languageCode.startsWith("bn") -> "bn-BD"
                        languageCode.startsWith("hi") -> "hi-IN"
                        languageCode.startsWith("ar") -> "ar"
                        else -> "en-US"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                statusText = "Speak now"
            } catch (e: Exception) {
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
