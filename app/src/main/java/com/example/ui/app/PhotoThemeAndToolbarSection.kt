package com.example.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.model.KeyboardToolId
import com.example.ui.theme.TGMagenta
import kotlinx.coroutines.launch

@Composable
fun ToolbarCustomizerCard(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val activeToolbarTools by settingsManager.toolbarTools.collectAsState(initial = KeyboardToolId.DEFAULT_TOOLBAR)

    val toolLabels = mapOf(
        KeyboardToolId.GEMINI_AI to "TypeGenius AI",
        KeyboardToolId.STICKERS to "স্টিকার ও কাওমোজি (Stickers)",
        KeyboardToolId.TEXT_EDITING to "টেক্সট এডিটর (Text Edit)",
        KeyboardToolId.EMOJI to "ইমোজি ও জিআইএফ (Emoji)",
        KeyboardToolId.CLIPBOARD to "ক্লিপবোর্ড হিস্ট্রি (Clipboard)",
        KeyboardToolId.TRANSLATOR to "অফলাইন ট্রান্সলেটর (Translate)",
        KeyboardToolId.CALCULATOR to "ক্যালকুলেটর ও কনভার্টার (Calc)",
        KeyboardToolId.UTILITIES to "নোটপ্যাড, তারিখ ও TTS (Notes)",
        KeyboardToolId.FANCY_FONTS to "স্টাইলিশ ফন্ট (Fancy Fonts)",
        KeyboardToolId.THEMES to "থিমস (Themes)",
        KeyboardToolId.INCOGNITO to "প্রাইভেট ইনকগনিটো (Incognito)",
        KeyboardToolId.SETTINGS to "কীবোর্ড সেটিংস (Settings)",
        KeyboardToolId.GIF to "জিআইএফ (GIF)",
        KeyboardToolId.ONE_HANDED to "এক হাত মোড (One-hand)",
        KeyboardToolId.FLOATING to "ফ্লোটিং কীবোর্ড (Floating)",
        KeyboardToolId.RESIZE to "সাইজ পরিবর্তন (Resize)",
        KeyboardToolId.NUMPAD to "নাম্বার প্যাড (Numpad)",
        KeyboardToolId.SHARE to "শেয়ার অ্যাপ (Share)"
    )

    val orderedTools = activeToolbarTools + KeyboardToolId.entries.filterNot { it in activeToolbarTools }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("toolbar_customizer_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = TGMagenta)
                    Column {
                        Text(
                            "টুলবার কাস্টমাইজেশন (Toolbar)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "কীবোর্ডের উপরের স্ট্রিপের টুল চালু/বন্ধ ও ক্রম সাজান",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = {
                        scope.launch { settingsManager.setToolbarTools(KeyboardToolId.DEFAULT_TOOLBAR) }
                    },
                    modifier = Modifier.testTag("toolbar_reset_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset Toolbar", tint = TGMagenta, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রিসেট", fontSize = 12.sp, color = TGMagenta)
                }
            }

            orderedTools.forEach { tool ->
                val isEnabled = activeToolbarTools.contains(tool)
                val activeIndex = activeToolbarTools.indexOf(tool)
                val label = toolLabels[tool] ?: tool.label

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isEnabled) TGMagenta.copy(alpha = 0.06f)
                            else Color.Transparent
                        )
                        .clickable {
                            val newList = if (isEnabled) {
                                if (activeToolbarTools.size > 1) activeToolbarTools - tool else activeToolbarTools
                            } else {
                                activeToolbarTools + tool
                            }
                            scope.launch { settingsManager.setToolbarTools(newList) }
                        }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                enabled = activeIndex > 0,
                                onClick = {
                                    if (activeIndex > 0) {
                                        val mutable = activeToolbarTools.toMutableList()
                                        mutable[activeIndex] = mutable[activeIndex - 1].also {
                                            mutable[activeIndex - 1] = mutable[activeIndex]
                                        }
                                        scope.launch { settingsManager.setToolbarTools(mutable) }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (activeIndex > 0) TGMagenta else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(
                                enabled = activeIndex < activeToolbarTools.lastIndex,
                                onClick = {
                                    if (activeIndex in 0 until activeToolbarTools.lastIndex) {
                                        val mutable = activeToolbarTools.toMutableList()
                                        mutable[activeIndex] = mutable[activeIndex + 1].also {
                                            mutable[activeIndex + 1] = mutable[activeIndex]
                                        }
                                        scope.launch { settingsManager.setToolbarTools(mutable) }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (activeIndex < activeToolbarTools.lastIndex) TGMagenta else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            val newList = if (!checked) {
                                if (activeToolbarTools.size > 1) activeToolbarTools - tool else activeToolbarTools
                            } else {
                                activeToolbarTools + tool
                            }
                            scope.launch { settingsManager.setToolbarTools(newList) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TGMagenta
                        ),
                        modifier = Modifier.testTag("toolbar_toggle_${tool.id}")
                    )
                }
            }
        }
    }
}
