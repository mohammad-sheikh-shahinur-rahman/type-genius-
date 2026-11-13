package com.example.ui.keyboard

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LanguageId
import com.example.model.SwipeSensitivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SpacebarComponent(
    activeLanguage: LanguageId,
    enabledLanguages: List<LanguageId>,
    isSwipeEnabled: Boolean,
    swipeSensitivity: SwipeSensitivity,
    isHapticEnabled: Boolean,
    showLanguageLabel: Boolean,
    isLongPressEnabled: Boolean,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    onTapSpace: () -> Unit,
    onSwipeNextLanguage: () -> Unit,
    onSwipePrevLanguage: () -> Unit,
    onLongPressLanguagePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val canSwipe = isSwipeEnabled && enabledLanguages.size > 1
    val thresholdPx = with(density) { swipeSensitivity.thresholdDp.dp.toPx() }
    val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()

    var isPressed by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var swipeTriggered by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val animatedDragX by animateFloatAsState(
        targetValue = if (isPressed) dragOffsetX.coerceIn(-thresholdPx * 1.3f, thresholdPx * 1.3f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

    val currentBgColor by animateColorAsState(
        targetValue = when {
            swipeTriggered -> accentColor.copy(alpha = 0.25f)
            isPressed -> backgroundColor.copy(alpha = 0.85f)
            else -> backgroundColor
        },
        label = "spacebarBg"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(if (isPressed) 0.dp else 1.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(currentBgColor)
            .testTag("spacebar_key")
            .semantics {
                contentDescription = "Spacebar, current language ${activeLanguage.displayName}"
            }
            .pointerInput(canSwipe, thresholdPx, enabledLanguages, isLongPressEnabled) {
                awaitEachGesture {
                    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    dragOffsetX = 0f
                    swipeTriggered = false
                    var hasSwiped = false
                    var isLongPressed = false

                    var longPressJob: Job? = null
                    if (isLongPressEnabled) {
                        longPressJob = coroutineScope.launch {
                            delay(longPressTimeoutMs)
                            if (!hasSwiped && isPressed) {
                                isLongPressed = true
                                triggerHaptic()
                                onLongPressLanguagePicker()
                            }
                        }
                    }

                    var totalDeltaX = 0f
                    var totalDeltaY = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            // UP event
                            longPressJob?.cancel()
                            if (!hasSwiped && !isLongPressed) {
                                // Clean tap: check if movement was within touch slop
                                if (abs(totalDeltaX) < touchSlopPx && abs(totalDeltaY) < touchSlopPx) {
                                    triggerHaptic()
                                    onTapSpace()
                                }
                            }
                            change.consume()
                            break
                        }

                        // MOVE event
                        val deltaX = change.position.x - down.position.x
                        val deltaY = change.position.y - down.position.y
                        totalDeltaX = deltaX
                        totalDeltaY = deltaY
                        dragOffsetX = deltaX

                        if (abs(deltaX) > touchSlopPx) {
                            longPressJob?.cancel()
                        }

                        if (canSwipe && !hasSwiped && !isLongPressed) {
                            if (deltaX > thresholdPx) {
                                // Swipe Left-to-Right -> Next Language
                                hasSwiped = true
                                swipeTriggered = true
                                triggerHaptic()
                                onSwipeNextLanguage()
                                change.consume()
                            } else if (deltaX < -thresholdPx) {
                                // Swipe Right-to-Left -> Previous Language
                                hasSwiped = true
                                swipeTriggered = true
                                triggerHaptic()
                                onSwipePrevLanguage()
                                change.consume()
                            }
                        }
                    }

                    isPressed = false
                    dragOffsetX = 0f
                    longPressJob?.cancel()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .offset { IntOffset(animatedDragX.roundToInt(), 0) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canSwipe && isPressed && dragOffsetX < -touchSlopPx) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Swipe Previous",
                    tint = accentColor,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 4.dp)
                )
            }

            // Language Pill / Space Label
            if (showLanguageLabel) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPressed || swipeTriggered) accentColor.copy(alpha = 0.18f) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = activeLanguage,
                        transitionSpec = {
                            if (dragOffsetX > 0) {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            }
                        },
                        label = "languagePill"
                    ) { targetLang ->
                        Text(
                            text = if (canSwipe && isPressed) "◀  ${targetLang.spacebarLabel}  ▶" else targetLang.spacebarLabel,
                            color = textColor.copy(alpha = 0.72f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    text = "space",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (canSwipe && isPressed && dragOffsetX > touchSlopPx) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe Next",
                    tint = accentColor,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}
