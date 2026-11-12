package com.example.service

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
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
import com.example.data.local.ClipboardItem
import com.example.data.local.TypeGeniusDatabase
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
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
    private lateinit var database: TypeGeniusDatabase
    private lateinit var voiceInputController: VoiceInputController

    // Keyboard internal states
    private var activeLanguage by mutableStateOf(LanguageId.BANGLA_PROBHAT)
    private var enabledLanguages by mutableStateOf(
        listOf(
            LanguageId.BANGLA_PROBHAT,
            LanguageId.BANGLA_PHONETIC,
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

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        settingsManager = SettingsManager.getInstance(this)
        dictionaryManager = DictionaryManager(this)
        database = TypeGeniusDatabase.getDatabase(this)
        voiceInputController = VoiceInputController(this)

        serviceScope.launch {
            val savedLangCode = settingsManager.activeLanguageCode.first()
            activeLanguage = LanguageId.fromCode(savedLangCode)

            val enabledCodes = settingsManager.enabledLanguages.first()
            val list = enabledCodes.map { LanguageId.fromCode(it) }
            if (list.isNotEmpty()) {
                enabledLanguages = list
                if (!enabledLanguages.contains(activeLanguage)) {
                    activeLanguage = enabledLanguages.first()
                }
            }
        }

        setupClipboardMonitoring()
    }

    private fun setupClipboardMonitoring() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
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
                    settingsManager = settingsManager,
                    clipboardItems = clipItems,
                    onKeyAction = { handleKeyAction(it) },
                    onSuggestionClick = { handleSuggestionCommit(it) },
                    onSwipeNextLanguage = { cycleLanguage(forward = true) },
                    onSwipePrevLanguage = { cycleLanguage(forward = false) },
                    onLanguageSelected = { selectLanguage(it) },
                    onOpenSettings = { openCompanionSettings() },
                    onClipboardItemClick = { text ->
                        currentInputConnection?.commitText(text, 1)
                        viewMode = KeyboardViewMode.LETTERS
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
                    onDirectGeminiInsert = { result ->
                        currentInputConnection?.commitText(result, 1)
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
        viewMode = KeyboardViewMode.LETTERS
        isShifted = false

        serviceScope.launch {
            val enabledCodes = settingsManager.enabledLanguages.first()
            val list = enabledCodes.map { LanguageId.fromCode(it) }
            if (list.isNotEmpty()) {
                enabledLanguages = list
            }
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

    // --- Key Event & Action Handling ---
    private fun handleKeyAction(action: KeyAction) {
        val ic = currentInputConnection ?: return

        when (action.type) {
            KeyType.CHARACTER -> {
                val char = action.label

                if (activeLanguage == LanguageId.BANGLA_PHONETIC && char.matches(Regex("[a-zA-Z]"))) {
                    // Phonetic Bengali composing
                    composingBuffer += char
                    val candidates = BengaliPhoneticParser.parsePhonetic(composingBuffer)
                    val top = candidates.firstOrNull() ?: composingBuffer
                    ic.setComposingText(top, 1)
                    suggestions = candidates
                } else {
                    if (composingBuffer.isNotEmpty()) {
                        commitComposing()
                    }
                    ic.commitText(char, 1)
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                    }
                    updateSuggestions()
                }
            }

            KeyType.SPACEBAR -> {
                if (composingBuffer.isNotEmpty()) {
                    val topCandidate = suggestions.firstOrNull() ?: composingBuffer
                    ic.commitText(topCandidate, 1)
                    serviceScope.launch {
                        dictionaryManager.recordWordUsed(topCandidate, activeLanguage)
                    }
                    composingBuffer = ""
                }
                ic.commitText(" ", 1)
                updateSuggestions()
            }

            KeyType.BACKSPACE -> {
                if (composingBuffer.isNotEmpty()) {
                    composingBuffer = composingBuffer.dropLast(1)
                    if (composingBuffer.isEmpty()) {
                        ic.commitText("", 1)
                        suggestions = emptyList()
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
                    updateSuggestions()
                }
            }

            KeyType.ENTER -> {
                if (composingBuffer.isNotEmpty()) {
                    commitComposing()
                }
                val editorInfo = currentInputEditorInfo
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION

                if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    sendKeyEvents(KeyEvent.KEYCODE_ENTER)
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

            KeyType.LANGUAGE_SWITCH -> {
                cycleLanguage(forward = true)
            }

            else -> {}
        }
    }

    private fun commitComposing() {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val topCandidate = suggestions.firstOrNull() ?: composingBuffer
            ic.commitText(topCandidate, 1)
            serviceScope.launch {
                dictionaryManager.recordWordUsed(topCandidate, activeLanguage)
            }
            composingBuffer = ""
            suggestions = emptyList()
        }
    }

    private fun handleSuggestionCommit(candidate: String) {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            ic.commitText(candidate, 1)
            composingBuffer = ""
        } else {
            // Replace current incomplete word before cursor
            ic.deleteSurroundingText(1, 0)
            ic.commitText(candidate + " ", 1)
        }
        serviceScope.launch {
            dictionaryManager.recordWordUsed(candidate, activeLanguage)
        }
        updateSuggestions()
    }

    // --- Language Cycling for Spacebar Swipe ---
    private fun cycleLanguage(forward: Boolean) {
        if (enabledLanguages.size <= 1) return

        if (composingBuffer.isNotEmpty()) {
            commitComposing()
        }

        val currentIndex = enabledLanguages.indexOf(activeLanguage)
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

        serviceScope.launch {
            settingsManager.setActiveLanguageCode(language.code)
            updateSuggestions()
        }
    }

    private fun updateSuggestions() {
        serviceScope.launch {
            if (composingBuffer.isNotEmpty()) {
                suggestions = BengaliPhoneticParser.parsePhonetic(composingBuffer)
            } else {
                val ic = currentInputConnection
                val textBefore = ic?.getTextBeforeCursor(20, 0)?.toString() ?: ""
                val lastWord = textBefore.trimEnd().split("\\s+".toRegex()).lastOrNull() ?: ""
                suggestions = dictionaryManager.getPredictions(lastWord, activeLanguage)
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
}
