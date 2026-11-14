package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
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

    var isSessionDesiredState by mutableStateOf(false)
        private set

    val isSessionActive: Boolean
        get() = isListening || isSessionDesiredState

    var isProcessing by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("এখন বলুন...")
        private set

    var partialText by mutableStateOf("")
        private set

    var rmsLevel by mutableFloatStateOf(0f)
        private set

    var permissionMissing by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    /**
     * When true, TypeGenius prioritizes on-device / offline speech recognition packs
     * so voice typing works smoothly without internet for Bangla & English.
     */
    var preferOffline by mutableStateOf(true)
        private set

    var isUsingOfflineEngine by mutableStateOf(false)
        private set

    var isDeviceOffline by mutableStateOf(false)
        private set

    var offlineModelMissing by mutableStateOf(false)
        private set

    var activeVoiceLanguageCode by mutableStateOf("bn")
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isSessionDesired = false
    private var hardErrorStreak = 0
    private var onDeviceRetryBypassed = false
    private var lastCyclePartial = ""
    private var activeOnPermissionRequired: () -> Unit = {}
    private var activeOnTextRecognized: (String) -> Unit = {}

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) {
            // In Robolectric or restricted sandboxes, avoid false-offline failures unless verifiable
            true
        }
    }

    fun isOnDeviceEngineAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)
    }

    fun activeVoiceBadgeLabel(): String {
        val code = activeVoiceLanguageCode.lowercase()
        return when {
            code == "banglish" || code.startsWith("bn") || code == "syl" || code == "as" -> "বাংলা"
            code.startsWith("hi") -> "हिन्दी"
            code.startsWith("ar") -> "العربية"
            code.startsWith("ur") -> "اردو"
            code.startsWith("fa") -> "فارسی"
            else -> "EN"
        }
    }

    fun toggleContinuousMode() {
        continuousMode = !continuousMode
        if (isSessionDesired) {
            statusText = buildListeningPrompt()
        }
    }

    fun togglePreferOffline() {
        preferOffline = !preferOffline
        onDeviceRetryBypassed = false
        offlineModelMissing = false
        if (isSessionDesired) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.post { launchRecognizerCycle() }
        }
    }

    fun toggleBanglaEnglishVoice() {
        val next = if (activeVoiceLanguageCode.startsWith("bn") || activeVoiceLanguageCode == "banglish" || activeVoiceLanguageCode == "syl") {
            "en"
        } else {
            "bn"
        }
        activeVoiceLanguageCode = next
        onDeviceRetryBypassed = false
        offlineModelMissing = false
        if (isSessionDesired) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.post { launchRecognizerCycle() }
        }
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
        activeVoiceLanguageCode = languageCode
        activeOnPermissionRequired = onPermissionRequired
        activeOnTextRecognized = onTextRecognized
        isSessionDesired = true
        isSessionDesiredState = true
        hardErrorStreak = 0
        onDeviceRetryBypassed = false
        offlineModelMissing = false
        lastCyclePartial = ""
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post { launchRecognizerCycle() }
    }

    fun downloadOfflineVoicePackOrOpenSettings() {
        val langTag = resolveLanguageTag(activeVoiceLanguageCode)
        var downloadTriggered = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && isOnDeviceEngineAvailable()) {
            runCatching {
                val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                val intent = createSpeechIntent(langTag, forceOffline = true)
                recognizer.triggerModelDownload(intent)
                recognizer.destroy()
                downloadTriggered = true
            }
        }
        if (downloadTriggered) {
            Toast.makeText(
                context,
                "$langTag অফলাইন ভয়েস প্যাক ডাউনলোড হচ্ছে (Google Voice Settings)...",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        // Fallback: open Google Voice Search / Offline Speech Recognition Settings
        val candidateActions = listOf(
            "com.google.android.voicesearch.INSTALL_OFFLINE_SPEECH_PACKS",
            Settings.ACTION_VOICE_INPUT_SETTINGS,
            Settings.ACTION_INPUT_METHOD_SETTINGS,
            Settings.ACTION_SETTINGS
        )
        for (action in candidateActions) {
            val started = runCatching {
                val intent = Intent(action).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (started) {
                Toast.makeText(
                    context,
                    "অফলাইনে বাংলা ও English বলতে Offline Speech Pack ডাউনলোড করুন",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }
    }

    private fun buildListeningPrompt(): String {
        val lang = activeVoiceBadgeLabel()
        return when {
            isUsingOfflineEngine -> "অফলাইনে শুনছি ($lang)..."
            continuousMode -> "শুনছি ($lang • Auto)..."
            else -> "এখন বলুন ($lang)..."
        }
    }

    private fun createSpeechIntent(langTag: String, forceOffline: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, forceOffline)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 12000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2800L)
        }
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

    private fun teardownRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    private fun flushPartialIfUncommitted() {
        val pending = lastCyclePartial.trim()
        if (pending.isNotEmpty()) {
            lastCyclePartial = ""
            partialText = ""
            activeOnTextRecognized(pending)
        }
    }

    private fun launchRecognizerCycle() {
        if (!isSessionDesired) return

        if (!hasAudioPermission()) {
            isSessionDesired = false
            isSessionDesiredState = false
            permissionMissing = true
            isListening = false
            isProcessing = false
            hasError = true
            rmsLevel = 0f
            partialText = ""
            lastCyclePartial = ""
            statusText = "মাইক্রোফোন অনুমতি দিন"
            activeOnPermissionRequired()
            return
        }
        permissionMissing = false
        hasError = false
        partialText = ""
        lastCyclePartial = ""
        rmsLevel = 0f

        val online = isNetworkConnected()
        isDeviceOffline = !online
        val useOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !onDeviceRetryBypassed &&
            (preferOffline || !online) &&
            isOnDeviceEngineAvailable()

        val standardAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        if (!useOnDevice && !standardAvailable) {
            isSessionDesired = false
            isSessionDesiredState = false
            statusText = "এই ডিভাইসে ভয়েস সার্ভিস পাওয়া যায়নি"
            isListening = false
            isProcessing = false
            hasError = true
            return
        }

        val usedOnDeviceThisCycle = useOnDevice
        isUsingOfflineEngine = usedOnDeviceThisCycle || preferOffline || !online
        val langTag = resolveLanguageTag(activeVoiceLanguageCode)

        try {
            teardownRecognizer()
            val recognizer = if (usedOnDeviceThisCycle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    isProcessing = false
                    hasError = false
                    offlineModelMissing = false
                    partialText = ""
                    statusText = buildListeningPrompt()
                }

                override fun onBeginningOfSpeech() {
                    hardErrorStreak = 0
                    isProcessing = false
                    statusText = if (usedOnDeviceThisCycle) "অফলাইন রেকর্ড হচ্ছে..." else "শুনছি..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    rmsLevel = (rmsLevel * 0.40f) + (normalized * 0.60f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isProcessing = true
                    rmsLevel = 0.18f
                    statusText = "প্রসেস হচ্ছে..."
                }

                override fun onError(error: Int) {
                    val isPermissionError = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                    val isLanguageOrOfflineUnavailable =
                        error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                    val isSilentOrIdleError = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                        error == SpeechRecognizer.ERROR_CLIENT
                    val isRecoverableSystemError = error == SpeechRecognizer.ERROR_AUDIO ||
                        error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                        error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_NETWORK

                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        flushPartialIfUncommitted()
                    } else {
                        lastCyclePartial = ""
                    }

                    // If OnDevice failed because this OEM's offline pack isn't installed yet:
                    // - If online, seamlessly fall back to standard SpeechRecognizer.
                    // - If offline, show explicit 1-tap Offline Pack Download prompt!
                    if (usedOnDeviceThisCycle && (isLanguageOrOfflineUnavailable || error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_NETWORK)) {
                        if (isNetworkConnected() && standardAvailable && !onDeviceRetryBypassed) {
                            onDeviceRetryBypassed = true
                            isUsingOfflineEngine = false
                            scheduleNextCycle(160L)
                            return
                        } else if (isLanguageOrOfflineUnavailable) {
                            isSessionDesired = false
                            isSessionDesiredState = false
                            isListening = false
                            isProcessing = false
                            rmsLevel = 0f
                            hasError = true
                            offlineModelMissing = true
                            partialText = ""
                            statusText = "${activeVoiceBadgeLabel()} অফলাইন প্যাক নামান"
                            return
                        }
                    }

                    if (!isPermissionError && isSessionDesired && continuousMode) {
                        if (isSilentOrIdleError) {
                            hardErrorStreak = 0
                            isListening = true
                            isProcessing = false
                            hasError = false
                            rmsLevel = 0f
                            partialText = ""
                            statusText = buildListeningPrompt()
                            val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) 380L else 240L
                            scheduleNextCycle(delay)
                            return
                        }
                        if (isRecoverableSystemError && hardErrorStreak < 6) {
                            hardErrorStreak++
                            isListening = true
                            isProcessing = false
                            hasError = false
                            rmsLevel = 0f
                            partialText = ""
                            statusText = "মাইক পুনরায় সংযোগ হচ্ছে..."
                            scheduleNextCycle(520L)
                            return
                        }
                    }

                    isSessionDesired = false
                    isSessionDesiredState = false
                    isListening = false
                    isProcessing = false
                    rmsLevel = 0f
                    hasError = true
                    partialText = ""
                    lastCyclePartial = ""
                    statusText = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            permissionMissing = true
                            "মাইক্রোফোন অনুমতি দিন"
                        }
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                            offlineModelMissing = true
                            "${activeVoiceBadgeLabel()} অফলাইন প্যাক নামান"
                        }
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            offlineModelMissing = true
                            "নেট নেই — অফলাইন প্যাক নামান"
                        }
                        SpeechRecognizer.ERROR_NO_MATCH -> "শোনা যায়নি — আবার মাইক চাপুন"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি — মাইক চাপুন"
                        else -> "আবার বলতে মাইক চাপুন"
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val committed = matches?.firstOrNull()?.trim().orEmpty()
                    if (committed.isNotEmpty()) {
                        hardErrorStreak = 0
                        offlineModelMissing = false
                        lastCyclePartial = ""
                        activeOnTextRecognized(committed)
                    } else {
                        flushPartialIfUncommitted()
                    }
                    isProcessing = false
                    hasError = false
                    rmsLevel = 0f
                    partialText = ""

                    if (isSessionDesired && continuousMode) {
                        isListening = true
                        statusText = buildListeningPrompt()
                        scheduleNextCycle(260L)
                    } else {
                        isSessionDesired = false
                        isSessionDesiredState = false
                        isListening = false
                        statusText = if (committed.isNotEmpty()) "আরও বলতে মাইক চাপুন" else "বলতে মাইক চাপুন"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        hardErrorStreak = 0
                        lastCyclePartial = partial
                        partialText = partial
                        statusText = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer = recognizer
            val intent = createSpeechIntent(
                langTag = langTag,
                forceOffline = preferOffline || !online
            )
            recognizer.startListening(intent)
            isListening = true
            isProcessing = false
            statusText = buildListeningPrompt()
        } catch (_: SecurityException) {
            isSessionDesired = false
            isSessionDesiredState = false
            permissionMissing = true
            isListening = false
            isProcessing = false
            hasError = true
            rmsLevel = 0f
            lastCyclePartial = ""
            statusText = "মাইক্রোফোন অনুমতি দিন"
            activeOnPermissionRequired()
        } catch (_: Exception) {
            if (usedOnDeviceThisCycle && standardAvailable && !onDeviceRetryBypassed) {
                onDeviceRetryBypassed = true
                scheduleNextCycle(180L)
            } else if (isSessionDesired && continuousMode && hardErrorStreak < 4) {
                hardErrorStreak++
                scheduleNextCycle(450L)
            } else {
                isSessionDesired = false
                isSessionDesiredState = false
                isListening = false
                isProcessing = false
                hasError = true
                rmsLevel = 0f
                lastCyclePartial = ""
                statusText = "আবার বলতে মাইক চাপুন"
            }
        }
    }

    internal fun resolveLanguageTag(languageCode: String): String {
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
        isSessionDesiredState = false
        flushPartialIfUncommitted()
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            teardownRecognizer()
            isListening = false
            isProcessing = false
            hasError = false
            rmsLevel = 0f
            partialText = ""
            lastCyclePartial = ""
            statusText = "এখন বলুন..."
        }
    }

    fun destroy() {
        stopListening()
    }
}
