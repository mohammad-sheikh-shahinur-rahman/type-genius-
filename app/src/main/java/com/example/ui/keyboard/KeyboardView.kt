package com.example.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.SettingsManager
import com.example.data.local.ClipboardItem
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import com.example.model.OneHandedMode
import com.example.model.SwipeSensitivity
import com.example.service.VoiceInputController
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardView(
    activeLanguage: LanguageId,
    enabledLanguages: List<LanguageId>,
    isShifted: Boolean,
    isCapsLock: Boolean,
    viewMode: KeyboardViewMode,
    suggestions: List<String>,
    isTypingActive: Boolean,
    settingsManager: SettingsManager,
    clipboardItems: List<ClipboardItem>,
    isTranslatorOpen: Boolean = false,
    translatorSourceText: String = "",
    translatorComposingPreview: String = "",
    isTranslatorVoiceActive: Boolean = false,
    onOpenTranslator: () -> Unit = {},
    onCloseTranslator: () -> Unit = {},
    onTranslatorSourceTextChange: (String) -> Unit = {},
    onTranslatorClear: () -> Unit = {},
    onTranslatorPaste: () -> Unit = {},
    onTranslatorCopy: (String) -> Unit = {},
    onTranslatorShare: (String) -> Unit = {},
    onTranslatorInsert: (String) -> Unit = {},
    onTranslatorToggleVoice: (String) -> Unit = {},
    onTranslatorStopVoice: () -> Unit = {},
    onKeyAction: (KeyAction) -> Unit,
    onSelectViewMode: (KeyboardViewMode) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSwipeNextLanguage: () -> Unit,
    onSwipePrevLanguage: () -> Unit,
    onLanguageSelected: (LanguageId) -> Unit,
    onOpenSettings: () -> Unit,
    onShareApp: () -> Unit,
    onClipboardItemClick: (String) -> Unit,
    onTogglePinClipboardItem: (ClipboardItem) -> Unit,
    onDeleteClipboardItem: (ClipboardItem) -> Unit,
    onClearClipboard: () -> Unit,
    onMoveCursor: (dx: Int, dy: Int, select: Boolean) -> Unit,
    onMoveToStart: (select: Boolean) -> Unit,
    onMoveToEnd: (select: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    voiceController: VoiceInputController? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    val isSwipeEnabled by settingsManager.isSpacebarSwipeEnabled.collectAsState(initial = true)
    val swipeSensitivity by settingsManager.swipeSensitivity.collectAsState(initial = SwipeSensitivity.MEDIUM)
    val isHapticEnabled by settingsManager.isHapticEnabled.collectAsState(initial = true)
    val showLanguageOnSpace by settingsManager.showActiveLanguageOnSpacebar.collectAsState(initial = true)
    val longPressLangPicker by settingsManager.longPressLanguageSelector.collectAsState(initial = true)
    val showAltLangKey by settingsManager.showAlternativeLanguageKey.collectAsState(initial = true)
    val currentTheme by settingsManager.keyboardTheme.collectAsState(initial = KeyboardTheme.MATERIAL_YOU)
    val numberRowEnabled by settingsManager.numberRowEnabled.collectAsState(initial = false)
    val oneHandedMode by settingsManager.oneHandedMode.collectAsState(initial = OneHandedMode.FULL)
    val heightPreset by settingsManager.keyboardHeightPreset.collectAsState(initial = KeyboardHeightPreset.NORMAL)
    val floatingMode by settingsManager.floatingMode.collectAsState(initial = false)
    val translatorSrc by settingsManager.translatorSourceCode.collectAsState(initial = "bn")
    val translatorTgt by settingsManager.translatorTargetCode.collectAsState(initial = "en")
    val geminiApiKey by settingsManager.geminiApiKey.collectAsState(initial = "")

    val colors = remember(currentTheme, isSystemDark) {
        KeyboardThemeData.getColors(currentTheme, context, isSystemDark)
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var translatorPickerTarget by remember { mutableStateOf<TranslatorPickerTarget?>(null) }
    var isTranslatorLiveMode by remember { mutableStateOf(true) }
    var isVoiceTypingActive by remember { mutableStateOf(false) }
    var isToolboxExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(isTypingActive) {
        if (isTypingActive && viewMode == KeyboardViewMode.LETTERS) {
            isToolboxExpanded = false
        }
    }

    fun haptic() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val containerAlignment = when (oneHandedMode) {
        OneHandedMode.LEFT -> Alignment.CenterStart
        OneHandedMode.RIGHT -> Alignment.CenterEnd
        OneHandedMode.FULL -> Alignment.Center
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (floatingMode) {
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    Modifier
                }
            )
            .then(
                if (floatingMode) {
                    Modifier
                        .shadow(12.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                } else {
                    Modifier
                }
            )
            .background(colors.background),
        contentAlignment = containerAlignment
    ) {
        val isNarrow = maxWidth < 350.dp
        val isWide = maxWidth >= 600.dp
        val widthFraction = when {
            floatingMode -> 1f
            oneHandedMode != OneHandedMode.FULL -> 0.87f
            else -> 1f
        }
        val responsiveRowHeight = when {
            isNarrow -> (heightPreset.rowHeightDp - 4).dp
            isWide -> (heightPreset.rowHeightDp + 4).dp
            else -> heightPreset.rowHeightDp.dp
        }
        val responsiveGap = if (isNarrow) 3.dp else 4.dp

        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .widthIn(max = 680.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp)
        ) {
            // 0. Docked Live Translator Overlay above active keyboard (Screenshots 1 & 3)
            if (isTranslatorOpen) {
                InKeyboardLiveTranslatorBox(
                    sourceText = translatorSourceText,
                    composingPreview = translatorComposingPreview,
                    sourceCode = translatorSrc,
                    targetCode = translatorTgt,
                    isLiveMode = isTranslatorLiveMode,
                    isVoiceActive = isTranslatorVoiceActive,
                    geminiApiKey = geminiApiKey,
                    onToggleLiveMode = { isTranslatorLiveMode = !isTranslatorLiveMode },
                    onOpenLanguagePicker = { target -> translatorPickerTarget = target },
                    onSwapLanguages = {
                        coroutineScope.launch {
                            settingsManager.setTranslatorLanguages(translatorTgt, translatorSrc)
                        }
                    },
                    onSelectSourceLanguage = { newSrc ->
                        coroutineScope.launch {
                            settingsManager.setTranslatorLanguages(newSrc, translatorTgt)
                        }
                    },
                    onPaste = onTranslatorPaste,
                    onCopySource = onTranslatorCopy,
                    onClear = onTranslatorClear,
                    onMicClick = { onTranslatorToggleVoice(translatorSrc) },
                    onCopyOutput = onTranslatorCopy,
                    onShareOutput = onTranslatorShare,
                    onInsertOutput = onTranslatorInsert,
                    onQuickEmojiClick = { emoji ->
                        onKeyAction(KeyAction(emoji, null, KeyType.CHARACTER))
                    },
                    onClose = {
                        translatorPickerTarget = null
                        onTranslatorStopVoice()
                        onCloseTranslator()
                    },
                    onHaptic = { haptic() }
                )
            }

            // 1. Top Bar (Toolbox / Suggestions / Voice / Translator Listening / Text Editing header)
            KeyboardTopBar(
                colors = colors,
                activeLanguage = activeLanguage,
                viewMode = viewMode,
                suggestions = suggestions,
                isToolboxExpanded = isToolboxExpanded,
                isVoiceTypingActive = isVoiceTypingActive,
                isTranslatorOpen = isTranslatorOpen,
                isTranslatorVoiceActive = isTranslatorVoiceActive,
                voiceController = voiceController,
                onToggleToolbox = { isToolboxExpanded = !isToolboxExpanded },
                onSelectViewMode = { targetMode ->
                    if (targetMode == KeyboardViewMode.TRANSLATOR) {
                        onOpenTranslator()
                    } else {
                        onSelectViewMode(targetMode)
                    }
                },
                onToggleTranslator = {
                    if (isTranslatorOpen) {
                        translatorPickerTarget = null
                        onTranslatorStopVoice()
                        onCloseTranslator()
                    } else {
                        onOpenTranslator()
                    }
                },
                onSuggestionClick = onSuggestionClick,
                onOpenSettings = onOpenSettings,
                onStartVoiceTyping = {
                    if (isTranslatorOpen) {
                        onTranslatorToggleVoice(translatorSrc)
                    } else {
                        isVoiceTypingActive = true
                        voiceController?.startListening(
                            languageCode = activeLanguage.code,
                            onPermissionRequired = { onOpenSettings() }
                        ) { spoken ->
                            onKeyAction(KeyAction("$spoken ", null, KeyType.CHARACTER))
                        }
                    }
                },
                onStopVoiceTyping = {
                    if (isTranslatorVoiceActive) {
                        onTranslatorStopVoice()
                    } else {
                        voiceController?.stopListening()
                        isVoiceTypingActive = false
                    }
                },
                onToggleVoiceListening = {
                    if (isTranslatorOpen || isTranslatorVoiceActive) {
                        onTranslatorToggleVoice(translatorSrc)
                    } else {
                        voiceController?.toggleListening(
                            languageCode = activeLanguage.code,
                            onPermissionRequired = { onOpenSettings() }
                        ) { spoken ->
                            onKeyAction(KeyAction("$spoken ", null, KeyType.CHARACTER))
                        }
                    }
                },
                onHaptic = { haptic() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Panel Content
            when (viewMode) {
                KeyboardViewMode.EMOJI -> {
                    EmojiPanel(
                        colors = colors,
                        onEmojiSelected = { emoji ->
                            haptic()
                            onKeyAction(KeyAction(emoji, null, KeyType.CHARACTER))
                        },
                        onBackToKeyboard = {
                            haptic()
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        },
                        onBackspace = {
                            haptic()
                            onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE))
                        }
                    )
                }

                KeyboardViewMode.CLIPBOARD -> {
                    ClipboardPanel(
                        items = clipboardItems,
                        colors = colors,
                        onItemSelected = { text ->
                            haptic()
                            onClipboardItemClick(text)
                        },
                        onTogglePin = { item ->
                            haptic()
                            onTogglePinClipboardItem(item)
                        },
                        onDeleteItem = onDeleteClipboardItem,
                        onClearAll = onClearClipboard,
                        onClose = {
                            haptic()
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        }
                    )
                }

                KeyboardViewMode.NUMPAD -> {
                    NumpadPanel(
                        colors = colors,
                        onKeyAction = onKeyAction,
                        onBackToLetters = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.TEXT_EDITOR -> {
                    TextEditorPanel(
                        colors = colors,
                        onMoveCursor = onMoveCursor,
                        onMoveToStart = onMoveToStart,
                        onMoveToEnd = onMoveToEnd,
                        onSelectAll = onSelectAll,
                        onCopy = onCopy,
                        onPaste = onPaste,
                        onBackspace = { onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE)) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.TRANSLATOR -> {
                    LaunchedEffect(Unit) {
                        onOpenTranslator()
                    }
                }

                KeyboardViewMode.THEME_PICKER -> {
                    InKeyboardThemePanel(
                        colors = colors,
                        currentTheme = currentTheme,
                        isSystemDark = isSystemDark,
                        onSelectTheme = { theme ->
                            coroutineScope.launch { settingsManager.setKeyboardTheme(theme) }
                        },
                        onClose = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.MORE_TOOLS -> {
                    MoreToolsPanel(
                        colors = colors,
                        oneHandedMode = oneHandedMode,
                        heightPreset = heightPreset,
                        floatingMode = floatingMode,
                        onSelectViewMode = onSelectViewMode,
                        onCycleOneHanded = {
                            val entries = OneHandedMode.entries
                            val next = entries[(entries.indexOf(oneHandedMode) + 1) % entries.size]
                            coroutineScope.launch { settingsManager.setOneHandedMode(next) }
                        },
                        onCycleHeight = {
                            val entries = KeyboardHeightPreset.entries
                            val next = entries[(entries.indexOf(heightPreset) + 1) % entries.size]
                            coroutineScope.launch { settingsManager.setKeyboardHeightPreset(next) }
                        },
                        onToggleFloating = {
                            coroutineScope.launch { settingsManager.setFloatingMode(!floatingMode) }
                        },
                        onShareApp = onShareApp,
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.LETTERS,
                KeyboardViewMode.SYMBOLS_1,
                KeyboardViewMode.SYMBOLS_2 -> {
                    if (numberRowEnabled && viewMode == KeyboardViewMode.LETTERS) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(responsiveRowHeight - 6.dp)
                                .zIndex(2f),
                            horizontalArrangement = Arrangement.spacedBy(responsiveGap)
                        ) {
                            KeyboardLayouts.getNumberRow(activeLanguage).forEach { key ->
                                KeyView(
                                    keyAction = key,
                                    colors = colors,
                                    modifier = Modifier.weight(key.weight),
                                    onClick = {
                                        haptic()
                                        onKeyAction(key)
                                    },
                                    onLongClick = { secondaryChar ->
                                        haptic()
                                        onKeyAction(KeyAction(secondaryChar, null, KeyType.CHARACTER))
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    val rows = when (viewMode) {
                        KeyboardViewMode.LETTERS -> KeyboardLayouts.getAlphabetRows(activeLanguage, isShifted || isCapsLock)
                        KeyboardViewMode.SYMBOLS_1 -> KeyboardLayouts.getSymbols1(activeLanguage)
                        KeyboardViewMode.SYMBOLS_2 -> KeyboardLayouts.symbols2
                        else -> KeyboardLayouts.getAlphabetRows(activeLanguage, false)
                    }

                    var activePopupRow by remember { mutableStateOf(-1) }

                    rows.forEachIndexed { rowIndex, rowKeys ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(responsiveRowHeight)
                                .zIndex(if (activePopupRow == rowIndex) 50f else 1f)
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(responsiveGap)
                        ) {
                            rowKeys.forEach { key ->
                                val isShiftActive = key.type == KeyType.SHIFT && (isShifted || isCapsLock)
                                KeyView(
                                    keyAction = key,
                                    colors = colors,
                                    isHighlight = isShiftActive,
                                    modifier = Modifier.weight(key.weight),
                                    onClick = {
                                        haptic()
                                        onKeyAction(key)
                                    },
                                    onLongClick = { secondaryChar ->
                                        haptic()
                                        onKeyAction(KeyAction(secondaryChar, null, KeyType.CHARACTER))
                                    },
                                    onPopupStateChange = { visible ->
                                        activePopupRow = if (visible) rowIndex else if (activePopupRow == rowIndex) -1 else activePopupRow
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Action Key Row (?১২৩ | ব | , | প্রভাত | । | 😊 | ↵)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(responsiveRowHeight)
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(responsiveGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBengaliOrSylheti = activeLanguage in listOf(
                            LanguageId.BANGLA_PROBHAT,
                            LanguageId.BANGLA_PHONETIC,
                            LanguageId.BANGLA_NATIONAL,
                            LanguageId.SYLHETI,
                            LanguageId.ASSAMESE
                        )
                        val modeLabel = if (viewMode == KeyboardViewMode.LETTERS) {
                            if (isBengaliOrSylheti) "?১২৩" else "?123"
                        } else {
                            "ABC"
                        }
                        KeyView(
                            keyAction = KeyAction(modeLabel, null, KeyType.MODE_SWITCH, weight = 1.25f),
                            colors = colors,
                            shape = RoundedCornerShape(10.dp),
                            customBgColor = colors.keySpecialBackground,
                            modifier = Modifier.weight(1.25f),
                            onClick = {
                                haptic()
                                onKeyAction(KeyAction(modeLabel, null, KeyType.MODE_SWITCH))
                            }
                        )

                        if (showAltLangKey && enabledLanguages.size > 1) {
                            val langBadge = when (activeLanguage) {
                                LanguageId.BANGLA_PROBHAT,
                                LanguageId.BANGLA_PHONETIC,
                                LanguageId.BANGLA_NATIONAL -> "ব"
                                LanguageId.SYLHETI -> "সি"
                                LanguageId.ASSAMESE -> "অ"
                                LanguageId.HINDI, LanguageId.MARATHI, LanguageId.NEPALI, LanguageId.SANSKRIT -> "अ"
                                LanguageId.ARABIC, LanguageId.URDU, LanguageId.PERSIAN -> "ع"
                                else -> activeLanguage.spacebarLabel.take(2)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(0.92f)
                                    .fillMaxHeight()
                                    .shadow(1.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.keySpecialBackground)
                                    .testTag("bottom_language_switch_key")
                                    .combinedClickable(
                                        onClick = {
                                            haptic()
                                            onSwipeNextLanguage()
                                        },
                                        onLongClick = {
                                            haptic()
                                            showLanguageDialog = true
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = langBadge,
                                    color = colors.accent,
                                    fontSize = if (langBadge.length > 1) 14.sp else 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        KeyView(
                            keyAction = KeyAction(
                                label = ",",
                                secondaryLabel = if (isBengaliOrSylheti) "<" else null,
                                type = KeyType.CHARACTER,
                                weight = 0.85f
                            ),
                            colors = colors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.85f),
                            onClick = {
                                haptic()
                                onKeyAction(KeyAction(",", null, KeyType.CHARACTER))
                            },
                            onLongClick = { sec ->
                                haptic()
                                onKeyAction(KeyAction(sec, null, KeyType.CHARACTER))
                            }
                        )

                        SpacebarComponent(
                            activeLanguage = activeLanguage,
                            enabledLanguages = enabledLanguages,
                            isSwipeEnabled = isSwipeEnabled,
                            swipeSensitivity = swipeSensitivity,
                            isHapticEnabled = isHapticEnabled,
                            showLanguageLabel = showLanguageOnSpace,
                            isLongPressEnabled = longPressLangPicker,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.textPrimary,
                            accentColor = colors.accent,
                            onTapSpace = {
                                onKeyAction(KeyAction(" ", null, KeyType.SPACEBAR))
                            },
                            onSwipeNextLanguage = onSwipeNextLanguage,
                            onSwipePrevLanguage = onSwipePrevLanguage,
                            onLongPressLanguagePicker = {
                                showLanguageDialog = true
                            },
                            modifier = Modifier.weight(3.2f)
                        )

                        val punctuationChar = when (activeLanguage) {
                            LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.BANGLA_NATIONAL,
                            LanguageId.SYLHETI, LanguageId.ASSAMESE,
                            LanguageId.HINDI, LanguageId.NEPALI, LanguageId.MARATHI, LanguageId.SANSKRIT -> "।"
                            else -> "."
                        }
                        val secondaryPunct = if (punctuationChar == "।") "." else ","
                        KeyView(
                            keyAction = KeyAction(
                                label = punctuationChar,
                                secondaryLabel = secondaryPunct,
                                type = KeyType.CHARACTER,
                                weight = 0.85f
                            ),
                            colors = colors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.85f),
                            onClick = {
                                haptic()
                                onKeyAction(KeyAction(punctuationChar, null, KeyType.CHARACTER))
                            },
                            onLongClick = { sec ->
                                haptic()
                                onKeyAction(KeyAction(sec, null, KeyType.CHARACTER))
                            }
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.88f)
                                .fillMaxHeight()
                                .shadow(1.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.keySpecialBackground)
                                .testTag("bottom_emoji_key")
                                .clickable {
                                    haptic()
                                    onKeyAction(KeyAction("😊", null, KeyType.EMOJI_PICKER))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mood,
                                contentDescription = "Emoji",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        KeyView(
                            keyAction = KeyAction("↵", null, KeyType.ENTER, weight = 1.35f),
                            colors = colors,
                            isEnterKey = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.35f),
                            onClick = {
                                haptic()
                                onKeyAction(KeyAction("↵", null, KeyType.ENTER))
                            }
                        )
                    }
                }
            }

            // Reserve clean system navigation bar space so OS single chin bar (⌄ / ⌨) never overlaps keys
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(4.dp)
            )
        }

        // Scrollable Inline Language Selection Overlay
        if (showLanguageDialog) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(200f)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable { showLanguageDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.background,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 460.dp)
                        .heightIn(max = 260.dp)
                        .padding(6.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Switch Keyboard Language (${enabledLanguages.size})",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = 165.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(enabledLanguages) { lang ->
                                val isSelected = lang == activeLanguage
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            haptic()
                                            onLanguageSelected(lang)
                                            showLanguageDialog = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(lang.nativeName, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(lang.displayName, color = colors.textSecondary, fontSize = 11.sp)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = colors.accent)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                showLanguageDialog = false
                                onOpenSettings()
                            }) {
                                Text("Manage ${LanguageId.entries.size} Packs", color = colors.accent, fontSize = 12.sp)
                            }
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text("Close", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Scrollable Inline Translator Language Picker Modal (Screenshot 2 — NO Dialog window)
        val activePicker = translatorPickerTarget
        if (activePicker != null) {
            TranslatorLanguagePickerOverlay(
                pickerTarget = activePicker,
                selectedCode = if (activePicker == TranslatorPickerTarget.SOURCE) translatorSrc else translatorTgt,
                colors = colors,
                onLanguageSelected = { selectedCode ->
                    coroutineScope.launch {
                        if (activePicker == TranslatorPickerTarget.SOURCE) {
                            settingsManager.setTranslatorLanguages(selectedCode, translatorTgt)
                        } else {
                            settingsManager.setTranslatorLanguages(translatorSrc, selectedCode)
                        }
                    }
                },
                onDismiss = { translatorPickerTarget = null },
                onHaptic = { haptic() },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun KeyView(
    keyAction: KeyAction,
    colors: KeyboardColors,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    isEnterKey: Boolean = false,
    shape: Shape = RoundedCornerShape(9.dp),
    customBgColor: Color? = null,
    onClick: () -> Unit,
    onLongClick: ((String) -> Unit)? = null,
    onPopupStateChange: ((Boolean) -> Unit)? = null
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    var showPreviewPopup by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val bg = when {
        isEnterKey -> colors.enterKeyBackground
        isHighlight -> colors.accent
        isPressed -> colors.keyPressedBackground
        customBgColor != null -> customBgColor
        keyAction.type != KeyType.CHARACTER && keyAction.type != KeyType.SPACEBAR -> colors.keySpecialBackground
        else -> colors.keyBackground
    }

    val fg = when {
        isEnterKey -> colors.enterKeyText
        isHighlight -> colors.enterKeyText
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(if (showPreviewPopup) 100f else 0f)
            .testTag("key_${keyAction.label}")
            .semantics {
                contentDescription = if (keyAction.secondaryLabel != null) {
                    "${keyAction.label}, long press for ${keyAction.secondaryLabel}"
                } else {
                    keyAction.label
                }
            }
            .pointerInput(keyAction) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var isLongClickFired = false
                    val longPressTimeoutMs = 280L

                    var job: kotlinx.coroutines.Job? = null
                    if (keyAction.secondaryLabel != null && onLongClick != null) {
                        job = coroutineScope.launch {
                            kotlinx.coroutines.delay(longPressTimeoutMs)
                            if (isPressed) {
                                isLongClickFired = true
                                showPreviewPopup = true
                                onPopupStateChange?.invoke(true)
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            job?.cancel()
                            if (isLongClickFired && keyAction.secondaryLabel != null) {
                                onLongClick?.invoke(keyAction.secondaryLabel)
                            } else {
                                onClick()
                            }
                            change.consume()
                            break
                        }
                    }

                    job?.cancel()
                    isPressed = false
                    if (showPreviewPopup) {
                        showPreviewPopup = false
                        onPopupStateChange?.invoke(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(1.dp, shape)
                .background(bg, shape)
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            if (keyAction.secondaryLabel != null) {
                Text(
                    text = keyAction.secondaryLabel,
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 3.dp, top = 1.dp)
                )
            }

            Text(
                text = keyAction.label,
                color = fg,
                fontSize = if (keyAction.label.length > 2) 11.sp else if (keyAction.secondaryLabel != null) 16.sp else 18.sp,
                fontWeight = if (isEnterKey || isHighlight) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = if (keyAction.secondaryLabel != null) 3.dp else 0.dp)
            )
        }

        if (showPreviewPopup && keyAction.secondaryLabel != null) {
            Box(
                modifier = Modifier
                    .offset(y = (-48).dp)
                    .zIndex(200f)
                    .size(width = 46.dp, height = 50.dp)
                    .shadow(10.dp, RoundedCornerShape(10.dp))
                    .background(colors.keySpecialBackground, RoundedCornerShape(10.dp))
                    .border(1.5.dp, colors.accent, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = keyAction.secondaryLabel,
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
