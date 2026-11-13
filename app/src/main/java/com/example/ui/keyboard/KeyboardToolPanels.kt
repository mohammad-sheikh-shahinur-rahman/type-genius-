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
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardToolId
import com.example.model.KeyboardViewMode
import com.example.model.OneHandedMode

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
                    .height(58.dp),
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
                .height(186.dp),
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
                .height(56.dp),
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
            label = "Text editing",
            icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
            testTag = "tool_text_editing",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.TEXT_EDITOR) }
        ),
        ToolTile(
            id = KeyboardToolId.GEMINI_AI,
            label = "TypeGenius AI",
            icon = Icons.Default.AutoAwesome,
            badgeCount = liveGrammarIssueCount,
            testTag = "tool_gemini_ai",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.GEMINI_AI) }
        ),
        ToolTile(
            id = KeyboardToolId.STICKERS,
            label = "Stickers",
            icon = Icons.Default.Mood,
            testTag = "tool_stickers",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.STICKERS_KAOMOJI) }
        ),
        ToolTile(
            id = KeyboardToolId.ONE_HANDED,
            label = if (oneHandedMode == OneHandedMode.FULL) "One-hand" else oneHandedMode.label,
            icon = Icons.Default.Smartphone,
            active = oneHandedMode != OneHandedMode.FULL,
            testTag = "tool_one_handed",
            onClick = { onHaptic(); onCycleOneHanded() }
        ),
        ToolTile(
            id = KeyboardToolId.FANCY_FONTS,
            label = "Fancy Fonts",
            icon = Icons.Default.TextFormat,
            testTag = "tool_fancy_fonts",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.FANCY_FONTS) }
        ),
        ToolTile(
            id = KeyboardToolId.CLIPBOARD,
            label = "Clipboard",
            icon = Icons.Default.ContentPaste,
            testTag = "tool_clipboard",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.CLIPBOARD) }
        ),
        ToolTile(
            id = KeyboardToolId.TRANSLATOR,
            label = "Translate",
            icon = Icons.Default.Translate,
            testTag = "tool_translate",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.TRANSLATOR) }
        ),
        ToolTile(
            id = KeyboardToolId.THEMES,
            label = "Themes",
            icon = Icons.Default.Palette,
            testTag = "tool_themes",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.THEME_PICKER) }
        ),
        ToolTile(
            id = KeyboardToolId.INCOGNITO,
            label = "Incognito",
            icon = Icons.Default.VisibilityOff,
            active = isIncognitoMode,
            testTag = "tool_incognito",
            onClick = { onHaptic(); onToggleIncognito() }
        ),
        ToolTile(
            id = KeyboardToolId.FLOATING,
            label = if (floatingMode) "Docked" else "Floating",
            icon = Icons.Default.PictureInPictureAlt,
            active = floatingMode,
            testTag = "tool_floating",
            onClick = { onHaptic(); onToggleFloating() }
        ),
        ToolTile(
            id = KeyboardToolId.RESIZE,
            label = "Size (${heightPreset.label})",
            icon = Icons.Default.AspectRatio,
            active = heightPreset != KeyboardHeightPreset.NORMAL,
            testTag = "tool_resize",
            onClick = { onHaptic(); onCycleHeight() }
        ),
        ToolTile(
            id = KeyboardToolId.EMOJI,
            label = "Emoji",
            icon = Icons.Default.Mood,
            testTag = "tool_emoji",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.EMOJI) }
        ),
        ToolTile(
            id = KeyboardToolId.GIF,
            label = "GIF",
            icon = Icons.Default.Gif,
            testTag = "tool_gif",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.EMOJI) }
        ),
        ToolTile(
            id = KeyboardToolId.NUMPAD,
            label = "Numpad",
            icon = Icons.Default.Dialpad,
            testTag = "tool_numpad",
            onClick = { onHaptic(); onSelectViewMode(KeyboardViewMode.NUMPAD) }
        ),
        ToolTile(
            id = KeyboardToolId.SETTINGS,
            label = "Settings",
            icon = Icons.Default.Settings,
            testTag = "tool_settings",
            onClick = { onHaptic(); onOpenSettings() }
        ),
        ToolTile(
            id = KeyboardToolId.SHARE,
            label = "Share",
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
                    text = if (isDropTargetActive) "Drop here to remove from top bar" else "Hold & drag between bar and panel",
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
                        text = "Reset",
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

                                // Top-right badge toggles inclusion in Top Bar (backup to drag-and-drop)
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
                                        contentDescription = if (isInTopBar) "On Top Bar" else "Add to Top Bar",
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
