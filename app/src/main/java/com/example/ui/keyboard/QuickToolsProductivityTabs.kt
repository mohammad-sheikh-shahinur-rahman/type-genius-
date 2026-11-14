package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BengaliCalendarUtils
import com.example.data.dictionary.ProductivityTextTools
import com.example.data.dictionary.QuickTodoItem
import java.util.Locale

@Composable
fun ListAndEmailBlocksSection(
    colors: KeyboardColors,
    onInsertSnippet: (String) -> Unit,
    onInsertNumberedList: (bengaliDigits: Boolean) -> Unit,
    onHaptic: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "১-ট্যাপ লিস্ট, বুলেট ও ইমেইল ব্লক",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.accent.copy(alpha = 0.16f))
                    .clickable {
                        onHaptic()
                        onInsertNumberedList(true)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("list_numbered_bn")
            ) {
                Text("১. বাংলা লিস্ট", color = colors.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.accent.copy(alpha = 0.16f))
                    .clickable {
                        onHaptic()
                        onInsertNumberedList(false)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("list_numbered_en")
            ) {
                Text("1. Numbered", color = colors.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            ProductivityTextTools.listAndEmailPresets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.keyBackground)
                        .clickable {
                            onHaptic()
                            onInsertSnippet(preset.content)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("preset_${preset.id}")
                ) {
                    Text(preset.label, color = colors.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AutofillVaultTab(
    colors: KeyboardColors,
    profileMap: Map<String, String>,
    editingFieldKey: String,
    editingDraftValue: String,
    isInlineEditActive: Boolean,
    onInsertValue: (String) -> Unit,
    onStartEditField: (key: String, currentValue: String) -> Unit,
    onSaveInlineEdit: () -> Unit,
    onCancelInlineEdit: () -> Unit,
    onOpenSettingsProfile: () -> Unit,
    onHaptic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "১-ট্যাপ অটো-ফিল ভল্ট (১০০% অফলাইন)",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.keySpecialBackground)
                    .clickable {
                        onHaptic()
                        onOpenSettingsProfile()
                    }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("সেটিংস", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isInlineEditActive) {
            val schema = ProductivityTextTools.autofillSchema.find { it.key == editingFieldKey }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .border(1.2.dp, colors.accent, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${schema?.label ?: editingFieldKey} এডিট হচ্ছে (নিচের কীবোর্ডে টাইপ করুন)",
                        color = colors.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = editingDraftValue.ifEmpty { schema?.placeholder ?: "মান লিখুন..." },
                        color = if (editingDraftValue.isEmpty()) colors.textSecondary else colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                onCancelInlineEdit()
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text("বাতিল", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent)
                            .clickable {
                                onHaptic()
                                onSaveInlineEdit()
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("autofill_save_inline")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "সেভ", tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("সেভ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        ProductivityTextTools.autofillSchema.forEach { field ->
            val savedVal = profileMap[field.key].orEmpty()
            val hasValue = savedVal.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .clickable {
                        onHaptic()
                        if (hasValue) {
                            onInsertValue(savedVal)
                        } else {
                            onStartEditField(field.key, "")
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("autofill_row_${field.key}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = field.label,
                        color = colors.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasValue) savedVal else "+ যোগ করতে ট্যাপ করুন (${field.placeholder})",
                        color = if (hasValue) colors.textPrimary else colors.accent,
                        fontSize = 12.5.sp,
                        fontWeight = if (hasValue) FontWeight.Medium else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (hasValue) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(colors.accent.copy(alpha = 0.16f))
                                .clickable {
                                    onHaptic()
                                    onInsertValue(savedVal)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("বসান", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = {
                            onHaptic()
                            onStartEditField(field.key, savedVal)
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("autofill_edit_${field.key}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "${field.label} এডিট",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoAndPomodoroTab(
    colors: KeyboardColors,
    todos: List<QuickTodoItem>,
    todoDraftInput: String,
    isTodoInputActive: Boolean,
    pomodoroRemainingSec: Int,
    pomodoroIsBreak: Boolean,
    pomodoroRunning: Boolean,
    onTogglePomodoroRun: () -> Unit,
    onResetPomodoro: (isBreak: Boolean) -> Unit,
    onInsertTimerStatus: (String) -> Unit,
    onToggleTodoInput: () -> Unit,
    onSubmitTodoDraft: () -> Unit,
    onToggleTodoDone: (String) -> Unit,
    onDeleteTodo: (String) -> Unit,
    onInsertTodoText: (String) -> Unit,
    onInsertAllTodos: () -> Unit,
    onHaptic: () -> Unit
) {
    val mins = pomodoroRemainingSec / 60
    val secs = pomodoroRemainingSec % 60
    val formattedClock = String.format(Locale.US, "%02d:%02d", mins, secs)
    val bnClock = BengaliCalendarUtils.toBengaliDigits(formattedClock)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Pomodoro Focus / Break Mini Timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(if (pomodoroRunning) colors.accent.copy(alpha = 0.14f) else colors.keyBackground)
                .border(
                    width = 1.dp,
                    color = if (pomodoroRunning) colors.accent else colors.keySpecialBackground,
                    shape = RoundedCornerShape(11.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Timer, contentDescription = "পোমোডোরো", tint = colors.accent, modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        text = if (pomodoroIsBreak) "ব্রেক টাইমার" else "ফোকাস টাইমার",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = bnClock,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (!pomodoroIsBreak) colors.accent.copy(alpha = 0.2f) else colors.keySpecialBackground)
                        .clickable { onHaptic(); onResetPomodoro(false) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("২৫মি", color = if (!pomodoroIsBreak) colors.accent else colors.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (pomodoroIsBreak) colors.accent.copy(alpha = 0.2f) else colors.keySpecialBackground)
                        .clickable { onHaptic(); onResetPomodoro(true) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("৫মি", color = if (pomodoroIsBreak) colors.accent else colors.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable { onHaptic(); onTogglePomodoroRun() }
                        .testTag("pomodoro_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pomodoroRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (pomodoroRunning) "বিরতি" else "শুরু",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onHaptic(); onResetPomodoro(pomodoroIsBreak) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "রিসেট", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            val label = if (pomodoroIsBreak) "ব্রেক" else "ফোকাস"
                            onInsertTimerStatus("⏱ $label টাইমার: $bnClock বাকি")
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("বসান", color = colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Quick Add Task Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.keyBackground)
                .border(
                    width = if (isTodoInputActive) 1.2.dp else 0.5.dp,
                    color = if (isTodoInputActive) colors.accent else colors.keySpecialBackground,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable {
                    onHaptic()
                    onToggleTodoInput()
                }
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .testTag("todo_input_trigger"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (todoDraftInput.isNotEmpty()) todoDraftInput else "+ নতুন টু-ডু টাস্ক লিখুন (ট্যাপ করুন)...",
                color = if (todoDraftInput.isNotEmpty()) colors.textPrimary else colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (todoDraftInput.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.accent)
                        .clickable {
                            onHaptic()
                            onSubmitTodoDraft()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("todo_add_submit_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "যোগ", tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("যোগ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (todos.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pending = todos.count { !it.done }
                Text(
                    text = "কাজের তালিকা (${BengaliCalendarUtils.toBengaliDigits(pending.toString())} বাকি)",
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent.copy(alpha = 0.14f))
                        .clickable { onHaptic(); onInsertAllTodos() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("সব চেকলিস্ট বসান", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (todos.isEmpty()) {
            Text(
                text = "এখনো কোনো টাস্ক নেই। উপরের বক্সে ট্যাপ করে দ্রুত কাজ লিখে রাখুন!",
                color = colors.textSecondary,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            todos.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.keyBackground)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onHaptic(); onToggleTodoDone(item.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.done) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "সম্পন্ন টগল",
                            tint = if (item.done) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = item.text,
                        color = if (item.done) colors.textSecondary else colors.textPrimary,
                        fontSize = 12.sp,
                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onHaptic(); onInsertTodoText(item.text) }
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onHaptic(); onDeleteTodo(item.id) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
