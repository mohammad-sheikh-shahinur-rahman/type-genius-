package com.example.ui.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dictionary.FancyFontStyle
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardToolId
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import com.example.model.OneHandedMode
import com.example.service.VoiceInputController

@Composable
fun KeyboardTopBar(
    colors: KeyboardColors,
    activeLanguage: LanguageId,
    viewMode: KeyboardViewMode,
    suggestions: List<String>,
    toolbarTools: List<KeyboardToolId> = KeyboardToolId.DEFAULT_TOOLBAR,
    isToolboxExpanded: Boolean,
    isVoiceTypingActive: Boolean,
    isTranslatorOpen: Boolean = false,
    isTranslatorVoiceActive: Boolean = false,
    isIncognitoMode: Boolean = false,
    oneHandedMode: OneHandedMode = OneHandedMode.FULL,
    floatingMode: Boolean = false,
    heightPreset: KeyboardHeightPreset = KeyboardHeightPreset.NORMAL,
    liveGrammarIssueCount: Int = 0,
    canUndoDelete: Boolean = false,
    activeFancyStyle: FancyFontStyle = FancyFontStyle.NORMAL,
    draggingTool: KeyboardToolId? = null,
    isDragTargetActive: Boolean = false,
    voiceController: VoiceInputController?,
    onTopBarBoundsChanged: (Rect) -> Unit = {},
    onToolItemBoundsChanged: (KeyboardToolId, Rect) -> Unit = { _, _ -> },
    onStartToolDrag: (KeyboardToolId, Offset) -> Unit = { _, _ -> },
    onDragToolMove: (Offset) -> Unit = {},
    onEndToolDrag: () -> Unit = {},
    onToggleToolbox: () -> Unit,
    onSelectViewMode: (KeyboardViewMode) -> Unit,
    onToggleTranslator: () -> Unit = { onSelectViewMode(KeyboardViewMode.TRANSLATOR) },
    onToggleIncognito: () -> Unit = {},
    onCycleOneHanded: () -> Unit = {},
    onCycleHeight: () -> Unit = {},
    onToggleFloating: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onUndoDelete: () -> Unit = {},
    onResetFancyStyle: () -> Unit = {},
    onSuggestionClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onStartVoiceTyping: () -> Unit,
    onStopVoiceTyping: () -> Unit,
    onToggleVoiceListening: () -> Unit,
    onHaptic: () -> Unit
) {
    if (isTranslatorVoiceActive) {
        EnhancedVoiceBar(
            colors = colors,
            languageLabel = activeLanguage.spacebarLabel,
            voiceController = voiceController,
            isTranslatorMode = true,
            onCloseOrBack = {
                onHaptic()
                onStopVoiceTyping()
            },
            onToggleListening = {
                onHaptic()
                onToggleVoiceListening()
            },
            onOpenSettings = onOpenSettings
        )
        return
    }

    if (isVoiceTypingActive) {
        EnhancedVoiceBar(
            colors = colors,
            languageLabel = activeLanguage.spacebarLabel,
            voiceController = voiceController,
            isTranslatorMode = false,
            onCloseOrBack = {
                onHaptic()
                onStopVoiceTyping()
            },
            onToggleListening = {
                onHaptic()
                onToggleVoiceListening()
            },
            onOpenSettings = onOpenSettings
        )
        return
    }

    if (viewMode == KeyboardViewMode.TEXT_EDITOR) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.keySpecialBackground)
                    .testTag("text_editor_back")
                    .clickable {
                        onHaptic()
                        onSelectViewMode(KeyboardViewMode.LETTERS)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Keyboard",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Text editing",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val topBarBg by animateColorAsState(
        targetValue = if (isDragTargetActive) colors.accent.copy(alpha = 0.20f) else colors.suggestionBackground,
        animationSpec = tween(150),
        label = "topBarBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onGloballyPositioned { coords -> onTopBarBoundsChanged(coords.boundsInRoot()) }
            .clip(RoundedCornerShape(10.dp))
            .background(topBarBg)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isSubPanel = viewMode != KeyboardViewMode.LETTERS &&
            viewMode != KeyboardViewMode.SYMBOLS_1 &&
            viewMode != KeyboardViewMode.SYMBOLS_2

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.keySpecialBackground)
                .testTag("toolbox_drawer_button")
                .clickable {
                    onHaptic()
                    if (isSubPanel) {
                        onSelectViewMode(KeyboardViewMode.LETTERS)
                    } else {
                        onSelectViewMode(KeyboardViewMode.MORE_TOOLS)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSubPanel) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.GridView,
                contentDescription = if (isSubPanel) "Back to Keyboard" else "All Keyboard Tools",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (isToolboxExpanded || viewMode == KeyboardViewMode.MORE_TOOLS) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                toolbarTools.forEach { tool ->
                    val isToolActive = when (tool) {
                        KeyboardToolId.TEXT_EDITING -> viewMode == KeyboardViewMode.TEXT_EDITOR
                        KeyboardToolId.GEMINI_AI -> viewMode == KeyboardViewMode.GEMINI_AI
                        KeyboardToolId.EMOJI -> viewMode == KeyboardViewMode.EMOJI
                        KeyboardToolId.STICKERS -> viewMode == KeyboardViewMode.STICKERS_KAOMOJI
                        KeyboardToolId.CLIPBOARD -> viewMode == KeyboardViewMode.CLIPBOARD
                        KeyboardToolId.TRANSLATOR -> isTranslatorOpen || viewMode == KeyboardViewMode.TRANSLATOR
                        KeyboardToolId.FANCY_FONTS -> viewMode == KeyboardViewMode.FANCY_FONTS || activeFancyStyle != FancyFontStyle.NORMAL
                        KeyboardToolId.THEMES -> viewMode == KeyboardViewMode.THEME_PICKER
                        KeyboardToolId.INCOGNITO -> isIncognitoMode
                        KeyboardToolId.ONE_HANDED -> oneHandedMode != OneHandedMode.FULL
                        KeyboardToolId.FLOATING -> floatingMode
                        KeyboardToolId.RESIZE -> heightPreset != KeyboardHeightPreset.NORMAL
                        KeyboardToolId.NUMPAD -> viewMode == KeyboardViewMode.NUMPAD
                        KeyboardToolId.SETTINGS, KeyboardToolId.GIF, KeyboardToolId.SHARE -> false
                    }

                    val activeTint = if (tool == KeyboardToolId.INCOGNITO && isIncognitoMode) {
                        Color(0xFFE040FB)
                    } else if (isToolActive) {
                        colors.accent
                    } else {
                        colors.textPrimary
                    }

                    var itemBounds by remember(tool) { mutableStateOf(Rect.Zero) }
                    val isBeingDragged = draggingTool == tool

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .alpha(if (isBeingDragged) 0.35f else 1f)
                            .onGloballyPositioned { coords ->
                                val rect = coords.boundsInRoot()
                                itemBounds = rect
                                onToolItemBoundsChanged(tool, rect)
                            }
                            .pointerInput(tool) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { localOffset ->
                                        onHaptic()
                                        onStartToolDrag(
                                            tool,
                                            Offset(itemBounds.left + localOffset.x, itemBounds.top + localOffset.y)
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        onDragToolMove(
                                            Offset(itemBounds.left + change.position.x, itemBounds.top + change.position.y)
                                        )
                                    },
                                    onDragEnd = { onEndToolDrag() },
                                    onDragCancel = { onEndToolDrag() }
                                )
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isToolActive) activeTint.copy(alpha = 0.16f) else Color.Transparent)
                            .testTag(tool.testTag)
                            .clickable {
                                onHaptic()
                                when (tool) {
                                    KeyboardToolId.TEXT_EDITING -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.TEXT_EDITOR) KeyboardViewMode.LETTERS else KeyboardViewMode.TEXT_EDITOR
                                    )
                                    KeyboardToolId.GEMINI_AI -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.GEMINI_AI) KeyboardViewMode.LETTERS else KeyboardViewMode.GEMINI_AI
                                    )
                                    KeyboardToolId.EMOJI -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.EMOJI) KeyboardViewMode.LETTERS else KeyboardViewMode.EMOJI
                                    )
                                    KeyboardToolId.STICKERS -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.STICKERS_KAOMOJI) KeyboardViewMode.LETTERS else KeyboardViewMode.STICKERS_KAOMOJI
                                    )
                                    KeyboardToolId.GIF -> onSelectViewMode(KeyboardViewMode.EMOJI)
                                    KeyboardToolId.CLIPBOARD -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.CLIPBOARD) KeyboardViewMode.LETTERS else KeyboardViewMode.CLIPBOARD
                                    )
                                    KeyboardToolId.TRANSLATOR -> onToggleTranslator()
                                    KeyboardToolId.SETTINGS -> onOpenSettings()
                                    KeyboardToolId.THEMES -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.THEME_PICKER) KeyboardViewMode.LETTERS else KeyboardViewMode.THEME_PICKER
                                    )
                                    KeyboardToolId.FANCY_FONTS -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.FANCY_FONTS) KeyboardViewMode.LETTERS else KeyboardViewMode.FANCY_FONTS
                                    )
                                    KeyboardToolId.INCOGNITO -> onToggleIncognito()
                                    KeyboardToolId.ONE_HANDED -> onCycleOneHanded()
                                    KeyboardToolId.FLOATING -> onToggleFloating()
                                    KeyboardToolId.RESIZE -> onCycleHeight()
                                    KeyboardToolId.NUMPAD -> onSelectViewMode(
                                        if (viewMode == KeyboardViewMode.NUMPAD) KeyboardViewMode.LETTERS else KeyboardViewMode.NUMPAD
                                    )
                                    KeyboardToolId.SHARE -> onShareApp()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        TopBarToolContent(
                            tool = tool,
                            tint = activeTint,
                            colors = colors,
                            activeFancyStyle = activeFancyStyle,
                            liveGrammarIssueCount = liveGrammarIssueCount
                        )
                    }
                }
            }

            // Always accessible Suggestions toggle ("T") button
            ToolboxIconItem(
                active = false,
                accent = colors.accent,
                modifier = Modifier.testTag("toolbox_toggle_button"),
                onClick = {
                    onHaptic()
                    onToggleToolbox()
                }
            ) {
                Text(
                    text = "T",
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            val displayCandidates = if (suggestions.isNotEmpty()) {
                suggestions.take(4)
            } else {
                listOf(activeLanguage.nativeName, "TypeGenius", "Offline")
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            onHaptic()
                            onToggleToolbox()
                        }
                        .testTag("toolbox_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("T", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (activeFancyStyle != FancyFontStyle.NORMAL) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("active_fancy_font_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = activeFancyStyle.badgeLabel,
                            color = colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                onHaptic()
                                onSelectViewMode(KeyboardViewMode.FANCY_FONTS)
                            }
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset to Normal Font",
                            tint = colors.accent,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable {
                                    onHaptic()
                                    onResetFancyStyle()
                                }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (isIncognitoMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF7B1FA2).copy(alpha = 0.25f))
                            .clickable {
                                onHaptic()
                                onToggleIncognito()
                            }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .testTag("incognito_status_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Incognito Active",
                            tint = Color(0xFFE040FB),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                }

                if (canUndoDelete) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent.copy(alpha = 0.18f))
                            .clickable {
                                onHaptic()
                                onUndoDelete()
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("undo_swipe_delete_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo deleted word",
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Undo",
                            color = colors.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                displayCandidates.forEachIndexed { index, candidate ->
                    val isRealPrediction = suggestions.isNotEmpty()
                    val isGrammarFix = isRealPrediction && candidate.startsWith("✓ ")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = if (isGrammarFix) 4.dp else 0.dp, horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isGrammarFix) Modifier.background(colors.accent.copy(alpha = 0.16f))
                                else Modifier
                            )
                            .clickable(enabled = isRealPrediction) {
                                onHaptic()
                                onSuggestionClick(candidate)
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = candidate,
                            color = when {
                                isGrammarFix -> colors.accent
                                index == 0 && isRealPrediction -> colors.accent
                                else -> colors.textPrimary
                            },
                            fontSize = if (isGrammarFix) 12.5.sp else 13.sp,
                            fontWeight = if (isGrammarFix || (index == 0 && isRealPrediction)) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (index < displayCandidates.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(colors.textSecondary.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }

        // Right Voice Microphone Button
        IconButton(
            onClick = {
                onHaptic()
                onStartVoiceTyping()
            },
            modifier = Modifier
                .size(36.dp)
                .testTag("toolbar_voice_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Typing",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TopBarToolContent(
    tool: KeyboardToolId,
    tint: Color,
    colors: KeyboardColors,
    activeFancyStyle: FancyFontStyle,
    liveGrammarIssueCount: Int
) {
    when (tool) {
        KeyboardToolId.TEXT_EDITING -> Icon(
            imageVector = Icons.AutoMirrored.Filled.FormatIndentIncrease,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.GEMINI_AI -> Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = tool.label,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
            if (liveGrammarIssueCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (liveGrammarIssueCount > 9) "9+" else liveGrammarIssueCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        KeyboardToolId.EMOJI -> Icon(
            imageVector = Icons.Default.Mood,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        KeyboardToolId.STICKERS -> Icon(
            imageVector = Icons.Default.Mood,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        KeyboardToolId.GIF -> Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(colors.keySpecialBackground)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GIF",
                color = tint,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        KeyboardToolId.CLIPBOARD -> Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.TRANSLATOR -> Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.SETTINGS -> Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        KeyboardToolId.THEMES -> Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        KeyboardToolId.FANCY_FONTS -> Text(
            text = if (activeFancyStyle != FancyFontStyle.NORMAL) activeFancyStyle.badgeLabel else "Aa",
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        KeyboardToolId.INCOGNITO -> Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.ONE_HANDED -> Icon(
            imageVector = Icons.Default.Smartphone,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.FLOATING -> Icon(
            imageVector = Icons.Default.PictureInPictureAlt,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.RESIZE -> Icon(
            imageVector = Icons.Default.AspectRatio,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.NUMPAD -> Icon(
            imageVector = Icons.Default.Dialpad,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        KeyboardToolId.SHARE -> Icon(
            imageVector = Icons.Default.Share,
            contentDescription = tool.label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun EnhancedVoiceBar(
    colors: KeyboardColors,
    languageLabel: String,
    voiceController: VoiceInputController?,
    isTranslatorMode: Boolean,
    onCloseOrBack: () -> Unit,
    onToggleListening: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isListening = voiceController?.isListening == true
    val isProcessing = voiceController?.isProcessing == true
    val isContinuous = voiceController?.continuousMode ?: true
    val hasError = voiceController?.hasError == true
    val rms = voiceController?.rmsLevel ?: 0f
    val partial = voiceController?.partialText.orEmpty()
    val statusText = when {
        partial.isNotBlank() -> partial
        else -> voiceController?.statusText ?: "Speak now"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micAura"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voiceWavePhase"
    )

    val micBgColor by animateColorAsState(
        targetValue = when {
            isListening -> colors.accent
            hasError -> Color(0xFFE53935)
            else -> colors.accent.copy(alpha = 0.22f)
        },
        animationSpec = tween(220),
        label = "micBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.suggestionBackground,
                        if (isListening) colors.accent.copy(alpha = 0.14f) else colors.keySpecialBackground
                    )
                )
            )
    ) {
        // Subtle animated fluid soundwave ribbon behind the controls (no glowing border)
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .alpha(if (isListening) 0.38f else if (isProcessing) 0.22f else 0.08f)
        ) {
            val midY = size.height * 0.56f
            val amp1 = size.height * (0.09f + rms * 0.26f)
            val amp2 = size.height * (0.06f + rms * 0.18f)

            val path1 = Path()
            val path2 = Path()
            val steps = 36
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val x = t * size.width
                val env = sin(t * PI).toFloat()
                val y1 = midY + sin(t * 4.5f * PI.toFloat() + wavePhase) * amp1 * env
                val y2 = midY + sin(t * 6.0f * PI.toFloat() - wavePhase * 1.25f) * amp2 * env
                if (i == 0) {
                    path1.moveTo(x, y1)
                    path2.moveTo(x, y2)
                } else {
                    path1.lineTo(x, y1)
                    path2.lineTo(x, y2)
                }
            }
            val waveColor = if (hasError) Color(0xFFEF5350) else colors.accent
            drawPath(
                path = path1,
                color = waveColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = path2,
                color = waveColor.copy(alpha = 0.45f),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseOrBack,
                modifier = Modifier
                    .size(38.dp)
                    .testTag(if (isTranslatorMode) "translator_voice_back_button" else "voice_close_button")
            ) {
                Icon(
                    imageVector = if (isTranslatorMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                    contentDescription = if (isTranslatorMode) "Stop Listening" else "Close Voice Typing",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accent.copy(alpha = 0.16f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = languageLabel,
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            VoiceWaveformEqualizer(
                isListening = isListening,
                isProcessing = isProcessing,
                rmsLevel = rms,
                pulsePhase = pulsePhase,
                accentColor = if (hasError) Color(0xFFEF5350) else colors.accent
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = voiceController?.permissionMissing == true) {
                        onOpenSettings()
                    },
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = statusText,
                    transitionSpec = {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(110))
                    },
                    label = "voiceStatusText"
                ) { text ->
                    Text(
                        text = text,
                        color = if (hasError) Color(0xFFEF5350) else colors.textPrimary,
                        fontSize = if (partial.isNotBlank()) 13.5.sp else 12.5.sp,
                        fontWeight = if (partial.isNotBlank() || isListening) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Continuous mode toggle pill (Auto-continue vs Single phrase)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isContinuous) colors.accent.copy(alpha = 0.20f)
                        else colors.keySpecialBackground
                    )
                    .clickable(enabled = voiceController != null) {
                        voiceController?.toggleContinuousMode()
                    }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("voice_continuous_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isContinuous) "Auto ∞" else "1×",
                    color = if (isContinuous) colors.accent else colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isListening) {
                    val outerScale = 1.04f + (0.18f * pulsePhase) + (0.24f * rms)
                    val innerScale = 1.0f + (0.10f * (1f - pulsePhase)) + (0.14f * rms)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(outerScale)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.16f * (1f - pulsePhase * 0.5f)))
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(innerScale)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.26f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(micBgColor)
                        .testTag(if (isTranslatorMode) "translator_voice_mic_circle" else "voice_mic_pill")
                        .clickable(onClick = onToggleListening),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isListening) "Listening, tap to pause" else "Tap microphone to speak",
                        tint = if (isListening || hasError) Color.White else colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveformEqualizer(
    isListening: Boolean,
    isProcessing: Boolean,
    rmsLevel: Float,
    pulsePhase: Float,
    accentColor: Color
) {
    val barMultipliers = listOf(0.42f, 0.72f, 1.0f, 0.88f, 0.65f, 0.92f, 0.48f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(26.dp)
    ) {
        barMultipliers.forEachIndexed { idx, mult ->
            val phaseShift = if (idx % 2 == 0) pulsePhase else 1f - pulsePhase
            val targetHeight = when {
                isProcessing -> 7f + 10f * phaseShift
                isListening -> {
                    val activeWave = (rmsLevel * 20f * mult) + (4.5f * phaseShift)
                    (5f + activeWave).coerceIn(5f, 24f)
                }
                else -> 5f
            }
            val animHeight by animateDpAsState(
                targetValue = targetHeight.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "eqBar$idx"
            )
            Box(
                modifier = Modifier
                    .width(2.8.dp)
                    .height(animHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isListening || isProcessing) accentColor
                        else accentColor.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

@Composable
private fun ToolboxIconItem(
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
