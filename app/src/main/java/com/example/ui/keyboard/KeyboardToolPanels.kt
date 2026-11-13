package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardViewMode
import com.example.model.OneHandedMode
import kotlinx.coroutines.launch

@Composable
fun NumpadPanel(
    colors: KeyboardColors,
    onKeyAction: (KeyAction) -> Unit,
    onBackToLetters: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3", "-"),
        listOf("4", "5", "6", "+"),
        listOf("7", "8", "9", "⌫"),
        listOf("ABC", ",", "0", ".", "↵")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                row.forEach { label ->
                    val type = when (label) {
                        "⌫" -> KeyType.BACKSPACE
                        "↵" -> KeyType.ENTER
                        "ABC" -> KeyType.MODE_SWITCH
                        else -> KeyType.CHARACTER
                    }
                    val isEnter = type == KeyType.ENTER
                    val isSpecial = type != KeyType.CHARACTER
                    Box(
                        modifier = Modifier
                            .weight(if (label == "0") 1.3f else 1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isEnter -> colors.enterKeyBackground
                                    isSpecial -> colors.keySpecialBackground
                                    else -> colors.keyBackground
                                }
                            )
                            .testTag("numpad_$label")
                            .clickable {
                                onHaptic()
                                if (label == "ABC") onBackToLetters()
                                else onKeyAction(KeyAction(label, null, type))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isEnter) colors.enterKeyText else colors.textPrimary,
                            fontSize = if (label == "ABC") 14.sp else 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Matches Screenshot 2 (`Text editing`):
 * Col 1 (tall): [<]
 * Col 2: [^], [Select], [v]
 * Col 3 (tall): [>]
 * Col 4: [Select all], [Copy], [Paste]
 * Bottom Row: [|<], [>|], [⌫]
 */
@Composable
fun TextEditorPanel(
    colors: KeyboardColors,
    onMoveCursor: (dx: Int, dy: Int, select: Boolean) -> Unit,
    onMoveToStart: (select: Boolean) -> Unit,
    onMoveToEnd: (select: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onBackspace: () -> Unit,
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
        // 4-Column Main Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Col 1: Tall Left Arrow (<)
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
                    contentDescription = "Move Cursor Left",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Col 2: Up, Select, Down
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorIconTile(
                    icon = Icons.Default.KeyboardArrowUp,
                    description = "Move Cursor Up",
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    onHaptic()
                    onMoveCursor(0, -1, selectMode)
                }

                EditorTextTile(
                    text = "Select",
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
                    description = "Move Cursor Down",
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    onHaptic()
                    onMoveCursor(0, 1, selectMode)
                }
            }

            // Col 3: Tall Right Arrow (>)
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
                    contentDescription = "Move Cursor Right",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Col 4: Select all, Copy, Paste
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorTextTile(
                    text = "Select all",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_select_all")
                ) {
                    onHaptic()
                    onSelectAll()
                }
                EditorTextTile(
                    text = "Copy",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_copy")
                ) {
                    onHaptic()
                    onCopy()
                }
                EditorTextTile(
                    text = "Paste",
                    isHighlighted = false,
                    colors = colors,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_paste")
                ) {
                    onHaptic()
                    onPaste()
                }
            }
        }

        // Bottom Row: |< , >| , ⌫ (Matches Screenshot 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorTextTile(
                text = "|<",
                isHighlighted = false,
                colors = colors,
                isSpecial = true,
                modifier = Modifier.weight(1.2f).fillMaxHeight().testTag("editor_line_start")
            ) {
                onHaptic()
                onMoveToStart(selectMode)
            }

            EditorTextTile(
                text = ">|",
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

/**
 * Matches Screenshot 3 Tools Drawer Grid:
 * "Hold and drag to customize" + Rounded tile grid with icon + label underneath.
 */
@Composable
fun MoreToolsPanel(
    colors: KeyboardColors,
    oneHandedMode: OneHandedMode,
    heightPreset: KeyboardHeightPreset,
    floatingMode: Boolean,
    onSelectViewMode: (KeyboardViewMode) -> Unit,
    onCycleOneHanded: () -> Unit,
    onCycleHeight: () -> Unit,
    onToggleFloating: () -> Unit,
    onShareApp: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class ToolTile(
        val label: String,
        val icon: ImageVector,
        val active: Boolean = false,
        val testTag: String,
        val onClick: () -> Unit
    )

    val tools = listOf(
        ToolTile(
            label = if (oneHandedMode == OneHandedMode.FULL) "One-handed" else oneHandedMode.label,
            icon = Icons.Default.Smartphone,
            active = oneHandedMode != OneHandedMode.FULL,
            testTag = "tool_one_handed",
            onClick = { onHaptic(); onCycleOneHanded() }
        ),
        ToolTile(
            label = "Text editing",
            icon = Icons.Default.FormatIndentIncrease,
            testTag = "tool_text_editing",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.TEXT_EDITOR) }
        ),
        ToolTile(
            label = "Share",
            icon = Icons.Default.Share,
            testTag = "tool_share",
            onClick = { onHaptic(); onShareApp() }
        ),
        ToolTile(
            label = "Translate",
            icon = Icons.Default.Translate,
            testTag = "tool_translate",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.TRANSLATOR) }
        ),
        ToolTile(
            label = if (floatingMode) "Docked" else "Floating",
            icon = Icons.Default.PictureInPictureAlt,
            active = floatingMode,
            testTag = "tool_floating",
            onClick = { onHaptic(); onToggleFloating() }
        ),
        ToolTile(
            label = "Resize (${heightPreset.label})",
            icon = Icons.Default.AspectRatio,
            active = heightPreset != KeyboardHeightPreset.NORMAL,
            testTag = "tool_resize",
            onClick = { onHaptic(); onCycleHeight() }
        ),
        ToolTile(
            label = "Emoji",
            icon = Icons.Default.Mood,
            testTag = "tool_emoji",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.EMOJI) }
        ),
        ToolTile(
            label = "Numpad",
            icon = Icons.Default.Dialpad,
            testTag = "tool_numpad",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.NUMPAD) }
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val cols = if (maxWidth >= 520.dp) 4 else 4
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tap a tool to launch or toggle",
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            tools.chunked(cols).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(item.testTag)
                                .clickable(onClick = item.onClick),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (item.active) colors.accent.copy(alpha = 0.25f)
                                        else colors.keyBackground
                                    )
                                    .then(
                                        if (item.active) Modifier.border(1.dp, colors.accent, RoundedCornerShape(14.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (item.active) colors.accent else colors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = item.label,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
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
            Text("Themes & Material You", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onHaptic(); onClose() }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Themes", tint = colors.textSecondary)
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
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = preview.accent, modifier = Modifier.size(16.dp))
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
            fontSize = 13.sp,
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
