package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import com.example.service.VoiceInputController

@Composable
fun KeyboardTopBar(
    colors: KeyboardColors,
    activeLanguage: LanguageId,
    viewMode: KeyboardViewMode,
    suggestions: List<String>,
    isToolboxExpanded: Boolean,
    isVoiceTypingActive: Boolean,
    isTranslatorOpen: Boolean = false,
    isTranslatorVoiceActive: Boolean = false,
    isIncognitoMode: Boolean = false,
    canUndoDelete: Boolean = false,
    voiceController: VoiceInputController?,
    onToggleToolbox: () -> Unit,
    onSelectViewMode: (KeyboardViewMode) -> Unit,
    onToggleTranslator: () -> Unit = { onSelectViewMode(KeyboardViewMode.TRANSLATOR) },
    onToggleIncognito: () -> Unit = {},
    onUndoDelete: () -> Unit = {},
    onSuggestionClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onStartVoiceTyping: () -> Unit,
    onStopVoiceTyping: () -> Unit,
    onToggleVoiceListening: () -> Unit,
    onHaptic: () -> Unit
) {
    if (isTranslatorVoiceActive) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    onHaptic()
                    onStopVoiceTyping()
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("translator_voice_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Stop Listening",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = if (voiceController?.isListening == true && voiceController.statusText != "Speak now") {
                    voiceController.statusText
                } else {
                    "Listening..."
                },
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = voiceController?.permissionMissing == true) {
                    onOpenSettings()
                }
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0084F4))
                    .testTag("translator_voice_mic_circle")
                    .clickable {
                        onHaptic()
                        onToggleVoiceListening()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        return
    }

    if (isVoiceTypingActive) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF191916))
        ) {
            IconButton(
                onClick = {
                    onHaptic()
                    onStopVoiceTyping()
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(40.dp)
                    .testTag("voice_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Voice Typing",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = voiceController?.statusText ?: "Speak now",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(enabled = voiceController?.permissionMissing == true) {
                        onOpenSettings()
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 62.dp, height = 46.dp)
                    .clip(RoundedCornerShape(topStart = 23.dp, bottomStart = 23.dp))
                    .background(Color(0xFFFFC107))
                    .testTag("voice_mic_pill")
                    .clickable {
                        onHaptic()
                        onToggleVoiceListening()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Microphone",
                    tint = if (voiceController?.isListening == true) Color(0xFF1B5E20) else Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        return
    }

    // Text Editing dedicated top bar matching Screenshot 2
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.suggestionBackground)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left circular GridView (⊞) or Back (←) button matching Screenshots 1 & 3
        val isSubPanel = viewMode != KeyboardViewMode.LETTERS && viewMode != KeyboardViewMode.SYMBOLS_1 && viewMode != KeyboardViewMode.SYMBOLS_2
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
            // Material You Clean Top Strip matching Screenshot 1 & 3 + T suggestion toggle
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolboxIconItem(
                    active = viewMode == KeyboardViewMode.EMOJI,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_emoji"),
                    onClick = {
                        onHaptic()
                        onSelectViewMode(if (viewMode == KeyboardViewMode.EMOJI) KeyboardViewMode.LETTERS else KeyboardViewMode.EMOJI)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = "Emoji & Kaomoji",
                        tint = if (viewMode == KeyboardViewMode.EMOJI) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // GIF / Kaomoji pill badge matching Screenshot 1 & 3
                ToolboxIconItem(
                    active = viewMode == KeyboardViewMode.EMOJI,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_gif"),
                    onClick = {
                        onHaptic()
                        onSelectViewMode(KeyboardViewMode.EMOJI)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.3.dp, colors.textSecondary.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GIF",
                            color = colors.textPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ToolboxIconItem(
                    active = viewMode == KeyboardViewMode.CLIPBOARD,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_clipboard"),
                    onClick = {
                        onHaptic()
                        onSelectViewMode(if (viewMode == KeyboardViewMode.CLIPBOARD) KeyboardViewMode.LETTERS else KeyboardViewMode.CLIPBOARD)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Clipboard History",
                        tint = if (viewMode == KeyboardViewMode.CLIPBOARD) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                ToolboxIconItem(
                    active = isTranslatorOpen || viewMode == KeyboardViewMode.TRANSLATOR,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_translator"),
                    onClick = {
                        onHaptic()
                        onToggleTranslator()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Offline Translator",
                        tint = if (isTranslatorOpen || viewMode == KeyboardViewMode.TRANSLATOR) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                ToolboxIconItem(
                    active = false,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_settings"),
                    onClick = {
                        onHaptic()
                        onOpenSettings()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                ToolboxIconItem(
                    active = viewMode == KeyboardViewMode.THEME_PICKER,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_themes"),
                    onClick = {
                        onHaptic()
                        onSelectViewMode(if (viewMode == KeyboardViewMode.THEME_PICKER) KeyboardViewMode.LETTERS else KeyboardViewMode.THEME_PICKER)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes & Material You",
                        tint = if (viewMode == KeyboardViewMode.THEME_PICKER) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                ToolboxIconItem(
                    active = viewMode == KeyboardViewMode.FANCY_FONTS,
                    accent = colors.accent,
                    modifier = Modifier.testTag("toolbox_fonts"),
                    onClick = {
                        onHaptic()
                        onSelectViewMode(
                            if (viewMode == KeyboardViewMode.FANCY_FONTS) KeyboardViewMode.LETTERS
                            else KeyboardViewMode.FANCY_FONTS
                        )
                    }
                ) {
                    Text(
                        text = "Aa",
                        color = if (viewMode == KeyboardViewMode.FANCY_FONTS) colors.accent else colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                ToolboxIconItem(
                    active = isIncognitoMode,
                    accent = Color(0xFF9C27B0),
                    modifier = Modifier.testTag("toolbox_incognito"),
                    onClick = {
                        onHaptic()
                        onToggleIncognito()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = if (isIncognitoMode) "Disable Incognito Mode" else "Enable Incognito Mode",
                        tint = if (isIncognitoMode) Color(0xFFE040FB) else colors.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

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
            }
        } else {
            // Suggestion Bar Mode with hairline dividers
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isRealPrediction) {
                                onHaptic()
                                onSuggestionClick(candidate)
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = candidate,
                            color = if (index == 0 && isRealPrediction) colors.accent else colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (index == 0 && isRealPrediction) FontWeight.Bold else FontWeight.Medium,
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
