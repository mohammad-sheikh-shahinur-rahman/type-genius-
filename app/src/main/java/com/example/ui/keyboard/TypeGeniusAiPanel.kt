package com.example.ui.keyboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.CorrectionCategory
import com.example.data.ai.TypeGeniusAiAction
import com.example.data.ai.TypeGeniusAiEngine
import com.example.data.ai.TypeGeniusAiCorrection
import com.example.data.ai.TypeGeniusAiResult
import com.example.model.LanguageId
import kotlinx.coroutines.launch

@Composable
fun TypeGeniusAiPanel(
    colors: KeyboardColors,
    activeLanguage: LanguageId,
    geminiApiKey: String,
    sourceText: String,
    canUndo: Boolean,
    onRefreshSourceText: () -> String,
    onApplyText: (String) -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedAction by remember { mutableStateOf(TypeGeniusAiAction.CHECK) }
    var currentSource by remember(sourceText) { mutableStateOf(sourceText) }
    var result by remember { mutableStateOf<TypeGeniusAiResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val skippedIndices = remember { mutableStateListOf<Int>() }
    val previousVariations = remember { mutableStateListOf<String>() }

    fun execute(action: TypeGeniusAiAction, isVariation: Boolean = false) {
        val latest = onRefreshSourceText().ifBlank { currentSource }
        currentSource = latest
        if (!isVariation) {
            previousVariations.clear()
        } else {
            result?.rewrittenText?.takeIf { it.isNotBlank() }?.let { prev ->
                if (prev !in previousVariations) previousVariations.add(prev)
            }
        }
        selectedAction = action
        skippedIndices.clear()
        isLoading = true
        scope.launch {
            result = TypeGeniusAiEngine.runAction(
                text = latest,
                action = action,
                activeLanguage = activeLanguage,
                apiKey = geminiApiKey,
                avoidPrevious = if (isVariation) previousVariations.toList() else emptyList()
            )
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        execute(TypeGeniusAiAction.CHECK)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "TypeGenius AI",
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = if (geminiApiKey.isNotBlank()) "TypeGenius Gemini AI" else "TypeGenius Smart Proofread",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result?.statusNote?.ifBlank { "Analyzing field text..." }
                            ?: if (geminiApiKey.isNotBlank()) "Direct Gemini API active" else "On-device mode (add API key in Settings)",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canUndo) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent.copy(alpha = 0.18f))
                            .clickable {
                                onHaptic()
                                onUndo()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("typegenius_undo_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo AI edit",
                                tint = colors.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Undo", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = { onHaptic(); onOpenSettings() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Gemini API Key Settings",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = { onHaptic(); onClose() },
                    modifier = Modifier.size(30.dp).testTag("typegenius_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close TypeGenius AI",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Action Mode Chips (Check, Fix, Improve, Shorten, Formal, Casual, Native, English)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TypeGeniusAiAction.entries.forEach { action ->
                val isSelected = selectedAction == action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.accent else colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            execute(action)
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("typegenius_action_${action.name.lowercase()}")
                ) {
                    Text(
                        text = action.label,
                        color = if (isSelected) Color.White else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Body Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.keyBackground)
                .padding(10.dp)
        ) {
            when {
                isLoading -> {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (geminiApiKey.isNotBlank()) "Asking Gemini..." else "Checking text...",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                currentSource.isBlank() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No text in field yet",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Type a message first, then tap Check, Rewrite, or Native.",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                selectedAction == TypeGeniusAiAction.CHECK -> {
                    val corrections = result?.corrections.orEmpty()
                    if (corrections.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "No mistakes found",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentSource,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val activeList = corrections.filterIndexed { idx, _ -> idx !in skippedIndices }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                corrections.forEachIndexed { idx, c ->
                                    val isKept = idx !in skippedIndices
                                    CorrectionCard(
                                        correction = c,
                                        isKept = isKept,
                                        colors = colors,
                                        onToggleKeep = {
                                            onHaptic()
                                            if (isKept) skippedIndices.add(idx) else skippedIndices.remove(idx)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.keySpecialBackground)
                                        .clickable {
                                            onHaptic()
                                            execute(TypeGeniusAiAction.CHECK)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Re-check", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1.6f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (activeList.isNotEmpty()) colors.accent else colors.keySpecialBackground)
                                        .clickable(enabled = activeList.isNotEmpty()) {
                                            onHaptic()
                                            val patched = TypeGeniusAiEngine.applyCorrections(currentSource, activeList)
                                            onApplyText(patched)
                                            currentSource = patched
                                            execute(TypeGeniusAiAction.CHECK)
                                        }
                                        .padding(vertical = 8.dp)
                                        .testTag("typegenius_apply_fixes_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Replace (${activeList.size})",
                                        color = if (activeList.isNotEmpty()) Color.White else colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    val res = result
                    val rewritten = res?.rewrittenText.orEmpty()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = rewritten.ifBlank { currentSource },
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            res?.nativeReadAs?.takeIf { it.isNotBlank() }?.let { backRead ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keySpecialBackground)
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Read as: $backRead",
                                        color = colors.textSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.keySpecialBackground)
                                    .clickable {
                                        onHaptic()
                                        execute(selectedAction, isVariation = true)
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("typegenius_try_another_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                    Text("Try another", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.accent)
                                    .clickable(enabled = rewritten.isNotBlank()) {
                                        onHaptic()
                                        onApplyText(rewritten)
                                        currentSource = rewritten
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("typegenius_replace_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Replace", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectionCard(
    correction: TypeGeniusAiCorrection,
    isKept: Boolean,
    colors: KeyboardColors,
    onToggleKeep: () -> Unit
) {
    val catColor = when (correction.category) {
        CorrectionCategory.GRAMMAR -> Color(0xFF42A5F5)
        CorrectionCategory.SPELLING -> Color(0xFFEF5350)
        CorrectionCategory.PUNCTUATION -> Color(0xFFFFA726)
        CorrectionCategory.STYLE -> Color(0xFFAB47BC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isKept) colors.keySpecialBackground
                else colors.keySpecialBackground.copy(alpha = 0.45f)
            )
            .clickable(onClick = onToggleKeep)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(catColor.copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = correction.category.name.lowercase(),
                        color = catColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFFEF5350), textDecoration = TextDecoration.LineThrough)) {
                            append(correction.original)
                        }
                        append("  →  ")
                        withStyle(SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)) {
                            append(correction.replacement)
                        }
                    },
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = correction.explanation,
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isKept) colors.accent else colors.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isKept) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isKept) "Keep fix" else "Skip fix",
                tint = if (isKept) Color.White else colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
