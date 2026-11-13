package com.example.ui.keyboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LanguageId
import com.example.ui.theme.TGMagenta
import com.example.ui.theme.TGViolet
import kotlin.math.roundToInt

@Composable
fun SpacebarSwipeTutorialOverlay(
    colors: KeyboardColors,
    enabledLanguages: List<LanguageId>,
    onDismiss: () -> Unit,
    onSwipeSuccess: () -> Unit
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var isSuccess by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "swipe_anim")
    val handOffsetX by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .zIndex(500f)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1033),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .border(2.dp, TGMagenta, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(TGMagenta, TGViolet))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Swipe,
                        contentDescription = "Swipe Tutorial",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isSuccess) "দারুণ! ভাষা সফলভাবে পরিবর্তিত হয়েছে!" else "স্পেসবার সোয়াইপ টিউটোরিয়াল",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isSuccess)
                        "আপনি সফলভাবে ভাষা পরিবর্তন করে ফেলেছেন। এখন যে কোনো সময় দ্রুত ভাষা বদলাতে এটি ব্যবহার করুন।"
                    else
                        "স্পেসবারের ওপর আঙুল রেখে ডানে বা বামে স্লাইড (Swipe) করুন। এর মাধ্যমে সেকেন্ডের মধ্যে আপনার পছন্দের ভাষা (যেমন: বাংলা, ইংরেজি, সিলেটি, আরবি) পরিবর্তন করতে পারবেন!",
                    color = Color(0xFFD6D8E0),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Spacebar Simulator Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.keyBackground)
                        .border(1.5.dp, TGMagenta.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(swipeOffset) > 40f) {
                                        isSuccess = true
                                        onSwipeSuccess()
                                    }
                                    swipeOffset = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    swipeOffset += dragAmount
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "◄  ",
                            color = TGMagenta,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "স্পেসবারে আঙুল স্লাইড করুন (Swipe Here)",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  ►",
                            color = TGMagenta,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Animated Finger Indicator
                    if (!isSuccess) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(handOffsetX.roundToInt() + swipeOffset.roundToInt(), 0) }
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TGMagenta.copy(alpha = 0.8f))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👆", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("tutorial_got_it_button")
                ) {
                    Text(
                        text = if (isSuccess) "শুরু করুন (Get Started)" else "বুঝেছি, স্কিপ করুন (Got It)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
