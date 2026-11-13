package com.example.ui.keyboard

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickToolsNotepadPanel(
    colors: KeyboardColors,
    notepadDraft: String,
    userTemplates: List<String>,
    onInsertSnippet: (String) -> Unit,
    onSaveNotepadDraft: (String) -> Unit,
    onAddTemplate: (String) -> Unit,
    onRemoveTemplate: (String) -> Unit,
    onGetFieldTextForTts: () -> String,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Date/Time & Templates, 1 = Scratchpad + TTS
    var draftText by remember(notepadDraft) { mutableStateOf(notepadDraft) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var engine: TextToSpeech? = null
        runCatching {
            engine = TextToSpeech(context.applicationContext) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    runCatching {
                        val bnLocale = Locale("bn", "BD")
                        val res = engine?.setLanguage(bnLocale)
                        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                            engine?.language = Locale.US
                        }
                    }
                }
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                tts?.stop()
                tts?.shutdown()
            }
        }
    }

    fun speakText(raw: String) {
        val clean = raw.trim()
        if (clean.isBlank() || tts == null || !ttsReady) return
        runCatching {
            val isBangla = clean.any { it.code in 0x0980..0x09FF }
            if (isBangla) {
                val r = tts.setLanguage(Locale("bn", "BD"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.US
                }
            } else {
                tts.language = Locale.US
            }
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tg_tts_${System.currentTimeMillis()}")
        }
    }

    val now = remember { Date() }
    val enDate = remember(now) { SimpleDateFormat("dd MMM yyyy", Locale.US).format(now) }
    val enTime = remember(now) { SimpleDateFormat("hh:mm a", Locale.US).format(now) }
    val enDay = remember(now) { SimpleDateFormat("EEEE", Locale.US).format(now) }
    val bnDate = remember(now) {
        val raw = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD")).format(now)
        toBengaliDigits(raw)
    }
    val bnTime = remember(now) {
        val raw = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(now)
        toBengaliDigits(raw)
    }
    val bnDay = remember(now) { SimpleDateFormat("EEEE", Locale("bn", "BD")).format(now) }

    val dateTimeChips = listOf(
        "📅 $enDate" to enDate,
        "🇧🇩 $bnDate" to bnDate,
        "🕒 $enTime" to enTime,
        "🕒 $bnTime" to bnTime,
        "📆 $enDay" to enDay,
        "📆 $bnDay" to bnDay
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(262.dp)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Date & Templates" to 0, "Scratchpad & TTS" to 1).forEach { (title, idx) ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colors.accent else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                activeTab = idx
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("util_tab_$idx")
                    ) {
                        Text(
                            text = title,
                            color = if (selected) Color.White else colors.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.16f))
                        .clickable {
                            onHaptic()
                            val toRead = if (activeTab == 1 && draftText.isNotBlank()) {
                                draftText
                            } else {
                                onGetFieldTextForTts().ifBlank { draftText }
                            }
                            speakText(toRead)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("util_tts_speak_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read Aloud", tint = colors.accent, modifier = Modifier.size(15.dp))
                        Text("Speak", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = { onHaptic(); onClose() }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Utilities", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (activeTab == 0) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    Text("Live Date & Time Stamps", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dateTimeChips.forEach { (label, value) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onHaptic()
                                    onInsertSnippet(value)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("datetime_chip_$value")
                        ) {
                            Text(label, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick Saved Templates (${userTemplates.size})", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    if (draftText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.accent.copy(alpha = 0.16f))
                                .clickable {
                                    onHaptic()
                                    onAddTemplate(draftText.trim())
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                                Text("Save Note as Template", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                userTemplates.forEach { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keyBackground)
                            .clickable {
                                onHaptic()
                                onInsertSnippet(template)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template,
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onHaptic()
                                onRemoveTemplate(template)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Template", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextField(
                    value = draftText,
                    onValueChange = {
                        draftText = it
                        onSaveNotepadDraft(it)
                    },
                    placeholder = { Text("এখানে খসড়া নোট লিখে রাখুন (অটো সেভ হবে)...", color = colors.textSecondary, fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.keyBackground,
                        unfocusedContainerColor = colors.keyBackground,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedIndicatorColor = colors.accent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("util_scratchpad_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keySpecialBackground)
                            .clickable(enabled = draftText.isNotBlank()) {
                                onHaptic()
                                draftText = ""
                                onSaveNotepadDraft("")
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Clear", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keySpecialBackground)
                            .clickable(enabled = draftText.isNotBlank()) {
                                onHaptic()
                                onAddTemplate(draftText.trim())
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Template", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (draftText.isNotBlank()) colors.accent else colors.keySpecialBackground)
                            .clickable(enabled = draftText.isNotBlank()) {
                                onHaptic()
                                onInsertSnippet(draftText)
                            }
                            .padding(vertical = 8.dp)
                            .testTag("util_insert_draft_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Insert Draft",
                            color = if (draftText.isNotBlank()) Color.White else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun toBengaliDigits(input: String): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return buildString(input.length) {
        for (c in input) {
            if (c in '0'..'9') append(bnDigits[c - '0']) else append(c)
        }
    }
}
