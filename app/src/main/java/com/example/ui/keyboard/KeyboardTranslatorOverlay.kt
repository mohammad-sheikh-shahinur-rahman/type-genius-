package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.ai.GeminiDirectClient
import com.example.data.translator.OfflineTranslationResult
import com.example.data.translator.OfflineTranslator
import kotlinx.coroutines.launch

enum class TranslatorPickerTarget {
    SOURCE,
    TARGET
}

private val QuickEmojiStrip = listOf(
    "🫡", "🐰", "❤️", "🇵🇸", "😊", "🤍", "😂", "😭", "😮", "😍", "👍", "🙏", "🇧🇩", "✨"
)

private val TranslatorBarBg = Color(0xFF151C26)
private val TranslatorCardBg = Color(0xFF111824)
private val TranslatorPillBg = Color(0xFF1F2836)
private val TranslatorBorder = Color(0xFF2B394D)
private val TranslatorAccentBlue = Color(0xFF2B8CEE)
private val TranslatorLiveGreen = Color(0xFF34D399)
private val TranslatorClearRed = Color(0xFFEF6C6C)

@Composable
fun InKeyboardLiveTranslatorBox(
    sourceText: String,
    composingPreview: String,
    sourceCode: String,
    targetCode: String,
    isLiveMode: Boolean,
    isVoiceActive: Boolean,
    geminiApiKey: String,
    onToggleLiveMode: () -> Unit,
    onOpenLanguagePicker: (TranslatorPickerTarget) -> Unit,
    onSwapLanguages: () -> Unit,
    onSelectSourceLanguage: (String) -> Unit,
    onPaste: () -> Unit,
    onCopySource: (String) -> Unit,
    onClear: () -> Unit,
    onMicClick: () -> Unit,
    onCopyOutput: (String) -> Unit,
    onShareOutput: (String) -> Unit,
    onInsertOutput: (String) -> Unit,
    onQuickEmojiClick: (String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val langs = OfflineTranslator.supportedLanguages

    val combinedSourceText = remember(sourceText, composingPreview) {
        if (composingPreview.isEmpty()) sourceText else "$sourceText$composingPreview"
    }

    var manualTriggerTick by remember { mutableStateOf(0) }
    var aiPolishedResult by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }

    var offlineResult by remember {
        mutableStateOf(
            if (isLiveMode) {
                OfflineTranslator.translate(combinedSourceText, sourceCode, targetCode)
            } else {
                OfflineTranslationResult("", sourceCode, targetCode, false)
            }
        )
    }

    LaunchedEffect(Unit) {
        OfflineTranslator.initialize(context)
    }

    LaunchedEffect(combinedSourceText, sourceCode, targetCode, isLiveMode, manualTriggerTick) {
        aiPolishedResult = null
        if (combinedSourceText.isBlank()) {
            offlineResult = OfflineTranslationResult("", sourceCode, targetCode, false)
            return@LaunchedEffect
        }
        if (isLiveMode || manualTriggerTick > 0) {
            offlineResult = OfflineTranslator.translate(combinedSourceText, sourceCode, targetCode)
            offlineResult = OfflineTranslator.translateWithMemory(context, combinedSourceText, sourceCode, targetCode)
        }
    }

    val srcLang = langs.find { it.code == sourceCode }
    val tgtLang = langs.find { it.code == targetCode }
    val srcNative = srcLang?.nativeName ?: sourceCode
    val tgtNative = tgtLang?.nativeName ?: targetCode
    val isTargetRtl = targetCode in setOf("ar", "fa", "ur", "he")

    val detectedLang = remember(offlineResult.detectedSourceLanguage, sourceCode, combinedSourceText) {
        val det = offlineResult.detectedSourceLanguage
        if (combinedSourceText.isNotBlank() && det != null && det != sourceCode && !(sourceCode == "banglish" && det == "bn")) {
            langs.find { it.code == det }
        } else {
            null
        }
    }

    val outputText = aiPolishedResult ?: offlineResult.translatedText

    Column(modifier = modifier.fillMaxWidth()) {
        // Dark Docked Translator Area matching Screenshots 1 & 3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(TranslatorBarBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: <-   [ Source v ]  <->  [ Target v ]   [ Live ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        onHaptic()
                        onClose()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ime_translator_close")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Translator",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LanguageSelectorPill(
                        label = srcNative,
                        testTag = "ime_translator_src_btn",
                        onClick = {
                            onHaptic()
                            onOpenLanguagePicker(TranslatorPickerTarget.SOURCE)
                        }
                    )

                    IconButton(
                        onClick = {
                            onHaptic()
                            onSwapLanguages()
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("ime_translator_swap_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Languages",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    LanguageSelectorPill(
                        label = tgtNative,
                        testTag = "ime_translator_tgt_btn",
                        onClick = {
                            onHaptic()
                            onOpenLanguagePicker(TranslatorPickerTarget.TARGET)
                        }
                    )
                }

                // [ Live ] toggle pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isLiveMode) TranslatorPillBg else Color(0xFF1A202C))
                        .border(
                            width = 1.dp,
                            color = if (isLiveMode) TranslatorLiveGreen.copy(alpha = 0.45f) else TranslatorBorder,
                            shape = CircleShape
                        )
                        .testTag("ime_translator_live_toggle")
                        .clickable {
                            onHaptic()
                            onToggleLiveMode()
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLiveMode) "Live" else "Manual",
                        color = if (isLiveMode) TranslatorLiveGreen else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Inner Rounded Translator Card (SOURCE TEXT + optional TRANSLATED TEXT)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TranslatorCardBg)
                    .border(1.dp, TranslatorBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOURCE TEXT",
                        color = Color(0xFF8FA0B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    if (detectedLang != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TranslatorAccentBlue.copy(alpha = 0.2f))
                                .clickable {
                                    onHaptic()
                                    onSelectSourceLanguage(detectedLang.code)
                                }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Use ${detectedLang.nativeName}?",
                                color = Color(0xFF60A5FA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Scrollable Source Display Box (fed by live keyboard below + voice + paste)
                val scrollState = rememberScrollState()
                LaunchedEffect(combinedSourceText) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 26.dp, max = 56.dp)
                        .verticalScroll(scrollState)
                        .testTag("ime_translator_input"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (combinedSourceText.isEmpty()) {
                        Text(
                            text = if (isLiveMode) "অনুবাদ করতে লিখুন (Live Mode)" else "অনুবাদ করতে লিখুন",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = combinedSourceText,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Source Actions Row: [Paste] [Copy] [Clear]  ...  (Mic)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TranslatorActionChip(
                            icon = Icons.Default.ContentPaste,
                            label = "Paste",
                            iconTint = TranslatorAccentBlue,
                            testTag = "ime_translator_paste_btn",
                            onClick = {
                                onHaptic()
                                onPaste()
                            }
                        )

                        if (combinedSourceText.isNotEmpty()) {
                            TranslatorActionChip(
                                icon = Icons.Default.ContentCopy,
                                label = "Copy",
                                iconTint = Color(0xFFCBD5E1),
                                testTag = "ime_translator_copy_src_btn",
                                onClick = {
                                    onHaptic()
                                    onCopySource(combinedSourceText)
                                }
                            )

                            TranslatorActionChip(
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                label = "Clear",
                                iconTint = TranslatorClearRed,
                                testTag = "ime_translator_clear_btn",
                                onClick = {
                                    onHaptic()
                                    onClear()
                                }
                            )

                            if (!isLiveMode) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TranslatorAccentBlue)
                                        .clickable {
                                            onHaptic()
                                            manualTriggerTick++
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Translate",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Voice Microphone Circle Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isVoiceActive) TranslatorAccentBlue else TranslatorPillBg)
                            .border(
                                1.dp,
                                if (isVoiceActive) Color(0xFF60A5FA) else TranslatorBorder,
                                CircleShape
                            )
                            .testTag("ime_translator_mic_btn")
                            .clickable {
                                onHaptic()
                                onMicClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Translate",
                            tint = if (isVoiceActive) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Translated Output Section matching Screenshot 3
                if (outputText.isNotBlank() && combinedSourceText.isNotBlank()) {
                    HorizontalDivider(color = TranslatorBorder, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (aiPolishedResult != null) "TRANSLATED TEXT (GEMINI)" else "TRANSLATED TEXT",
                            color = TranslatorAccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        if (aiPolishedResult == null) {
                            Text(
                                text = "100% Offline",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    val outScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 58.dp)
                            .verticalScroll(outScroll)
                    ) {
                        Text(
                            text = outputText,
                            color = Color(0xFF58A6FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = if (isTargetRtl) TextAlign.End else TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ime_translator_output")
                        )
                    }

                    // Output Actions Row: [Copy] [Share] ... [ > Insert ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TranslatorActionChip(
                                icon = Icons.Default.ContentCopy,
                                label = "Copy",
                                iconTint = TranslatorAccentBlue,
                                testTag = "ime_translator_copy_out_btn",
                                onClick = {
                                    onHaptic()
                                    onCopyOutput(outputText)
                                }
                            )

                            TranslatorActionChip(
                                icon = Icons.Default.Share,
                                label = "Share",
                                iconTint = TranslatorAccentBlue,
                                testTag = "ime_translator_share_btn",
                                onClick = {
                                    onHaptic()
                                    onShareOutput(outputText)
                                }
                            )

                            if (geminiApiKey.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F7B47))
                                        .clickable {
                                            onHaptic()
                                            aiLoading = true
                                            scope.launch {
                                                val res = GeminiDirectClient().executeDirectPrompt(
                                                    apiKey = geminiApiKey,
                                                    prompt = "Translate accurately from $srcNative to $tgtNative, return only the translation: $combinedSourceText"
                                                )
                                                aiLoading = false
                                                res.onSuccess { aiPolishedResult = it }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (aiLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Optional Gemini Polish",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Primary [ > Insert ] pill button
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TranslatorAccentBlue)
                                .testTag("ime_translator_insert_btn")
                                .clickable {
                                    onHaptic()
                                    onInsertOutput(outputText)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Insert Translation",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Insert",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Emoji Strip directly beneath Translator Box (Screenshots 1 & 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickEmojiStrip.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable {
                            onHaptic()
                            onQuickEmojiClick(emoji)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectorPill(
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(TranslatorPillBg)
            .border(1.dp, TranslatorBorder, CircleShape)
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun TranslatorActionChip(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF17202E))
            .border(1.dp, Color(0xFF2D3B4E), CircleShape)
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Inline Compose Language Picker Overlay matching Screenshot 2:
 * "অনুবাদ ভাষা বেছে নিন (Target)" / "(Source)" with radio list and green "✓ 100% Offline" subtitles.
 * Uses inline Box overlay (never Dialog/AlertDialog) to avoid WindowManager$BadTokenException inside IME.
 */
@Composable
fun TranslatorLanguagePickerOverlay(
    pickerTarget: TranslatorPickerTarget,
    selectedCode: String,
    colors: KeyboardColors,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langs = OfflineTranslator.supportedLanguages
    val titleSuffix = if (pickerTarget == TranslatorPickerTarget.SOURCE) "Source" else "Target"

    Box(
        modifier = modifier
            .zIndex(210f)
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 420.dp)
                .heightIn(max = 420.dp)
                .padding(8.dp)
                .clickable(enabled = false) {}
                .testTag("ime_translator_language_picker")
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    text = "অনুবাদ ভাষা বেছে নিন ($titleSuffix)",
                    color = Color(0xFF1A1C1E),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 290.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(langs, key = { it.code }) { lang ->
                        val isSelected = lang.code == selectedCode
                        val displayTitle = if (lang.name == lang.nativeName) {
                            lang.name
                        } else {
                            "${lang.name} / ${lang.nativeName}"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) TranslatorAccentBlue.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onHaptic()
                                    onLanguageSelected(lang.code)
                                    onDismiss()
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onHaptic()
                                    onLanguageSelected(lang.code)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TranslatorAccentBlue,
                                    unselectedColor = Color(0xFF44474E)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = displayTitle,
                                    color = Color(0xFF1A1C1E),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "✓ 100% Offline",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onHaptic()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("ime_translator_picker_cancel")
                    ) {
                        Text(
                            text = "Cancel",
                            color = TranslatorAccentBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
