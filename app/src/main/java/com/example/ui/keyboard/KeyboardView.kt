package com.example.ui.keyboard

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Gif
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.CustomFontManager
import com.example.data.KeyboardFontPreset
import com.example.data.SettingsManager
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.UnicodeTextStyler
import com.example.data.local.ClipboardItem
import com.example.data.local.SmartClipInfo
import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardToolId
import com.example.model.KeyboardViewMode
import com.example.model.LanguageId
import com.example.model.OneHandedMode
import com.example.model.PanelTextInputTarget
import com.example.model.SoundProfile
import com.example.model.SwipeSensitivity
import com.example.model.VibrationStrength
import com.example.service.KeySoundEngine
import com.example.service.VoiceInputController
import java.io.File
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
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
    activeFancyStyle: FancyFontStyle = FancyFontStyle.NORMAL,
    lastTypedWordPreview: String = "",
    canUndoDelete: Boolean = false,
    isAutoIncognitoActive: Boolean = false,
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
    onCopyClipboardItem: (ClipboardItem) -> Unit = {},
    onTogglePinClipboardItem: (ClipboardItem) -> Unit,
    onDeleteClipboardItem: (ClipboardItem) -> Unit,
    onClearClipboard: () -> Unit,
    onClearEverythingClipboard: () -> Unit = {},
    onMoveCursor: (dx: Int, dy: Int, select: Boolean) -> Unit,
    onMoveToStart: (select: Boolean) -> Unit,
    onMoveToEnd: (select: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit = {},
    onPaste: () -> Unit,
    onDeleteWordBackward: () -> Unit = {},
    onUndoDelete: () -> Unit = {},
    onGlideCommitted: (trail: List<Pair<Float, Float>>, keyCenters: Map<String, Pair<Float, Float>>) -> Unit = { _, _ -> },
    onSelectFancyStyle: (FancyFontStyle) -> Unit = {},
    onTransformLastWord: (FancyFontStyle) -> Unit = {},
    onSendFancyStyleCard: (FancyFontStyle, HandwritingPaperTheme, HandwritingCardFormat, String) -> Unit = { _, _, _, _ -> },
    onOpenHandwritingImport: () -> Unit = {},
    onInsertTextSnippet: (String) -> Unit = {},
    handwritingLiveText: String = "",
    aiSourceText: String = "",
    canUndoAiEdit: Boolean = false,
    liveGrammarIssueCount: Int = 0,
    smartClipSuggestion: SmartClipInfo? = null,
    onSmartClipClick: (SmartClipInfo) -> Unit = {},
    onDismissSmartClip: () -> Unit = {},
    onRefreshAiSourceText: () -> String = { "" },
    onApplyAiText: (String) -> Unit = {},
    onUndoAiText: () -> Unit = {},
    panelInputTarget: PanelTextInputTarget = PanelTextInputTarget.NONE,
    clipboardSearchQuery: String = "",
    gifSearchQuery: String = "",
    customStickerText: String = "অস্থির হইছে বস!",
    onSetPanelInputTarget: (PanelTextInputTarget) -> Unit = {},
    onClearPanelSearchQuery: (PanelTextInputTarget) -> Unit = {},
    onCustomStickerTextChange: (String) -> Unit = {},
    onSendMediaFile: (file: File, mimeType: String, shareOnly: Boolean, fallbackText: String) -> Unit = { _, _, _, _ -> },
    voiceController: VoiceInputController? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val isSystemDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    val isSwipeEnabled by settingsManager.isSpacebarSwipeEnabled.collectAsState(initial = true)
    val swipeSensitivity by settingsManager.swipeSensitivity.collectAsState(initial = SwipeSensitivity.MEDIUM)
    val isHapticEnabled by settingsManager.isHapticEnabled.collectAsState(initial = true)
    val vibrationStrength by settingsManager.vibrationStrength.collectAsState(initial = VibrationStrength.MEDIUM)
    val showLanguageOnSpace by settingsManager.showActiveLanguageOnSpacebar.collectAsState(initial = true)
    val longPressLangPicker by settingsManager.longPressLanguageSelector.collectAsState(initial = true)
    val showAltLangKey by settingsManager.showAlternativeLanguageKey.collectAsState(initial = true)
    val currentTheme by settingsManager.keyboardTheme.collectAsState(initial = KeyboardTheme.MATERIAL_YOU)
    val numberRowEnabled by settingsManager.numberRowEnabled.collectAsState(initial = false)
    val oneHandedMode by settingsManager.oneHandedMode.collectAsState(initial = OneHandedMode.FULL)
    val heightPreset by settingsManager.keyboardHeightPreset.collectAsState(initial = KeyboardHeightPreset.NORMAL)
    val floatingMode by settingsManager.floatingMode.collectAsState(initial = false)
    val floatingOffsetXSaved by settingsManager.floatingOffsetX.collectAsState(initial = 0f)
    val floatingOffsetYSaved by settingsManager.floatingOffsetY.collectAsState(initial = 0f)
    val soundOnKeyPress by settingsManager.soundOnKeyPress.collectAsState(initial = false)
    val soundVolume by settingsManager.soundVolume.collectAsState(initial = 0.75f)
    val soundProfile by settingsManager.soundProfile.collectAsState(initial = SoundProfile.SYSTEM)
    val otpAutoExpireMinutes by settingsManager.otpAutoExpireMinutes.collectAsState(initial = 3)
    val perAppSmartLanguageEnabled by settingsManager.perAppSmartLanguageEnabled.collectAsState(initial = true)
    val translatorSrc by settingsManager.translatorSourceCode.collectAsState(initial = "bn")
    val translatorTgt by settingsManager.translatorTargetCode.collectAsState(initial = "en")
    val geminiApiKey by settingsManager.geminiApiKey.collectAsState(initial = "")
    val wallpaperPath by settingsManager.wallpaperPath.collectAsState(initial = "")
    val wallpaperDimAlpha by settingsManager.wallpaperDimAlpha.collectAsState(initial = 0.42f)
    val keyBackgroundAlpha by settingsManager.keyBackgroundAlpha.collectAsState(initial = 0.78f)
    val isIncognitoMode by settingsManager.isIncognitoMode.collectAsState(initial = false)
    val isGlideTypingEnabled by settingsManager.isGlideTypingEnabled.collectAsState(initial = true)
    val isBackspaceSwipeEnabled by settingsManager.isBackspaceSwipeEnabled.collectAsState(initial = true)
    val largeNumberRow by settingsManager.largeNumberRow.collectAsState(initial = false)
    val hideLongPressHints by settingsManager.hideLongPressHints.collectAsState(initial = false)
    val heightPortraitPct by settingsManager.keyboardHeightPortrait.collectAsState(initial = 100)
    val heightLandscapePct by settingsManager.keyboardHeightLandscape.collectAsState(initial = 100)
    val oneHandedWidthPortraitPct by settingsManager.oneHandedWidthPortrait.collectAsState(initial = 85)
    val oneHandedWidthLandscapePct by settingsManager.oneHandedWidthLandscape.collectAsState(initial = 40)
    val toolbarTools by settingsManager.toolbarTools.collectAsState(initial = KeyboardToolId.DEFAULT_TOOLBAR)
    val keyboardFontId by settingsManager.keyboardFontId.collectAsState(initial = KeyboardFontPreset.ATMA_HANDWRITING.id)
    val customFontPath by settingsManager.customFontPath.collectAsState(initial = "")
    val customFontName by settingsManager.customFontName.collectAsState(initial = "")
    val customAiPrompts by settingsManager.customAiPrompts.collectAsState(initial = emptyList())
    val notepadDraft by settingsManager.notepadDraft.collectAsState(initial = "")
    val userTemplates by settingsManager.userTemplates.collectAsState(initial = SettingsManager.DEFAULT_USER_TEMPLATES)
    val keyPopupEnabled by settingsManager.keyPopupEnabled.collectAsState(initial = true)
    val spacebarCursorScrubEnabled by settingsManager.spacebarCursorScrubEnabled.collectAsState(initial = true)
    val avroJuktakkhorBarEnabled by settingsManager.avroJuktakkhorBarEnabled.collectAsState(initial = true)
    val handwritingSignature by settingsManager.handwritingSignature.collectAsState(initial = "")
    val activeKeyboardFontFamily = remember(keyboardFontId, customFontPath, activeFancyStyle) {
        val effectiveId = activeFancyStyle.handwritingPresetId() ?: keyboardFontId
        CustomFontManager.getFontFamily(context, effectiveId, customFontPath)
    }
    var selectedPaperTheme by remember { mutableStateOf(HandwritingPaperTheme.NOTEBOOK) }
    var selectedCardFormat by remember { mutableStateOf(HandwritingCardFormat.CHAT_CARD) }

    val wallpaperBitmap = remember(wallpaperPath) {
        if (wallpaperPath.isNotBlank()) {
            try {
                val file = File(wallpaperPath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            } catch (_: Throwable) {
                null
            }
        } else null
    }

    val baseColors = remember(currentTheme, isSystemDark) {
        KeyboardThemeData.getColors(currentTheme, context, isSystemDark)
    }

    val colors = remember(baseColors, wallpaperBitmap, keyBackgroundAlpha) {
        if (wallpaperBitmap != null) {
            baseColors.copy(
                background = Color.Transparent,
                keyBackground = Color(0xFF1F1F24).copy(alpha = keyBackgroundAlpha),
                keySpecialBackground = Color(0xFF2D2E35).copy(alpha = (keyBackgroundAlpha + 0.08f).coerceAtMost(0.95f)),
                keyPressedBackground = baseColors.accent.copy(alpha = 0.45f),
                textPrimary = Color.White,
                textSecondary = Color(0xFFD6D8E0),
                suggestionBackground = Color(0xFF121216).copy(alpha = 0.75f),
                pillKeyBackground = Color(0xFF2D2E35).copy(alpha = (keyBackgroundAlpha + 0.08f).coerceAtMost(0.95f))
            )
        } else {
            baseColors
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var translatorPickerTarget by remember { mutableStateOf<TranslatorPickerTarget?>(null) }
    var isTranslatorLiveMode by remember { mutableStateOf(true) }
    var isVoiceTypingActive by remember { mutableStateOf(false) }
    var isToolboxExpanded by remember { mutableStateOf(true) }

    val controllerSessionActive = voiceController?.isSessionActive == true
    LaunchedEffect(controllerSessionActive) {
        isVoiceTypingActive = controllerSessionActive
    }

    // Cross-zone Drag & Drop state between Top Bar and More Tools Panel
    var rootOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var topBarBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val topBarItemBounds = remember { mutableStateMapOf<KeyboardToolId, Rect>() }
    var panelBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var draggingTool by remember { mutableStateOf<KeyboardToolId?>(null) }
    var dragOriginIsTopBar by remember { mutableStateOf(false) }
    var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }

    val dragZonePaddingPx = with(density) { 14.dp.toPx() }
    val isOverTopBar = draggingTool != null && topBarBoundsInRoot != Rect.Zero &&
        dragPointerInRoot.x in (topBarBoundsInRoot.left - dragZonePaddingPx)..(topBarBoundsInRoot.right + dragZonePaddingPx) &&
        dragPointerInRoot.y in (topBarBoundsInRoot.top - dragZonePaddingPx)..(topBarBoundsInRoot.bottom + dragZonePaddingPx)
    val isOverPanel = draggingTool != null &&
        viewMode == KeyboardViewMode.MORE_TOOLS &&
        panelBoundsInRoot != Rect.Zero &&
        dragPointerInRoot.x in panelBoundsInRoot.left..panelBoundsInRoot.right &&
        dragPointerInRoot.y in (panelBoundsInRoot.top + 2f)..panelBoundsInRoot.bottom

    fun finishToolDragDrop() {
        val tool = draggingTool ?: return
        when {
            isOverPanel && dragOriginIsTopBar -> {
                if (tool in toolbarTools && toolbarTools.size > 1) {
                    val updated = toolbarTools.filterNot { it == tool }
                    coroutineScope.launch { settingsManager.setToolbarTools(updated) }
                }
            }
            isOverTopBar -> {
                val withoutTool = toolbarTools.filterNot { it == tool }
                val insertIdx = withoutTool.count { existing ->
                    val rect = topBarItemBounds[existing]
                    rect != null && rect.center.x < dragPointerInRoot.x
                }.coerceIn(0, withoutTool.size)
                val updated = withoutTool.toMutableList().apply { add(insertIdx, tool) }
                coroutineScope.launch { settingsManager.setToolbarTools(updated) }
            }
        }
        draggingTool = null
    }

    LaunchedEffect(isTypingActive) {
        if (isTypingActive && viewMode == KeyboardViewMode.LETTERS) {
            isToolboxExpanded = false
        }
    }

    val vibrator = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    fun haptic() {
        if (!isHapticEnabled || vibrationStrength == VibrationStrength.OFF) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        vibrationStrength.durationMs,
                        vibrationStrength.amplitude
                    )
                )
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (_: Exception) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val containerAlignment = when (oneHandedMode) {
        OneHandedMode.LEFT -> Alignment.CenterStart
        OneHandedMode.RIGHT -> Alignment.CenterEnd
        OneHandedMode.FULL, OneHandedMode.SPLIT -> Alignment.Center
    }

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(floatingOffsetXSaved, floatingOffsetYSaved, floatingMode) {
        if (floatingMode) {
            dragOffsetX = floatingOffsetXSaved
            dragOffsetY = floatingOffsetYSaved
        } else {
            dragOffsetX = 0f
            dragOffsetY = 0f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> rootOriginInRoot = coords.positionInRoot() }
            .then(
                if (floatingMode) {
                    Modifier
                        .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                } else {
                    Modifier.padding(bottom = 12.dp)
                }
            )
            .then(
                if (floatingMode) {
                    Modifier
                        .shadow(14.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                } else {
                    Modifier
                }
            )
            .background(colors.background),
        contentAlignment = containerAlignment
    ) {
        val isNarrow = maxWidth < 350.dp
        val isWide = maxWidth >= 600.dp
        val isSplitMode = oneHandedMode == OneHandedMode.SPLIT
        val splitGap = if (isWide) 64.dp else 30.dp
        val widthFraction = when {
            floatingMode -> 1f
            oneHandedMode == OneHandedMode.LEFT || oneHandedMode == OneHandedMode.RIGHT -> {
                val pct = if (isWide) oneHandedWidthLandscapePct else oneHandedWidthPortraitPct
                (pct / 100f).coerceIn(0.40f, 1.0f)
            }
            else -> 1f
        }
        val heightScale = (if (isWide) heightLandscapePct else heightPortraitPct) / 100f
        val scaledRowDp = heightPreset.rowHeightDp * heightScale
        val responsiveRowHeight = when {
            isNarrow -> (scaledRowDp - 3f).dp
            isWide -> (scaledRowDp + 4f).dp
            else -> scaledRowDp.dp
        }
        val responsiveGap = if (isNarrow) 2.dp else 3.dp

        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = wallpaperDimAlpha))
            )
        }

        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = activeKeyboardFontFamily)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .widthIn(max = if (isWide) 840.dp else 680.dp)
                .padding(horizontal = 2.dp, vertical = 4.dp)
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

            // 0.5 Live Handwriting Output Bar when a Handwriting Font is active
            if (activeFancyStyle.isHandwritingFont && viewMode == KeyboardViewMode.LETTERS && !isTranslatorOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(selectedPaperTheme.bgStart))
                        .border(1.dp, colors.accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("handwriting_live_output_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = handwritingLiveText.ifBlank { "হাতের লেখায় পাঠাতে লিখুন..." },
                            fontFamily = activeKeyboardFontFamily,
                            color = if (handwritingLiveText.isBlank()) {
                                Color(selectedPaperTheme.inkColor).copy(alpha = 0.5f)
                            } else {
                                Color(selectedPaperTheme.inkColor)
                            },
                            fontSize = 15.5.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            HandwritingPaperTheme.entries.forEach { paper ->
                                val isPaperSelected = selectedPaperTheme == paper
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (isPaperSelected) colors.accent
                                            else Color(selectedPaperTheme.inkColor).copy(alpha = 0.12f)
                                        )
                                        .clickable {
                                            haptic()
                                            selectedPaperTheme = paper
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = paper.label,
                                        color = if (isPaperSelected) Color.White else Color(selectedPaperTheme.inkColor),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            HandwritingCardFormat.entries.forEach { fmt ->
                                val isFmtSelected = selectedCardFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (isFmtSelected) Color(0xFF6A1B9A)
                                            else Color(selectedPaperTheme.inkColor).copy(alpha = 0.10f)
                                        )
                                        .clickable {
                                            haptic()
                                            selectedCardFormat = fmt
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("handwriting_format_${fmt.id}")
                                ) {
                                    Text(
                                        text = fmt.label,
                                        color = if (isFmtSelected) Color.White else Color(selectedPaperTheme.inkColor),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.accent)
                            .clickable {
                                haptic()
                                onSendFancyStyleCard(activeFancyStyle, selectedPaperTheme, selectedCardFormat, handwritingSignature)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("handwriting_send_image_btn")
                    ) {
                        Text(
                            text = if (selectedCardFormat == HandwritingCardFormat.PORTRAIT_LETTER) "✉️ চিঠি" else "✍️ পাঠান",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 0.75 Avro Juktakkhor Quick-Hint Bar when Avro Phonetic is active
            if (activeLanguage == LanguageId.BANGLA_PHONETIC &&
                viewMode == KeyboardViewMode.LETTERS &&
                !isTranslatorOpen &&
                avroJuktakkhorBarEnabled
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("avro_juktakkhor_hint_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(colors.accent.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "যুক্তাক্ষর",
                                color = colors.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        KeyboardLayouts.avroJuktakkhorHints.forEach { hint ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.keySpecialBackground)
                                    .clickable {
                                        haptic()
                                        onInsertTextSnippet(hint.glyph)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                    .testTag("avro_hint_${hint.avroCode}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = hint.glyph,
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = hint.avroCode,
                                    color = colors.textSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.keySpecialBackground)
                            .clickable {
                                haptic()
                                coroutineScope.launch { settingsManager.setAvroJuktakkhorBarEnabled(false) }
                            }
                            .testTag("avro_hint_close_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "যুক্তাক্ষর বার লুকান",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // 1. Top Bar (Toolbox / Suggestions / Voice / Translator Listening / Text Editing header)
            KeyboardTopBar(
                colors = colors,
                activeLanguage = activeLanguage,
                viewMode = viewMode,
                suggestions = suggestions,
                toolbarTools = toolbarTools,
                isToolboxExpanded = isToolboxExpanded,
                isVoiceTypingActive = isVoiceTypingActive,
                isTranslatorOpen = isTranslatorOpen,
                isTranslatorVoiceActive = isTranslatorVoiceActive,
                isIncognitoMode = isIncognitoMode,
                isAutoIncognitoMode = isAutoIncognitoActive,
                oneHandedMode = oneHandedMode,
                floatingMode = floatingMode,
                heightPreset = heightPreset,
                liveGrammarIssueCount = liveGrammarIssueCount,
                canUndoDelete = canUndoDelete,
                smartClipSuggestion = smartClipSuggestion,
                activeFancyStyle = activeFancyStyle,
                draggingTool = draggingTool,
                isDragTargetActive = isOverTopBar,
                voiceController = voiceController,
                onSmartClipClick = onSmartClipClick,
                onDismissSmartClip = onDismissSmartClip,
                onTopBarBoundsChanged = { topBarBoundsInRoot = it },
                onToolItemBoundsChanged = { tool, rect -> topBarItemBounds[tool] = rect },
                onStartToolDrag = { tool, startRootPos ->
                    draggingTool = tool
                    dragOriginIsTopBar = true
                    dragPointerInRoot = startRootPos
                },
                onDragToolMove = { moveRootPos -> dragPointerInRoot = moveRootPos },
                onEndToolDrag = { finishToolDragDrop() },
                onToggleToolbox = { isToolboxExpanded = !isToolboxExpanded },
                onToggleIncognito = {
                    coroutineScope.launch { settingsManager.setIncognitoMode(!isIncognitoMode) }
                },
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
                onUndoDelete = onUndoDelete,
                onResetFancyStyle = { onSelectFancyStyle(FancyFontStyle.NORMAL) },
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
                    } else if (voiceController?.isSessionActive == true) {
                        voiceController.stopListening()
                        isVoiceTypingActive = false
                    } else {
                        isVoiceTypingActive = true
                        voiceController?.startListening(
                            languageCode = activeLanguage.code,
                            onPermissionRequired = { onOpenSettings() }
                        ) { spoken ->
                            val clean = spoken.trim()
                            if (clean.isNotEmpty()) {
                                val styled = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                                    UnicodeTextStyler.styleText(clean, activeFancyStyle)
                                } else clean
                                onKeyAction(KeyAction("$styled ", null, KeyType.CHARACTER))
                            }
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
                            val clean = spoken.trim()
                            if (clean.isNotEmpty()) {
                                val styled = if (activeFancyStyle != FancyFontStyle.NORMAL) {
                                    UnicodeTextStyler.styleText(clean, activeFancyStyle)
                                } else clean
                                onKeyAction(KeyAction("$styled ", null, KeyType.CHARACTER))
                            }
                        }
                        isVoiceTypingActive = voiceController?.isSessionActive == true
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
                    val isSearchTarget = panelInputTarget == PanelTextInputTarget.CLIPBOARD_SEARCH
                    ClipboardPanel(
                        items = clipboardItems,
                        colors = colors,
                        searchQuery = clipboardSearchQuery,
                        isSearchActive = isSearchTarget,
                        otpExpireMinutes = otpAutoExpireMinutes,
                        onToggleSearch = {
                            haptic()
                            onSetPanelInputTarget(
                                if (isSearchTarget) PanelTextInputTarget.NONE else PanelTextInputTarget.CLIPBOARD_SEARCH
                            )
                        },
                        onClearSearch = {
                            onClearPanelSearchQuery(PanelTextInputTarget.CLIPBOARD_SEARCH)
                        },
                        onCycleOtpExpireMinutes = {
                            val next = when (otpAutoExpireMinutes) {
                                3 -> 5
                                5 -> 0
                                else -> 3
                            }
                            coroutineScope.launch { settingsManager.setOtpAutoExpireMinutes(next) }
                        },
                        onItemSelected = { text ->
                            haptic()
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onClipboardItemClick(text)
                        },
                        onCopyItem = { item ->
                            haptic()
                            onCopyClipboardItem(item)
                        },
                        onTogglePin = { item ->
                            haptic()
                            onTogglePinClipboardItem(item)
                        },
                        onDeleteItem = { item ->
                            haptic()
                            onDeleteClipboardItem(item)
                        },
                        onClearAll = {
                            haptic()
                            onClearClipboard()
                        },
                        onClearEverything = {
                            haptic()
                            onClearEverythingClipboard()
                        },
                        onClose = {
                            haptic()
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        }
                    )
                    if (isSearchTarget) {
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactPanelInlineKeyboard(
                            activeLanguage = activeLanguage,
                            isShifted = isShifted,
                            isCapsLock = isCapsLock,
                            colors = colors,
                            rowHeight = (responsiveRowHeight * 0.78f).coerceAtLeast(38.dp),
                            gap = responsiveGap,
                            hideSecondaryHint = hideLongPressHints,
                            onKeyAction = { action ->
                                haptic()
                                onKeyAction(action)
                            },
                            onDone = {
                                haptic()
                                onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            }
                        )
                    }
                }

                KeyboardViewMode.GIF -> {
                    val isGifSearchTarget = panelInputTarget == PanelTextInputTarget.GIF_SEARCH
                    GifPanel(
                        colors = colors,
                        searchQuery = gifSearchQuery,
                        isSearchActive = isGifSearchTarget,
                        onToggleSearch = {
                            haptic()
                            onSetPanelInputTarget(
                                if (isGifSearchTarget) PanelTextInputTarget.NONE else PanelTextInputTarget.GIF_SEARCH
                            )
                        },
                        onClearSearch = {
                            onClearPanelSearchQuery(PanelTextInputTarget.GIF_SEARCH)
                        },
                        onSendGifFile = { file, shareOnly, fallback ->
                            haptic()
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onSendMediaFile(file, "image/gif", shareOnly, fallback)
                        },
                        onClose = {
                            haptic()
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        },
                        onHaptic = { haptic() }
                    )
                    if (isGifSearchTarget) {
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactPanelInlineKeyboard(
                            activeLanguage = activeLanguage,
                            isShifted = isShifted,
                            isCapsLock = isCapsLock,
                            colors = colors,
                            rowHeight = (responsiveRowHeight * 0.78f).coerceAtLeast(38.dp),
                            gap = responsiveGap,
                            hideSecondaryHint = hideLongPressHints,
                            onKeyAction = { action ->
                                haptic()
                                onKeyAction(action)
                            },
                            onDone = {
                                haptic()
                                onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            }
                        )
                    }
                }

                KeyboardViewMode.NUMPAD -> {
                    val isBnFamily = activeLanguage in listOf(
                        LanguageId.BANGLA_PROBHAT,
                        LanguageId.BANGLA_PHONETIC,
                        LanguageId.BANGLA_NATIONAL,
                        LanguageId.BANGLA_UNIBIJOY,
                        LanguageId.SYLHETI,
                        LanguageId.ASSAMESE
                    )
                    NumpadPanel(
                        colors = colors,
                        isBengaliDefault = isBnFamily,
                        onKeyAction = onKeyAction,
                        onBackToLetters = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.QUICK_SETTINGS -> {
                    QuickSettingsPanel(
                        colors = colors,
                        heightPortraitPct = heightPortraitPct,
                        soundEnabled = soundOnKeyPress,
                        soundVolume = soundVolume,
                        soundProfile = soundProfile,
                        oneHandedMode = oneHandedMode,
                        floatingMode = floatingMode,
                        isIncognitoMode = isIncognitoMode,
                        perAppSmartLanguageEnabled = perAppSmartLanguageEnabled,
                        onHeightPctChange = { pct ->
                            coroutineScope.launch {
                                settingsManager.setKeyboardHeightPortrait(pct)
                                settingsManager.setKeyboardHeightLandscape(pct)
                            }
                        },
                        onToggleSound = { enabled ->
                            coroutineScope.launch { settingsManager.setSoundOnKeyPress(enabled) }
                            if (enabled) KeySoundEngine.preview(context, soundProfile)
                        },
                        onSoundVolumeChange = { vol ->
                            coroutineScope.launch { settingsManager.setSoundVolume(vol) }
                        },
                        onSelectSoundProfile = { profile ->
                            coroutineScope.launch { settingsManager.setSoundProfile(profile) }
                            KeySoundEngine.preview(context, profile)
                        },
                        onCycleOneHanded = {
                            val entries = OneHandedMode.entries
                            val next = entries[(entries.indexOf(oneHandedMode) + 1) % entries.size]
                            coroutineScope.launch { settingsManager.setOneHandedMode(next) }
                        },
                        onToggleFloating = {
                            coroutineScope.launch { settingsManager.setFloatingMode(!floatingMode) }
                        },
                        onToggleIncognito = {
                            coroutineScope.launch { settingsManager.setIncognitoMode(!isIncognitoMode) }
                        },
                        onTogglePerAppSmartLang = {
                            coroutineScope.launch {
                                settingsManager.setPerAppSmartLanguageEnabled(!perAppSmartLanguageEnabled)
                            }
                        },
                        onOpenFullSettings = onOpenSettings,
                        onClose = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.DEV_MATH_SYMBOLS -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Dev & Math",
                                    color = colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            KeyboardLayouts.devMathQuickSnippets.forEach { snippet ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keySpecialBackground)
                                        .clickable {
                                            haptic()
                                            onInsertTextSnippet(snippet)
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                        .testTag("dev_math_snippet_$snippet")
                                ) {
                                    Text(
                                        text = snippet,
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        KeyboardLayouts.devMathSymbols.forEachIndexed { rIdx, rowKeys ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(responsiveRowHeight)
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.spacedBy(responsiveGap)
                            ) {
                                rowKeys.forEachIndexed { kIdx, key ->
                                    KeyView(
                                        keyAction = key,
                                        colors = colors,
                                        hideSecondaryHint = hideLongPressHints,
                                        alignPopupEnd = kIdx >= rowKeys.size / 2,
                                        isCompactRow = rIdx < 2,
                                        modifier = Modifier.weight(key.weight),
                                        onClick = {
                                            haptic()
                                            onKeyAction(key)
                                        },
                                        onLongClick = { sec ->
                                            haptic()
                                            onKeyAction(KeyAction(sec, null, KeyType.CHARACTER))
                                        },
                                        onBackspaceRepeat = if (key.type == KeyType.BACKSPACE) {
                                            {
                                                haptic()
                                                onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE))
                                            }
                                        } else null
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(responsiveRowHeight),
                            horizontalArrangement = Arrangement.spacedBy(responsiveGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeyView(
                                keyAction = KeyAction("অক্ষর", null, KeyType.MODE_SWITCH, weight = 1.25f),
                                colors = colors,
                                shape = RoundedCornerShape(10.dp),
                                customBgColor = colors.keySpecialBackground,
                                modifier = Modifier.weight(1.25f),
                                onClick = {
                                    haptic()
                                    onSelectViewMode(KeyboardViewMode.LETTERS)
                                }
                            )
                            KeyView(
                                keyAction = KeyAction("()", "{}", KeyType.CHARACTER, weight = 0.9f),
                                colors = colors,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.9f),
                                onClick = {
                                    haptic()
                                    onInsertTextSnippet("()")
                                },
                                onLongClick = {
                                    haptic()
                                    onInsertTextSnippet("{}")
                                }
                            )
                            SpacebarComponent(
                                activeLanguage = activeLanguage,
                                enabledLanguages = enabledLanguages,
                                isSwipeEnabled = isSwipeEnabled,
                                swipeSensitivity = swipeSensitivity,
                                isHapticEnabled = isHapticEnabled,
                                showLanguageLabel = false,
                                isLongPressEnabled = false,
                                backgroundColor = colors.keyBackground,
                                textColor = colors.textPrimary,
                                accentColor = colors.accent,
                                onTapSpace = { onKeyAction(KeyAction(" ", null, KeyType.SPACEBAR)) },
                                onSwipeNextLanguage = onSwipeNextLanguage,
                                onSwipePrevLanguage = onSwipePrevLanguage,
                                onLongPressLanguagePicker = {},
                                modifier = Modifier.weight(2.8f)
                            )
                            KeyView(
                                keyAction = KeyAction(";", ":", KeyType.CHARACTER, weight = 0.85f),
                                colors = colors,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.85f),
                                onClick = {
                                    haptic()
                                    onKeyAction(KeyAction(";", null, KeyType.CHARACTER))
                                },
                                onLongClick = { sec ->
                                    haptic()
                                    onKeyAction(KeyAction(sec, null, KeyType.CHARACTER))
                                }
                            )
                            KeyView(
                                keyAction = KeyAction("↵", null, KeyType.ENTER, weight = 1.3f),
                                colors = colors,
                                isEnterKey = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f),
                                onClick = {
                                    haptic()
                                    onKeyAction(KeyAction("↵", null, KeyType.ENTER))
                                }
                            )
                        }
                    }
                }

                KeyboardViewMode.TEXT_EDITOR -> {
                    TextEditorPanel(
                        colors = colors,
                        onMoveCursor = onMoveCursor,
                        onMoveToStart = onMoveToStart,
                        onMoveToEnd = onMoveToEnd,
                        onSelectAll = onSelectAll,
                        onCopy = onCopy,
                        onCut = onCut,
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

                KeyboardViewMode.FANCY_FONTS -> {
                    val isBengaliLang = activeLanguage in listOf(
                        LanguageId.BANGLA_PROBHAT,
                        LanguageId.BANGLA_PHONETIC,
                        LanguageId.BANGLA_NATIONAL,
                        LanguageId.BANGLA_UNIBIJOY,
                        LanguageId.SYLHETI,
                        LanguageId.ASSAMESE
                    )
                    FancyFontsPanel(
                        colors = colors,
                        activeStyle = activeFancyStyle,
                        sampleWord = handwritingLiveText.ifBlank { lastTypedWordPreview },
                        customFontPath = customFontPath,
                        customFontName = customFontName,
                        isBengaliActive = isBengaliLang,
                        onSelectLiveStyle = onSelectFancyStyle,
                        onTransformLastWord = onTransformLastWord,
                        onSendStyleCard = { style ->
                            onSendFancyStyleCard(style, selectedPaperTheme, selectedCardFormat, handwritingSignature)
                        },
                        onOpenHandwritingImport = onOpenHandwritingImport,
                        onInsertSnippet = onInsertTextSnippet,
                        onClose = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.GEMINI_AI -> {
                    TypeGeniusAiPanel(
                        colors = colors,
                        activeLanguage = activeLanguage,
                        geminiApiKey = geminiApiKey,
                        sourceText = aiSourceText,
                        canUndo = canUndoAiEdit,
                        customAiPrompts = customAiPrompts,
                        onRefreshSourceText = onRefreshAiSourceText,
                        onApplyText = onApplyAiText,
                        onUndo = onUndoAiText,
                        onOpenSettings = onOpenSettings,
                        onClose = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.CALCULATOR -> {
                    CalculatorAndConverterPanel(
                        colors = colors,
                        onInsertResult = { res ->
                            onInsertTextSnippet(res)
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        },
                        onClose = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.UTILITIES -> {
                    QuickToolsNotepadPanel(
                        colors = colors,
                        notepadDraft = notepadDraft,
                        userTemplates = userTemplates,
                        onInsertSnippet = { snippet ->
                            onInsertTextSnippet(snippet)
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        },
                        onSaveNotepadDraft = { draft ->
                            coroutineScope.launch { settingsManager.setNotepadDraft(draft) }
                        },
                        onAddTemplate = { tmpl ->
                            coroutineScope.launch { settingsManager.addUserTemplate(tmpl) }
                        },
                        onRemoveTemplate = { tmpl ->
                            coroutineScope.launch { settingsManager.removeUserTemplate(tmpl) }
                        },
                        onGetFieldTextForTts = onRefreshAiSourceText,
                        onClose = { onSelectViewMode(KeyboardViewMode.LETTERS) },
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.STICKERS_KAOMOJI -> {
                    val isStickerTextTarget = panelInputTarget == PanelTextInputTarget.STICKER_CUSTOM_TEXT
                    StickersAndKaomojiPanel(
                        colors = colors,
                        customStickerText = customStickerText,
                        isEditingCustomText = isStickerTextTarget,
                        onCustomStickerTextChange = onCustomStickerTextChange,
                        onToggleCustomTextKeyboard = {
                            haptic()
                            onSetPanelInputTarget(
                                if (isStickerTextTarget) PanelTextInputTarget.NONE else PanelTextInputTarget.STICKER_CUSTOM_TEXT
                            )
                        },
                        onInsertText = { text ->
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onInsertTextSnippet(text)
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        },
                        onSendStickerImage = { file, shareOnly ->
                            haptic()
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onSendMediaFile(file, "image/png", shareOnly, customStickerText)
                        },
                        onClose = {
                            onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            onSelectViewMode(KeyboardViewMode.LETTERS)
                        },
                        onHaptic = { haptic() }
                    )
                    if (isStickerTextTarget) {
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactPanelInlineKeyboard(
                            activeLanguage = activeLanguage,
                            isShifted = isShifted,
                            isCapsLock = isCapsLock,
                            colors = colors,
                            rowHeight = (responsiveRowHeight * 0.78f).coerceAtLeast(38.dp),
                            gap = responsiveGap,
                            hideSecondaryHint = hideLongPressHints,
                            onKeyAction = { action ->
                                haptic()
                                onKeyAction(action)
                            },
                            onDone = {
                                haptic()
                                onSetPanelInputTarget(PanelTextInputTarget.NONE)
                            }
                        )
                    }
                }

                KeyboardViewMode.MORE_TOOLS -> {
                    MoreToolsPanel(
                        colors = colors,
                        toolbarTools = toolbarTools,
                        oneHandedMode = oneHandedMode,
                        heightPreset = heightPreset,
                        floatingMode = floatingMode,
                        isIncognitoMode = isIncognitoMode,
                        liveGrammarIssueCount = liveGrammarIssueCount,
                        draggingTool = draggingTool,
                        isDropTargetActive = isOverPanel && dragOriginIsTopBar,
                        onPanelBoundsChanged = { panelBoundsInRoot = it },
                        onStartToolDrag = { tool, startRootPos ->
                            draggingTool = tool
                            dragOriginIsTopBar = false
                            dragPointerInRoot = startRootPos
                        },
                        onDragToolMove = { moveRootPos -> dragPointerInRoot = moveRootPos },
                        onEndToolDrag = { finishToolDragDrop() },
                        onToggleToolInToolbar = { tool ->
                            val updated = if (tool in toolbarTools) {
                                if (toolbarTools.size > 1) toolbarTools.filterNot { it == tool } else toolbarTools
                            } else {
                                toolbarTools + tool
                            }
                            coroutineScope.launch { settingsManager.setToolbarTools(updated) }
                        },
                        onResetToolbarTools = {
                            coroutineScope.launch { settingsManager.setToolbarTools(KeyboardToolId.DEFAULT_TOOLBAR) }
                        },
                        onSelectViewMode = onSelectViewMode,
                        onOpenSettings = onOpenSettings,
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
                        onToggleIncognito = {
                            coroutineScope.launch { settingsManager.setIncognitoMode(!isIncognitoMode) }
                        },
                        onShareApp = onShareApp,
                        onHaptic = { haptic() }
                    )
                }

                KeyboardViewMode.LETTERS,
                KeyboardViewMode.SYMBOLS_1,
                KeyboardViewMode.SYMBOLS_2 -> {
                    if (numberRowEnabled && viewMode == KeyboardViewMode.LETTERS) {
                        val nums = KeyboardLayouts.getNumberRow(activeLanguage)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (largeNumberRow) responsiveRowHeight else (responsiveRowHeight - 4.dp))
                                .zIndex(2f),
                            horizontalArrangement = Arrangement.spacedBy(responsiveGap)
                        ) {
                            val numSplit = if (isSplitMode) nums.size / 2 else nums.size
                            nums.take(numSplit).forEachIndexed { idx, key ->
                                KeyView(
                                    keyAction = key,
                                    colors = colors,
                                    hideSecondaryHint = hideLongPressHints,
                                    showTapMagnifier = keyPopupEnabled,
                                    alignPopupEnd = idx >= nums.size / 2,
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
                            if (isSplitMode) {
                                Spacer(modifier = Modifier.width(splitGap))
                                nums.drop(numSplit).forEachIndexed { idx, key ->
                                    KeyView(
                                        keyAction = key,
                                        colors = colors,
                                        hideSecondaryHint = hideLongPressHints,
                                        showTapMagnifier = keyPopupEnabled,
                                        alignPopupEnd = true,
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
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    val rows = when (viewMode) {
                        KeyboardViewMode.LETTERS -> KeyboardLayouts.getAlphabetRows(activeLanguage, isShifted || isCapsLock)
                        KeyboardViewMode.SYMBOLS_1 -> KeyboardLayouts.getSymbols1(activeLanguage)
                        KeyboardViewMode.SYMBOLS_2 -> KeyboardLayouts.symbols2
                        else -> KeyboardLayouts.getAlphabetRows(activeLanguage, false)
                    }

                    var activePopupRow by remember { mutableStateOf(-1) }
                    val keyCenters = remember(activeLanguage, viewMode, isSplitMode, isShifted, isCapsLock) {
                        mutableMapOf<String, Pair<Float, Float>>()
                    }
                    var rowsOriginInRoot by remember { mutableStateOf(Offset.Zero) }
                    var glideTrail by remember { mutableStateOf<List<Offset>>(emptyList()) }
                    val glideActivationPx = with(density) { 42.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                rowsOriginInRoot = coords.positionInRoot()
                            }
                            .pointerInput(isGlideTypingEnabled, viewMode, activeLanguage) {
                                if (!isGlideTypingEnabled || viewMode != KeyboardViewMode.LETTERS) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    val localTrail = mutableListOf(down.position)
                                    var totalDist = 0f
                                    var isGlideGesture = false

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break

                                        val prev = localTrail.last()
                                        val stepDist = hypot(change.position.x - prev.x, change.position.y - prev.y)
                                        if (stepDist > 4f) {
                                            totalDist += stepDist
                                            localTrail.add(change.position)
                                            if (totalDist > glideActivationPx) {
                                                isGlideGesture = true
                                                glideTrail = localTrail.toList()
                                            }
                                        }
                                    }

                                    if (isGlideGesture && localTrail.size >= 3) {
                                        haptic()
                                        onGlideCommitted(
                                            localTrail.map { it.x to it.y },
                                            keyCenters.toMap()
                                        )
                                    }
                                    glideTrail = emptyList()
                                }
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            rows.forEachIndexed { rowIndex, rowKeys ->
                                val splitAt = if (isSplitMode) (rowKeys.size + 1) / 2 else rowKeys.size
                                val isDenseRow = rowKeys.size >= 11
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(responsiveRowHeight)
                                        .zIndex(if (activePopupRow == rowIndex) 50f else 1f)
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(responsiveGap)
                                ) {
                                    @Composable
                                    fun renderKey(key: KeyAction, keyIdx: Int) {
                                        val isShiftActive = key.type == KeyType.SHIFT && (isShifted || isCapsLock)
                                        KeyView(
                                            keyAction = key,
                                            colors = colors,
                                            isHighlight = isShiftActive,
                                            hideSecondaryHint = hideLongPressHints,
                                            showTapMagnifier = keyPopupEnabled,
                                            alignPopupEnd = keyIdx >= rowKeys.size / 2,
                                            isCompactRow = isDenseRow,
                                            activeFontStyle = if (viewMode == KeyboardViewMode.LETTERS) activeFancyStyle else FancyFontStyle.NORMAL,
                                            modifier = Modifier
                                                .weight(key.weight)
                                                .then(
                                                    if (viewMode == KeyboardViewMode.LETTERS && key.type == KeyType.CHARACTER && key.label.length == 1) {
                                                        Modifier.onGloballyPositioned { coords ->
                                                            val pos = coords.positionInRoot()
                                                            val cx = pos.x + coords.size.width / 2f - rowsOriginInRoot.x
                                                            val cy = pos.y + coords.size.height / 2f - rowsOriginInRoot.y
                                                            keyCenters[key.label] = cx to cy
                                                        }
                                                    } else Modifier
                                                ),
                                            onClick = {
                                                haptic()
                                                onKeyAction(key)
                                            },
                                            onLongClick = { secondaryAction ->
                                                haptic()
                                                when (secondaryAction) {
                                                    "Copy" -> onCopy()
                                                    "Paste" -> onPaste()
                                                    else -> onKeyAction(KeyAction(secondaryAction, null, KeyType.CHARACTER))
                                                }
                                            },
                                            onBackspaceRepeat = if (key.type == KeyType.BACKSPACE) {
                                                {
                                                    haptic()
                                                    onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE))
                                                }
                                            } else null,
                                            onBackspaceSwipeWord = if (key.type == KeyType.BACKSPACE && isBackspaceSwipeEnabled) {
                                                {
                                                    haptic()
                                                    onDeleteWordBackward()
                                                }
                                            } else null,
                                            onPopupStateChange = { visible ->
                                                activePopupRow = if (visible) rowIndex else if (activePopupRow == rowIndex) -1 else activePopupRow
                                            }
                                        )
                                    }

                                    rowKeys.take(splitAt).forEachIndexed { idx, key -> renderKey(key, idx) }
                                    if (isSplitMode && splitAt < rowKeys.size) {
                                        Spacer(modifier = Modifier.width(splitGap))
                                        rowKeys.drop(splitAt).forEachIndexed { idx, key -> renderKey(key, splitAt + idx) }
                                    }
                                }
                            }
                        }

                        if (glideTrail.size >= 2) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(60f)
                            ) {
                                val path = Path().apply {
                                    moveTo(glideTrail.first().x, glideTrail.first().y)
                                    for (i in 1 until glideTrail.size) {
                                        lineTo(glideTrail[i].x, glideTrail[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.linearGradient(
                                        listOf(colors.accent.copy(alpha = 0.55f), colors.accent)
                                    ),
                                    style = Stroke(
                                        width = 12f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
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
                            LanguageId.BANGLA_UNIBIJOY,
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
                            Box(
                                modifier = Modifier
                                    .weight(0.92f)
                                    .fillMaxHeight()
                                    .shadow(1.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.keySpecialBackground)
                                    .testTag("bottom_language_switch_key")
                                    .semantics {
                                        contentDescription = "Switch language (${activeLanguage.displayName})"
                                    }
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
                                Image(
                                    painter = painterResource(id = R.drawable.ic_tg_logo),
                                    contentDescription = "TypeGenius Switch Language",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
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
                            isCursorScrubEnabled = spacebarCursorScrubEnabled,
                            onCursorScrubStep = { step -> onMoveCursor(step, 0, false) },
                            modifier = Modifier.weight(if (isSplitMode) 1.6f else 3.2f)
                        )

                        if (isSplitMode) {
                            Spacer(modifier = Modifier.width(splitGap))
                            SpacebarComponent(
                                activeLanguage = activeLanguage,
                                enabledLanguages = enabledLanguages,
                                isSwipeEnabled = isSwipeEnabled,
                                swipeSensitivity = swipeSensitivity,
                                isHapticEnabled = isHapticEnabled,
                                showLanguageLabel = false,
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
                                isCursorScrubEnabled = spacebarCursorScrubEnabled,
                                onCursorScrubStep = { step -> onMoveCursor(step, 0, false) },
                                modifier = Modifier.weight(1.6f)
                            )
                        }

                        val punctuationChar = when (activeLanguage) {
                            LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.BANGLA_NATIONAL,
                            LanguageId.BANGLA_UNIBIJOY, LanguageId.SYLHETI, LanguageId.ASSAMESE,
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
                                modifier = Modifier.size(24.dp)
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

            if (floatingMode) {
                // Draggable Floating Keyboard Handle Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.suggestionBackground)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-140f, 140f)
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-520f, 120f)
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        settingsManager.setFloatingOffsets(dragOffsetX, dragOffsetY)
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("floating_drag_handle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(36.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "ফ্লোটিং কীবোর্ড সরান",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ড্র্যাগ করে সরান",
                            color = colors.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.keySpecialBackground)
                            .clickable {
                                haptic()
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                coroutineScope.launch {
                                    settingsManager.setFloatingOffsets(0f, 0f)
                                    settingsManager.setFloatingMode(false)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("floating_dock_button")
                    ) {
                        Text(
                            text = "ডক করুন",
                            color = colors.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Reserve clean system navigation bar space with generous padding so OS single chin bar never touches or overlaps keys
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(16.dp)
                )
            }
        }
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
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(lang.nativeName, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            if (lang.isRtl) {
                                                Text(
                                                    text = "RTL",
                                                    color = colors.accent,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text("${lang.displayName} • ${lang.scriptLabel}", color = colors.textSecondary, fontSize = 11.sp)
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

        // Floating drag-and-drop preview pill
        val activeDragged = draggingTool
        if (activeDragged != null) {
            val localX = (dragPointerInRoot.x - rootOriginInRoot.x).roundToInt()
            val localY = (dragPointerInRoot.y - rootOriginInRoot.y).roundToInt()
            val actionHint = when {
                isOverTopBar && activeDragged !in toolbarTools -> "Add to bar"
                isOverTopBar -> "Move on bar"
                isOverPanel && dragOriginIsTopBar -> "Remove from bar"
                else -> activeDragged.label
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.accent,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((localX - 110).coerceAtLeast(8), (localY - 120).coerceAtLeast(4)) }
                    .zIndex(260f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = toolIconForGhost(activeDragged),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = actionHint,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun toolIconForGhost(tool: KeyboardToolId) = when (tool) {
    KeyboardToolId.TEXT_EDITING -> Icons.AutoMirrored.Filled.FormatIndentIncrease
    KeyboardToolId.GEMINI_AI -> Icons.Default.AutoAwesome
    KeyboardToolId.EMOJI -> Icons.Default.Mood
    KeyboardToolId.STICKERS -> Icons.Default.Mood
    KeyboardToolId.GIF -> Icons.Default.Gif
    KeyboardToolId.CLIPBOARD -> Icons.Default.ContentPaste
    KeyboardToolId.TRANSLATOR -> Icons.Default.Translate
    KeyboardToolId.CALCULATOR -> Icons.Default.Calculate
    KeyboardToolId.UTILITIES -> Icons.AutoMirrored.Filled.EventNote
    KeyboardToolId.FANCY_FONTS -> Icons.Default.TextFormat
    KeyboardToolId.THEMES -> Icons.Default.Palette
    KeyboardToolId.INCOGNITO -> Icons.Default.VisibilityOff
    KeyboardToolId.SETTINGS -> Icons.Default.Settings
    KeyboardToolId.ONE_HANDED -> Icons.Default.Smartphone
    KeyboardToolId.FLOATING -> Icons.Default.PictureInPictureAlt
    KeyboardToolId.RESIZE -> Icons.Default.AspectRatio
    KeyboardToolId.NUMPAD -> Icons.Default.Dialpad
    KeyboardToolId.DEV_MATH -> Icons.Default.Code
    KeyboardToolId.QUICK_SETTINGS -> Icons.Default.Tune
    KeyboardToolId.SHARE -> Icons.Default.Share
}

@Composable
private fun CompactPanelInlineKeyboard(
    activeLanguage: LanguageId,
    isShifted: Boolean,
    isCapsLock: Boolean,
    colors: KeyboardColors,
    rowHeight: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    hideSecondaryHint: Boolean,
    onKeyAction: (KeyAction) -> Unit,
    onDone: () -> Unit
) {
    val rows = remember(activeLanguage, isShifted, isCapsLock) {
        KeyboardLayouts.getAlphabetRows(activeLanguage, isShifted || isCapsLock)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.suggestionBackground.copy(alpha = 0.65f))
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                rowKeys.forEachIndexed { idx, key ->
                    KeyView(
                        keyAction = key,
                        colors = colors,
                        isHighlight = key.type == KeyType.SHIFT && (isShifted || isCapsLock),
                        hideSecondaryHint = hideSecondaryHint,
                        alignPopupEnd = idx >= rowKeys.size / 2,
                        isCompactRow = true,
                        modifier = Modifier.weight(key.weight),
                        onClick = { onKeyAction(key) },
                        onLongClick = { sec ->
                            onKeyAction(KeyAction(sec, null, KeyType.CHARACTER))
                        },
                        onBackspaceRepeat = if (key.type == KeyType.BACKSPACE) {
                            { onKeyAction(KeyAction("⌫", null, KeyType.BACKSPACE)) }
                        } else null
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((rowHeight * 0.92f).coerceAtLeast(36.dp)),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyView(
                keyAction = KeyAction("🌐 ${activeLanguage.spacebarLabel}", null, KeyType.LANGUAGE_SWITCH, weight = 1.4f),
                colors = colors,
                customBgColor = colors.keySpecialBackground,
                modifier = Modifier.weight(1.4f),
                onClick = { onKeyAction(KeyAction("🌐", null, KeyType.LANGUAGE_SWITCH)) }
            )
            KeyView(
                keyAction = KeyAction("স্পেস", null, KeyType.SPACEBAR, weight = 3.0f),
                colors = colors,
                customBgColor = colors.keyBackground,
                modifier = Modifier.weight(3.0f),
                onClick = { onKeyAction(KeyAction(" ", null, KeyType.SPACEBAR)) }
            )
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .shadow(1.dp, RoundedCornerShape(9.dp))
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.enterKeyBackground)
                    .clickable(onClick = onDone)
                    .testTag("panel_inline_keyboard_done"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "সম্পন্ন ✓",
                    color = colors.enterKeyText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatBengaliSignForDisplay(raw: String): String {
    return when (raw) {
        "\u09CD\u09B0" -> "\u25CC\u09CD\u09B0"
        "\u09CD\u09AF" -> "\u25CC\u09CD\u09AF"
        "\u09CD\u09AF\u09BE" -> "\u25CC\u09CD\u09AF\u09BE"
        "\u09B0\u09CD" -> "\u09B0\u09CD\u25CC"
        "\u09CD\u09AC" -> "\u25CC\u09CD\u09AC"
        "\u09CD\u09AE" -> "\u25CC\u09CD\u09AE"
        "\u09CD\u09A8" -> "\u25CC\u09CD\u09A8"
        "\u09CD\u09B2" -> "\u25CC\u09CD\u09B2"
        "া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ", "্", "ং", "ঃ", "ঁ" -> "\u25CC$raw"
        else -> raw
    }
}

@Composable
fun KeyView(
    keyAction: KeyAction,
    colors: KeyboardColors,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    isEnterKey: Boolean = false,
    hideSecondaryHint: Boolean = false,
    showTapMagnifier: Boolean = true,
    alignPopupEnd: Boolean = false,
    isCompactRow: Boolean = false,
    activeFontStyle: FancyFontStyle = FancyFontStyle.NORMAL,
    shape: Shape = RoundedCornerShape(9.dp),
    customBgColor: Color? = null,
    onClick: () -> Unit,
    onLongClick: ((String) -> Unit)? = null,
    onBackspaceRepeat: (() -> Unit)? = null,
    onBackspaceSwipeWord: (() -> Unit)? = null,
    onPopupStateChange: ((Boolean) -> Unit)? = null
) {
    val view = LocalView.current
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }
    var showPreviewPopup by remember { mutableStateOf(false) }
    var selectedVariantIdx by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val dragAwayThresholdPx = with(density) { 18.dp.toPx() }
    val swipeDeleteStepPx = with(density) { 30.dp.toPx() }
    val variantStepPx = with(density) { 38.dp.toPx() }

    val variants = remember(keyAction) {
        if (keyAction.extraVariants.isNotEmpty()) {
            keyAction.extraVariants
        } else {
            listOfNotNull(keyAction.secondaryLabel)
        }
    }

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

    val displayPrimary = remember(keyAction.label, keyAction.type, activeFontStyle) {
        val base = formatBengaliSignForDisplay(keyAction.label)
        if (activeFontStyle != FancyFontStyle.NORMAL &&
            !activeFontStyle.isDecoratorFrame &&
            keyAction.type == KeyType.CHARACTER &&
            keyAction.label.length == 1 &&
            base == keyAction.label
        ) {
            UnicodeTextStyler.styleSingleInput(keyAction.label, activeFontStyle)
        } else {
            base
        }
    }
    val displaySecondary = remember(keyAction.secondaryLabel) {
        keyAction.secondaryLabel?.let { formatBengaliSignForDisplay(it) }
    }
    val hasSecondary = displaySecondary != null && !hideSecondaryHint
    val showTapBubble = isPressed && !showPreviewPopup && showTapMagnifier &&
        keyAction.type == KeyType.CHARACTER && displayPrimary.length <= 3

    LaunchedEffect(showPreviewPopup, showTapBubble) {
        onPopupStateChange?.invoke(showPreviewPopup || showTapBubble)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(if (showPreviewPopup) 100f else if (showTapBubble) 80f else 0f)
            .testTag("key_${keyAction.label}")
            .semantics {
                contentDescription = if (keyAction.secondaryLabel != null) {
                    "${keyAction.label}, long press for ${keyAction.secondaryLabel}"
                } else {
                    keyAction.label
                }
            }
            .pointerInput(keyAction, variants, alignPopupEnd, onBackspaceSwipeWord != null, onBackspaceRepeat != null) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    selectedVariantIdx = 0
                    var isLongClickFired = false
                    var draggedAway = false
                    var swipeDeleteConsumed = false
                    var lastSwipeX = down.position.x
                    val longPressTimeoutMs = 260L

                    var job: kotlinx.coroutines.Job? = null
                    if (keyAction.type == KeyType.BACKSPACE && onBackspaceRepeat != null) {
                        job = coroutineScope.launch {
                            delay(360L)
                            while (isPressed && !swipeDeleteConsumed) {
                                isLongClickFired = true
                                onBackspaceRepeat()
                                delay(70L)
                            }
                        }
                    } else if (variants.isNotEmpty() && onLongClick != null) {
                        job = coroutineScope.launch {
                            delay(longPressTimeoutMs)
                            if (isPressed && !draggedAway) {
                                isLongClickFired = true
                                selectedVariantIdx = 0
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
                            if (!draggedAway && !swipeDeleteConsumed) {
                                if (isLongClickFired && variants.isNotEmpty()) {
                                    val chosen = variants.getOrElse(selectedVariantIdx) { variants.first() }
                                    onLongClick?.invoke(chosen)
                                } else if (!isLongClickFired) {
                                    onClick()
                                }
                            }
                            change.consume()
                            break
                        }

                        if (keyAction.type == KeyType.BACKSPACE && onBackspaceSwipeWord != null) {
                            val dx = change.position.x - lastSwipeX
                            if (dx <= -swipeDeleteStepPx) {
                                swipeDeleteConsumed = true
                                job?.cancel()
                                lastSwipeX = change.position.x
                                onBackspaceSwipeWord()
                            }
                        } else if (keyAction.type == KeyType.CHARACTER) {
                            if (showPreviewPopup && variants.size > 1) {
                                val dx = change.position.x - down.position.x
                                val idxOffset = if (alignPopupEnd) {
                                    ((-dx) / variantStepPx).roundToInt()
                                } else {
                                    (dx / variantStepPx).roundToInt()
                                }
                                selectedVariantIdx = idxOffset.coerceIn(0, variants.lastIndex)
                            } else if (!showPreviewPopup) {
                                val dist = hypot(
                                    change.position.x - down.position.x,
                                    change.position.y - down.position.y
                                )
                                if (dist > dragAwayThresholdPx) {
                                    draggedAway = true
                                    isPressed = false
                                    job?.cancel()
                                }
                            }
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
                .padding(horizontal = 1.dp, vertical = 1.dp)
        ) {
            displaySecondary?.takeIf { !hideSecondaryHint }?.let { secText ->
                val isActionHint = secText == "Copy" || secText == "Paste"
                Text(
                    text = secText,
                    color = if (isActionHint) colors.accent else colors.textSecondary.copy(alpha = 0.85f),
                    fontSize = when {
                        isActionHint -> 7.5.sp
                        isCompactRow -> 10.5.sp
                        else -> 11.5.sp
                    },
                    fontWeight = if (isActionHint) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 3.dp, top = 1.dp)
                )
            }

            val mainFontSize = when {
                keyAction.label.length > 3 -> 13.5.sp
                keyAction.label.length > 2 -> 14.5.sp
                keyAction.label.length == 2 && !displayPrimary.contains('\u25CC') -> 18.sp
                isCompactRow && hasSecondary -> 22.sp
                isCompactRow -> 24.sp
                hasSecondary -> 24.sp
                else -> 26.sp
            }

            Text(
                text = displayPrimary,
                color = fg,
                fontSize = mainFontSize,
                fontWeight = if (isEnterKey || isHighlight) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = if (hasSecondary) 5.dp else 0.dp)
            )
        }

        if (showTapBubble) {
            Box(
                modifier = Modifier
                    .offset(y = (-48).dp)
                    .zIndex(150f)
                    .widthIn(min = 44.dp)
                    .height(46.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .background(colors.keySpecialBackground, RoundedCornerShape(10.dp))
                    .border(1.2.dp, colors.accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayPrimary,
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        if (showPreviewPopup && variants.isNotEmpty()) {
            if (variants.size == 1) {
                val singleText = formatBengaliSignForDisplay(variants.first())
                val isWordAction = singleText == "Copy" || singleText == "Paste"
                Box(
                    modifier = Modifier
                        .offset(y = (-58).dp)
                        .zIndex(200f)
                        .widthIn(min = if (isWordAction) 68.dp else 52.dp)
                        .height(52.dp)
                        .shadow(10.dp, RoundedCornerShape(10.dp))
                        .background(colors.keySpecialBackground, RoundedCornerShape(10.dp))
                        .border(1.5.dp, colors.accent, RoundedCornerShape(10.dp))
                        .padding(horizontal = if (isWordAction) 10.dp else 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = singleText,
                        color = colors.textPrimary,
                        fontSize = if (isWordAction) 14.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else {
                val orderedEntries = if (alignPopupEnd) {
                    variants.mapIndexed { index, value -> index to value }.reversed()
                } else {
                    variants.mapIndexed { index, value -> index to value }
                }
                Row(
                    modifier = Modifier
                        .align(if (alignPopupEnd) Alignment.TopEnd else Alignment.TopStart)
                        .wrapContentWidth(
                            align = if (alignPopupEnd) Alignment.End else Alignment.Start,
                            unbounded = true
                        )
                        .offset(y = (-56).dp)
                        .zIndex(200f)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .background(colors.keySpecialBackground, RoundedCornerShape(12.dp))
                        .border(1.5.dp, colors.accent, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    orderedEntries.forEach { (origIdx, rawVariant) ->
                        val isSelected = origIdx == selectedVariantIdx
                        val isWordAction = rawVariant == "Copy" || rawVariant == "Paste"
                        Box(
                            modifier = Modifier
                                .widthIn(min = if (isWordAction) 52.dp else 38.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else Color.Transparent)
                                .padding(horizontal = if (isWordAction) 8.dp else 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatBengaliSignForDisplay(rawVariant),
                                color = if (isSelected) colors.enterKeyText else colors.textPrimary,
                                fontSize = if (isWordAction) 13.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
