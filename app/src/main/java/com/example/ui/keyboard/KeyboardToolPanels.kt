package com.example.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BengaliCalendarUtils
import com.example.data.dictionary.TextTransformMode
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardToolId
import com.example.model.KeyboardViewMode
import com.example.model.OneHandedMode
import com.example.model.SoundProfile
import kotlin.math.roundToInt

/**
 * সুন্দর ও আধুনিক Material 3 নাম্বার প্যাড (বাংলা ↔ English সংখ্যা টগল + কারেন্সি/অপারেটর রিবন)।
 */
@Composable
fun NumpadPanel(
    colors: KeyboardColors,
    isBengaliDefault: Boolean = true,
    onKeyAction: (KeyAction) -> Unit,
    onBackToLetters: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var useBanglaDigits by remember(isBengaliDefault) { mutableStateOf(isBengaliDefault) }

    val quickSymbols = listOf("৳", "$", "%", "(", ")", "=", ",", ":")
    val bnGrid = listOf(
        listOf("১" to "1", "২" to "2", "৩" to "3", "÷" to null),
        listOf("৪" to "4", "৫" to "5", "৬" to "6", "×" to null),
        listOf("৭" to "7", "৮" to "8", "৯" to "9", "-" to null),
        listOf("." to null, "০" to "0", "০০" to "00", "+" to null)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Top Utility Ribbon: Bangla/English Script Toggle + Currency & Math Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accent.copy(alpha = 0.18f))
                    .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable {
                        onHaptic()
                        useBanglaDigits = !useBanglaDigits
                    }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("numpad_script_toggle")
            ) {
                Text(
                    text = if (useBanglaDigits) "১২৩ → 123" else "123 → ১২৩",
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickSymbols.forEach { sym ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                onKeyAction(KeyAction(sym, null, KeyType.CHARACTER))
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("numpad_quick_$sym")
                    ) {
                        Text(
                            text = sym,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Main 4x4 Digits + Right Action Column
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Left 4 Columns (3 digit cols + 1 operator col)
            Column(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                bnGrid.forEach { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        row.forEach { (bnLabel, enAlt) ->
                            val primary = if (useBanglaDigits || enAlt == null) bnLabel else enAlt
                            val sub = when {
                                enAlt == null -> null
                                useBanglaDigits -> enAlt
                                else -> bnLabel
                            }
                            val isOperator = enAlt == null && bnLabel != "."
                            val bgColor = if (isOperator) colors.keySpecialBackground else colors.keyBackground

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .shadow(1.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .testTag("numpad_$primary")
                                    .clickable {
                                        onHaptic()
                                        onKeyAction(KeyAction(primary, sub, KeyType.CHARACTER))
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = primary,
                                        color = if (isOperator) colors.accent else colors.textPrimary,
                                        fontSize = if (primary.length >= 2) 17.sp else 20.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (sub != null) {
                                        Text(
                                            text = sub,
                                            color = colors.textSecondary.copy(alpha = 0.68f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Action Column: Backspace, ABC/অক্ষর, Enter
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.keySpecialBackground)
                        .testTag("numpad_⌫")
                        .clickable {
                            onHaptic()
                            onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌫", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.keySpecialBackground)
                        .testTag("numpad_ABC")
                        .clickable {
                            onHaptic()
                            onBackToLetters()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("অক্ষর", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.enterKeyBackground)
                        .testTag("numpad_↵")
                        .clickable {
                            onHaptic()
                            onKeyAction(KeyAction("↵", null, KeyType.ENTER))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↵", color = colors.enterKeyText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickSettingsPanel(
    colors: KeyboardColors,
    heightPortraitPct: Int,
    soundEnabled: Boolean,
    soundVolume: Float,
    soundProfile: SoundProfile,
    oneHandedMode: OneHandedMode,
    floatingMode: Boolean,
    isIncognitoMode: Boolean,
    perAppSmartLanguageEnabled: Boolean,
    onHeightPctChange: (Int) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onSelectSoundProfile: (SoundProfile) -> Unit,
    onCycleOneHanded: () -> Unit,
    onToggleFloating: () -> Unit,
    onToggleIncognito: () -> Unit,
    onTogglePerAppSmartLang: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(266.dp)
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
                Text(
                    text = "দ্রুত উচ্চতা ও সাউন্ড সেটিংস",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            onOpenFullSettings()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("সব সেটিংস", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onHaptic(); onClose() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Keyboard Height Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.keyBackground)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("কীবোর্ডের উচ্চতা (Height)", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${BengaliCalendarUtils.toBengaliDigits(heightPortraitPct.toString())}%",
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = heightPortraitPct.toFloat().coerceIn(70f, 135f),
                onValueChange = { onHeightPctChange(it.roundToInt()) },
                valueRange = 70f..135f,
                colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                modifier = Modifier.height(28.dp).testTag("quick_height_slider")
            )
        }

        // Sound Volume Slider & Profiles
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.keyBackground)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
                    Text("কী-সাউন্ড ভলিউম", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (soundEnabled) "${BengaliCalendarUtils.toBengaliDigits((soundVolume * 100).roundToInt().toString())}%" else "বন্ধ",
                        color = colors.accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (soundEnabled) colors.accent else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                onToggleSound(!soundEnabled)
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("quick_sound_toggle")
                    ) {
                        Text(
                            text = if (soundEnabled) "চালু" else "বন্ধ",
                            color = if (soundEnabled) Color.White else colors.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Slider(
                value = if (soundEnabled) soundVolume else 0f,
                onValueChange = { v ->
                    if (!soundEnabled && v > 0.05f) onToggleSound(true)
                    onSoundVolumeChange(v)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                modifier = Modifier.height(28.dp).testTag("quick_sound_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SoundProfile.entries.forEach { profile ->
                    val selected = soundProfile == profile
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) colors.accent.copy(alpha = 0.22f) else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                if (!soundEnabled) onToggleSound(true)
                                onSelectSoundProfile(profile)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = profile.label.substringBefore(" ("),
                            color = if (selected) colors.accent else colors.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Ergonomic & Smart Automation Toggles Row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickModeChip(
                label = if (floatingMode) "ফ্লোটিং: চালু" else "ফ্লোটিং মোড",
                active = floatingMode,
                colors = colors,
                onClick = { onHaptic(); onToggleFloating() }
            )
            QuickModeChip(
                label = "এক হাতে (${oneHandedMode.label})",
                active = oneHandedMode != OneHandedMode.FULL,
                colors = colors,
                onClick = { onHaptic(); onCycleOneHanded() }
            )
            QuickModeChip(
                label = if (perAppSmartLanguageEnabled) "স্মার্ট অটো-ভাষা: চালু" else "স্মার্ট অটো-ভাষা",
                active = perAppSmartLanguageEnabled,
                colors = colors,
                onClick = { onHaptic(); onTogglePerAppSmartLang() }
            )
            QuickModeChip(
                label = if (isIncognitoMode) "গোপন মোড: চালু" else "গোপন মোড",
                active = isIncognitoMode,
                colors = colors,
                onClick = { onHaptic(); onToggleIncognito() }
            )
        }
    }
}

@Composable
private fun QuickModeChip(
    label: String,
    active: Boolean,
    colors: KeyboardColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) colors.accent.copy(alpha = 0.20f) else colors.keyBackground)
            .border(
                width = 1.dp,
                color = if (active) colors.accent else colors.keySpecialBackground,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (active) colors.accent else colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TextEditorPanel(
    colors: KeyboardColors,
    onMoveCursor: (dx: Int, dy: Int, select: Boolean) -> Unit,
    onMoveToStart: (select: Boolean) -> Unit,
    onMoveToEnd: (select: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit = {},
    onPaste: () -> Unit,
    onBackspace: () -> Unit,
    onTransformText: (TextTransformMode) -> Unit = {},
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .testTag("editor_left")
                    .clickable {
                        onHaptic()
                        onMoveCursor(-1, 0, selectMode)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "বামে নিন",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorIconTile(
                    icon = Icons.Default.KeyboardArrowUp,
                    description = "উপরে নিন",
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    onHaptic()
                    onMoveCursor(0, -1, selectMode)
                }

                EditorTextTile(
                    text = "নির্বাচন",
                    isHighlighted = selectMode,
                    colors = colors,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("editor_select_toggle")
                ) {
                    onHaptic()
                    selectMode = !selectMode
                }

                EditorIconTile(
                    icon = Icons.Default.KeyboardArrowDown,
                    description = "নিচে নিন",
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    onHaptic()
                    onMoveCursor(0, 1, selectMode)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .testTag("editor_right")
                    .clickable {
                        onHaptic()
                        onMoveCursor(1, 0, selectMode)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "ডানে নিন",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorTextTile(
                    text = "সব নির্বাচন",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_select_all")
                ) {
                    onHaptic()
                    onSelectAll()
                }
                EditorTextTile(
                    text = "কপি",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_copy")
                ) {
                    onHaptic()
                    onCopy()
                }
                EditorTextTile(
                    text = "কাট",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_cut")
                ) {
                    onHaptic()
                    onCut()
                }
                EditorTextTile(
                    text = "পেস্ট",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_paste")
                ) {
                    onHaptic()
                    onPaste()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "রূপান্তর:",
                color = colors.textSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            TextTransformMode.entries.forEach { mode ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            onTransformText(mode)
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("editor_transform_${mode.tagSuffix}")
                ) {
                    Text(
                        text = mode.label,
                        color = colors.accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorTextTile(
                text = "|< শুরু",
                isHighlighted = false,
                colors = colors,
                isSpecial = true,
                modifier = Modifier.weight(1.2f).fillMaxHeight().testTag("editor_line_start")
            ) {
                onHaptic()
                onMoveToStart(selectMode)
            }

            EditorTextTile(
                text = "শেষ >|",
                isHighlighted = false,
                colors = colors,
                isSpecial = true,
                modifier = Modifier.weight(1.2f).fillMaxHeight().testTag("editor_line_end")
            ) {
                onHaptic()
                onMoveToEnd(selectMode)
            }

            EditorTextTile(
                text = "⌫",
                isHighlighted = false,
                colors = colors,
                isSpecial = true,
                modifier = Modifier.weight(0.95f).fillMaxHeight().testTag("editor_backspace")
            ) {
                onHaptic()
                onBackspace()
            }
        }
    }
}

@Composable
fun MoreToolsPanel(
    colors: KeyboardColors,
    toolbarTools: List<KeyboardToolId>,
    oneHandedMode: OneHandedMode,
    heightPreset: KeyboardHeightPreset,
    floatingMode: Boolean,
    isIncognitoMode: Boolean = false,
    liveGrammarIssueCount: Int = 0,
    draggingTool: KeyboardToolId? = null,
    isDropTargetActive: Boolean = false,
    onPanelBoundsChanged: (Rect) -> Unit = {},
    onStartToolDrag: (KeyboardToolId, Offset) -> Unit = { _, _ -> },
    onDragToolMove: (Offset) -> Unit = {},
    onEndToolDrag: () -> Unit = {},
    onToggleToolInToolbar: (KeyboardToolId) -> Unit = {},
    onResetToolbarTools: () -> Unit = {},
    onSelectViewMode: (KeyboardViewMode) -> Unit,
    onOpenSettings: () -> Unit = {},
    onCycleOneHanded: () -> Unit,
    onCycleHeight: () -> Unit,
    onToggleFloating: () -> Unit,
    onToggleIncognito: () -> Unit = {},
    onShareApp: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class ToolTile(
        val id: KeyboardToolId,
        val label: String,
        val icon: ImageVector,
        val active: Boolean = false,
        val badgeCount: Int = 0,
        val testTag: String,
        val onClick: () -> Unit
    )

    val tools = listOf(
        ToolTile(
            id = KeyboardToolId.TEXT_EDITING,
            label = "টেক্সট এডিট",
            icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
            testTag = "tool_text_editing",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.TEXT_EDITOR) }
        ),
        ToolTile(
            id = KeyboardToolId.GEMINI_AI,
            label = "বাংলাবিদ AI",
            icon = Icons.Default.AutoAwesome,
            badgeCount = liveGrammarIssueCount,
            testTag = "tool_gemini_ai",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.GEMINI_AI) }
        ),
        ToolTile(
            id = KeyboardToolId.GIF,
            label = "GIF",
            icon = Icons.Default.Gif,
            testTag = "tool_gif",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.GIF) }
        ),
        ToolTile(
            id = KeyboardToolId.STICKERS,
            label = "স্টিকার",
            icon = Icons.Default.Mood,
            testTag = "tool_stickers",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.STICKERS_KAOMOJI) }
        ),
        ToolTile(
            id = KeyboardToolId.CLIPBOARD,
            label = "ক্লিপবোর্ড",
            icon = Icons.Default.ContentPaste,
            testTag = "tool_clipboard",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.CLIPBOARD) }
        ),
        ToolTile(
            id = KeyboardToolId.TRANSLATOR,
            label = "অনুবাদ",
            icon = Icons.Default.Translate,
            testTag = "tool_translate",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.TRANSLATOR) }
        ),
        ToolTile(
            id = KeyboardToolId.DEV_MATH,
            label = "কোড ও গণিত",
            icon = Icons.Default.Code,
            testTag = "tool_dev_math",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.DEV_MATH_SYMBOLS) }
        ),
        ToolTile(
            id = KeyboardToolId.CALCULATOR,
            label = "ক্যালকুলেটর",
            icon = Icons.Default.Calculate,
            testTag = "tool_calculator",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.CALCULATOR) }
        ),
        ToolTile(
            id = KeyboardToolId.UTILITIES,
            label = "তারিখ ও নোট",
            icon = Icons.AutoMirrored.Filled.EventNote,
            testTag = "tool_utilities",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.UTILITIES) }
        ),
        ToolTile(
            id = KeyboardToolId.FANCY_FONTS,
            label = "ফন্ট স্টুডিও",
            icon = Icons.Default.TextFormat,
            testTag = "tool_fancy_fonts",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.FANCY_FONTS) }
        ),
        ToolTile(
            id = KeyboardToolId.THEMES,
            label = "থিম",
            icon = Icons.Default.Palette,
            testTag = "tool_themes",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.THEME_PICKER) }
        ),
        ToolTile(
            id = KeyboardToolId.QUICK_SETTINGS,
            label = "উচ্চতা ও সাউন্ড",
            icon = Icons.Default.Tune,
            testTag = "tool_quick_settings",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.QUICK_SETTINGS) }
        ),
        ToolTile(
            id = KeyboardToolId.INCOGNITO,
            label = "গোপন মোড",
            icon = Icons.Default.VisibilityOff,
            active = isIncognitoMode,
            testTag = "tool_incognito",
            onClick = { onHaptic(); onToggleIncognito() }
        ),
        ToolTile(
            id = KeyboardToolId.ONE_HANDED,
            label = if (oneHandedMode == OneHandedMode.FULL) "এক হাতে" else oneHandedMode.label,
            icon = Icons.Default.Smartphone,
            active = oneHandedMode != OneHandedMode.FULL,
            testTag = "tool_one_handed",
            onClick = { onHaptic(); onCycleOneHanded() }
        ),
        ToolTile(
            id = KeyboardToolId.FLOATING,
            label = if (floatingMode) "ডক করুন" else "ফ্লোটিং",
            icon = Icons.Default.PictureInPictureAlt,
            active = floatingMode,
            testTag = "tool_floating",
            onClick = { onHaptic(); onToggleFloating() }
        ),
        ToolTile(
            id = KeyboardToolId.RESIZE,
            label = "সাইজ (${heightPreset.label})",
            icon = Icons.Default.AspectRatio,
            active = heightPreset != KeyboardHeightPreset.NORMAL,
            testTag = "tool_resize",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.QUICK_SETTINGS) }
        ),
        ToolTile(
            id = KeyboardToolId.EMOJI,
            label = "ইমোজি",
            icon = Icons.Default.Mood,
            testTag = "tool_emoji",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.EMOJI) }
        ),
        ToolTile(
            id = KeyboardToolId.NUMPAD,
            label = "নাম্বার প্যাড",
            icon = Icons.Default.Dialpad,
            testTag = "tool_numpad",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.NUMPAD) }
        ),
        ToolTile(
            id = KeyboardToolId.SETTINGS,
            label = "সেটিংস",
            icon = Icons.Default.Settings,
            testTag = "tool_settings",
            onClick = { onHaptic(); onOpenSettings() }
        ),
        ToolTile(
            id = KeyboardToolId.SHARE,
            label = "শেয়ার",
            icon = Icons.Default.Share,
            testTag = "tool_share",
            onClick = { onHaptic(); onShareApp() }
        )
    )

    val panelBg by animateColorAsState(
        targetValue = if (isDropTargetActive) colors.accent.copy(alpha = 0.08f) else colors.background,
        animationSpec = tween(150),
        label = "panelDropBg"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> onPanelBoundsChanged(coords.boundsInRoot()) }
            .clip(RoundedCornerShape(12.dp))
            .background(panelBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val cols = if (maxWidth >= 520.dp) 5 else 4
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDropTargetActive) "টপ বার থেকে সরাতে এখানে ছাড়ুন" else "টুলবারে রাখতে চেপে ধরে টেনে নিন",
                    color = if (isDropTargetActive) colors.accent else colors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = if (isDropTargetActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            onResetToolbarTools()
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("tools_reset_toolbar_button")
                ) {
                    Text(
                        text = "রিসেট",
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            tools.chunked(cols).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        val isInTopBar = item.id in toolbarTools
                        val isBeingDragged = draggingTool == item.id
                        var tileBounds by remember(item.id) { mutableStateOf(Rect.Zero) }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .alpha(if (isBeingDragged) 0.35f else 1f)
                                .onGloballyPositioned { tileBounds = it.boundsInRoot() }
                                .pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { localOffset ->
                                            onHaptic()
                                            onStartToolDrag(
                                                item.id,
                                                Offset(tileBounds.left + localOffset.x, tileBounds.top + localOffset.y)
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            onDragToolMove(
                                                Offset(tileBounds.left + change.position.x, tileBounds.top + change.position.y)
                                            )
                                        },
                                        onDragEnd = { onEndToolDrag() },
                                        onDragCancel = { onEndToolDrag() }
                                    )
                                }
                                .testTag(item.testTag)
                                .clickable(onClick = item.onClick),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (item.active) colors.accent.copy(alpha = 0.22f)
                                        else colors.keyBackground
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (item.active) colors.accent else colors.textPrimary,
                                    modifier = Modifier.size(21.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isInTopBar) colors.accent else colors.keySpecialBackground
                                        )
                                        .clickable {
                                            onHaptic()
                                            onToggleToolInToolbar(item.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isInTopBar) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = if (isInTopBar) "টপ বারে আছে" else "টপ বারে যোগ করুন",
                                        tint = if (isInTopBar) Color.White else colors.textSecondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }

                                if (item.badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.accent)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                                            color = Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.label,
                                color = colors.textSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(cols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun InKeyboardThemePanel(
    colors: KeyboardColors,
    currentTheme: KeyboardTheme,
    isSystemDark: Boolean,
    onSelectTheme: (KeyboardTheme) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("থিম ও Material You", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onHaptic(); onClose() }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = colors.textSecondary)
            }
        }

        Column(
            modifier = Modifier
                .height(178.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyboardTheme.entries.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { theme ->
                        val preview = KeyboardThemeData.getColors(theme, context, isSystemDark)
                        val selected = theme == currentTheme
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(preview.background)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) colors.accent else preview.keySpecialBackground,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onHaptic()
                                    onSelectTheme(theme)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(preview.accent)
                                )
                                Text(
                                    text = theme.displayName.substringBefore(" ("),
                                    color = preview.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = "নির্বাচিত", tint = preview.accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EditorTextTile(
    text: String,
    isHighlighted: Boolean,
    colors: KeyboardColors,
    isSpecial: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isHighlighted -> colors.enterKeyBackground
                    isSpecial -> colors.keySpecialBackground
                    else -> colors.keyBackground
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isHighlighted) colors.enterKeyText else colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EditorIconTile(
    icon: ImageVector,
    description: String,
    colors: KeyboardColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.keyBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
    }
}
