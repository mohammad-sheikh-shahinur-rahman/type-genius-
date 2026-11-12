package com.example.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.data.ai.GeminiDirectClient
import com.example.data.local.ClipboardItem
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardTheme
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.example.service.VoiceInputController
import kotlinx.coroutines.launch

@Composable
fun KeyboardView(
    activeLanguage: LanguageId,
    enabledLanguages: List<LanguageId>,
    isShifted: Boolean,
    isCapsLock: Boolean,
    viewMode: KeyboardViewMode,
    suggestions: List<String>,
    settingsManager: SettingsManager,
    clipboardItems: List<ClipboardItem>,
    onKeyAction: (KeyAction) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSwipeNextLanguage: () -> Unit,
    onSwipePrevLanguage: () -> Unit,
    onLanguageSelected: (LanguageId) -> Unit,
    onOpenSettings: () -> Unit,
    onClipboardItemClick: (String) -> Unit,
    onDeleteClipboardItem: (ClipboardItem) -> Unit,
    onClearClipboard: () -> Unit,
    onDirectGeminiInsert: (String) -> Unit,
    voiceController: VoiceInputController? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val isSwipeEnabled by settingsManager.isSpacebarSwipeEnabled.collectAsState(initial = true)
    val swipeSensitivity by settingsManager.swipeSensitivity.collectAsState(initial = SwipeSensitivity.MEDIUM)
    val isHapticEnabled by settingsManager.isHapticEnabled.collectAsState(initial = true)
    val showLanguageOnSpace by settingsManager.showActiveLanguageOnSpacebar.collectAsState(initial = true)
    val longPressLangPicker by settingsManager.longPressLanguageSelector.collectAsState(initial = true)
    val showAltLangKey by settingsManager.showAlternativeLanguageKey.collectAsState(initial = true)
    val currentTheme by settingsManager.keyboardTheme.collectAsState(initial = KeyboardTheme.TYPEGENIUS_PURPLE)
    val numberRowEnabled by settingsManager.numberRowEnabled.collectAsState(initial = true)
    val geminiApiKey by settingsManager.geminiApiKey.collectAsState(initial = "")

    val colors = remember(currentTheme) { KeyboardThemeData.getColors(currentTheme) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var isVoiceTypingActive by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }

    fun haptic() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    if (viewMode == KeyboardViewMode.EMOJI) {
        EmojiPanel(
            colors = colors,
            onEmojiSelected = { emoji ->
                haptic()
                onKeyAction(KeyAction(emoji, null, KeyType.CHARACTER))
            },
            onBackToKeyboard = {
                haptic()
                onKeyAction(KeyAction("ABC", null, KeyType.MODE_SWITCH))
            },
            onBackspace = {
                haptic()
                onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE))
            },
            modifier = modifier
        )
        return
    }

    if (viewMode == KeyboardViewMode.CLIPBOARD) {
        ClipboardPanel(
            items = clipboardItems,
            colors = colors,
            onItemSelected = { text ->
                haptic()
                onClipboardItemClick(text)
            },
            onDeleteItem = onDeleteClipboardItem,
            onClearAll = onClearClipboard,
            onClose = {
                haptic()
                onKeyAction(KeyAction("ABC", null, KeyType.MODE_SWITCH))
            },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
        // --- 1. Suggestion & Tool Bar or Voice Typing Banner ---
        if (isVoiceTypingActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF221636))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            voiceController?.stopListening()
                            isVoiceTypingActive = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Voice Typing",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = voiceController?.statusText ?: "Speak now",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Yellow Mic Button (Screenshot 3)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFC107))
                        .clickable {
                            haptic()
                            voiceController?.toggleListening(activeLanguage.code) { spoken ->
                                onKeyAction(KeyAction(spoken + " ", null, KeyType.CHARACTER))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.suggestionBackground)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (suggestions.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(suggestions) { candidate ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keyBackground)
                                    .clickable {
                                        haptic()
                                        onSuggestionClick(candidate)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = candidate,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "TypeGenius • ${activeLanguage.displayName}",
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    )
                }

                // Quick Toolbar Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic()
                            showAiDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Tools",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic()
                            onKeyAction(KeyAction("CLIP", null, KeyType.CLIPBOARD))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Clipboard",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic()
                            showLanguageDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic()
                            onOpenSettings()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Voice Typing Button (Screenshot 1)
                    IconButton(
                        onClick = {
                            haptic()
                            isVoiceTypingActive = true
                            voiceController?.startListening(activeLanguage.code) { spoken ->
                                onKeyAction(KeyAction(spoken + " ", null, KeyType.CHARACTER))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Typing",
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 2. Number Row (Optional) ---
        if (numberRowEnabled && viewMode == KeyboardViewMode.LETTERS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                KeyboardLayouts.numberRow.forEach { key ->
                    KeyView(
                        keyAction = key,
                        colors = colors,
                        modifier = Modifier.weight(key.weight),
                        onClick = {
                            haptic()
                            onKeyAction(key)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        // --- 3. Alphabet / Symbols Rows ---
        val rows = when (viewMode) {
            KeyboardViewMode.LETTERS -> KeyboardLayouts.getAlphabetRows(activeLanguage, isShifted || isCapsLock)
            KeyboardViewMode.SYMBOLS_1 -> KeyboardLayouts.symbols1
            KeyboardViewMode.SYMBOLS_2 -> KeyboardLayouts.symbols2
            else -> KeyboardLayouts.getAlphabetRows(activeLanguage, false)
        }

        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                        }
                    )
                }
            }
        }

        // --- 4. Bottom Functional Row with Spacebar ---
        // Matching Screenshots 1, 2, 4: ?১২৩ | 🌐 | , | ◀ প্রভাত ▶ | । | 😊 | ↵
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Mode Switch Key (?১২৩ / ?123 / ABC)
            val isBengali = activeLanguage == LanguageId.BANGLA_PROBHAT || activeLanguage == LanguageId.BANGLA_NATIONAL
            val modeLabel = if (viewMode == KeyboardViewMode.LETTERS) (if (isBengali) "?১২৩" else "?123") else "ABC"
            KeyView(
                keyAction = KeyAction(modeLabel, null, KeyType.MODE_SWITCH, weight = 1.15f),
                colors = colors,
                modifier = Modifier.weight(1.15f),
                onClick = {
                    haptic()
                    onKeyAction(KeyAction(modeLabel, null, KeyType.MODE_SWITCH))
                }
            )

            // 2. Alternative Language Key (Globe icon)
            if (showAltLangKey && enabledLanguages.size > 1) {
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            haptic()
                            onSwipeNextLanguage()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Language",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Comma Key (Screenshot 1, 2)
            KeyView(
                keyAction = KeyAction(",", null, KeyType.CHARACTER, weight = 0.95f),
                colors = colors,
                modifier = Modifier.weight(0.95f),
                onClick = {
                    haptic()
                    onKeyAction(KeyAction(",", null, KeyType.CHARACTER))
                }
            )

            // 4. SPACEBAR WITH SWIPE GESTURE (labeled ◀ প্রভাত ▶)
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
                modifier = Modifier.weight(3.8f)
            )

            // 5. Dari / Period Key (। for Bengali, . for others)
            val punctuationChar = if (isBengali) "।" else "."
            KeyView(
                keyAction = KeyAction(punctuationChar, null, KeyType.CHARACTER, weight = 0.95f),
                colors = colors,
                modifier = Modifier.weight(0.95f),
                onClick = {
                    haptic()
                    onKeyAction(KeyAction(punctuationChar, null, KeyType.CHARACTER))
                }
            )

            // 6. Emoji Key (😊)
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.keySpecialBackground)
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

            // 7. Enter / Action Key (↵)
            KeyView(
                keyAction = KeyAction("↵", null, KeyType.ENTER, weight = 1.25f),
                colors = colors,
                isEnterKey = true,
                modifier = Modifier.weight(1.25f),
                onClick = {
                    haptic()
                    onKeyAction(KeyAction("↵", null, KeyType.ENTER))
                }
            )
        }
    }

    // --- Language Selection Modal (Inline Overlay) ---
    if (showLanguageDialog) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { showLanguageDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.background,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(8.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Switch Keyboard Language",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    enabledLanguages.forEach { lang ->
                        val isSelected = lang == activeLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    haptic()
                                    onLanguageSelected(lang)
                                    showLanguageDialog = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = lang.displayName,
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = colors.accent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showLanguageDialog = false
                            onOpenSettings()
                        }) {
                            Text("Manage Languages", color = colors.accent, fontSize = 12.sp)
                        }
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text("Close", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // --- Direct Gemini AI Dialog (Inline Overlay) ---
    if (showAiDialog) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { showAiDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.background,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(8.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.accent
                        )
                        Text(
                            text = "TypeGenius AI Assistant",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Direct-to-API zero backend intelligence",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (geminiApiKey.isBlank()) {
                        Text(
                            text = "Configure your own Gemini API key in TypeGenius Settings to enable instant AI grammar check, tone polishing, and Bengali translations.",
                            color = colors.textPrimary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                showAiDialog = false
                                onOpenSettings()
                            }
                        ) {
                            Text("Open Settings to Add Key", color = colors.accent, fontSize = 12.sp)
                        }
                    } else {
                        val sampleOptions = listOf(
                            "Check Grammar & Polish" to "Fix any grammar mistakes and polish this text clearly: ",
                            "Translate to Bengali" to "Translate this English text into natural fluent Bengali: ",
                            "Translate to English" to "Translate this Bengali text into clear modern English: ",
                            "Professional Tone" to "Rewrite this text to sound professional and polite: "
                        )

                        sampleOptions.forEach { (title, promptPrefix) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.keyBackground)
                                    .clickable {
                                        val activeInput = suggestions.firstOrNull() ?: "Hello world"
                                        aiLoading = true
                                        aiError = null
                                        aiResult = null
                                        coroutineScope.launch {
                                            val client = GeminiDirectClient()
                                            val res = client.executeDirectPrompt(
                                                apiKey = geminiApiKey,
                                                prompt = "$promptPrefix $activeInput"
                                            )
                                            aiLoading = false
                                            res.onSuccess { output ->
                                                aiResult = output
                                            }.onFailure { err ->
                                                aiError = err.message
                                            }
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(title, color = colors.textPrimary, fontSize = 12.sp)
                            }
                        }

                        if (aiLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(18.dp))
                                Text("Contacting Gemini directly...", color = colors.textSecondary, fontSize = 11.sp)
                            }
                        }

                        aiResult?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.accent.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Result:", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(result, color = colors.textPrimary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(onClick = {
                                        onDirectGeminiInsert(result)
                                        showAiDialog = false
                                    }) {
                                        Text("Insert into Text", color = colors.accent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        aiError?.let { err ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(err, color = Color(0xFFFF5252), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAiDialog = false }) {
                            Text("Close", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
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
    onClick: () -> Unit,
    onLongClick: ((String) -> Unit)? = null
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    var showPreviewPopup by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val bg = when {
        isEnterKey -> colors.enterKeyBackground
        isHighlight -> colors.accent
        isPressed -> colors.keyPressedBackground
        keyAction.type != KeyType.CHARACTER && keyAction.type != KeyType.SPACEBAR -> colors.keySpecialBackground
        else -> colors.keyBackground
    }

    val fg = when {
        isEnterKey -> colors.enterKeyText
        isHighlight -> Color.White
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .shadow(1.dp, RoundedCornerShape(7.dp))
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
                                onLongClick(keyAction.secondaryLabel)
                            } else {
                                onClick()
                            }
                            change.consume()
                            break
                        }
                    }

                    job?.cancel()
                    isPressed = false
                    showPreviewPopup = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Popup Preview (Screenshot 4)
        if (showPreviewPopup && keyAction.secondaryLabel != null) {
            Box(
                modifier = Modifier
                    .offset(y = (-52).dp)
                    .zIndex(100f)
                    .size(width = 46.dp, height = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF283344))
                    .border(1.5.dp, Color(0xFF4C5D75), RoundedCornerShape(8.dp))
                    .shadow(8.dp, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = keyAction.secondaryLabel,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Key Content with Primary & Secondary character (Screenshot 1, 2)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            if (keyAction.secondaryLabel != null) {
                Text(
                    text = keyAction.secondaryLabel,
                    color = colors.textSecondary.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Text(
                text = keyAction.label,
                color = fg,
                fontSize = if (keyAction.label.length > 2) 11.sp else if (keyAction.secondaryLabel != null) 16.sp else 17.sp,
                fontWeight = if (isEnterKey || isHighlight) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .align(if (keyAction.secondaryLabel != null) Alignment.BottomCenter else Alignment.Center)
                    .padding(bottom = if (keyAction.secondaryLabel != null) 2.dp else 0.dp)
            )
        }
    }
}
