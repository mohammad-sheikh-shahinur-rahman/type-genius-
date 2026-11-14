package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

enum class VoiceRoutingMode(val shortLabel: String) {
    /** Uses online cloud speech when internet is available, and automatically switches to offline on-device engine when offline. */
    AUTO("অটো"),
    /** Forces 100% offline/on-device speech recognition even when network is available. */
    OFFLINE_ONLY("অফলাইন"),
    /** Prefers online cloud recognition. */
    ONLINE_ONLY("অনলাইন")
}

class VoiceInputController(private val context: Context) {

    var continuousMode by mutableStateOf(true)
        private set

    var routingMode by mutableStateOf(VoiceRoutingMode.AUTO)
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

    var isUsingOfflineEngine by mutableStateOf(false)
        private set

    var isDeviceOffline by mutableStateOf(false)
        private set

    var offlineModelMissing by mutableStateOf(false)
        private set

    var isCurrentOfflinePackInstalled by mutableStateOf<Boolean?>(null)
        private set

    var activeVoiceLanguageCode by mutableStateOf("bn")
        private set

    var alternativeHypotheses by mutableStateOf<List<String>>(emptyList())
        private set

    /** Backwards-compatible flag indicating offline preference when explicit OFFLINE_ONLY or currently offline in AUTO. */
    val preferOffline: Boolean
        get() = routingMode == VoiceRoutingMode.OFFLINE_ONLY ||
            (routingMode == VoiceRoutingMode.AUTO && isDeviceOffline)

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val supportExecutor = Executors.newSingleThreadExecutor()
    private var isSessionDesired = false
    private var hardErrorStreak = 0
    private var onDeviceRetryBypassed = false
    private var triedAltBanglaLocale = false
    private var lastCyclePartial = ""
    private var lastCommittedPhrase = ""
    private var lastCommittedTimeMs = 0L
    private var activeOnPermissionRequired: () -> Unit = {}
    private var activeOnTextRecognized: (String) -> Unit = {}
    private var networkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainHandler.post {
                val wasOffline = isDeviceOffline
                isDeviceOffline = false
                if (wasOffline && isSessionDesired && routingMode == VoiceRoutingMode.AUTO) {
                    // Network came back: upgrade smoothly to online engine on next cycle
                    onDeviceRetryBypassed = false
                    statusText = buildListeningPrompt()
                }
            }
        }

        override fun onLost(network: Network) {
            mainHandler.post {
                val onlineNow = isNetworkConnected()
                isDeviceOffline = !onlineNow
                if (!onlineNow && isSessionDesired && routingMode == VoiceRoutingMode.AUTO) {
                    // Network dropped mid-session: seamlessly continue offline
                    statusText = buildListeningPrompt()
                }
            }
        }
    }

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

    fun cycleRoutingMode() {
        routingMode = when (routingMode) {
            VoiceRoutingMode.AUTO -> VoiceRoutingMode.OFFLINE_ONLY
            VoiceRoutingMode.OFFLINE_ONLY -> VoiceRoutingMode.ONLINE_ONLY
            VoiceRoutingMode.ONLINE_ONLY -> VoiceRoutingMode.AUTO
        }
        onDeviceRetryBypassed = false
        triedAltBanglaLocale = false
        offlineModelMissing = false
        if (isSessionDesired) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.post { launchRecognizerCycle() }
        }
    }

    /** Backwards-compatible alias that toggles between AUTO and OFFLINE_ONLY. */
    fun togglePreferOffline() {
        routingMode = if (routingMode == VoiceRoutingMode.OFFLINE_ONLY) {
            VoiceRoutingMode.AUTO
        } else {
            VoiceRoutingMode.OFFLINE_ONLY
        }
        onDeviceRetryBypassed = false
        triedAltBanglaLocale = false
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
        triedAltBanglaLocale = false
        offlineModelMissing = false
        refreshOnDeviceSupportStatus()
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
        triedAltBanglaLocale = false
        offlineModelMissing = false
        lastCyclePartial = ""
        alternativeHypotheses = emptyList()
        registerNetworkCallbackIfNeeded()
        refreshOnDeviceSupportStatus()
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post { launchRecognizerCycle() }
    }

    private fun refreshOnDeviceSupportStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !isOnDeviceEngineAvailable()) {
            isCurrentOfflinePackInstalled = null
            return
        }
        val langTag = resolveEffectiveLanguageTag()
        runCatching {
            val checker = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            val intent = createSpeechIntent(langTag, forceOffline = true)
            checker.checkRecognitionSupport(
                intent,
                supportExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        val installed = recognitionSupport.installedOnDeviceLanguages
                        val ready = installed.any {
                            it.equals(langTag, ignoreCase = true) ||
                                it.startsWith(langTag.substringBefore('-'), ignoreCase = true)
                        }
                        mainHandler.post {
                            isCurrentOfflinePackInstalled = ready
                            runCatching { checker.destroy() }
                        }
                    }

                    override fun onError(error: Int) {
                        mainHandler.post {
                            runCatching { checker.destroy() }
                        }
                    }
                }
            )
        }
    }

    fun downloadOfflineVoicePackOrOpenSettings() {
        val langTag = resolveEffectiveLanguageTag()
        var downloadTriggered = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && isOnDeviceEngineAvailable()) {
            runCatching {
                val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                val intent = createSpeechIntent(langTag, forceOffline = true)
                recognizer.triggerModelDownload(intent)
                // Also warm en-US offline model when requesting offline support so EN <-> BN works seamlessly
                if (!langTag.startsWith("en", ignoreCase = true)) {
                    recognizer.triggerModelDownload(createSpeechIntent("en-US", forceOffline = true))
                }
                recognizer.destroy()
                downloadTriggered = true
            }
        }
        if (downloadTriggered) {
            Toast.makeText(
                context,
                "$langTag ও English অফলাইন ভয়েস প্যাক ডাউনলোড শুরু হয়েছে...",
                Toast.LENGTH_LONG
            ).show()
            return
        }
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

    private fun registerNetworkCallbackIfNeeded() {
        if (networkCallbackRegistered) return
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, networkCallback)
            networkCallbackRegistered = true
        }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) return
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.unregisterNetworkCallback(networkCallback)
        }
        networkCallbackRegistered = false
    }

    private fun shouldUseOfflineEngineNow(online: Boolean): Boolean {
        return when (routingMode) {
            VoiceRoutingMode.ONLINE_ONLY -> false
            VoiceRoutingMode.OFFLINE_ONLY -> true
            VoiceRoutingMode.AUTO -> !online
        }
    }

    private fun resolveEffectiveLanguageTag(): String {
        val base = resolveLanguageTag(activeVoiceLanguageCode)
        return if (triedAltBanglaLocale && base == "bn-BD") "bn-IN" else base
    }

    private fun buildListeningPrompt(): String {
        val lang = activeVoiceBadgeLabel()
        return when {
            isUsingOfflineEngine -> "অফলাইন ইঞ্জিন শুনছে ($lang)..."
            continuousMode -> "অনলাইনে শুনছি ($lang • Auto)..."
            else -> "অনলাইনে বলুন ($lang)..."
        }
    }

    private fun createSpeechIntent(langTag: String, forceOffline: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Request up to 5 hypotheses for N-best offline reranking & quick alternatives
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, forceOffline)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, if (forceOffline) 9000L else 12000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, if (forceOffline) 3400L else 4500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, if (forceOffline) 1900L else 2800L)
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

    private fun commitProcessedSpeech(
        candidates: List<String>,
        confidences: FloatArray? = null
    ): Boolean {
        val langTag = resolveEffectiveLanguageTag()
        val rawBest = OfflineVoicePostProcessor.pickBestCandidate(
            candidates = candidates,
            confidences = confidences,
            languageTag = langTag
        )
        val processed = OfflineVoicePostProcessor.processSpokenText(
            rawText = rawBest,
            languageTag = langTag,
            previousCommittedTail = lastCommittedPhrase
        )
        if (processed.isEmpty()) return false

        val now = System.currentTimeMillis()
        if (OfflineVoicePostProcessor.isDuplicateEcho(processed, lastCommittedPhrase, now - lastCommittedTimeMs)) {
            return false
        }

        lastCommittedPhrase = processed
        lastCommittedTimeMs = now

        alternativeHypotheses = candidates
            .map { OfflineVoicePostProcessor.processSpokenText(it, langTag, lastCommittedPhrase) }
            .filter { it.isNotEmpty() && it != processed }
            .distinct()
            .take(2)

        activeOnTextRecognized(processed)
        return true
    }

    private fun flushPartialIfUncommitted() {
        val pending = lastCyclePartial.trim()
        if (pending.isNotEmpty()) {
            lastCyclePartial = ""
            partialText = ""
            commitProcessedSpeech(listOf(pending))
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
        val preferOfflineNow = shouldUseOfflineEngineNow(online)
        val useDedicatedOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !onDeviceRetryBypassed &&
            preferOfflineNow &&
            isOnDeviceEngineAvailable()

        val standardAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        if (!useDedicatedOnDevice && !standardAvailable) {
            isSessionDesired = false
            isSessionDesiredState = false
            statusText = "এই ডিভাইসে ভয়েস সার্ভিস পাওয়া যায়নি"
            isListening = false
            isProcessing = false
            hasError = true
            return
        }

        isUsingOfflineEngine = preferOfflineNow
        val langTag = resolveEffectiveLanguageTag()

        try {
            teardownRecognizer()
            val recognizer = if (useDedicatedOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                    statusText = if (preferOfflineNow) "অফলাইন রেকর্ড হচ্ছে..." else "অনলাইন রেকর্ড হচ্ছে..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    rmsLevel = (rmsLevel * 0.40f) + (normalized * 0.60f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isProcessing = true
                    rmsLevel = 0.18f
                    statusText = if (preferOfflineNow) "অফলাইনে প্রসেস হচ্ছে..." else "প্রসেস হচ্ছে..."
                }

                override fun onError(error: Int) {
                    val isPermissionError = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                    val isLanguageOrOfflineUnavailable =
                        error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                    val isNetworkFail = error == SpeechRecognizer.ERROR_NETWORK ||
                        error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                    val isSilentOrIdleError = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                        error == SpeechRecognizer.ERROR_CLIENT
                    val isRecoverableSystemError = error == SpeechRecognizer.ERROR_AUDIO ||
                        error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED

                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        flushPartialIfUncommitted()
                    } else {
                        lastCyclePartial = ""
                    }

                    // 1. Offline Bangla fallback: if bn-BD offline pack is absent, try bn-IN offline pack automatically!
                    if (isLanguageOrOfflineUnavailable && !triedAltBanglaLocale && resolveLanguageTag(activeVoiceLanguageCode) == "bn-BD") {
                        triedAltBanglaLocale = true
                        scheduleNextCycle(140L)
                        return
                    }

                    // 2. If online recognizer hit network failure in AUTO mode, fall back seamlessly to On-Device Offline Engine!
                    if (isNetworkFail && routingMode == VoiceRoutingMode.AUTO && !preferOfflineNow) {
                        isDeviceOffline = true
                        isUsingOfflineEngine = true
                        onDeviceRetryBypassed = false
                        triedAltBanglaLocale = false
                        statusText = "নেট দুর্বল — অফলাইন ইঞ্জিন চালু..."
                        scheduleNextCycle(180L)
                        return
                    }

                    // 3. If dedicated OnDevice recognizer couldn't run while online in non-OFFLINE_ONLY mode, fall back to cloud
                    if (useDedicatedOnDevice && (isLanguageOrOfflineUnavailable || error == SpeechRecognizer.ERROR_CLIENT)) {
                        if (isNetworkConnected() && routingMode != VoiceRoutingMode.OFFLINE_ONLY && standardAvailable && !onDeviceRetryBypassed) {
                            onDeviceRetryBypassed = true
                            isUsingOfflineEngine = false
                            scheduleNextCycle(160L)
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
                            val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) 360L else 220L
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
                            scheduleNextCycle(500L)
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
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val didCommit = commitProcessedSpeech(matches, confidences)
                    if (didCommit) {
                        hardErrorStreak = 0
                        offlineModelMissing = false
                        lastCyclePartial = ""
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
                        scheduleNextCycle(240L)
                    } else {
                        isSessionDesired = false
                        isSessionDesiredState = false
                        isListening = false
                        statusText = if (didCommit) "আরও বলতে মাইক চাপুন" else "বলতে মাইক চাপুন"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        hardErrorStreak = 0
                        lastCyclePartial = partial
                        val preview = OfflineVoicePostProcessor.processSpokenText(
                            rawText = partial,
                            languageTag = langTag,
                            previousCommittedTail = lastCommittedPhrase
                        )
                        partialText = preview.ifBlank { partial }
                        statusText = partialText
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer = recognizer
            val intent = createSpeechIntent(
                langTag = langTag,
                forceOffline = preferOfflineNow
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
            if (useDedicatedOnDevice && routingMode != VoiceRoutingMode.OFFLINE_ONLY && standardAvailable && !onDeviceRetryBypassed) {
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
        unregisterNetworkCallback()
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
        runCatching { supportExecutor.shutdownNow() }
    }
}
