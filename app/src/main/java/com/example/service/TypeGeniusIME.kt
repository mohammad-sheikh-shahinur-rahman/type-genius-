package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import com.example.data.SettingsManager
import com.example.data.dictionary.DictionaryManager
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.OfflineGlideEngine
import com.example.data.dictionary.OfflineMathEvaluator
import com.example.data.dictionary.UnicodeTextStyler
import com.example.data.local.ClipboardItem
import com.example.data.local.TextShortcutRepository
import com.example.data.local.TypeGeniusDatabase
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.data.translator.OfflineTranslator
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import com.example.model.SoundProfile
import com.example.ui.keyboard.KeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private var audioManager: AudioManager? = null

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
    private var lastTypedWordPreview by mutableStateOf("")
    private var lastDeletedUndoText by mutableStateOf("")
    private var lastGlideCommittedWord by mutableStateOf("")

    private var soundEnabled = false
    private var soundProfile = SoundProfile.SYSTEM
    private var spellCheckEnabled = true
    private var autoCapitalizeEnabled = true
    private var isIncognitoEnabled = false

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        settingsManager = SettingsManager.getInstance(this)
        dictionaryManager = DictionaryManager(this)
        shortcutRepository = TextShortcutRepository(this)
        database = TypeGeniusDatabase.getDatabase(this)
        voiceInputController = VoiceInputController(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        serviceScope.launch {
            launch { OfflineTranslator.initialize(this@TypeGeniusIME) }
            launch { shortcutRepository.ensureSeededDefaults() }
            launch { settingsManager.soundOnKeyPress.collect { soundEnabled = it } }
            launch { settingsManager.soundProfile.collect { soundProfile = it } }
            launch { settingsManager.spellCheckEnabled.collect { spellCheckEnabled = it } }
            launch { settingsManager.autoCapitalize.collect { autoCapitalizeEnabled = it } }
            launch { settingsManager.isIncognitoMode.collect { isIncognitoEnabled = it } }
            launch {
                settingsManager.enabledLanguages.collect { enabledCodes ->
                    val list = enabledCodes.map { LanguageId.fromCode(it) }
                    if (list.isNotEmpty()) {
                        enabledLanguages = list
                        if (!enabledLanguages.contains(activeLanguage)) {
                            activeLanguage = enabledLanguages.first()
                        }
                    }
                }
            }
            val savedLangCode = settingsManager.activeLanguageCode.first()
            activeLanguage = LanguageId.fromCode(savedLangCode)
        }

        setupClipboardMonitoring()
    }

    private fun setupClipboardMonitoring() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                if (isIncognitoEnabled) return@OnPrimaryClipChangedListener
                val clip = cm?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrBlank()) {
                        serviceScope.launch(Dispatchers.IO) {
                            database.clipboardDao().insert(
                                ClipboardItem(content = text, timestamp = System.currentTimeMillis())
                            )
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
                            openTranslator()
                        } else {
                            if (composingBuffer.isNotEmpty()) commitComposing()
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
                        viewMode = KeyboardViewMode.LETTERS
                    },
                    onTogglePinClipboardItem = { item ->
                        serviceScope.launch(Dispatchers.IO) {
                            database.clipboardDao().update(item.copy(isPinned = !item.isPinned))
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
                    onMoveCursor = { dx, dy, select -> moveCursor(dx, dy, select) },
                    onMoveToStart = { select -> sendEditorKey(KeyEvent.KEYCODE_MOVE_HOME, select) },
                    onMoveToEnd = { select -> sendEditorKey(KeyEvent.KEYCODE_MOVE_END, select) },
                    onSelectAll = {
                        currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                    },
                    onCopy = {
                        currentInputConnection?.performContextMenuAction(android.R.id.copy)
                    },
                    onPaste = {
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    },
                    onDeleteWordBackward = { deleteWordBackward() },
                    onUndoDelete = { undoLastDeletedWord() },
                    onGlideCommitted = { trail, keyCenters -> handleGlideSwipe(trail, keyCenters) },
                    onSelectFancyStyle = { style ->
                        activeFancyStyle = style
                        if (style == FancyFontStyle.NORMAL) viewMode = KeyboardViewMode.LETTERS
                    },
                    onTransformLastWord = { style -> transformLastWordWithStyle(style) },
                    onInsertTextSnippet = { snippet ->
                        if (isTranslatorOpen) translatorSourceText += snippet
                        else currentInputConnection?.commitText(snippet, 1)
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
        isTypingActive = false
        isTranslatorVoiceActive = false
        lastDeletedUndoText = ""
        lastGlideCommittedWord = ""
        viewMode = KeyboardViewMode.LETTERS
        updateAutoCapitalizeState()

        serviceScope.launch {
            val savedCode = settingsManager.activeLanguageCode.first()
            activeLanguage = LanguageId.fromCode(savedCode)
            updateSuggestions()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        currentInputConnection?.finishComposingText()
        composingBuffer = ""
        translatorComposingPreview = ""
        isTypingActive = false
        if (isTranslatorVoiceActive) {
            voiceInputController.stopListening()
            isTranslatorVoiceActive = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        voiceInputController.destroy()

        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}
    }

    private fun playKeyClickSound(keyType: KeyType) {
        if (!soundEnabled) return
        val effect = when (keyType) {
            KeyType.SPACEBAR -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyType.BACKSPACE -> AudioManager.FX_KEYPRESS_DELETE
            KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        audioManager?.playSoundEffect(effect, soundProfile.volume)
    }

    private fun handleKeyAction(action: KeyAction) {
        playKeyClickSound(action.type)
        if (action.type != KeyType.BACKSPACE) {
            lastDeletedUndoText = ""
            lastGlideCommittedWord = ""
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

                if (activeLanguage == LanguageId.BANGLA_PHONETIC && rawChar.matches(Regex("[a-zA-Z]"))) {
                    composingBuffer += rawChar
                    val candidates = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                    val top = candidates.firstOrNull() ?: composingBuffer
                    ic.setComposingText(top, 1)
                    suggestions = candidates
                } else {
                    if (composingBuffer.isNotEmpty()) {
                        commitComposing()
                    }
                    val charToCommit = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                        UnicodeTextStyler.styleSingleInput(rawChar, activeFancyStyle)
                    } else rawChar
                    ic.commitText(charToCommit, 1)
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                    updateAutoCapitalizeState()
                    updateSuggestions()
                }
            }

            KeyType.SPACEBAR -> {
                if (composingBuffer.isNotEmpty()) {
                    val topCandidate = suggestions.firstOrNull() ?: composingBuffer
                    ic.commitText(topCandidate, 1)
                    lastTypedWordPreview = topCandidate
                    if (!isIncognitoEnabled) {
                        serviceScope.launch {
                            dictionaryManager.recordWordUsed(topCandidate, activeLanguage)
                        }
                    }
                    composingBuffer = ""
                } else {
                    val before = ic.getTextBeforeCursor(32, 0)?.toString() ?: ""
                    val token = before.takeLastWhile { !it.isWhitespace() }
                    if (token.isNotEmpty()) lastTypedWordPreview = token
                }
                ic.commitText(" ", 1)
                isTypingActive = false
                updateAutoCapitalizeState()
                updateSuggestions()
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
                        ic.setComposingText(top, 1)
                        suggestions = candidates
                    }
                } else {
                    val selectedText = ic.getSelectedText(0)
                    if (!selectedText.isNullOrEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        ic.deleteSurroundingText(1, 0)
                    }
                    updateAutoCapitalizeState()
                    updateSuggestions()
                }
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
                    translatorComposingPreview = candidates.firstOrNull() ?: composingBuffer
                    suggestions = candidates
                } else {
                    if (composingBuffer.isNotEmpty()) {
                        val committed = suggestions.firstOrNull() ?: translatorComposingPreview.ifEmpty { composingBuffer }
                        translatorSourceText += committed
                        composingBuffer = ""
                        translatorComposingPreview = ""
                    }
                    translatorSourceText += char
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                    updateTranslatorSuggestions()
                }
            }

            KeyType.SPACEBAR -> {
                if (composingBuffer.isNotEmpty()) {
                    val committed = suggestions.firstOrNull() ?: translatorComposingPreview.ifEmpty { composingBuffer }
                    translatorSourceText += committed
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
                        translatorComposingPreview = candidates.firstOrNull() ?: composingBuffer
                        suggestions = candidates
                    }
                } else if (translatorSourceText.isNotEmpty()) {
                    translatorSourceText = translatorSourceText.dropLast(1)
                    updateTranslatorSuggestions()
                }
            }

            KeyType.ENTER -> {
                if (composingBuffer.isNotEmpty()) {
                    val committed = suggestions.firstOrNull() ?: translatorComposingPreview.ifEmpty { composingBuffer }
                    translatorSourceText += committed
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
                    translatorSourceText += suggestions.firstOrNull() ?: translatorComposingPreview.ifEmpty { composingBuffer }
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
                isTranslatorVoiceActive = false
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
                val top = suggestions.firstOrNull() ?: translatorComposingPreview.ifEmpty { composingBuffer }
                translatorSourceText += top
                if (!isIncognitoEnabled) {
                    serviceScope.launch {
                        dictionaryManager.recordWordUsed(top, activeLanguage)
                    }
                }
                composingBuffer = ""
                translatorComposingPreview = ""
                isTypingActive = false
            }
            return
        }
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val topCandidate = suggestions.firstOrNull() ?: composingBuffer
            ic.commitText(topCandidate, 1)
            lastTypedWordPreview = topCandidate
            if (!isIncognitoEnabled) {
                serviceScope.launch {
                    dictionaryManager.recordWordUsed(topCandidate, activeLanguage)
                }
            }
            composingBuffer = ""
            isTypingActive = false
            suggestions = dictionaryManager.getDefaultSuggestions(activeLanguage)
        }
    }

    private fun handleSuggestionCommit(candidate: String) {
        lastDeletedUndoText = ""
        if (isTranslatorOpen) {
            translatorSourceText = if (composingBuffer.isNotEmpty()) {
                "$translatorSourceText$candidate "
            } else if (lastGlideCommittedWord.isNotEmpty() && translatorSourceText.endsWith("$lastGlideCommittedWord ")) {
                translatorSourceText.dropLast(lastGlideCommittedWord.length + 1) + "$candidate "
            } else {
                val currentToken = translatorSourceText.takeLastWhile { !it.isWhitespace() }
                val base = if (currentToken.isNotEmpty()) {
                    translatorSourceText.dropLast(currentToken.length)
                } else {
                    translatorSourceText
                }
                "$base$candidate "
            }
            composingBuffer = ""
            translatorComposingPreview = ""
            lastGlideCommittedWord = ""
            isTypingActive = false
            if (!isIncognitoEnabled) {
                serviceScope.launch {
                    dictionaryManager.recordWordUsed(candidate, activeLanguage)
                }
            }
            updateTranslatorSuggestions()
            return
        }
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            ic.commitText("$candidate ", 1)
            composingBuffer = ""
        } else if (lastGlideCommittedWord.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(lastGlideCommittedWord.length + 2, 0)?.toString() ?: ""
            if (before.endsWith("$lastGlideCommittedWord ")) {
                ic.deleteSurroundingText(lastGlideCommittedWord.length + 1, 0)
            }
            ic.commitText("$candidate ", 1)
        } else {
            val before = ic.getTextBeforeCursor(32, 0)?.toString() ?: ""
            val currentToken = before.takeLastWhile { !it.isWhitespace() }
            if (currentToken.isNotEmpty()) {
                ic.deleteSurroundingText(currentToken.length, 0)
            }
            ic.commitText("$candidate ", 1)
        }
        lastTypedWordPreview = candidate
        lastGlideCommittedWord = ""
        isTypingActive = false
        if (!isIncognitoEnabled) {
            serviceScope.launch {
                dictionaryManager.recordWordUsed(candidate, activeLanguage)
            }
        }
        updateAutoCapitalizeState()
        updateSuggestions()
    }

    private fun deleteWordBackward() {
        if (isTranslatorOpen) {
            if (composingBuffer.isNotEmpty()) {
                val preview = translatorComposingPreview.ifEmpty { composingBuffer }
                lastDeletedUndoText = preview + lastDeletedUndoText
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
            updateTranslatorSuggestions()
            return
        }

        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val top = suggestions.firstOrNull() ?: composingBuffer
            ic.commitText("", 1)
            lastDeletedUndoText = top + lastDeletedUndoText
            composingBuffer = ""
            isTypingActive = false
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
        }
        updateAutoCapitalizeState()
        updateSuggestions()
    }

    private fun undoLastDeletedWord() {
        val toRestore = lastDeletedUndoText
        if (toRestore.isEmpty()) return
        lastDeletedUndoText = ""
        if (isTranslatorOpen) {
            translatorSourceText += toRestore
            updateTranslatorSuggestions()
        } else {
            currentInputConnection?.commitText(toRestore, 1)
            updateAutoCapitalizeState()
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
                ic.commitText("$topStyled ", 1)
                suggestions = candidates
                updateAutoCapitalizeState()
            }

            if (!isIncognitoEnabled) {
                dictionaryManager.recordWordUsed(rawTop, activeLanguage)
            }
        }
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
        val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
        val trimmed = before.trimEnd()
        val trailingSpaces = before.length - trimmed.length
        val lastWord = trimmed.takeLastWhile { !it.isWhitespace() }
        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length + trailingSpaces, 0)
            ic.commitText(UnicodeTextStyler.styleText(lastWord, style) + " ", 1)
            lastTypedWordPreview = lastWord
        } else {
            ic.commitText(UnicodeTextStyler.styleText("TypeGenius", style), 1)
        }
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
        viewMode = KeyboardViewMode.LETTERS
        composingBuffer = ""
        isTypingActive = false
        isShifted = false
        isCapsLock = false
        lastGlideCommittedWord = ""

        serviceScope.launch {
            settingsManager.setActiveLanguageCode(language.code)
            updateSuggestions()
        }
    }

    private fun updateSuggestions() {
        serviceScope.launch {
            if (composingBuffer.isNotEmpty()) {
                val shortcutMatches = shortcutRepository.findMatchingExpansions(composingBuffer)
                suggestions = (shortcutMatches + BengaliPhoneticParser.parsePhonetic(composingBuffer))
                    .distinct()
                    .take(6)
            } else {
                val ic = currentInputConnection
                val textBefore = ic?.getTextBeforeCursor(28, 0)?.toString() ?: ""
                val lastWord = if (textBefore.isNotEmpty() && !textBefore.last().isWhitespace()) {
                    textBefore.takeLastWhile { !it.isWhitespace() }
                } else {
                    ""
                }
                if (lastWord.isNotEmpty()) {
                    lastTypedWordPreview = lastWord
                }
                val math = OfflineMathEvaluator.evaluateIfTriggered(lastWord)
                val shortcutMatches = if (lastWord.isNotEmpty()) {
                    shortcutRepository.findMatchingExpansions(lastWord)
                } else emptyList()
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

    private fun openCompanionSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(intent)
    }

    private fun shareTypeGenius() {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out TypeGenius Keyboard — 100% offline & backendless keyboard with Bengali Probhat, Sylheti, 32 languages, and Material You dynamic themes!"
                )
            }
            val chooser = Intent.createChooser(sendIntent, "Share TypeGenius Keyboard").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(chooser)
        } catch (_: Exception) {}
    }
}
