package com.example.ui.keyboard

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.data.BengaliCalendarUtils
import com.example.data.IslamicToolkit
import com.example.data.dictionary.QuickTodoItem
import com.example.model.PanelTextInputTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickToolsNotepadPanel(
    colors: KeyboardColors,
    notepadDraft: String,
    userTemplates: List<String>,
    autofillProfile: Map<String, String> = emptyMap(),
    todoItems: List<QuickTodoItem> = emptyList(),
    panelInputTarget: PanelTextInputTarget = PanelTextInputTarget.NONE,
    todoDraftInput: String = "",
    autofillEditingKey: String = "",
    autofillDraftValue: String = "",
    pomodoroRemainingSec: Int = 25 * 60,
    pomodoroIsBreak: Boolean = false,
    pomodoroRunning: Boolean = false,
    tasbihCount: Int = 0,
    tasbihDhikrIndex: Int = 0,
    onIncrementTasbih: () -> Unit = {},
    onResetTasbih: () -> Unit = {},
    onCycleTasbihDhikr: () -> Unit = {},
    onTogglePomodoroRun: () -> Unit = {},
    onResetPomodoro: (isBreak: Boolean) -> Unit = {},
    onStartTodoInput: () -> Unit = {},
    onSubmitTodoDraft: () -> Unit = {},
    onToggleTodoDone: (String) -> Unit = {},
    onDeleteTodo: (String) -> Unit = {},
    onStartAutofillEdit: (key: String, currentValue: String) -> Unit = { _, _ -> },
    onSaveAutofillInlineEdit: () -> Unit = {},
    onCancelPanelInput: () -> Unit = {},
    onToggleNotepadInput: () -> Unit = {},
    onOpenSettingsProfile: () -> Unit = {},
    onInsertNumberedList: (bengaliDigits: Boolean) -> Unit = {},
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
    var activeTab by remember { mutableIntStateOf(0) } // 0 = তারিখ/লিস্ট, 1 = অটো-ফিল, 2 = টু-ডু/টাইমার, 3 = নোটপ্যাড
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var engine: TextToSpeech? = null
        runCatching {
            engine = TextToSpeech(context.applicationContext) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    runCatching {
                        val bnLocale = Locale.Builder().setLanguage("bn").setRegion("BD").build()
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
                val r = tts.setLanguage(Locale.Builder().setLanguage("bn").setRegion("BD").build())
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
    val banglaCal = remember(now) { BengaliCalendarUtils.toBanglaDate(now) }
    val banglaTimeNow = remember(now) { BengaliCalendarUtils.formatBanglaTime(now) }
    val enDate = remember(now) { BengaliCalendarUtils.formatEnglishDate(now) }
    val isoDate = remember(now) { BengaliCalendarUtils.formatIsoDate(now) }
    val enTime = remember(now) { BengaliCalendarUtils.formatEnglishTime(now) }
    val hijriCal = remember(now) { IslamicToolkit.toHijriDate(now) }
    val enDay = remember(now) { BengaliCalendarUtils.formatEnglishWeekday(now) }
    val bnGregDate = remember(now) {
        val bnLocale = Locale.Builder().setLanguage("bn").setRegion("BD").build()
        val raw = SimpleDateFormat("dd MMMM yyyy", bnLocale).format(now)
        BengaliCalendarUtils.toBengaliDigits(raw)
    }

    val dateTimeChips = listOf(
        "🇧🇩 ${banglaCal.formattedFull}" to banglaCal.formattedFull,
        "🌙 ${hijriCal.formattedBn}" to hijriCal.formattedBn,
        "📅 ${banglaCal.formattedWithWeekday}" to banglaCal.formattedWithWeekday,
        "🗓️ ${banglaCal.formattedShort}" to banglaCal.formattedShort,
        "🕒 $banglaTimeNow" to banglaTimeNow,
        "🗓️ $bnGregDate" to bnGregDate,
        "📅 $enDate" to enDate,
        "🕒 $enTime" to enTime,
        "📆 $isoDate" to isoDate,
        "📆 $enDay" to enDay
    )

    val isInlineTypingOpen = panelInputTarget in setOf(
        PanelTextInputTarget.UTILITIES_TODO_INPUT,
        PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT,
        PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT
    )

    val tabItems = listOf(
        "তারিখ ও লিস্ট" to 0,
        "ইসলামি ☪" to 1,
        "অটো-ফিল" to 2,
        (if (pomodoroRunning) "টু-ডু • ⏱" else "টু-ডু ও টাইমার") to 3,
        "নোটপ্যাড" to 4
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isInlineTypingOpen) 166.dp else 266.dp)
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                tabItems.forEach { (title, idx) ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colors.accent else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                if (isInlineTypingOpen) onCancelPanelInput()
                                activeTab = idx
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("util_tab_$idx")
                    ) {
                        Text(
                            text = title,
                            color = if (selected) Color.White else colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.16f))
                        .clickable {
                            onHaptic()
                            val toRead = if (activeTab == 4 && notepadDraft.isNotBlank()) {
                                notepadDraft
                            } else {
                                onGetFieldTextForTts().ifBlank { notepadDraft }
                            }
                            speakText(toRead)
                        }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                        .testTag("util_tts_speak_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "শুনুন", tint = colors.accent, modifier = Modifier.size(14.dp))
                        Text("শুনুন", color = colors.accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = { onHaptic(); onCancelPanelInput(); onClose() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                        Text("১-ট্যাপ সময়, ইংরেজি ও বাংলা তারিখ (বঙ্গাব্দ)", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dateTimeChips.forEachIndexed { idx, (label, value) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (idx == 0) colors.accent.copy(alpha = 0.16f) else colors.keyBackground)
                                    .clickable {
                                        onHaptic()
                                        onInsertSnippet(value)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("datetime_chip_$idx")
                            ) {
                                Text(
                                    label,
                                    color = if (idx == 0) colors.accent else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    ListAndEmailBlocksSection(
                        colors = colors,
                        onInsertSnippet = onInsertSnippet,
                        onInsertNumberedList = onInsertNumberedList,
                        onHaptic = onHaptic
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ডাইনামিক টেমপ্লেট ({তারিখ}, {সময়}, {বার}, {ক্লিপ})",
                            color = colors.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (notepadDraft.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accent.copy(alpha = 0.16f))
                                    .clickable {
                                        onHaptic()
                                        onAddTemplate(notepadDraft.trim())
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                                    Text("নোট সেভ", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                fontSize = 12.sp,
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
                                Icon(Icons.Default.Delete, contentDescription = "টেমপ্লেট মুছুন", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            1 -> {
                Box(modifier = Modifier.weight(1f)) {
                    IslamicToolsTab(
                        colors = colors,
                        tasbihCount = tasbihCount,
                        tasbihDhikrIndex = tasbihDhikrIndex,
                        onIncrementTasbih = onIncrementTasbih,
                        onResetTasbih = onResetTasbih,
                        onCycleTasbihDhikr = onCycleTasbihDhikr,
                        onInsertSnippet = onInsertSnippet,
                        onHaptic = onHaptic
                    )
                }
            }

            2 -> {
                Box(modifier = Modifier.weight(1f)) {
                    AutofillVaultTab(
                        colors = colors,
                        profileMap = autofillProfile,
                        editingFieldKey = autofillEditingKey,
                        editingDraftValue = autofillDraftValue,
                        isInlineEditActive = panelInputTarget == PanelTextInputTarget.UTILITIES_AUTOFILL_EDIT,
                        onInsertValue = { value -> onInsertSnippet(value) },
                        onStartEditField = onStartAutofillEdit,
                        onSaveInlineEdit = onSaveAutofillInlineEdit,
                        onCancelInlineEdit = onCancelPanelInput,
                        onOpenSettingsProfile = onOpenSettingsProfile,
                        onHaptic = onHaptic
                    )
                }
            }

            3 -> {
                Box(modifier = Modifier.weight(1f)) {
                    TodoAndPomodoroTab(
                        colors = colors,
                        todos = todoItems,
                        todoDraftInput = todoDraftInput,
                        isTodoInputActive = panelInputTarget == PanelTextInputTarget.UTILITIES_TODO_INPUT,
                        pomodoroRemainingSec = pomodoroRemainingSec,
                        pomodoroIsBreak = pomodoroIsBreak,
                        pomodoroRunning = pomodoroRunning,
                        onTogglePomodoroRun = onTogglePomodoroRun,
                        onResetPomodoro = onResetPomodoro,
                        onInsertTimerStatus = onInsertSnippet,
                        onToggleTodoInput = onStartTodoInput,
                        onSubmitTodoDraft = onSubmitTodoDraft,
                        onToggleTodoDone = onToggleTodoDone,
                        onDeleteTodo = onDeleteTodo,
                        onInsertTodoText = onInsertSnippet,
                        onInsertAllTodos = {
                            val formatted = todoItems.joinToString("\n") { item ->
                                if (item.done) "☑ ${item.text}" else "☐ ${item.text}"
                            }
                            if (formatted.isNotBlank()) onInsertSnippet(formatted)
                        },
                        onHaptic = onHaptic
                    )
                }
            }

            else -> {
                val isNotepadTyping = panelInputTarget == PanelTextInputTarget.UTILITIES_NOTEPAD_INPUT
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.keyBackground)
                            .border(
                                width = if (isNotepadTyping) 1.3.dp else 0.6.dp,
                                color = if (isNotepadTyping) colors.accent else colors.keySpecialBackground,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onHaptic()
                                onToggleNotepadInput()
                            }
                            .padding(10.dp)
                            .testTag("util_scratchpad_input")
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNotepadTyping) "নিচের কীবোর্ড দিয়ে নোট লিখুন (অটো-সেভ)" else "লিখতে বা এডিট করতে ট্যাপ করুন",
                                    color = if (isNotepadTyping) colors.accent else colors.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(Icons.Default.Edit, contentDescription = null, tint = colors.accent, modifier = Modifier.size(13.dp))
                            }
                            Text(
                                text = notepadDraft.ifBlank { "এখানে খসড়া নোট বা {তারিখ}/{সময়}/{ক্লিপ} দিয়ে টেমপ্লেট লিখুন..." },
                                color = if (notepadDraft.isBlank()) colors.textSecondary else colors.textPrimary,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.keySpecialBackground)
                                .clickable(enabled = notepadDraft.isNotBlank()) {
                                    onHaptic()
                                    onSaveNotepadDraft("")
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("মুছুন", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.keySpecialBackground)
                                .clickable(enabled = notepadDraft.isNotBlank()) {
                                    onHaptic()
                                    onAddTemplate(notepadDraft.trim())
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ টেমপ্লেট", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.4f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (notepadDraft.isNotBlank()) colors.accent else colors.keySpecialBackground)
                                .clickable(enabled = notepadDraft.isNotBlank()) {
                                    onHaptic()
                                    onCancelPanelInput()
                                    onInsertSnippet(notepadDraft)
                                }
                                .padding(vertical = 8.dp)
                                .testTag("util_insert_draft_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "লেখায় বসিয়ে দিন",
                                color = if (notepadDraft.isNotBlank()) Color.White else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
