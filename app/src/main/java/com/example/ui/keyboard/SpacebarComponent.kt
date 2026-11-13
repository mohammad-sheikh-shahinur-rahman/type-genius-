package com.example.ui.keyboard

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.sign

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

    val currentIndex = enabledLanguages.indexOf(activeLanguage).coerceAtLeast(0)
    val prevLanguage = if (enabledLanguages.size > 1) {
        enabledLanguages[(currentIndex - 1 + enabledLanguages.size) % enabledLanguages.size]
    } else activeLanguage
    val nextLanguage = if (enabledLanguages.size > 1) {
        enabledLanguages[(currentIndex + 1) % enabledLanguages.size]
    } else activeLanguage

    var isPressed by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var lastSwipeDirection by remember { mutableIntStateOf(1) }
    var swipeFlashActive by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val isDraggingHorizontally = canSwipe && isPressed && abs(dragOffsetX) > touchSlopPx * 0.65f
    val dragProgress = if (canSwipe && isPressed) {
        (abs(dragOffsetX) / thresholdPx).coerceIn(0f, 1f)
    } else 0f

    // Rubber-band curve so dragging feels organic and never stiff
    val rawDampedOffset = if (isPressed && canSwipe) {
        val maxVisual = thresholdPx * 0.65f
        val norm = (abs(dragOffsetX) / thresholdPx).coerceIn(0f, 1.4f)
        val curved = (norm / (1f + 0.35f * norm)) * maxVisual
        sign(dragOffsetX) * curved
    } else 0f

    val animatedDragX by animateFloatAsState(
        targetValue = rawDampedOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spacebarDragOffset"
    )

    val pressScale by animateFloatAsState(
        targetValue = when {
            swipeFlashActive -> 0.985f
            isPressed && !isDraggingHorizontally -> 0.975f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "spacebarScale"
    )

    val currentBgColor by animateColorAsState(
        targetValue = when {
            swipeFlashActive -> accentColor.copy(alpha = 0.22f)
            isDraggingHorizontally -> accentColor.copy(alpha = 0.08f + 0.12f * dragProgress)
            isPressed -> backgroundColor.copy(alpha = 0.84f)
            else -> backgroundColor
        },
        animationSpec = tween(durationMillis = 150),
        label = "spacebarBg"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(if (isPressed) 0.dp else 1.5.dp, RoundedCornerShape(10.dp))
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
                    swipeFlashActive = false
                    var anchorX = down.position.x
                    var hasSwipedAny = false
                    var isLongPressed = false

                    var longPressJob: Job? = null
                    if (isLongPressEnabled) {
                        longPressJob = coroutineScope.launch {
                            delay(longPressTimeoutMs)
                            if (!hasSwipedAny && isPressed && abs(dragOffsetX) < touchSlopPx) {
                                isLongPressed = true
                                triggerHaptic()
                                onLongPressLanguagePicker()
                            }
                        }
                    }

                    var maxMovedX = 0f
                    var maxMovedY = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            longPressJob?.cancel()
                            if (!hasSwipedAny && !isLongPressed) {
                                if (maxMovedX < touchSlopPx && maxMovedY < touchSlopPx) {
                                    triggerHaptic()
                                    onTapSpace()
                                }
                            }
                            change.consume()
                            break
                        }

                        val totalDeltaX = abs(change.position.x - down.position.x)
                        val totalDeltaY = abs(change.position.y - down.position.y)
                        if (totalDeltaX > maxMovedX) maxMovedX = totalDeltaX
                        if (totalDeltaY > maxMovedY) maxMovedY = totalDeltaY

                        val stepDeltaX = change.position.x - anchorX
                        dragOffsetX = stepDeltaX

                        if (maxMovedX > touchSlopPx * 0.8f) {
                            longPressJob?.cancel()
                        }

                        if (canSwipe && !isLongPressed) {
                            if (stepDeltaX >= thresholdPx) {
                                // Swipe Left-to-Right -> Next Language (supports fluid multi-step glide)
                                hasSwipedAny = true
                                lastSwipeDirection = 1
                                anchorX = change.position.x
                                dragOffsetX = 0f
                                triggerHaptic()
                                onSwipeNextLanguage()
                                swipeFlashActive = true
                                coroutineScope.launch {
                                    delay(180)
                                    swipeFlashActive = false
                                }
                                change.consume()
                            } else if (stepDeltaX <= -thresholdPx) {
                                // Swipe Right-to-Left -> Previous Language
                                hasSwipedAny = true
                                lastSwipeDirection = -1
                                anchorX = change.position.x
                                dragOffsetX = 0f
                                triggerHaptic()
                                onSwipePrevLanguage()
                                swipeFlashActive = true
                                coroutineScope.launch {
                                    delay(180)
                                    swipeFlashActive = false
                                }
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
        // Adjacent language peek indicators on edges during active drag
        if (canSwipe) {
            AnimatedVisibility(
                visible = isPressed && dragOffsetX < -touchSlopPx * 0.5f,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.88f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.88f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha((0.45f + 0.55f * dragProgress).coerceIn(0f, 1f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = prevLanguage.spacebarLabel,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = isPressed && dragOffsetX > touchSlopPx * 0.5f,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.88f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.88f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha((0.45f + 0.55f * dragProgress).coerceIn(0f, 1f))
                ) {
                    Text(
                        text = nextLanguage.spacebarLabel,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Center Language Label with fluid spring drag tracking & directional slide
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
                .offset { IntOffset(animatedDragX.roundToInt(), 0) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLanguageLabel) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                swipeFlashActive -> accentColor.copy(alpha = 0.2f)
                                isDraggingHorizontally -> accentColor.copy(alpha = 0.14f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = activeLanguage,
                        transitionSpec = {
                            val dir = if (lastSwipeDirection >= 0) 1 else -1
                            (slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { width -> -dir * (width / 2) } + fadeIn(tween(150))) togetherWith
                                (slideOutHorizontally(tween(130)) { width -> dir * (width / 2) } + fadeOut(tween(110)))
                        },
                        label = "languagePill"
                    ) { targetLang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (canSwipe && !isDraggingHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = targetLang.spacebarLabel,
                                color = if (swipeFlashActive || isDraggingHorizontally) accentColor else textColor.copy(alpha = 0.84f),
                                fontSize = 14.5.sp,
                                fontWeight = if (swipeFlashActive) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                            if (canSwipe && !isDraggingHorizontally) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
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
        }
    }
}
