package com.example.ui.app

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.zIndex
import com.example.ui.theme.TGMagenta
import com.example.ui.theme.TGViolet
import kotlin.math.abs
import kotlin.math.roundToInt

data class AppTutorialStep(
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val instruction: String,
    val isInteractiveSwipe: Boolean = false
)

@Composable
fun FullAppInteractiveTutorialOverlay(
    onDismiss: () -> Unit
) {
    val steps = remember {
        listOf(
            AppTutorialStep(
                title = "TypeGenius-এ স্বাগতম!",
                subtitle = "১০০% অফলাইন ও ব্যাকএন্ড-মুক্ত স্মার্ট অ্যান্ড্রয়েড কীবোর্ড।",
                iconEmoji = "🚀",
                instruction = "এই ইন্টারেক্টিভ টিউটোরিয়ালের মাধ্যমে কীবোর্ডের প্রধান ফিচারগুলো এক নজরে শিখে নিন।"
            ),
            AppTutorialStep(
                title = "৩৩টি ভাষা ও অভ্র ফোনেটিক",
                subtitle = "বাংলা প্রভাত, অভ্র, ইউনিবিজয় ও সিলেটি নাগরী।",
                iconEmoji = "⌨️",
                instruction = "অভ্র ফোনেটিক (Avro) মোডে 'ami' লিখলেই 'আমি' চলে আসে এবং কি-বোর্ডেই বাংলা ফোনেটিক ইঙ্গিত দেখা যায়।"
            ),
            AppTutorialStep(
                title = "স্পেসবার সোয়াইপ ভাষা পরিবর্তন",
                subtitle = "আঙুল না তুলে দ্রুত ভাষা বদলান।",
                iconEmoji = "⇄",
                instruction = "স্পেসবারের ওপর আঙুল রেখে ডানে বা বামে স্লাইড (Swipe) করে যেকোনো ভাষায় চলে যান। (নিচে প্র্যাকটিস করুন!)",
                isInteractiveSwipe = true
            ),
            AppTutorialStep(
                title = "১-ট্যাপ কন্টিনিউয়াস ভয়েস টাইপিং",
                subtitle = "মাইক্রোফোনে বারবার চাপ দিতে হবে না।",
                iconEmoji = "🎙️",
                instruction = "একবার ট্যাপ করুন এবং একটানা কথা বলুন। কীবোর্ড স্বয়ংক্রিয়ভাবে আপনার কথা লিখে যাবে।"
            ),
            AppTutorialStep(
                title = "অফলাইন ট্রান্সলেটর ও এআই",
                subtitle = "৩৮টি ভাষা ও লোকাল এআই প্রুফরিড টুলস।",
                iconEmoji = "🌐",
                instruction = "ইন্টারনেট ছাড়াই যেকোনো চ্যাটে সরাসরি অনুবাদ, ব্যাকরণ সংশোধন ও কাস্টম এআই প্রম্পট ব্যবহার করুন।"
            )
        )
    }

    var currentStepIdx by remember { mutableStateOf(0) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var swipeSuccess by remember { mutableStateOf(false) }

    val step = steps[currentStepIdx]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .zIndex(999f)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF191029),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .border(2.dp, TGMagenta, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = TGMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ধাপ ${currentStepIdx + 1} / ${steps.size}",
                            color = TGMagenta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("এড়িয়ে যান (Skip)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TGMagenta, TGViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(step.iconEmoji, fontSize = 28.sp)
                }

                Text(
                    text = step.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = step.subtitle,
                    color = TGMagenta,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = step.instruction,
                    color = Color(0xFFD6D8E0),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (step.isInteractiveSwipe) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF26143F))
                            .border(1.5.dp, if (swipeSuccess) Color(0xFF4CAF50) else TGMagenta, RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (abs(swipeOffset) > 35f) {
                                            swipeSuccess = true
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
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(if (swipeSuccess) "✓ সফলভাবে সোয়াইপ হয়েছে!" else "◄  স্পেসবার এখানে স্লাইড করুন  ►", color = if (swipeSuccess) Color(0xFF4CAF50) else Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStepIdx > 0) {
                        Button(
                            onClick = { currentStepIdx-- },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B213F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("পেছনে", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStepIdx < steps.lastIndex) {
                                currentStepIdx++
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(46.dp)
                            .testTag("tutorial_next_button")
                    ) {
                        Text(
                            text = if (currentStepIdx == steps.lastIndex) "শুরু করুন (Get Started)" else "পরবর্তী (Next)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
