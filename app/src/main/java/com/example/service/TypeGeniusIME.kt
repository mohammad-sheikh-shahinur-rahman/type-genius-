package com.example.service

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.CrashDiagnosticLogger
import com.example.data.CustomFontManager
import com.example.data.SettingsManager
import com.example.data.TypingStatsManager
import com.example.data.ai.TypeGeniusAiEngine
import com.example.data.dictionary.BigramPredictionEngine
import com.example.data.dictionary.DictionaryManager
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.GrammarSuggestion
import com.example.data.dictionary.OfflineGlideEngine
import com.example.data.dictionary.OfflineGrammarEngine
import com.example.data.dictionary.OfflineMathEvaluator
import com.example.data.dictionary.ProductivityTextTools
import com.example.data.dictionary.TextTransformMode
import com.example.data.dictionary.UnicodeTextStyler
import com.example.data.local.ClipboardItem
import com.example.data.local.SmartClipInfo
import com.example.data.local.SmartClipboardParser
import com.example.data.local.TextShortcutRepository
import com.example.data.local.TypeGeniusDatabase
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.data.translator.OfflineTranslator
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import com.example.model.PanelTextInputTarget
import com.example.model.SoundProfile
import com.example.ui.keyboard.HandwritingCardFormat
import com.example.ui.keyboard.HandwritingOutputGenerator
import com.example.ui.keyboard.HandwritingPaperTheme
import com.example.ui.keyboard.KeyboardView
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TypeGeniusIME : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var settingsManager: SettingsManager
    private lateinit var dictionaryManager: DictionaryManager
    private lateinit var shortcutRepository: TextShortcutRepository
    private lateinit var database: TypeGeniusDatabase
    private lateinit var voiceInputController: VoiceInputController
    private lateinit var keySoundEngine: KeySoundEngine
    private lateinit var statsManager: TypingStatsManager
    private var audioManager: AudioManager? = null
    private var lastWordCommitTimeMs: Long = 0L
    private var smartClipSuggestion by mutableStateOf<SmartClipInfo?>(null)

    private var activeLanguage by mutableStateOf(LanguageId.BANGLA_PROBHAT)
    private var enabledLanguages by mutableStateOf(
        listOf(
            LanguageId.BANGLA_PROBHAT,
            LanguageId.BANGLA_PHONETIC,
            LanguageId.SYLHETI,
            LanguageId.ENGLISH,
            LanguageId.HINDI,
            LanguageId.ARABIC
        )
    )
    private var isShifted by mutableStateOf(false)
    private var isCapsLock by mutableStateOf(false)
    private var viewMode by mutableStateOf(KeyboardViewMode.LETTERS)
    private var suggestions by mutableStateOf(listOf<String>())
    private var composingBuffer by mutableStateOf("")
    private var isTypingActive by mutableStateOf(false)

    private var isTranslatorOpen by mutableStateOf(false)
    private var translatorSourceText by mutableStateOf("")
    private var translatorComposingPreview by mutableStateOf("")
    private var isTranslatorVoiceActive by mutableStateOf(false)

    private var activeFancyStyle by mutableStateOf(FancyFontStyle.NORMAL)
    private var handwritingLiveText by mutableStateOf("")
    private var customFontPathCache by mutableStateOf("")
    private var customFontNameCache by mutableStateOf("")
    private var lastTypedWordPreview by mutableStateOf("")
    private var lastDeletedUndoText by mutableStateOf("")
    private var lastDeletedWordCount by mutableIntStateOf(0)
    private var lastGlideCommittedWord by mutableStateOf("")
    private var activeGrammarSuggestions: Map<String, GrammarSuggestion> = emptyMap()
    private var liveGrammarIssueCount by mutableIntStateOf(0)
    private var aiSourceSnapshot by mutableStateOf("")
    private var lastAiOriginalText by mutableStateOf("")
    private var lastAiReplacedText by mutableStateOf("")
    private var aiCapturedFromSelection = false
    private var aiCapturedBefore = ""
    private var aiCapturedAfter = ""

    private var soundEnabled = false
    private var soundVolume = 0.75f
    private var soundProfile = SoundProfile.SYSTEM
    private var spellCheckEnabled = true
    private var autoCapitalizeEnabled = true
    private var isIncognitoEnabled = false
    private var otpAutoExpireMinutes = 3
    private var perAppSmartLanguageEnabled = true
    private var perAppLanguageMap: Map<String, String> = emptyMap()
    private var currentAppPackage: String? = null
    private var isSensitiveLockedField = false
    private var isAutoFieldIncognito by mutableStateOf(false)
    private var handwritingSignatureCache by mutableStateOf("")
    private var latestClipboardCache by mutableStateOf("")

    private fun isPrivacyPaused(): Boolean = isIncognitoEnabled || isAutoFieldIncognito

    private var panelInputTarget by mutableStateOf(PanelTextInputTarget.NONE)
    private var clipboardSearchQuery by mutableStateOf("")
    private var gifSearchQuery by mutableStateOf("")
    private var customStickerText by mutableStateOf("অস্থির হইছে বস!")
    private var todoDraftInput by mutableStateOf("")
    private var autofillEditingKey by mutableStateOf("")
    private var autofillDraftValue by mutableStateOf("")
    private var notepadLiveDraft by mutableStateOf("")

    private var pomodoroRemainingSec by mutableIntStateOf(25 * 60)
    private var pomodoroIsBreak by mutableStateOf(false)
    private var pomodoroRunning by mutableStateOf(false)
    private var pomodoroJob: kotlinx.coroutines.Job? = null

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    private var hasInitializedLanguage = false

    override fun onCreate() {
        super.onCreate()
        CrashDiagnosticLogger.installUncaughtHandler(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        settingsManager = SettingsManager.getInstance(this)
        dictionaryManager = DictionaryManager(this)
        shortcutRepository = TextShortcutRepository(this)
        database = TypeGeniusDatabase.getDatabase(this)
        voiceInputController = VoiceInputController(this)
        keySoundEngine = KeySoundEngine.getInstance(this)
        statsManager = TypingStatsManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        serviceScope.launch {
            launch { OfflineTranslator.initialize(this@TypeGeniusIME) }
            launch { shortcutRepository.ensureSeededDefaults() }
            launch { settingsManager.soundOnKeyPress.collect { soundEnabled = it } }
            launch { settingsManager.soundVolume.collect { soundVolume = it } }
            launch { settingsManager.soundProfile.collect { soundProfile = it } }
            launch { settingsManager.spellCheckEnabled.collect { spellCheckEnabled = it } }
            launch { settingsManager.autoCapitalize.collect { autoCapitalizeEnabled = it } }
            launch { settingsManager.isIncognitoMode.collect { isIncognitoEnabled = it } }
            launch { settingsManager.activeFancyFontStyle.collect { activeFancyStyle = it } }
            launch { settingsManager.customFontPath.collect { customFontPathCache = it } }
            launch { settingsManager.customFontName.collect { customFontNameCache = it } }
            launch { settingsManager.handwritingSignature.collect { handwritingSignatureCache = it } }
            launch { settingsManager.notepadDraft.collect { notepadLiveDraft = it } }
            launch { settingsManager.otpAutoExpireMinutes.collect { otpAutoExpireMinutes = it } }
            launch { settingsManager.perAppSmartLanguageEnabled.collect { perAppSmartLanguageEnabled = it } }
            launch { settingsManager.perAppLanguageMap.collect { perAppLanguageMap = it } }
            launch(Dispatchers.IO) {
                while (true) {
                    runCatching {
                        database.clipboardDao().deleteExpired(System.currentTimeMillis())
                    }
                    delay(10_000L)
                }
            }
            launch {
                settingsManager.enabledLanguages.collect { enabledCodes ->
                    val list = enabledCodes.map { LanguageId.fromCode(it) }
                    if (list.isNotEmpty()) {
                        enabledLanguages = list
                        if (hasInitializedLanguage && !enabledLanguages.contains(activeLanguage)) {
                            activeLanguage = enabledLanguages.first()
                        }
                    }
                }
            }
            soundEnabled = settingsManager.soundOnKeyPress.first()
            soundVolume = settingsManager.soundVolume.first()
            soundProfile = settingsManager.soundProfile.first()
            otpAutoExpireMinutes = settingsManager.otpAutoExpireMinutes.first()
            perAppSmartLanguageEnabled = settingsManager.perAppSmartLanguageEnabled.first()
            perAppLanguageMap = settingsManager.perAppLanguageMap.first()
            val savedLangCode = settingsManager.activeLanguageCode.first()
            activeLanguage = LanguageId.fromCode(savedLangCode)
            hasInitializedLanguage = true
            activeFancyStyle = settingsManager.activeFancyFontStyle.first()
            customFontPathCache = settingsManager.customFontPath.first()
            customFontNameCache = settingsManager.customFontName.first()
        }

        setupClipboardMonitoring()
    }

    private fun setupClipboardMonitoring() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                if (isPrivacyPaused()) return@OnPrimaryClipChangedListener
                val clip = cm?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrBlank()) {
                        val parsed = SmartClipboardParser.classify(text)
                        smartClipSuggestion = if (SmartClipboardParser.isHighPrioritySmartClip(parsed)) parsed else null
                        val isSensitive = SmartClipboardParser.isSensitiveForAutoExpire(text, parsed)
                        val now = System.currentTimeMillis()
                        val expiresAt = if (isSensitive && otpAutoExpireMinutes > 0) {
                            now + otpAutoExpireMinutes * 60_000L
                        } else null
                        serviceScope.launch(Dispatchers.IO) {
                            val dao = database.clipboardDao()
                            val existing = dao.findByContent(text)
                            if (existing != null) {
                                val updatedExpiresAt = if (existing.isPinned) {
                                    null
                                } else {
                                    expiresAt
                                }
                                dao.update(
                                    existing.copy(
                                        timestamp = now,
                                        isSensitive = isSensitive,
                                        expiresAt = updatedExpiresAt
                                    )
                                )
                            } else {
                                dao.insert(
                                    ClipboardItem(
                                        content = text,
                                        timestamp = now,
                                        isSensitive = isSensitive,
                                        expiresAt = expiresAt
                                    )
                                )
                            }
                        }
                    }
                }
            }
            cm?.addPrimaryClipChangedListener(clipboardListener)
        } catch (_: Exception) {}
    }

    override fun onCreateInputView(): View {
        val window = window?.window
        val decorView = window?.decorView ?: window?.peekDecorView()

        decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val clipboardFlow = database.clipboardDao().getAllItems()
                val clipItems by clipboardFlow.collectAsState(initial = emptyList())
                latestClipboardCache = clipItems.firstOrNull()?.content.orEmpty()

                KeyboardView(
                    activeLanguage = activeLanguage,
                    enabledLanguages = enabledLanguages,
                    isShifted = isShifted,
                    isCapsLock = isCapsLock,
                    viewMode = viewMode,
                    suggestions = suggestions,
                    isTypingActive = isTypingActive,
                    settingsManager = settingsManager,
                    clipboardItems = clipItems,
                    isTranslatorOpen = isTranslatorOpen,
                    translatorSourceText = translatorSourceText,
                    translatorComposingPreview = translatorComposingPreview,
                    isTranslatorVoiceActive = isTranslatorVoiceActive,
                    activeFancyStyle = activeFancyStyle,
                    lastTypedWordPreview = lastTypedWordPreview,
                    canUndoDelete = lastDeletedUndoText.isNotEmpty(),
                    undoDeletedWordCount = lastDeletedWordCount,
                    isAutoIncognitoActive = isAutoFieldIncognito,
                    onOpenTranslator = { openTranslator() },
                    onCloseTranslator = { closeTranslator() },
                    onTranslatorSourceTextChange = { translatorSourceText = it },
                    onTranslatorClear = {
                        composingBuffer = ""
                        translatorComposingPreview = ""
                        translatorSourceText = ""
                        isTypingActive = false
                        serviceScope.launch {
                            suggestions = dictionaryManager.getDefaultSuggestions(activeLanguage)
                        }
                    },
                    onTranslatorPaste = { pasteIntoTranslator() },
                    onTranslatorCopy = { text -> copyToSystemClipboard(text) },
                    onTranslatorShare = { text -> shareTranslatedText(text) },
                    onTranslatorInsert = { text -> insertTranslatedText(text) },
                    onTranslatorToggleVoice = { langCode -> toggleTranslatorVoice(langCode) },
                    onTranslatorStopVoice = { stopTranslatorVoice() },
                    onKeyAction = { handleKeyAction(it) },
                    onSelectViewMode = { targetMode ->
                        if (targetMode == KeyboardViewMode.TRANSLATOR) {
                            panelInputTarget = PanelTextInputTarget.NONE
                            openTranslator()
                        } else {
                            if (composingBuffer.isNotEmpty()) commitComposing()
                            panelInputTarget = PanelTextInputTarget.NONE
                            if (targetMode == KeyboardViewMode.GEMINI_AI) {
                                aiSourceSnapshot = readFieldTextForAi()
                            }
                            viewMode = targetMode
                        }
                    },
                    onSuggestionClick = { handleSuggestionCommit(it) },
                    onSwipeNextLanguage = { cycleLanguage(forward = true) },
                    onSwipePrevLanguage = { cycleLanguage(forward = false) },
                    onLanguageSelected = { selectLanguage(it) },
                    onOpenSettings = { openCompanionSettings() },
                    onShareApp = { shareTypeGenius() },
                    onClipboardItemClick = { text ->
                        if (isTranslatorOpen) {
                            translatorSourceText += text
                        } else {
                            currentInputConnection?.commitText(text, 1)
                        }
                        refreshHandwritingLiveText()
                        viewMode = KeyboardViewMode.LETTERS
                    },
                    onCopyClipboardItem = { item ->
                        copyToSystemClipboard(item.content)
                    },
                    onTogglePinClipboardItem = { item ->
                        serviceScope.launch(Dispatchers.IO) {
                            val nowPinned = !item.isPinned
                            val newExpiresAt = if (nowPinned) {
                                null
                            } else if (item.isSensitive && otpAutoExpireMinutes > 0) {
                                System.currentTimeMillis() + otpAutoExpireMinutes * 60_000L
                            } else {
                                item.expiresAt
                            }
                            database.clipboardDao().update(
                                item.copy(
                                    isPinned = nowPinned,
                                    expiresAt = newExpiresAt
                                )
                            )
                        }
                    },
                    onDeleteClipboardItem = { item ->
                        serviceScope.launch(Dispatchers.IO) {
                            database.clipboardDao().delete(item)
                        }
                    },
                    onClearClipboard = {
                        serviceScope.launch(Dispatchers.IO) {
                            database.clipboardDao().clearUnpinned()
                        }
                    },
                    onClearEverythingClipboard = {
                        serviceScope.launch(Dispatchers.IO) {
                            database.clipboardDao().clearAll()
                        }
                    },
                    onMoveCursor = { dx, dy, select -> moveCursor(dx, dy, select) },
                    onMoveToStart = { select -> sendEditorKey(KeyEvent.KEYCODE_MOVE_HOME, select) },
                    onMoveToEnd = { select -> sendEditorKey(KeyEvent.KEYCODE_MOVE_END, select) },
                    onSelectAll = {
                        currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                    },
                    onCopy = { performSmartCopy() },
                    onCut = { performSmartCut() },
                    onPaste = { performSmartPaste() },
                    onDeleteWordBackward = { deleteWordBackward() },
                    onUndoDelete = { undoLastDeletedWord() },
                    onTransformText = { mode -> transformEditorText(mode) },
                    onInsertNumberedList = { bnDigits -> insertNumberedListPrefix(bnDigits) },
                    onGlideCommitted = { trail, keyCenters -> handleGlideSwipe(trail, keyCenters) },
                    onSelectFancyStyle = { style ->
                        if (style == FancyFontStyle.HANDWRITING_CUSTOM && !CustomFontManager.hasCustomFont(customFontPathCache)) {
                            Toast.makeText(
                                this@TypeGeniusIME,
                                "সেটিংস থেকে আপনার হাতের লেখা (.zip/.ttf) ফন্ট যুক্ত করুন",
                                Toast.LENGTH_LONG
                            ).show()
                            openCompanionSettings(openFontManager = true)
                            return@KeyboardView
                        }
                        activeFancyStyle = style
                        serviceScope.launch {
                            settingsManager.setActiveFancyFontStyle(style)
                            style.handwritingPresetId()?.let { presetId ->
                                settingsManager.setKeyboardFontId(presetId)
                            }
                        }
                        refreshHandwritingLiveText()
                        viewMode = KeyboardViewMode.LETTERS
                    },
                    onTransformLastWord = { style -> transformLastWordWithStyle(style) },
                    onSendFancyStyleCard = { style, paperTheme, format, signature ->
                        sendFancyStyleCard(style, paperTheme, format, signature)
                    },
                    onOpenHandwritingImport = { openCompanionSettings(openFontManager = true) },
                    onInsertTextSnippet = { rawSnippet ->
                        val resolved = ProductivityTextTools.resolveTemplateVariables(rawSnippet, latestClipboardCache)
                        if (isTranslatorOpen) translatorSourceText += resolved
                        else currentInputConnection?.commitText(resolved, 1)
                        refreshHandwritingLiveText()
                        updateSuggestions()
                    },
                    handwritingLiveText = handwritingLiveText,
                    aiSourceText = aiSourceSnapshot,
                    canUndoAiEdit = lastAiOriginalText.isNotEmpty() && lastAiReplacedText.isNotEmpty(),
                    liveGrammarIssueCount = liveGrammarIssueCount,
                    smartClipSuggestion = smartClipSuggestion,
                    onSmartClipClick = { info ->
                        if (isTranslatorOpen) {
                            translatorSourceText += info.extractedValue
                        } else {
                            currentInputConnection?.commitText(info.extractedValue, 1)
                        }
                        smartClipSuggestion = null
                        refreshHandwritingLiveText()
                        updateSuggestions()
                    },
                    onDismissSmartClip = { smartClipSuggestion = null },
                    onRefreshAiSourceText = { readFieldTextForAi() },
                    onApplyAiText = { newText -> applyAiReplacement(newText) },
                    onUndoAiText = { undoAiReplacement() },
                    panelInputTarget = panelInputTarget,
                    clipboardSearchQuery = clipboardSearchQuery,
                    gifSearchQuery = gifSearchQuery,
                    customStickerText = customStickerText,
                    todoDraftInput = todoDraftInput,
                    autofillEditingKey = autofillEditingKey,
                    autofillDraftValue = autofillDraftValue,
                    pomodoroRemainingSec = pomodoroRemainingSec,
                    pomodoroIsBreak = pomodoroIsBreak,
                    pomodoroRunning = pomodoroRunning,
                    onTogglePomodoroRun = { togglePomodoroTimer() },
                    onResetPomodoro = { isBreak -> resetPomodoroTimer(isBreak) },
                    onStartAutofillEdit = { key, currentValue ->
                        if (composingBuffer.isNotEmpty()) commitComposing()
                        autofillEditingKey = key
                        autofillDraftValue = currentValue
                        panelInputTarget = PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT
                    },
                    onSaveAutofillInlineEdit = { saveAutofillInlineEdit() },
                    onSubmitTodoDraft = { submitTodoDraft() },
                    onSetPanelInputTarget = { target ->
                        if (composingBuffer.isNotEmpty()) commitComposing()
                        panelInputTarget = target
                    },
                    onClearPanelSearchQuery = { target ->
                        composingBuffer = ""
                        when (target) {
                            PanelTextInputTarget.CLIPBOARD_SEARCH -> clipboardSearchQuery = ""
                            PanelTextInputTarget.GIF_SEARCH -> gifSearchQuery = ""
                            PanelTextInputTarget.STICKER_CUSTOM_TEXT -> customStickerText = ""
                            PanelTextInputTarget.UTILITIES_TODO_INPUT -> todoDraftInput = ""
                            PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT -> autofillDraftValue = ""
                            PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT -> setPanelTargetText("")
                            PanelTextInputTarget.NONE -> {}
                        }
                    },
                    onCustomStickerTextChange = { customStickerText = it },
                    onSendMediaFile = { file, mimeType, shareOnly, fallback ->
                        sendMediaFile(file, mimeType, shareOnly, fallback)
                    },
                    voiceController = voiceInputController
                )
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composingBuffer = ""
        translatorComposingPreview = ""
        handwritingLiveText = ""
        isTypingActive = false
        isTranslatorVoiceActive = false
        lastDeletedUndoText = ""
        lastDeletedWordCount = 0
        lastGlideCommittedWord = ""
        aiSourceSnapshot = ""
        lastAiOriginalText = ""
        lastAiReplacedText = ""
        liveGrammarIssueCount = 0
        panelInputTarget = PanelTextInputTarget.NONE
        viewMode = KeyboardViewMode.LETTERS
        currentAppPackage = info?.packageName

        val inputType = info?.inputType ?: 0
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        isSensitiveLockedField = cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_URI ||
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )
        val noPersonalizedLearning = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        isAutoFieldIncognito = noPersonalizedLearning || isSensitiveLockedField

        updateAutoCapitalizeState()

        serviceScope.launch {
            launch(Dispatchers.IO) {
                runCatching { database.clipboardDao().deleteExpired(System.currentTimeMillis()) }
            }
            if (!hasInitializedLanguage) {
                val savedCode = settingsManager.activeLanguageCode.first()
                activeLanguage = LanguageId.fromCode(savedCode)
                hasInitializedLanguage = true
            }
            soundEnabled = settingsManager.soundOnKeyPress.first()
            soundVolume = settingsManager.soundVolume.first()
            soundProfile = settingsManager.soundProfile.first()
            activeFancyStyle = settingsManager.activeFancyFontStyle.first()
            customFontPathCache = settingsManager.customFontPath.first()
            customFontNameCache = settingsManager.customFontName.first()
            handwritingSignatureCache = settingsManager.handwritingSignature.first()
            otpAutoExpireMinutes = settingsManager.otpAutoExpireMinutes.first()
            perAppSmartLanguageEnabled = settingsManager.perAppSmartLanguageEnabled.first()
            perAppLanguageMap = settingsManager.perAppLanguageMap.first()

            if (isSensitiveLockedField) {
                activeLanguage = enabledLanguages.find {
                    it == LanguageId.ENGLISH || it == LanguageId.ENGLISH_UK
                } ?: LanguageId.ENGLISH
            } else if (perAppSmartLanguageEnabled && !currentAppPackage.isNullOrBlank()) {
                val smartLang = settingsManager.resolveSmartLanguageForPackage(
                    packageName = currentAppPackage,
                    savedPerAppMap = perAppLanguageMap,
                    enabledLangs = enabledLanguages,
                    currentLang = activeLanguage
                )
                if (smartLang != null && smartLang != activeLanguage) {
                    activeLanguage = smartLang
                }
            }
            updateAutoCapitalizeState()
            refreshHandwritingLiveText()
            updateSuggestions()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (activeFancyStyle.isHandwritingFont) {
            refreshHandwritingLiveText()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        currentInputConnection?.finishComposingText()
        composingBuffer = ""
        translatorComposingPreview = ""
        panelInputTarget = PanelTextInputTarget.NONE
        isTypingActive = false
        voiceInputController.stopListening()
        isTranslatorVoiceActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        pomodoroJob?.cancel()
        store.clear()
        serviceScope.cancel()
        voiceInputController.destroy()
        keySoundEngine.release()

        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}
    }

    private fun playKeyClickSound(keyType: KeyType) {
        if (!soundEnabled) return
        keySoundEngine.play(soundProfile, keyType, soundVolume)
    }

    private fun recordCommittedWordMetrics(word: String, textBeforeWord: String = "") {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val now = System.currentTimeMillis()
        val elapsed = if (lastWordCommitTimeMs > 0L) (now - lastWordCommitTimeMs) else 1200L
        lastWordCommitTimeMs = now

        val prev = textBeforeWord.trimEnd().takeLastWhile { !it.isWhitespace() }
        if (!isPrivacyPaused() && prev.isNotBlank()) {
            BigramPredictionEngine.learnPair(prev, clean)
        }
        if (!isPrivacyPaused()) {
            serviceScope.launch {
                statsManager.recordCommittedWord(clean, activeLanguage, elapsed)
            }
        }
    }

    private fun handleKeyAction(action: KeyAction) {
        playKeyClickSound(action.type)
        if (action.type != KeyType.BACKSPACE) {
            lastDeletedUndoText = ""
            lastDeletedWordCount = 0
            lastGlideCommittedWord = ""
        }

        if (panelInputTarget != PanelTextInputTarget.NONE) {
            handlePanelInputKeyAction(action)
            return
        }

        if (isTranslatorOpen) {
            handleTranslatorKeyAction(action)
            return
        }

        val ic = currentInputConnection ?: return

        when (action.type) {
            KeyType.CHARACTER -> {
                val rawChar = action.label
                isTypingActive = true

                if (activeLanguage == LanguageId.BANGLA_PHONETIC && rawChar.length == 1 && rawChar[0] in 'a'..'z' || (activeLanguage == LanguageId.BANGLA_PHONETIC && rawChar.length == 1 && rawChar[0] in 'A'..'Z')) {
                    composingBuffer += rawChar
                    val candidates = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                    val top = candidates.firstOrNull { cand -> cand.any { it in '\u0980'..'\u09FF' } }
                        ?: candidates.firstOrNull()
                        ?: composingBuffer
                    val preview = if (activeFancyStyle != FancyFontStyle.NORMAL && !activeFancyStyle.isDecoratorFrame) {
                        UnicodeTextStyler.styleText(top, activeFancyStyle)
                    } else top
                    ic.setComposingText(preview, 1)
                    suggestions = candidates
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                } else {
                    if (composingBuffer.isNotEmpty()) {
                        commitComposing()
                    }
                    val isIndicCombine = rawChar.length == 1 && isIndicSignChar(rawChar[0])
                    val isCombiningFont = activeFancyStyle == FancyFontStyle.UNDERLINE ||
                        activeFancyStyle == FancyFontStyle.DOUBLE_UNDERLINE ||
                        activeFancyStyle == FancyFontStyle.STRIKETHROUGH
                    if (isIndicCombine && isCombiningFont) {
                        val prev = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
                        if (prev.isNotEmpty() && prev.last() in setOf('\u0332', '\u0333', '\u0336')) {
                            val mark = prev.last()
                            ic.deleteSurroundingText(1, 0)
                            ic.commitText("$rawChar$mark", 1)
                        } else {
                            ic.commitText(UnicodeTextStyler.styleSingleInput(rawChar, activeFancyStyle), 1)
                        }
                    } else {
                        val charToCommit = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                            UnicodeTextStyler.styleSingleInput(rawChar, activeFancyStyle)
                        } else rawChar
                        ic.commitText(charToCommit, 1)
                    }
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                    updateAutoCapitalizeState()
                    updateSuggestions()
                }
                refreshHandwritingLiveText()
            }

            KeyType.SPACEBAR -> {
                val contextBeforeSpace = ic.getTextBeforeCursor(48, 0)?.toString() ?: ""
                if (composingBuffer.isNotEmpty()) {
                    val topCandidate = if (activeLanguage == LanguageId.BANGLA_PHONETIC) {
                        val parsed = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                        parsed.firstOrNull { cand -> cand.any { it in '\u0980'..'\u09FF' } }
                            ?: suggestions.firstOrNull { !it.startsWith("✓ ") }
                            ?: composingBuffer
                    } else {
                        suggestions.firstOrNull { !it.startsWith("✓ ") } ?: composingBuffer
                    }
                    val toCommit = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                        UnicodeTextStyler.styleText(topCandidate, activeFancyStyle)
                    } else topCandidate
                    ic.commitText(toCommit, 1)
                    lastTypedWordPreview = topCandidate
                    recordCommittedWordMetrics(topCandidate, contextBeforeSpace)
                    if (!isPrivacyPaused()) {
                        serviceScope.launch {
                            dictionaryManager.recordWordUsed(topCandidate, activeLanguage)
                        }
                    }
                    composingBuffer = ""
                } else {
                    val token = contextBeforeSpace.takeLastWhile { !it.isWhitespace() }
                    if (token.isNotEmpty()) {
                        val plainToken = UnicodeTextStyler.unstyleToPlain(token)
                        val autoFixed = if (spellCheckEnabled && token == plainToken) {
                            OfflineGrammarEngine.autoCorrectWordOnSpace(plainToken, activeLanguage)
                        } else null
                        val wordToKeep = autoFixed ?: plainToken
                        lastTypedWordPreview = wordToKeep
                        val priorContext = contextBeforeSpace.dropLast(token.length)
                        recordCommittedWordMetrics(wordToKeep, priorContext)

                        if (autoFixed != null && autoFixed != token) {
                            val styledFixed = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                                UnicodeTextStyler.styleText(autoFixed, activeFancyStyle)
                            } else autoFixed
                            ic.deleteSurroundingText(token.length, 0)
                            ic.commitText(styledFixed, 1)
                        } else if (activeFancyStyle.isDecoratorFrame) {
                            val styled = UnicodeTextStyler.styleText(token, activeFancyStyle)
                            if (styled != token) {
                                ic.deleteSurroundingText(token.length, 0)
                                ic.commitText(styled, 1)
                            }
                        }
                    }
                }
                ic.commitText(" ", 1)
                isTypingActive = false
                updateAutoCapitalizeState()
                updateSuggestions()
                refreshHandwritingLiveText()
            }

            KeyType.BACKSPACE -> {
                if (composingBuffer.isNotEmpty()) {
                    composingBuffer = composingBuffer.dropLast(1)
                    if (composingBuffer.isEmpty()) {
                        ic.commitText("", 1)
                        isTypingActive = false
                        suggestions = dictionaryManager.getDefaultSuggestions(activeLanguage)
                    } else {
                        val candidates = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                        val top = candidates.firstOrNull() ?: composingBuffer
                        val preview = if (activeFancyStyle != FancyFontStyle.NORMAL && !activeFancyStyle.isDecoratorFrame) {
                            UnicodeTextStyler.styleText(top, activeFancyStyle)
                        } else top
                        ic.setComposingText(preview, 1)
                        suggestions = candidates
                    }
                } else {
                    val selectedText = ic.getSelectedText(0)
                    if (!selectedText.isNullOrEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        deleteSingleLogicalGlyph(ic)
                    }
                    updateAutoCapitalizeState()
                    updateSuggestions()
                }
                refreshHandwritingLiveText()
            }

            KeyType.ENTER -> {
                if (composingBuffer.isNotEmpty()) {
                    commitComposing()
                }
                isTypingActive = false
                val editorInfo = currentInputEditorInfo
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION

                if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    sendKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
                updateAutoCapitalizeState()
                refreshHandwritingLiveText()
            }

            KeyType.SHIFT -> {
                if (isShifted) {
                    if (isCapsLock) {
                        isCapsLock = false
                        isShifted = false
                    } else {
                        isCapsLock = true
                    }
                } else {
                    isShifted = true
                }
            }

            KeyType.MODE_SWITCH -> {
                if (composingBuffer.isNotEmpty()) commitComposing()
                viewMode = when (viewMode) {
                    KeyboardViewMode.LETTERS -> KeyboardViewMode.SYMBOLS_1
                    KeyboardViewMode.SYMBOLS_1 -> {
                        if (action.label == "1/2") KeyboardViewMode.SYMBOLS_2 else KeyboardViewMode.LETTERS
                    }
                    KeyboardViewMode.SYMBOLS_2 -> {
                        if (action.label == "2/2") KeyboardViewMode.SYMBOLS_1 else KeyboardViewMode.LETTERS
                    }
                    else -> KeyboardViewMode.LETTERS
                }
            }

            KeyType.EMOJI_PICKER -> {
                if (composingBuffer.isNotEmpty()) commitComposing()
                viewMode = KeyboardViewMode.EMOJI
            }

            KeyType.CLIPBOARD -> {
                if (composingBuffer.isNotEmpty()) commitComposing()
                viewMode = KeyboardViewMode.CLIPBOARD
            }

            KeyType.LANGUAGE_SWITCH -> cycleLanguage(forward = true)
            else -> {}
        }
    }

    private fun handleTranslatorKeyAction(action: KeyAction) {
        when (action.type) {
            KeyType.CHARACTER -> {
                val char = action.label
                isTypingActive = true
                if (activeLanguage == LanguageId.BANGLA_PHONETIC && char.matches(Regex("[a-zA-Z]"))) {
                    composingBuffer += char
                    val candidates = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                    val top = candidates.firstOrNull { c -> c.any { it in '\u0980'..'\u09FF' } }
                        ?: candidates.firstOrNull()
                        ?: composingBuffer
                    translatorComposingPreview = if (activeFancyStyle != FancyFontStyle.NORMAL && !activeFancyStyle.isDecoratorFrame) {
                        UnicodeTextStyler.styleText(top, activeFancyStyle)
                    } else top
                    suggestions = candidates
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                } else {
                    if (composingBuffer.isNotEmpty()) {
                        val committed = suggestions.firstOrNull() ?: composingBuffer
                        val styledCommitted = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                            UnicodeTextStyler.styleText(committed, activeFancyStyle)
                        } else committed
                        translatorSourceText += styledCommitted
                        composingBuffer = ""
                        translatorComposingPreview = ""
                    }
                    val charToAdd = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                        UnicodeTextStyler.styleSingleInput(char, activeFancyStyle)
                    } else char
                    translatorSourceText += charToAdd
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                    updateTranslatorSuggestions()
                }
            }

            KeyType.SPACEBAR -> {
                if (composingBuffer.isNotEmpty()) {
                    val committed = suggestions.firstOrNull() ?: composingBuffer
                    val styledCommitted = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                        UnicodeTextStyler.styleText(committed, activeFancyStyle)
                    } else committed
                    translatorSourceText += styledCommitted
                    serviceScope.launch {
                        dictionaryManager.recordWordUsed(committed, activeLanguage)
                    }
                    composingBuffer = ""
                    translatorComposingPreview = ""
                }
                translatorSourceText += " "
                isTypingActive = false
                updateTranslatorSuggestions()
            }

            KeyType.BACKSPACE -> {
                if (composingBuffer.isNotEmpty()) {
                    composingBuffer = composingBuffer.dropLast(1)
                    if (composingBuffer.isEmpty()) {
                        translatorComposingPreview = ""
                        isTypingActive = false
                        updateTranslatorSuggestions()
                    } else {
                        val candidates = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                        val top = candidates.firstOrNull() ?: composingBuffer
                        translatorComposingPreview = if (activeFancyStyle != FancyFontStyle.NORMAL && !activeFancyStyle.isDecoratorFrame) {
                            UnicodeTextStyler.styleText(top, activeFancyStyle)
                        } else top
                        suggestions = candidates
                    }
                } else if (translatorSourceText.isNotEmpty()) {
                    translatorSourceText = translatorSourceText.dropLast(1)
                    updateTranslatorSuggestions()
                }
            }

            KeyType.ENTER -> {
                if (composingBuffer.isNotEmpty()) {
                    val committed = suggestions.firstOrNull() ?: composingBuffer
                    val styledCommitted = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                        UnicodeTextStyler.styleText(committed, activeFancyStyle)
                    } else committed
                    translatorSourceText += styledCommitted
                    composingBuffer = ""
                    translatorComposingPreview = ""
                }
                translatorSourceText += "\n"
                isTypingActive = false
                updateTranslatorSuggestions()
            }

            KeyType.SHIFT -> {
                if (isShifted) {
                    if (isCapsLock) {
                        isCapsLock = false
                        isShifted = false
                    } else {
                        isCapsLock = true
                    }
                } else {
                    isShifted = true
                }
            }

            KeyType.MODE_SWITCH -> {
                if (composingBuffer.isNotEmpty()) {
                    val committed = suggestions.firstOrNull() ?: composingBuffer
                    translatorSourceText += if (activeFancyStyle != FancyFontStyle.NORMAL) {
                        UnicodeTextStyler.styleText(committed, activeFancyStyle)
                    } else committed
                    composingBuffer = ""
                    translatorComposingPreview = ""
                }
                viewMode = when (viewMode) {
                    KeyboardViewMode.LETTERS -> KeyboardViewMode.SYMBOLS_1
                    KeyboardViewMode.SYMBOLS_1 -> if (action.label == "1/2") KeyboardViewMode.SYMBOLS_2 else KeyboardViewMode.LETTERS
                    KeyboardViewMode.SYMBOLS_2 -> if (action.label == "2/2") KeyboardViewMode.SYMBOLS_1 else KeyboardViewMode.LETTERS
                    else -> KeyboardViewMode.LETTERS
                }
            }

            KeyType.EMOJI_PICKER -> viewMode = KeyboardViewMode.EMOJI
            KeyType.CLIPBOARD -> viewMode = KeyboardViewMode.CLIPBOARD
            KeyType.LANGUAGE_SWITCH -> cycleLanguage(forward = true)
            else -> {}
        }
    }

    private fun updateTranslatorSuggestions() {
        serviceScope.launch {
            val lastWord = if (translatorSourceText.isNotEmpty() && !translatorSourceText.last().isWhitespace()) {
                translatorSourceText.takeLastWhile { !it.isWhitespace() }
            } else {
                ""
            }
            val math = OfflineMathEvaluator.evaluateIfTriggered(lastWord)
            val shortcutMatches = if (lastWord.isNotEmpty()) {
                shortcutRepository.findMatchingExpansions(lastWord)
            } else emptyList()
            val grammarFixes = if (spellCheckEnabled && translatorSourceText.isNotBlank()) {
                OfflineGrammarEngine.analyzeContext(translatorSourceText.takeLast(80), activeLanguage)
            } else emptyList()
            activeGrammarSuggestions = grammarFixes.associateBy { it.displayLabel }

            val dictPreds = dictionaryManager.getPredictions(
                input = lastWord,
                languageId = activeLanguage,
                spellCheckEnabled = spellCheckEnabled
            )
            suggestions = buildList {
                math?.let {
                    add(it.resultString)
                    add(it.equationString)
                }
                addAll(grammarFixes.map { it.displayLabel })
                addAll(shortcutMatches)
                addAll(dictPreds)
            }.distinct().take(6)
        }
    }

    private fun openTranslator() {
        if (composingBuffer.isNotEmpty()) commitComposing()
        val selected = currentInputConnection?.getSelectedText(0)?.toString()
        if (!selected.isNullOrBlank() && translatorSourceText.isBlank()) {
            translatorSourceText = selected
        }
        isTranslatorOpen = true
        viewMode = KeyboardViewMode.LETTERS
    }

    private fun closeTranslator() {
        stopTranslatorVoice()
        composingBuffer = ""
        translatorComposingPreview = ""
        isTypingActive = false
        isTranslatorOpen = false
        updateSuggestions()
    }

    private fun pasteIntoTranslator() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
            if (!text.isNullOrBlank()) {
                translatorSourceText = if (translatorSourceText.isBlank()) text else "$translatorSourceText $text"
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {}
    }

    private fun copyToSystemClipboard(text: String) {
        if (text.isBlank()) return
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("TypeGenius Translation", text))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    private fun shareTranslatedText(text: String) {
        if (text.isBlank()) return
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Translation").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(chooser)
        } catch (_: Exception) {}
    }

    private fun insertTranslatedText(text: String) {
        if (text.isBlank()) return
        currentInputConnection?.commitText(text, 1)
        composingBuffer = ""
        translatorComposingPreview = ""
        translatorSourceText = ""
        isTypingActive = false
    }

    private fun performSmartCopy() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (isTranslatorOpen) {
                if (translatorSourceText.isNotBlank()) {
                    cm?.setPrimaryClip(ClipData.newPlainText("TypeGenius Copy", translatorSourceText))
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
                }
                return
            }
            val ic = currentInputConnection ?: return
            if (composingBuffer.isNotEmpty()) commitComposing()
            val selected = ic.getSelectedText(0)?.toString()
            if (!selected.isNullOrEmpty()) {
                cm?.setPrimaryClip(ClipData.newPlainText("TypeGenius Copy", selected))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            } else {
                val handled = ic.performContextMenuAction(android.R.id.copy)
                Toast.makeText(
                    this,
                    if (handled) "Copied" else "Select text first to copy",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {}
    }

    private fun performSmartCut() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (isTranslatorOpen) {
                if (translatorSourceText.isNotBlank()) {
                    cm?.setPrimaryClip(ClipData.newPlainText("TypeGenius Cut", translatorSourceText))
                    translatorSourceText = ""
                    composingBuffer = ""
                    translatorComposingPreview = ""
                    isTypingActive = false
                    updateTranslatorSuggestions()
                    Toast.makeText(this, "Cut", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Nothing to cut", Toast.LENGTH_SHORT).show()
                }
                return
            }
            val ic = currentInputConnection ?: return
            if (composingBuffer.isNotEmpty()) commitComposing()
            val selected = ic.getSelectedText(0)?.toString()
            if (!selected.isNullOrEmpty()) {
                cm?.setPrimaryClip(ClipData.newPlainText("TypeGenius Cut", selected))
                ic.commitText("", 1)
                updateAutoCapitalizeState()
                updateSuggestions()
                Toast.makeText(this, "Cut", Toast.LENGTH_SHORT).show()
            } else {
                val handled = ic.performContextMenuAction(android.R.id.cut)
                updateAutoCapitalizeState()
                updateSuggestions()
                Toast.makeText(
                    this,
                    if (handled) "Cut" else "Select text first to cut",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {}
    }

    private fun performSmartPaste() {
        try {
            if (isTranslatorOpen) {
                pasteIntoTranslator()
                return
            }
            val ic = currentInputConnection ?: return
            if (composingBuffer.isNotEmpty()) commitComposing()
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            val clipText = if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(this)?.toString()
            } else null

            if (!clipText.isNullOrEmpty()) {
                val styled = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                    UnicodeTextStyler.styleText(clipText, activeFancyStyle)
                } else clipText
                ic.commitText(styled, 1)
                updateAutoCapitalizeState()
                updateSuggestions()
            } else {
                val handled = ic.performContextMenuAction(android.R.id.paste)
                if (!handled) {
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {}
    }

    private fun toggleTranslatorVoice(sourceLangCode: String) {
        if (isTranslatorVoiceActive) {
            stopTranslatorVoice()
        } else {
            isTranslatorVoiceActive = true
            voiceInputController.startListening(
                languageCode = sourceLangCode,
                onPermissionRequired = {
                    isTranslatorVoiceActive = false
                    openCompanionSettings()
                }
            ) { spoken ->
                val clean = spoken.trim()
                if (clean.isNotEmpty()) {
                    translatorSourceText = if (translatorSourceText.isBlank()) {
                        clean
                    } else {
                        "${translatorSourceText.trimEnd()} $clean"
                    }
                }
                if (!voiceInputController.continuousMode) {
                    isTranslatorVoiceActive = false
                }
            }
        }
    }

    private fun stopTranslatorVoice() {
        voiceInputController.stopListening()
        isTranslatorVoiceActive = false
    }

    private fun updateAutoCapitalizeState() {
        if (!autoCapitalizeEnabled || isCapsLock) return
        val isLatinLanguage = activeLanguage in listOf(
            LanguageId.ENGLISH, LanguageId.ENGLISH_UK, LanguageId.SPANISH,
            LanguageId.FRENCH, LanguageId.GERMAN, LanguageId.PORTUGUESE,
            LanguageId.ITALIAN, LanguageId.TURKISH, LanguageId.INDONESIAN, LanguageId.MALAY
        )
        if (!isLatinLanguage) return

        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(4, 0)?.toString() ?: ""
        val shouldShift = before.isEmpty() || before.endsWith(". ") || before.endsWith("! ") || before.endsWith("? ") || before.endsWith("\n")
        isShifted = shouldShift
    }

    private fun moveCursor(dx: Int, dy: Int, select: Boolean) {
        val keyCode = when {
            dx < 0 -> KeyEvent.KEYCODE_DPAD_LEFT
            dx > 0 -> KeyEvent.KEYCODE_DPAD_RIGHT
            dy < 0 -> KeyEvent.KEYCODE_DPAD_UP
            dy > 0 -> KeyEvent.KEYCODE_DPAD_DOWN
            else -> return
        }
        sendEditorKey(keyCode, select)
    }

    private fun sendEditorKey(keyCode: Int, select: Boolean) {
        val ic = currentInputConnection ?: return
        val metaState = if (select) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState))
    }

    private fun commitComposing() {
        if (isTranslatorOpen) {
            if (composingBuffer.isNotEmpty()) {
                val top = suggestions.firstOrNull() ?: composingBuffer
                val styledTop = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                    UnicodeTextStyler.styleText(top, activeFancyStyle)
                } else top
                translatorSourceText += styledTop
                if (!isPrivacyPaused()) {
                    serviceScope.launch {
                        dictionaryManager.recordWordUsed(top, activeLanguage)
                    }
                }
                composingBuffer = ""
                translatorComposingPreview = ""
                isTypingActive = false
            }
            refreshHandwritingLiveText()
            return
        }
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val topCandidate = suggestions.firstOrNull() ?: composingBuffer
            val toCommit = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                UnicodeTextStyler.styleText(topCandidate, activeFancyStyle)
            } else topCandidate
            ic.commitText(toCommit, 1)
            lastTypedWordPreview = topCandidate
            val priorText = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
            recordCommittedWordMetrics(topCandidate, priorText.dropLast(toCommit.length.coerceAtMost(priorText.length)))
            if (!isPrivacyPaused()) {
                serviceScope.launch {
                    dictionaryManager.recordWordUsed(topCandidate, activeLanguage)
                }
            }
            composingBuffer = ""
            isTypingActive = false
            suggestions = dictionaryManager.getDefaultSuggestions(activeLanguage)
            refreshHandwritingLiveText()
        }
    }

    private fun handleSuggestionCommit(candidate: String) {
        lastDeletedUndoText = ""
        lastDeletedWordCount = 0
        val grammarFix = activeGrammarSuggestions[candidate]
        val commitValue = grammarFix?.correctedPhrase ?: candidate

        if (panelInputTarget != PanelTextInputTarget.NONE) {
            val current = getPanelTargetText()
            val updated = if (composingBuffer.isNotEmpty()) {
                val parsed = BengaliPhoneticParser.parsePhonetic(composingBuffer).firstOrNull() ?: composingBuffer
                val trimmedBase = if (current.endsWith(parsed)) current.dropLast(parsed.length) else current
                composingBuffer = ""
                "$trimmedBase$commitValue "
            } else {
                val token = current.takeLastWhile { !it.isWhitespace() }
                val base = if (token.isNotEmpty()) current.dropLast(token.length) else current
                "$base$commitValue "
            }
            setPanelTargetText(updated)
            isTypingActive = false
            suggestions = dictionaryManager.getDefaultSuggestions(activeLanguage)
            return
        }

        if (isTranslatorOpen) {
            val styledCommit = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                UnicodeTextStyler.styleText(commitValue, activeFancyStyle)
            } else commitValue
            if (grammarFix != null && composingBuffer.isEmpty() && translatorSourceText.length >= grammarFix.replaceCharLength) {
                val base = translatorSourceText.dropLast(grammarFix.replaceCharLength)
                val suffixSpace = if (grammarFix.trailingSpace) " " else " "
                translatorSourceText = "$base$styledCommit$suffixSpace"
            } else {
                translatorSourceText = if (composingBuffer.isNotEmpty()) {
                    "$translatorSourceText$styledCommit "
                } else if (lastGlideCommittedWord.isNotEmpty() && translatorSourceText.endsWith("$lastGlideCommittedWord ")) {
                    translatorSourceText.dropLast(lastGlideCommittedWord.length + 1) + "$styledCommit "
                } else {
                    val currentToken = translatorSourceText.takeLastWhile { !it.isWhitespace() }
                    val base = if (currentToken.isNotEmpty()) {
                        translatorSourceText.dropLast(currentToken.length)
                    } else {
                        translatorSourceText
                    }
                    "$base$styledCommit "
                }
            }
            composingBuffer = ""
            translatorComposingPreview = ""
            lastGlideCommittedWord = ""
            isTypingActive = false
            if (!isPrivacyPaused()) {
                serviceScope.launch {
                    dictionaryManager.recordWordUsed(commitValue, activeLanguage)
                }
            }
            updateTranslatorSuggestions()
            return
        }
        val ic = currentInputConnection ?: return
        if (com.example.data.dictionary.EmojiPredictionEngine.isEmojiSuggestion(candidate)) {
            ic.commitText("$candidate ", 1)
            suggestions = dictionaryManager.getDefaultSuggestions(activeLanguage)
            return
        }
        val styledCandidate = if (activeFancyStyle != FancyFontStyle.NORMAL) {
            UnicodeTextStyler.styleText(commitValue, activeFancyStyle)
        } else {
            commitValue
        }

        if (grammarFix != null && composingBuffer.isEmpty()) {
            ic.deleteSurroundingText(grammarFix.replaceCharLength, 0)
            ic.commitText("$styledCandidate ", 1)
        } else if (composingBuffer.isNotEmpty()) {
            ic.commitText("$styledCandidate ", 1)
            composingBuffer = ""
        } else if (lastGlideCommittedWord.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(lastGlideCommittedWord.length + 2, 0)?.toString() ?: ""
            if (before.endsWith("$lastGlideCommittedWord ")) {
                ic.deleteSurroundingText(lastGlideCommittedWord.length + 1, 0)
            }
            ic.commitText("$styledCandidate ", 1)
        } else {
            val before = ic.getTextBeforeCursor(48, 0)?.toString() ?: ""
            val currentToken = before.takeLastWhile { !it.isWhitespace() }
            if (currentToken.isNotEmpty()) {
                ic.deleteSurroundingText(currentToken.length, 0)
            }
            ic.commitText("$styledCandidate ", 1)
        }
        lastTypedWordPreview = commitValue
        lastGlideCommittedWord = ""
        isTypingActive = false
        val contextForBigram = ic.getTextBeforeCursor(48, 0)?.toString() ?: ""
        recordCommittedWordMetrics(commitValue, contextForBigram.dropLast((styledCandidate.length + 1).coerceAtMost(contextForBigram.length)))
        if (!isPrivacyPaused()) {
            serviceScope.launch {
                dictionaryManager.recordWordUsed(commitValue, activeLanguage)
            }
        }
        updateAutoCapitalizeState()
        refreshHandwritingLiveText()
        updateSuggestions()
    }

    private fun deleteWordBackward() {
        if (isTranslatorOpen) {
            if (composingBuffer.isNotEmpty()) {
                val preview = translatorComposingPreview.ifEmpty { composingBuffer }
                lastDeletedUndoText = preview + lastDeletedUndoText
                lastDeletedWordCount++
                composingBuffer = ""
                translatorComposingPreview = ""
                isTypingActive = false
                updateTranslatorSuggestions()
                return
            }
            if (translatorSourceText.isEmpty()) return
            val trimmedEnd = translatorSourceText.trimEnd()
            val cutStart = trimmedEnd.lastIndexOfAny(charArrayOf(' ', '\n', '\t')) + 1
            val deleted = translatorSourceText.substring(cutStart)
            translatorSourceText = translatorSourceText.substring(0, cutStart)
            lastDeletedUndoText = deleted + lastDeletedUndoText
            if (deleted.isNotBlank()) lastDeletedWordCount++
            updateTranslatorSuggestions()
            return
        }

        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val top = suggestions.firstOrNull() ?: composingBuffer
            ic.commitText("", 1)
            lastDeletedUndoText = top + lastDeletedUndoText
            lastDeletedWordCount++
            composingBuffer = ""
            isTypingActive = false
            refreshHandwritingLiveText()
            updateSuggestions()
            return
        }

        val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: return
        if (before.isEmpty()) return
        val trimmedEnd = before.trimEnd()
        val cutIndexInBefore = if (trimmedEnd.isEmpty()) 0 else {
            val ws = trimmedEnd.lastIndexOfAny(charArrayOf(' ', '\n', '\t'))
            if (ws >= 0) ws + 1 else 0
        }
        val deletedChunk = before.substring(cutIndexInBefore)
        if (deletedChunk.isNotEmpty()) {
            ic.deleteSurroundingText(deletedChunk.length, 0)
            lastDeletedUndoText = deletedChunk + lastDeletedUndoText
            if (deletedChunk.isNotBlank()) lastDeletedWordCount++
        }
        updateAutoCapitalizeState()
        refreshHandwritingLiveText()
        updateSuggestions()
    }

    private fun undoLastDeletedWord() {
        val toRestore = lastDeletedUndoText
        if (toRestore.isEmpty()) return
        lastDeletedUndoText = ""
        lastDeletedWordCount = 0
        if (isTranslatorOpen) {
            translatorSourceText += toRestore
            updateTranslatorSuggestions()
        } else {
            currentInputConnection?.commitText(toRestore, 1)
            updateAutoCapitalizeState()
            refreshHandwritingLiveText()
            updateSuggestions()
        }
    }

    private fun handleGlideSwipe(
        trail: List<Pair<Float, Float>>,
        keyCenters: Map<String, Pair<Float, Float>>
    ) {
        serviceScope.launch {
            val vocab = dictionaryManager.getVocabulary(activeLanguage)
            val candidates = OfflineGlideEngine.resolveGlideCandidates(
                trail = trail,
                keyCenters = keyCenters,
                vocabulary = vocab,
                languageId = activeLanguage,
                limit = 5
            )
            if (candidates.isEmpty()) return@launch

            val rawTop = candidates.first()
            val topStyled = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                UnicodeTextStyler.styleText(rawTop, activeFancyStyle)
            } else rawTop

            lastDeletedUndoText = ""
            lastDeletedWordCount = 0
            lastGlideCommittedWord = topStyled
            lastTypedWordPreview = rawTop

            if (isTranslatorOpen) {
                composingBuffer = ""
                translatorComposingPreview = ""
                translatorSourceText += "$topStyled "
                suggestions = candidates
            } else {
                val ic = currentInputConnection ?: return@launch
                if (composingBuffer.isNotEmpty()) {
                    ic.finishComposingText()
                    composingBuffer = ""
                }
                val priorText = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
                ic.commitText("$topStyled ", 1)
                recordCommittedWordMetrics(rawTop, priorText)
                suggestions = candidates
                updateAutoCapitalizeState()
                refreshHandwritingLiveText()
            }

            if (!isPrivacyPaused()) {
                dictionaryManager.recordWordUsed(rawTop, activeLanguage)
            }
        }
    }

    private fun deleteSingleLogicalGlyph(ic: android.view.inputmethod.InputConnection) {
        val before = ic.getTextBeforeCursor(4, 0)?.toString() ?: ""
        if (before.isEmpty()) {
            ic.deleteSurroundingText(1, 0)
            return
        }
        var deleteCount = 0
        var idx = before.length - 1
        // Consume any trailing combining diacritic marks (underline, strikethrough, etc.)
        while (idx >= 0 && before[idx] in setOf('\u0332', '\u0333', '\u0336', '\u0338')) {
            deleteCount++
            idx--
        }
        if (idx >= 1 && Character.isSurrogatePair(before[idx - 1], before[idx])) {
            deleteCount += 2
        } else if (idx >= 0) {
            deleteCount += 1
        }
        ic.deleteSurroundingText(deleteCount.coerceAtLeast(1), 0)
    }

    private fun isIndicSignChar(c: Char): Boolean {
        val code = c.code
        return (code in 0x0981..0x0983) ||
            code == 0x09BC ||
            (code in 0x09BE..0x09CC) ||
            code == 0x09CD ||
            code == 0x09D7 ||
            (code in 0x09E2..0x09E3)
    }

    private fun transformLastWordWithStyle(style: FancyFontStyle) {
        if (isTranslatorOpen) {
            val trimmed = translatorSourceText.trimEnd()
            val lastWord = trimmed.takeLastWhile { !it.isWhitespace() }
            if (lastWord.isNotEmpty()) {
                val base = trimmed.dropLast(lastWord.length)
                translatorSourceText = "$base${UnicodeTextStyler.styleText(lastWord, style)} "
            } else {
                translatorSourceText += UnicodeTextStyler.styleText("TypeGenius", style)
            }
            return
        }

        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) commitComposing()

        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) {
            ic.commitText(UnicodeTextStyler.styleText(selected, style), 1)
            lastTypedWordPreview = UnicodeTextStyler.unstyleToPlain(selected)
            viewMode = KeyboardViewMode.LETTERS
            refreshHandwritingLiveText()
            return
        }

        val before = ic.getTextBeforeCursor(80, 0)?.toString() ?: ""
        val trimmed = before.trimEnd()
        val trailingSpaces = before.length - trimmed.length
        val lastWord = trimmed.takeLastWhile { !it.isWhitespace() }
        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length + trailingSpaces, 0)
            ic.commitText(UnicodeTextStyler.styleText(lastWord, style) + " ", 1)
            lastTypedWordPreview = UnicodeTextStyler.unstyleToPlain(lastWord)
        } else {
            ic.commitText(UnicodeTextStyler.styleText("TypeGenius", style), 1)
        }
        refreshHandwritingLiveText()
        viewMode = KeyboardViewMode.LETTERS
    }

    private fun cycleLanguage(forward: Boolean) {
        if (enabledLanguages.size <= 1) return

        if (composingBuffer.isNotEmpty()) {
            commitComposing()
        }

        val currentIndex = enabledLanguages.indexOf(activeLanguage).coerceAtLeast(0)
        val nextIndex = if (forward) {
            (currentIndex + 1) % enabledLanguages.size
        } else {
            (currentIndex - 1 + enabledLanguages.size) % enabledLanguages.size
        }

        selectLanguage(enabledLanguages[nextIndex])
    }

    private fun selectLanguage(language: LanguageId) {
        activeLanguage = language
        if (panelInputTarget == PanelTextInputTarget.NONE) {
            viewMode = KeyboardViewMode.LETTERS
        }
        composingBuffer = ""
        isTypingActive = false
        isShifted = false
        isCapsLock = false
        lastGlideCommittedWord = ""

        serviceScope.launch {
            settingsManager.setActiveLanguageCode(language.code)
            val pkg = currentAppPackage
            if (perAppSmartLanguageEnabled && !isSensitiveLockedField && !pkg.isNullOrBlank()) {
                settingsManager.saveLanguageForPackage(pkg, language.code)
            }
            updateSuggestions()
        }
    }

    private fun readFieldTextForAi(): String {
        if (isTranslatorOpen) {
            val full = translatorSourceText
            aiSourceSnapshot = full
            return full
        }
        val ic = currentInputConnection ?: return aiSourceSnapshot
        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrBlank()) {
            aiCapturedFromSelection = true
            aiCapturedBefore = ""
            aiCapturedAfter = ""
            aiSourceSnapshot = selected
            return selected
        }
        aiCapturedFromSelection = false
        val before = ic.getTextBeforeCursor(800, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(200, 0)?.toString().orEmpty()
        aiCapturedBefore = before
        aiCapturedAfter = after
        val combined = before + after
        aiSourceSnapshot = combined
        return combined
    }

    private fun applyAiReplacement(newText: String) {
        if (newText.isBlank()) return
        if (isTranslatorOpen) {
            lastAiOriginalText = translatorSourceText
            lastAiReplacedText = newText
            translatorSourceText = newText
            aiSourceSnapshot = newText
            updateTranslatorSuggestions()
            return
        }
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) commitComposing()

        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrBlank()) {
            lastAiOriginalText = selected
            lastAiReplacedText = newText
            ic.commitText(newText, 1)
        } else {
            val curBefore = ic.getTextBeforeCursor(800, 0)?.toString().orEmpty()
            val curAfter = ic.getTextAfterCursor(200, 0)?.toString().orEmpty()
            lastAiOriginalText = if (aiSourceSnapshot.isNotBlank()) aiSourceSnapshot else (curBefore + curAfter)
            lastAiReplacedText = newText
            val delBefore = if (!aiCapturedFromSelection && curBefore.endsWith(aiCapturedBefore)) aiCapturedBefore.length else curBefore.length
            val delAfter = if (!aiCapturedFromSelection && curAfter.startsWith(aiCapturedAfter)) aiCapturedAfter.length else curAfter.length
            ic.deleteSurroundingText(delBefore, delAfter)
            ic.commitText(newText, 1)
        }
        aiCapturedFromSelection = false
        aiCapturedBefore = newText
        aiCapturedAfter = ""
        aiSourceSnapshot = newText
        updateAutoCapitalizeState()
        updateSuggestions()
    }

    private fun undoAiReplacement() {
        val original = lastAiOriginalText
        val replaced = lastAiReplacedText
        if (original.isEmpty() || replaced.isEmpty()) return

        if (isTranslatorOpen) {
            translatorSourceText = original
            aiSourceSnapshot = original
            lastAiOriginalText = ""
            lastAiReplacedText = ""
            updateTranslatorSuggestions()
            return
        }
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(replaced.length + 20, 0)?.toString().orEmpty()
        if (before.endsWith(replaced)) {
            ic.deleteSurroundingText(replaced.length, 0)
            ic.commitText(original, 1)
        } else {
            val selected = ic.getSelectedText(0)?.toString()
            if (selected == replaced) {
                ic.commitText(original, 1)
            }
        }
        aiCapturedBefore = original
        aiCapturedAfter = ""
        aiSourceSnapshot = original
        lastAiOriginalText = ""
        lastAiReplacedText = ""
        updateAutoCapitalizeState()
        updateSuggestions()
    }

    private fun updateSuggestions() {
        serviceScope.launch {
            if (composingBuffer.isNotEmpty()) {
                val snapshotBuffer = composingBuffer
                activeGrammarSuggestions = emptyMap()
                liveGrammarIssueCount = 0
                val phonetic = BengaliPhoneticParser.parsePhonetic(snapshotBuffer)
                val shortcutMatches = shortcutRepository.findMatchingExpansions(snapshotBuffer)
                if (composingBuffer == snapshotBuffer) {
                    suggestions = (phonetic + shortcutMatches)
                        .distinct()
                        .take(6)
                }
            } else {
                val ic = currentInputConnection
                val textBefore = ic?.getTextBeforeCursor(80, 0)?.toString() ?: ""
                val lastWord = if (textBefore.isNotEmpty() && !textBefore.last().isWhitespace()) {
                    textBefore.takeLastWhile { !it.isWhitespace() }
                } else {
                    ""
                }
                if (lastWord.isNotEmpty()) {
                    lastTypedWordPreview = UnicodeTextStyler.unstyleToPlain(lastWord)
                }
                val math = OfflineMathEvaluator.evaluateIfTriggered(lastWord)
                val unitConversion = if (math == null && lastWord.isNotEmpty()) {
                    ProductivityTextTools.evaluateInlineUnitOrCurrency(lastWord)
                } else null
                val shortcutMatches = if (lastWord.isNotEmpty()) {
                    shortcutRepository.findMatchingExpansions(lastWord)
                } else emptyList()
                val islamicMatches = if (lastWord.isNotEmpty()) {
                    com.example.data.IslamicToolkit.matchQuickIslamicSuggestions(UnicodeTextStyler.unstyleToPlain(lastWord))
                } else emptyList()

                val plainContext = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                    UnicodeTextStyler.unstyleToPlain(textBefore)
                } else textBefore
                val grammarFixes = if (spellCheckEnabled && plainContext.isNotBlank()) {
                    OfflineGrammarEngine.analyzeContext(plainContext, activeLanguage)
                } else emptyList()
                activeGrammarSuggestions = grammarFixes.associateBy { it.displayLabel }
                liveGrammarIssueCount = if (spellCheckEnabled && plainContext.isNotBlank()) {
                    TypeGeniusAiEngine.collectLocalCorrections(plainContext, activeLanguage).size
                } else 0

                val prevWord = if (lastWord.isEmpty()) {
                    BigramPredictionEngine.extractPreviousWordBeforeCursor(plainContext)
                } else ""
                val bigramPreds = if (prevWord.isNotEmpty()) {
                    BigramPredictionEngine.predictNextWords(prevWord, activeLanguage, limit = 4)
                } else emptyList()

                val dictPreds = dictionaryManager.getPredictions(
                    input = UnicodeTextStyler.unstyleToPlain(lastWord),
                    languageId = activeLanguage,
                    spellCheckEnabled = spellCheckEnabled
                )
                val predictedEmojis = if (lastWord.isNotEmpty()) {
                    com.example.data.dictionary.EmojiPredictionEngine.predictEmojis(UnicodeTextStyler.unstyleToPlain(lastWord)).take(2)
                } else emptyList()
                suggestions = buildList {
                    math?.let {
                        add(it.resultString)
                        add(it.equationString)
                    }
                    unitConversion?.let {
                        add(it.resultString)
                        add(it.detailLabel)
                    }
                    addAll(grammarFixes.map { it.displayLabel })
                    addAll(bigramPreds)
                    addAll(predictedEmojis)
                    addAll(islamicMatches)
                    addAll(shortcutMatches)
                    addAll(dictPreds)
                }.distinct().take(6)
            }
        }
    }

    private fun sendKeyEvents(keyCode: Int) {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun openCompanionSettings(openFontManager: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            if (openFontManager) {
                putExtra("open_font_manager", true)
            }
        }
        startActivity(intent)
    }

    private fun shareTypeGenius() {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out TypeGenius Keyboard — 100% offline & backendless keyboard with Bengali Probhat, UniBijoy, Avro, Sylheti, ${LanguageId.entries.size} languages, and Material You themes!"
                )
            }
            val chooser = Intent.createChooser(sendIntent, "Share TypeGenius Keyboard").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(chooser)
        } catch (_: Exception) {}
    }

    private fun getPanelTargetText(): String = when (panelInputTarget) {
        PanelTextInputTarget.CLIPBOARD_SEARCH -> clipboardSearchQuery
        PanelTextInputTarget.GIF_SEARCH -> gifSearchQuery
        PanelTextInputTarget.STICKER_CUSTOM_TEXT -> customStickerText
        PanelTextInputTarget.UTILITIES_TODO_INPUT -> todoDraftInput
        PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT -> autofillDraftValue
        PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT -> notepadLiveDraft
        PanelTextInputTarget.NONE -> ""
    }

    private fun setPanelTargetText(value: String) {
        val cap = when (panelInputTarget) {
            PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT -> 1000
            PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT -> 220
            PanelTextInputTarget.UTILITIES_TODO_INPUT -> 140
            else -> 64
        }
        val capped = value.take(cap)
        when (panelInputTarget) {
            PanelTextInputTarget.CLIPBOARD_SEARCH -> clipboardSearchQuery = capped
            PanelTextInputTarget.GIF_SEARCH -> gifSearchQuery = capped
            PanelTextInputTarget.STICKER_CUSTOM_TEXT -> customStickerText = capped
            PanelTextInputTarget.UTILITIES_TODO_INPUT -> todoDraftInput = capped
            PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT -> autofillDraftValue = capped
            PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT -> {
                notepadLiveDraft = capped
                serviceScope.launch { settingsManager.setNotepadDraft(capped) }
            }
            PanelTextInputTarget.NONE -> {}
        }
    }

    private fun submitTodoDraft() {
        val clean = todoDraftInput.trim()
        if (clean.isNotEmpty()) {
            serviceScope.launch { settingsManager.addQuickTodo(clean) }
            todoDraftInput = ""
        }
        composingBuffer = ""
        panelInputTarget = PanelTextInputTarget.NONE
    }

    private fun saveAutofillInlineEdit() {
        val k = autofillEditingKey
        val v = autofillDraftValue.trim()
        if (k.isNotBlank()) {
            serviceScope.launch { settingsManager.updateAutofillField(k, v) }
        }
        composingBuffer = ""
        panelInputTarget = PanelTextInputTarget.NONE
    }

    private fun togglePomodoroTimer() {
        if (pomodoroRunning) {
            pomodoroRunning = false
            pomodoroJob?.cancel()
            pomodoroJob = null
        } else {
            if (pomodoroRemainingSec <= 0) {
                pomodoroRemainingSec = if (pomodoroIsBreak) 5 * 60 else 25 * 60
            }
            pomodoroRunning = true
            pomodoroJob?.cancel()
            pomodoroJob = serviceScope.launch {
                while (pomodoroRunning && pomodoroRemainingSec > 0) {
                    delay(1000L)
                    if (pomodoroRunning && pomodoroRemainingSec > 0) {
                        pomodoroRemainingSec -= 1
                    }
                }
                if (pomodoroRemainingSec <= 0 && pomodoroRunning) {
                    pomodoroRunning = false
                    pomodoroIsBreak = !pomodoroIsBreak
                    pomodoroRemainingSec = if (pomodoroIsBreak) 5 * 60 else 25 * 60
                    Toast.makeText(
                        this@TypeGeniusIME,
                        if (pomodoroIsBreak) "ফোকাস সেশন সম্পন্ন! ৫ মিনিট ব্রেক শুরু করুন।" else "ব্রেক শেষ! নতুন ফোকাস সেশন প্রস্তুত।",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun resetPomodoroTimer(isBreak: Boolean) {
        pomodoroRunning = false
        pomodoroJob?.cancel()
        pomodoroJob = null
        pomodoroIsBreak = isBreak
        pomodoroRemainingSec = if (isBreak) 5 * 60 else 25 * 60
    }

    private fun transformEditorText(mode: TextTransformMode) {
        if (isTranslatorOpen) {
            val trimmed = translatorSourceText.trimEnd()
            val lastWord = trimmed.takeLastWhile { !it.isWhitespace() }
            if (lastWord.isNotEmpty()) {
                val base = trimmed.dropLast(lastWord.length)
                val trailingSpaces = translatorSourceText.length - trimmed.length
                val transformed = ProductivityTextTools.transformText(lastWord, mode)
                translatorSourceText = "$base$transformed" + " ".repeat(trailingSpaces)
            }
            return
        }

        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) commitComposing()

        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) {
            ic.commitText(ProductivityTextTools.transformText(selected, mode), 1)
            refreshHandwritingLiveText()
            updateSuggestions()
            return
        }

        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val trimmed = before.trimEnd()
        val trailingSpaces = before.length - trimmed.length
        val lastWord = trimmed.takeLastWhile { !it.isWhitespace() }
        if (lastWord.isNotEmpty()) {
            val transformed = ProductivityTextTools.transformText(lastWord, mode)
            ic.deleteSurroundingText(lastWord.length + trailingSpaces, 0)
            ic.commitText(transformed + " ".repeat(trailingSpaces), 1)
            lastTypedWordPreview = transformed
            refreshHandwritingLiveText()
            updateSuggestions()
        }
    }

    private fun insertNumberedListPrefix(bengaliDigits: Boolean) {
        if (isTranslatorOpen) {
            translatorSourceText += ProductivityTextTools.nextNumberedLinePrefix(translatorSourceText, bengaliDigits)
            return
        }
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) commitComposing()
        val before = ic.getTextBeforeCursor(120, 0)?.toString().orEmpty()
        val prefix = ProductivityTextTools.nextNumberedLinePrefix(before, bengaliDigits)
        ic.commitText(prefix, 1)
        refreshHandwritingLiveText()
        updateSuggestions()
    }

    private fun handlePanelInputKeyAction(action: KeyAction) {
        when (action.type) {
            KeyType.CHARACTER -> {
                val ch = action.label
                if (activeLanguage == LanguageId.BANGLA_PHONETIC && ch.length == 1 && (ch[0] in 'a'..'z' || ch[0] in 'A'..'Z')) {
                    val prevRendered = if (composingBuffer.isEmpty()) "" else {
                        BengaliPhoneticParser.parsePhonetic(composingBuffer).firstOrNull() ?: composingBuffer
                    }
                    composingBuffer += ch
                    val cands = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                    val nextRendered = cands.firstOrNull { c -> c.any { it in '\u0980'..'\u09FF' } }
                        ?: cands.firstOrNull()
                        ?: composingBuffer
                    val current = getPanelTargetText()
                    val base = if (prevRendered.isNotEmpty() && current.endsWith(prevRendered)) {
                        current.dropLast(prevRendered.length)
                    } else current
                    setPanelTargetText(base + nextRendered)
                    suggestions = cands
                    if (isShifted && !isCapsLock) isShifted = false
                } else {
                    composingBuffer = ""
                    setPanelTargetText(getPanelTargetText() + ch)
                    if (isShifted && !isCapsLock) isShifted = false
                }
            }
            KeyType.SPACEBAR -> {
                composingBuffer = ""
                setPanelTargetText(getPanelTargetText() + " ")
            }
            KeyType.BACKSPACE -> {
                if (composingBuffer.isNotEmpty()) {
                    val prevRendered = BengaliPhoneticParser.parsePhonetic(composingBuffer).firstOrNull() ?: composingBuffer
                    composingBuffer = composingBuffer.dropLast(1)
                    val current = getPanelTargetText()
                    val base = if (prevRendered.isNotEmpty() && current.endsWith(prevRendered)) {
                        current.dropLast(prevRendered.length)
                    } else current.dropLast(1)
                    val nextRendered = if (composingBuffer.isEmpty()) "" else {
                        BengaliPhoneticParser.parsePhonetic(composingBuffer).firstOrNull() ?: composingBuffer
                    }
                    setPanelTargetText(base + nextRendered)
                    suggestions = if (composingBuffer.isEmpty()) emptyList() else BengaliPhoneticParser.parsePhonetic(composingBuffer)
                } else {
                    val cur = getPanelTargetText()
                    if (cur.isNotEmpty()) {
                        setPanelTargetText(cur.dropLast(1))
                    }
                }
            }
            KeyType.SHIFT -> {
                if (isShifted) {
                    if (isCapsLock) {
                        isCapsLock = false
                        isShifted = false
                    } else {
                        isCapsLock = true
                    }
                } else {
                    isShifted = true
                }
            }
            KeyType.LANGUAGE_SWITCH -> cycleLanguage(forward = true)
            KeyType.ENTER -> {
                composingBuffer = ""
                when (panelInputTarget) {
                    PanelTextInputTarget.UTILITIES_TODO_INPUT -> submitTodoDraft()
                    PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT -> saveAutofillInlineEdit()
                    PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT -> setPanelTargetText(getPanelTargetText() + "\n")
                    else -> panelInputTarget = PanelTextInputTarget.NONE
                }
            }
            else -> {}
        }
    }

    private fun refreshHandwritingLiveText() {
        val raw = if (isTranslatorOpen) {
            translatorSourceText + translatorComposingPreview
        } else {
            val ic = currentInputConnection
            val before = ic?.getTextBeforeCursor(200, 0)?.toString().orEmpty()
            if (before.isNotBlank()) before
            else if (composingBuffer.isNotEmpty()) {
                suggestions.firstOrNull { !it.startsWith("✓ ") } ?: composingBuffer
            } else ""
        }
        val clean = raw.trimEnd('\r')
        val activeLine = if (clean.contains('\n')) clean.substringAfterLast('\n') else clean
        handwritingLiveText = activeLine.ifBlank { clean }.takeLast(160)
    }

    private fun sendFancyStyleCard(
        style: FancyFontStyle,
        paperTheme: HandwritingPaperTheme,
        format: HandwritingCardFormat = HandwritingCardFormat.CHAT_CARD,
        signature: String = handwritingSignatureCache
    ) {
        if (style == FancyFontStyle.HANDWRITING_CUSTOM && !CustomFontManager.hasCustomFont(customFontPathCache)) {
            Toast.makeText(
                this,
                "আগে সেটিংস থেকে আপনার হাতের লেখা (.zip/.ttf) ফন্ট ইমপোর্ট করুন",
                Toast.LENGTH_LONG
            ).show()
            openCompanionSettings()
            return
        }
        val ic = currentInputConnection
        if (composingBuffer.isNotEmpty() && !isTranslatorOpen) {
            commitComposing()
        }
        val draft = if (format == HandwritingCardFormat.PORTRAIT_LETTER) {
            val longSource = if (isTranslatorOpen) {
                translatorSourceText.trim()
            } else {
                ic?.getTextBeforeCursor(650, 0)?.toString()?.trim().orEmpty()
            }
            longSource.ifBlank { handwritingLiveText.trim() }.take(600)
        } else {
            handwritingLiveText.trim().ifBlank {
                readFieldTextForAi().trim().take(240)
            }
        }
        if (draft.isBlank()) {
            Toast.makeText(this, "হাতের লেখায় পাঠাতে আগে কিছু লিখুন", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val file = HandwritingOutputGenerator.generateHandwritingPngFile(
                context = this,
                rawText = draft,
                style = style,
                customFontPath = customFontPathCache,
                customFontName = customFontNameCache,
                paperTheme = paperTheme,
                format = format,
                signature = signature
            )
            sendMediaFile(
                file = file,
                mimeType = "image/png",
                shareOnly = false,
                fallbackText = draft,
                draftTextToClearOnDirectCommit = if (!isTranslatorOpen && ic != null) draft else ""
            )
        } catch (t: Throwable) {
            CrashDiagnosticLogger.logError(this, "TypeGeniusIME.sendFancyStyleCard", "Failed to generate handwriting card", t)
            Toast.makeText(this, "হাতের লেখা কার্ড তৈরি করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMediaFile(
        file: File,
        mimeType: String,
        shareOnly: Boolean,
        fallbackText: String,
        draftTextToClearOnDirectCommit: String = ""
    ) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val editorInfo = currentInputEditorInfo
            val ic = currentInputConnection

            var committedDirect = false
            if (!shareOnly && editorInfo != null && ic != null) {
                val targetPkg = editorInfo.packageName
                if (!targetPkg.isNullOrBlank()) {
                    runCatching {
                        grantUriPermission(targetPkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                val supportedMimes = EditorInfoCompat.getContentMimeTypes(editorInfo)
                val acceptsMime = supportedMimes.any { m ->
                    m.equals(mimeType, ignoreCase = true) ||
                        m.equals("image/*", ignoreCase = true) ||
                        m.equals("*/*", ignoreCase = true)
                }
                if (acceptsMime) {
                    val contentInfo = InputContentInfoCompat(
                        uri,
                        ClipDescription(fallbackText.ifBlank { "TypeGenius Media" }, arrayOf(mimeType)),
                        null
                    )
                    committedDirect = InputConnectionCompat.commitContent(
                        ic,
                        editorInfo,
                        contentInfo,
                        InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                        null
                    )
                }
            }

            if (committedDirect) {
                if (draftTextToClearOnDirectCommit.isNotBlank() && ic != null) {
                    val before = ic.getTextBeforeCursor(draftTextToClearOnDirectCommit.length + 8, 0)?.toString().orEmpty()
                    if (before.endsWith(draftTextToClearOnDirectCommit)) {
                        ic.deleteSurroundingText(draftTextToClearOnDirectCommit.length, 0)
                        refreshHandwritingLiveText()
                    }
                }
                viewMode = KeyboardViewMode.LETTERS
                Toast.makeText(this, "হাতের লেখা চ্যাটে পাঠানো হয়েছে ✓", Toast.LENGTH_SHORT).show()
                return
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, "TypeGenius Media", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "WhatsApp / Messenger-এ শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(chooser)
            viewMode = KeyboardViewMode.LETTERS
        } catch (_: Exception) {
            if (fallbackText.isNotBlank()) {
                currentInputConnection?.commitText(fallbackText, 1)
                Toast.makeText(this, "অ্যাপটি মিডিয়া সাপোর্ট না করায় টেক্সট বসানো হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
