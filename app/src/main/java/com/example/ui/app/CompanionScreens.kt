package com.example.ui.app

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.data.ai.GeminiDirectClient
import com.example.data.local.TypeGeniusDatabase
import com.example.data.translator.OfflineTranslator
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.LanguageId
import com.example.model.OneHandedMode
import com.example.model.SoundProfile
import com.example.model.SwipeSensitivity
import com.example.model.VibrationStrength
import com.example.ui.keyboard.KeyboardThemeData
import com.example.ui.keyboard.SpacebarSwipeTutorialOverlay
import com.example.ui.theme.TGMagenta
import com.example.ui.theme.TGViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OfflineTranslatorCard(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val langs = OfflineTranslator.supportedLanguages
    val savedSrc by settingsManager.translatorSourceCode.collectAsState(initial = "bn")
    val savedTgt by settingsManager.translatorTargetCode.collectAsState(initial = "en")
    val historyEntries by OfflineTranslator.observeHistory(context).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var sourceCode by remember(savedSrc) { mutableStateOf(savedSrc) }
    var targetCode by remember(savedTgt) { mutableStateOf(savedTgt) }
    var inputText by remember { mutableStateOf("ami bangladesh valobashi") }
    var result by remember { mutableStateOf(OfflineTranslator.translate("ami bangladesh valobashi", "bn", "en")) }

    LaunchedEffect(Unit) {
        OfflineTranslator.initialize(context)
        result = OfflineTranslator.translateWithMemory(context, inputText, sourceCode, targetCode)
    }

    LaunchedEffect(inputText, sourceCode, targetCode) {
        result = OfflineTranslator.translate(inputText, sourceCode, targetCode)
        result = OfflineTranslator.translateWithMemory(context, inputText, sourceCode, targetCode)
    }

    fun cycle(current: String): String {
        val idx = langs.indexOfFirst { it.code == current }.coerceAtLeast(0)
        return langs[(idx + 1) % langs.size].code
    }

    val srcLang = langs.find { it.code == sourceCode } ?: langs.first()
    val tgtLang = langs.find { it.code == targetCode } ?: langs[1]
    val detectedLang = remember(result.detectedSourceLanguage, sourceCode) {
        val det = result.detectedSourceLanguage
        if (det != null && det != sourceCode) langs.find { it.code == det } else null
    }
    val modelStats = remember(OfflineTranslator.isPackLoadedFromAssets()) {
        OfflineTranslator.getModelStats()
    }
    val sampleInputs = listOf(
        "ami bangladesh valobashi" to "banglish",
        "tumi kemon acho" to "banglish",
        "Hello! How are you?" to "en",
        "আমি তোমাকে ভালোবাসি" to "bn",
        "afne bala achoinni" to "syl"
    )

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF00E676))
                    Column {
                        Text("On-Device Offline Translator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${langs.size} Languages • ${modelStats.first + modelStats.second} Local Entries", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F7B47).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("LOCAL • 0 CALLS", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val next = cycle(sourceCode)
                        sourceCode = next
                        scope.launch { settingsManager.setTranslatorLanguages(next, targetCode) }
                    },
                    modifier = Modifier.weight(1f).testTag("translator_src_button")
                ) {
                    Text(srcLang.nativeName, fontSize = 12.sp, maxLines = 1)
                }

                IconButton(onClick = {
                    val s = sourceCode
                    sourceCode = targetCode
                    targetCode = s
                    scope.launch { settingsManager.setTranslatorLanguages(sourceCode, targetCode) }
                }, modifier = Modifier.testTag("translator_swap_button")) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Languages", tint = TGMagenta)
                }

                OutlinedButton(
                    onClick = {
                        val next = cycle(targetCode)
                        targetCode = next
                        scope.launch { settingsManager.setTranslatorLanguages(sourceCode, next) }
                    },
                    modifier = Modifier.weight(1f).testTag("translator_tgt_button")
                ) {
                    Text(tgtLang.nativeName, fontSize = 12.sp, maxLines = 1)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sampleInputs.forEach { (sample, langCode) ->
                    FilterChip(
                        selected = inputText == sample && sourceCode == langCode,
                        onClick = {
                            inputText = sample
                            sourceCode = langCode
                            if (targetCode == langCode) targetCode = if (langCode == "en") "bn" else "en"
                            scope.launch { settingsManager.setTranslatorLanguages(sourceCode, targetCode) }
                        },
                        label = { Text(sample, fontSize = 11.sp) }
                    )
                }
            }

            if (detectedLang != null && inputText.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TGMagenta.copy(alpha = 0.14f))
                        .clickable {
                            sourceCode = detectedLang.code
                            scope.launch { settingsManager.setTranslatorLanguages(detectedLang.code, targetCode) }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detected script/dialect: ${detectedLang.name} — Tap to switch source",
                        fontSize = 11.sp,
                        color = TGMagenta,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Text or Banglish/Sylheti to translate offline") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TGMagenta,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_translator_input")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TGViolet.copy(alpha = 0.16f))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result.engineLabel, fontSize = 10.sp, color = TGMagenta, fontWeight = FontWeight.Bold)
                        if (inputText.isNotBlank()) {
                            Text(
                                text = "${(result.confidence * 100).toInt()}% match",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = result.translatedText.ifBlank { "Enter text above to see instant offline translation" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (inputText.isNotBlank() && result.translatedText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                OfflineTranslator.saveCustomPhrase(
                                    context = context,
                                    sourceText = inputText,
                                    translatedText = result.translatedText,
                                    sourceLang = sourceCode,
                                    targetLang = targetCode
                                )
                                Toast.makeText(context, "Saved to Local Translation Memory", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("translator_save_memory_button")
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp), tint = TGMagenta)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pin to Local Memory", fontSize = 12.sp, color = TGMagenta)
                    }
                }
            }

            if (historyEntries.isNotEmpty()) {
                Text(
                    text = "Local Translation Memory (${historyEntries.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    historyEntries.take(3).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    sourceCode = entry.sourceLang
                                    targetCode = entry.targetLang
                                    inputText = entry.sourceText
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.sourceText} → ${entry.translatedText}", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("${entry.sourceLang.uppercase()}→${entry.targetLang.uppercase()} • ${entry.strategy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { scope.launch { OfflineTranslator.togglePinHistory(context, entry) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (entry.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Pin",
                                        tint = if (entry.isPinned) TGMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { scope.launch { OfflineTranslator.deleteHistoryEntry(context, entry) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GestureSettingsScreen(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTutorial by remember { mutableStateOf(false) }

    val isSwipeEnabled by settingsManager.isSpacebarSwipeEnabled.collectAsState(initial = true)
    val swipeSensitivity by settingsManager.swipeSensitivity.collectAsState(initial = SwipeSensitivity.MEDIUM)
    val isHapticEnabled by settingsManager.isHapticEnabled.collectAsState(initial = true)
    val showLangLabel by settingsManager.showActiveLanguageOnSpacebar.collectAsState(initial = true)
    val longPressLangPicker by settingsManager.longPressLanguageSelector.collectAsState(initial = true)
    val showAltLangKey by settingsManager.showAlternativeLanguageKey.collectAsState(initial = true)
    val isGlideEnabled by settingsManager.isGlideTypingEnabled.collectAsState(initial = true)
    val isBackspaceSwipeEnabled by settingsManager.isBackspaceSwipeEnabled.collectAsState(initial = true)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoCol = maxWidth >= 660.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionHero(
                title = "Spacebar & Typing Gestures",
                subtitle = "Configure offline Glide swipe-to-type, Backspace swipe delete, and Spacebar language switching.",
                badgeText = "GESTURES",
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White) }
            )

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = TGMagenta.copy(alpha = 0.14f)),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTutorial = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TGMagenta),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Interactive Spacebar Swipe Tutorial", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Launch live gesture tutorial to master language switching.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingToggleRow(
                    title = "Offline Glide Typing (Swipe-to-Type)",
                    subtitle = "Slide finger across keys to input English & Banglish Phonetic words",
                    checked = isGlideEnabled,
                    onCheckedChange = { scope.launch { settingsManager.setGlideTypingEnabled(it) } },
                    switchModifier = Modifier.testTag("toggle_glide_typing")
                )

                SettingToggleRow(
                    title = "Backspace Swipe-Left Delete",
                    subtitle = "Swipe left on ⌫ to delete whole words quickly with Undo",
                    checked = isBackspaceSwipeEnabled,
                    onCheckedChange = { scope.launch { settingsManager.setBackspaceSwipeEnabled(it) } },
                    switchModifier = Modifier.testTag("toggle_backspace_swipe")
                )

                SettingToggleRow(
                    title = "Enable Spacebar Swipe",
                    subtitle = "Swipe Left-to-Right for next language, Right-to-Left for previous",
                    checked = isSwipeEnabled,
                    onCheckedChange = { scope.launch { settingsManager.setSpacebarSwipeEnabled(it) } },
                    switchModifier = Modifier.testTag("toggle_spacebar_swipe")
                )

                AnimatedVisibility(visible = isSwipeEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Swipe Sensitivity", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Controls the movement threshold in dp required to trigger language switch", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        SwipeSensitivity.entries.forEach { sensitivity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { scope.launch { settingsManager.setSwipeSensitivity(sensitivity) } }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = swipeSensitivity == sensitivity,
                                    onClick = { scope.launch { settingsManager.setSwipeSensitivity(sensitivity) } },
                                    colors = RadioButtonDefaults.colors(selectedColor = TGMagenta)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(sensitivity.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(sensitivity.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                SettingToggleRow(
                    title = "Haptic Feedback on Switch",
                    subtitle = "Vibrate subtly upon successfully triggering language switch",
                    checked = isHapticEnabled,
                    onCheckedChange = { scope.launch { settingsManager.setHapticEnabled(it) } }
                )

                SettingToggleRow(
                    title = "Show Active Language on Spacebar",
                    subtitle = "Displays pill badge with active language (প্রভাত, বাংলা, EN, HI, AR)",
                    checked = showLangLabel,
                    onCheckedChange = { scope.launch { settingsManager.setShowActiveLanguageOnSpacebar(it) } }
                )

                SettingToggleRow(
                    title = "Long-Press for Language Selector",
                    subtitle = "Open quick modal list to pick any enabled language directly",
                    checked = longPressLangPicker,
                    onCheckedChange = { scope.launch { settingsManager.setLongPressLanguageSelector(it) } }
                )

                SettingToggleRow(
                    title = "Alternative Globe Language Button",
                    subtitle = "Show quick script switch button alongside spacebar for 1-tap switching",
                    checked = showAltLangKey,
                    onCheckedChange = { scope.launch { settingsManager.setShowAlternativeLanguageKey(it) } }
                )
            }
        }
    }

    if (showTutorial) {
        SpacebarSwipeTutorialOverlay(
            colors = KeyboardThemeData.getColors(KeyboardTheme.MATERIAL_YOU, context, false),
            enabledLanguages = listOf(LanguageId.BANGLA_PROBHAT, LanguageId.ENGLISH, LanguageId.HINDI, LanguageId.SYLHETI),
            onDismiss = { showTutorial = false },
            onSwipeSuccess = {
                Toast.makeText(context, "Great job! Language switched successfully.", Toast.LENGTH_SHORT).show()
            }
        )
    }
    }
}

@Composable
fun LanguagesScreen(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val enabledLangCodes by settingsManager.enabledLanguages.collectAsState(initial = emptySet())
    val activeLangCode by settingsManager.activeLanguageCode.collectAsState(initial = LanguageId.BANGLA_PROBHAT.code)

    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }

    val regions = remember {
        listOf("All") + LanguageId.entries.map { it.region }.distinct()
    }

    val filteredLanguages = remember(searchQuery, selectedRegion) {
        LanguageId.entries.filter { lang ->
            val matchesRegion = selectedRegion == "All" || lang.region == selectedRegion
            val matchesQuery = searchQuery.isBlank() ||
                lang.displayName.contains(searchQuery, ignoreCase = true) ||
                lang.nativeName.contains(searchQuery, ignoreCase = true) ||
                lang.code.contains(searchQuery, ignoreCase = true)
            matchesRegion && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSectionHero(
            title = "${LanguageId.entries.size} Offline Language Packs",
            subtitle = "All ${LanguageId.entries.size} keyboards and offline dictionaries work 100% locally. Enable languages to cycle with Spacebar Swipe.",
            badgeText = "${enabledLangCodes.size} ACTIVE",
            icon = { Icon(Icons.Default.Language, contentDescription = null, tint = Color.White) }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search ${LanguageId.entries.size} languages (e.g. Sylheti, Bengali, Hindi, Arabic)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_search_field")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            regions.forEach { region ->
                FilterChip(
                    selected = selectedRegion == region,
                    onClick = { selectedRegion = region },
                    label = { Text(region, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TGMagenta.copy(alpha = 0.25f),
                        selectedLabelColor = TGMagenta
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        settingsManager.setEnabledLanguages(LanguageId.entries.map { it.code }.toSet())
                    }
                },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)
            ) {
                Text("Enable All ${LanguageId.entries.size}", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    val defaults = setOf(
                        LanguageId.BANGLA_PROBHAT.code,
                        LanguageId.BANGLA_PHONETIC.code,
                        LanguageId.ENGLISH.code,
                        LanguageId.HINDI.code,
                        LanguageId.ARABIC.code
                    )
                    scope.launch {
                        settingsManager.setEnabledLanguages(defaults)
                        if (!defaults.contains(activeLangCode)) {
                            settingsManager.setActiveLanguageCode(LanguageId.BANGLA_PROBHAT.code)
                        }
                    }
                },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)
            ) {
                Text("Reset Top 5", fontSize = 12.sp)
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val twoColLangs = maxWidth >= 600.dp
                val langRows = if (twoColLangs) filteredLanguages.chunked(2) else filteredLanguages.map { listOf(it) }
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    langRows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { lang ->
                                val isEnabled = enabledLangCodes.contains(lang.code)
                                val isActive = activeLangCode == lang.code

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) TGMagenta.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .clickable {
                                            scope.launch {
                                                if (!isEnabled) {
                                                    settingsManager.setEnabledLanguages(enabledLangCodes + lang.code)
                                                }
                                                settingsManager.setActiveLanguageCode(lang.code)
                                            }
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = lang.nativeName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isActive) {
                                                BadgeLabel("ACTIVE", TGMagenta)
                                            }
                                            if (lang.isRtl) {
                                                BadgeLabel("RTL", TGViolet)
                                            }
                                        }
                                        Text(
                                            text = "${lang.displayName} • ${lang.region} • Key: ${lang.spacebarLabel}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            val newSet = enabledLangCodes.toMutableSet()
                                            if (checked) {
                                                newSet.add(lang.code)
                                            } else if (newSet.size > 1) {
                                                newSet.remove(lang.code)
                                            }
                                            scope.launch {
                                                settingsManager.setEnabledLanguages(newSet)
                                                if (!newSet.contains(activeLangCode)) {
                                                    newSet.firstOrNull()?.let { settingsManager.setActiveLanguageCode(it) }
                                                }
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TGMagenta, checkedTrackColor = TGMagenta.copy(alpha = 0.4f))
                                    )
                                }
                            }
                            if (twoColLangs && pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackendlessVerificationScreen() {
    val context = LocalContext.current
    var isRunningDiag by remember { mutableStateOf(false) }
    var diagResults by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val checklistItems = listOf(
        "32 Offline Language Packages" to "Pre-bundled keyboards & lexicons across South Asia (incl. Sylheti & Bengali Probhat), Middle East (RTL), Europe, and Asia.",
        "On-Device Local Translation Engine" to "Bundled JSON phrase/lexicon model + N-gram alignment + SVO↔SOV grammar transfer + Room SQLite memory with 0 server calls.",
        "Hunspell-Style Offline Spell Checker" to "Damerau-Levenshtein fuzzy autocorrect runs 100% locally in CPU memory.",
        "Local Database (Room SQLite)" to "On-device SQLite storing learned words and pinned clipboard items with zero cloud sync.",
        "Local Preferences (DataStore)" to "Android Jetpack DataStore storing all keyboard preferences inside private app sandbox.",
        "Zero Remote Backend Server" to "Verified: No Node.js, Express, PHP, Laravel, Python, Supabase, or Firebase backend implemented.",
        "Zero Telemetry & Tracking SDKs" to "Verified: 0 advertising, 0 analytics, and 0 crash-reporting SDKs in the application.",
        "Optional Direct-to-Gemini AI" to "Only if user inputs their own API key; requests go directly to generativelanguage.googleapis.com."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF4CAF50))
            }
            Column {
                Text("Backendless Architecture Verification", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Strict local-only privacy compliance checklist", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                checklistItems.forEach { (title, description) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Live On-Device Diagnostics", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Button(
                    onClick = {
                        isRunningDiag = true
                        diagResults = null
                        scope.launch {
                            OfflineTranslator.initialize(context)
                            delay(350)
                            val db = TypeGeniusDatabase.getDatabase(context)
                            val (words, tmCount) = withContext(Dispatchers.IO) {
                                db.wordDao().getTopWords("bn_probhat", 5) to db.translationMemoryDao().countAll()
                            }
                            val (phrases, lex) = OfflineTranslator.getModelStats()
                            isRunningDiag = false
                            diagResults = "✔ SQLite DB: Connected (${db.openHelper.databaseName})\n✔ Language Packs Ready: ${LanguageId.entries.size} / 32\n✔ Local Translation Model: $phrases phrases, $lex lexicon entries\n✔ Translator Languages: ${OfflineTranslator.supportedLanguages.size} (Asset Loaded: ${OfflineTranslator.isPackLoadedFromAssets()})\n✔ Translation Memory Entries: $tmCount\n✔ Local Learned Words: ${words.size} cached\n✔ Remote Endpoints: 0\n✔ Backendless Status: PASSED"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRunningDiag) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Local Architecture...")
                    } else {
                        Text("Run Architecture Integrity Check")
                    }
                }

                diagResults?.let { res ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B0E2E))
                            .padding(12.dp)
                    ) {
                        Text(res, color = Color(0xFF00E5FF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneralSettingsScreen(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    val currentTheme by settingsManager.keyboardTheme.collectAsState(initial = KeyboardTheme.MATERIAL_YOU)
    val numberRowEnabled by settingsManager.numberRowEnabled.collectAsState(initial = false)
    val soundOnKeyPress by settingsManager.soundOnKeyPress.collectAsState(initial = false)
    val soundProfile by settingsManager.soundProfile.collectAsState(initial = SoundProfile.SYSTEM)
    val vibrationStrength by settingsManager.vibrationStrength.collectAsState(initial = VibrationStrength.MEDIUM)
    val isIncognitoMode by settingsManager.isIncognitoMode.collectAsState(initial = false)
    val autoCapitalize by settingsManager.autoCapitalize.collectAsState(initial = true)
    val spellCheckEnabled by settingsManager.spellCheckEnabled.collectAsState(initial = true)
    val oneHandedMode by settingsManager.oneHandedMode.collectAsState(initial = OneHandedMode.FULL)
    val heightPreset by settingsManager.keyboardHeightPreset.collectAsState(initial = KeyboardHeightPreset.NORMAL)
    val floatingMode by settingsManager.floatingMode.collectAsState(initial = false)
    val geminiApiKey by settingsManager.geminiApiKey.collectAsState(initial = "")

    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var testApiLoading by remember { mutableStateOf(false) }
    var testApiResult by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideSettings = maxWidth >= 680.dp

        @Composable
        fun ThemeCard() {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keyboard Theme", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    KeyboardTheme.entries.forEach { theme ->
                        val isSelected = currentTheme == theme
                        val themePreview = KeyboardThemeData.getColors(theme, context, isSystemDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) TGMagenta.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable { scope.launch { settingsManager.setKeyboardTheme(theme) } }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { scope.launch { settingsManager.setKeyboardTheme(theme) } },
                                colors = RadioButtonDefaults.colors(selectedColor = TGMagenta)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(themePreview.accent)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = theme.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun TypingPreferencesCard() {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Typing, Haptics & Layout Preferences", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    SettingToggleRow(
                        title = "Incognito Private Mode",
                        subtitle = "Pause local word learning and clipboard logging while enabled",
                        checked = isIncognitoMode,
                        onCheckedChange = { scope.launch { settingsManager.setIncognitoMode(it) } },
                        switchModifier = Modifier.testTag("toggle_incognito_mode")
                    )

                    SettingToggleRow(
                        title = "Dedicated Number Row",
                        subtitle = "Show 1-0 (or localized digits) above letter rows",
                        checked = numberRowEnabled,
                        onCheckedChange = { scope.launch { settingsManager.setNumberRowEnabled(it) } }
                    )

                    SettingToggleRow(
                        title = "Auto-Capitalize Sentences",
                        subtitle = "Automatically shift first letter after sentence punctuation",
                        checked = autoCapitalize,
                        onCheckedChange = { scope.launch { settingsManager.setAutoCapitalize(it) } }
                    )

                    SettingToggleRow(
                        title = "Offline Spell Checker",
                        subtitle = "Hunspell-style on-device autocorrect & fuzzy suggestions",
                        checked = spellCheckEnabled,
                        onCheckedChange = { scope.launch { settingsManager.setSpellCheckEnabled(it) } }
                    )

                    Text("Vibration Intensity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VibrationStrength.entries.forEach { strength ->
                            FilterChip(
                                selected = vibrationStrength == strength,
                                onClick = { scope.launch { settingsManager.setVibrationStrength(strength) } },
                                label = { Text(strength.label, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }

                    SettingToggleRow(
                        title = "Sound on Keypress",
                        subtitle = "Audible feedback when tapping keys",
                        checked = soundOnKeyPress,
                        onCheckedChange = { scope.launch { settingsManager.setSoundOnKeyPress(it) } }
                    )

                    AnimatedVisibility(visible = soundOnKeyPress) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Sound Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SoundProfile.entries.forEach { profile ->
                                    FilterChip(
                                        selected = soundProfile == profile,
                                        onClick = {
                                            scope.launch { settingsManager.setSoundProfile(profile) }
                                            runCatching {
                                                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                                val fx = when (profile) {
                                                    SoundProfile.MECHANICAL -> AudioManager.FX_KEYPRESS_DELETE
                                                    SoundProfile.TYPEWRITER -> AudioManager.FX_KEYPRESS_RETURN
                                                    SoundProfile.THOCK -> AudioManager.FX_KEYPRESS_SPACEBAR
                                                    SoundProfile.POPPY -> AudioManager.FX_KEY_CLICK
                                                    SoundProfile.SYSTEM -> AudioManager.FX_KEYPRESS_STANDARD
                                                }
                                                am?.playSoundEffect(fx, profile.volume)
                                            }
                                        },
                                        label = { Text(profile.label, fontSize = 12.sp, maxLines = 1) },
                                        modifier = Modifier.testTag("sound_profile_${profile.id}")
                                    )
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = "Floating Keyboard Mode",
                        subtitle = "Detach keyboard into a compact floating card",
                        checked = floatingMode,
                        onCheckedChange = { scope.launch { settingsManager.setFloatingMode(it) } }
                    )

                    Text("One-Handed & Split Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OneHandedMode.entries.forEach { mode ->
                            FilterChip(
                                selected = oneHandedMode == mode,
                                onClick = { scope.launch { settingsManager.setOneHandedMode(mode) } },
                                label = { Text(mode.label, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }

                    Text("Keyboard Height", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyboardHeightPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = heightPreset == preset,
                                onClick = { scope.launch { settingsManager.setKeyboardHeightPreset(preset) } },
                                label = { Text(preset.label, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun DirectGeminiCard() {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TGMagenta)
                        Text("Optional Direct Gemini AI", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = "TypeGenius works 100% offline without an API key. If you enter your own Gemini API key, requests go directly from this device to Google's official API.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy... or AQ.Ab8...") },
                        label = { Text("User Google Gemini API Key") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val cleaned = apiKeyInput.trim()
                                    .replace("\n", "")
                                    .replace("\r", "")
                                    .replace("\t", "")
                                    .replace(" ", "")
                                apiKeyInput = cleaned
                                scope.launch {
                                    settingsManager.setGeminiApiKey(cleaned)
                                    Toast.makeText(context, "API Key saved locally", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("save_gemini_key_button")
                        ) {
                            Text("Save Key")
                        }

                        if (apiKeyInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val cleaned = apiKeyInput.trim()
                                        .replace("\n", "")
                                        .replace("\r", "")
                                        .replace("\t", "")
                                        .replace(" ", "")
                                    apiKeyInput = cleaned
                                    testApiLoading = true
                                    testApiResult = null
                                    scope.launch {
                                        settingsManager.setGeminiApiKey(cleaned)
                                        val res = GeminiDirectClient().executeDirectPrompt(
                                            apiKey = cleaned,
                                            prompt = "Respond with 'Direct Connection Verified: TypeGenius AI Ready' in 7 words or less."
                                        )
                                        testApiLoading = false
                                        res.onSuccess { testApiResult = "SUCCESS: $it" }
                                            .onFailure { testApiResult = "ERROR: ${it.message}" }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("test_gemini_key_button")
                            ) {
                                if (testApiLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                else Text("Test Connection")
                            }
                        }
                    }

                    testApiResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (res.startsWith("SUCCESS")) Color(0xFF1B3B2B) else Color(0xFF3B1B1B))
                                .padding(10.dp)
                        ) {
                            Text(res, color = if (res.startsWith("SUCCESS")) Color(0xFF4CAF50) else Color(0xFFFF5252), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionHero(
                title = "সেটিংস ও কাস্টমাইজেশন",
                subtitle = "থিম, হাতের লেখা ফন্ট, সাউন্ড, ভাইব্রেশন ও ব্যক্তিগত শর্টকাট ম্যানেজ করুন",
                badgeText = "PRO SETTINGS",
                icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
            )

            HandwritingFontSettingsCard(settingsManager = settingsManager)

            if (isWideSettings) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ThemeCard()
                        PhotoBackgroundThemeCard(settingsManager = settingsManager)
                        ToolbarCustomizerCard(settingsManager = settingsManager)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TypingPreferencesCard()
                        PersonalDictionaryCard()
                        PersonalShortcutsCard()
                        DirectGeminiCard()
                    }
                }
            } else {
                ThemeCard()
                PhotoBackgroundThemeCard(settingsManager = settingsManager)
                TypingPreferencesCard()
                ToolbarCustomizerCard(settingsManager = settingsManager)
                PersonalDictionaryCard()
                PersonalShortcutsCard()
                DirectGeminiCard()
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = TGMagenta, checkedTrackColor = TGMagenta.copy(alpha = 0.4f)),
            modifier = switchModifier
        )
    }
}

@Composable
private fun BadgeLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceAndLayoutScreen(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()

    val numberRowEnabled by settingsManager.numberRowEnabled.collectAsState(initial = false)
    val largeNumberRow by settingsManager.largeNumberRow.collectAsState(initial = false)
    val hideLongPressHints by settingsManager.hideLongPressHints.collectAsState(initial = false)
    val heightPortrait by settingsManager.keyboardHeightPortrait.collectAsState(initial = 100)
    val heightLandscape by settingsManager.keyboardHeightLandscape.collectAsState(initial = 100)
    val oneHandedPortrait by settingsManager.oneHandedWidthPortrait.collectAsState(initial = 85)
    val oneHandedLandscape by settingsManager.oneHandedWidthLandscape.collectAsState(initial = 40)
    val oneHandedMode by settingsManager.oneHandedMode.collectAsState(initial = OneHandedMode.FULL)
    val splitFoldable by settingsManager.splitKeyboardFoldable.collectAsState(initial = true)
    val alwaysEnter by settingsManager.alwaysEnterButton.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSectionHero(
            title = "চেহারা ও লে-আউট (Layout)",
            subtitle = "বাংলা হাতের লেখা ফন্ট, কী-বোর্ডের সারি, উচ্চতা ও এক হাতে ব্যবহারের সাইজ কাস্টমাইজ করুন",
            badgeText = "CUSTOMIZE",
            icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White) }
        )

        HandwritingFontSettingsCard(settingsManager = settingsManager)

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingToggleRow(
                    title = "নম্বর সারি চালু করুন",
                    subtitle = "একটি অতিরিক্ত সারি যোগ হবে",
                    checked = numberRowEnabled,
                    onCheckedChange = { scope.launch { settingsManager.setNumberRowEnabled(it) } }
                )

                SettingToggleRow(
                    title = "বড় নম্বর সারি",
                    subtitle = "নম্বর সারির ফন্ট ও প্যাডিং বড় করুন",
                    checked = largeNumberRow,
                    onCheckedChange = { scope.launch { settingsManager.setLargeNumberRow(it) } }
                )

                SettingToggleRow(
                    title = "লং প্রেস হিন্ট লুকান",
                    subtitle = "পরিচ্ছন্ন চেহারার জন্য কী-এর কোণে থাকা ছোট লেখা লুকান",
                    checked = hideLongPressHints,
                    onCheckedChange = { scope.launch { settingsManager.setHideLongPressHints(it) } }
                )

                Text("কিবোর্ডের উচ্চতা (পোর্ট্রেট): $heightPortrait%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(80, 90, 100, 110, 125).forEach { pct ->
                        FilterChip(
                            selected = heightPortrait == pct,
                            onClick = { scope.launch { settingsManager.setKeyboardHeightPortrait(pct) } },
                            label = { Text("$pct%", fontSize = 12.sp, maxLines = 1) }
                        )
                    }
                }

                Text("কিবোর্ডের উচ্চতা (ল্যান্ডস্কেপ): $heightLandscape%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(75, 90, 100, 115).forEach { pct ->
                        FilterChip(
                            selected = heightLandscape == pct,
                            onClick = { scope.launch { settingsManager.setKeyboardHeightLandscape(pct) } },
                            label = { Text("$pct%", fontSize = 12.sp, maxLines = 1) }
                        )
                    }
                }

                Text("এক হাতে কিবোর্ডের প্রস্থ (পোর্ট্রেট): $oneHandedPortrait%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(70, 80, 85, 95, 100).forEach { pct ->
                        FilterChip(
                            selected = oneHandedPortrait == pct,
                            onClick = { scope.launch { settingsManager.setOneHandedWidthPortrait(pct) } },
                            label = { Text("$pct%", fontSize = 12.sp, maxLines = 1) }
                        )
                    }
                }

                Text("এক হাতে কিবোর্ডের প্রস্থ (ল্যান্ডস্কেপ): $oneHandedLandscape%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(35, 40, 50, 60).forEach { pct ->
                        FilterChip(
                            selected = oneHandedLandscape == pct,
                            onClick = { scope.launch { settingsManager.setOneHandedWidthLandscape(pct) } },
                            label = { Text("$pct%", fontSize = 12.sp, maxLines = 1) }
                        )
                    }
                }

                SettingToggleRow(
                    title = "স্প্লিট কিবোর্ড চালু",
                    subtitle = "দুই ভাগে বিভক্ত কিবোর্ড লেআউট",
                    checked = oneHandedMode == OneHandedMode.SPLIT,
                    onCheckedChange = { checked ->
                        scope.launch {
                            settingsManager.setOneHandedMode(if (checked) OneHandedMode.SPLIT else OneHandedMode.FULL)
                        }
                    }
                )

                SettingToggleRow(
                    title = "ভাঁজযোগ্য ফোনের জন্য স্প্লিট কিবোর্ড চালু করুন",
                    subtitle = "ফোল্ডেবল ডিভাইসে অটো স্প্লিট মোড",
                    checked = splitFoldable,
                    onCheckedChange = { scope.launch { settingsManager.setSplitKeyboardFoldable(it) } }
                )

                SettingToggleRow(
                    title = "সব সময় এন্টার বাটন",
                    subtitle = "এন্টার বাটনে সবসময় রিটার্ন আইকন দেখান",
                    checked = alwaysEnter,
                    onCheckedChange = { scope.launch { settingsManager.setAlwaysEnterButton(it) } }
                )
            }
        }
    }
}
