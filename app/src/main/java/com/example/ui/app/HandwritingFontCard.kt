package com.example.ui.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomFontManager
import com.example.data.KeyboardFontPreset
import com.example.data.SettingsManager
import com.example.data.dictionary.FancyFontStyle
import com.example.model.KeyboardTheme
import com.example.ui.keyboard.HandwritingCardFormat
import com.example.ui.keyboard.HandwritingOutputGenerator
import com.example.ui.keyboard.HandwritingPaperTheme
import com.example.ui.keyboard.KeyboardThemeData
import com.example.ui.theme.TGMagenta
import com.example.ui.theme.TGViolet
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HandwritingFontSettingsCard(
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedFontId by settingsManager.keyboardFontId.collectAsState(initial = KeyboardFontPreset.ATMA_HANDWRITING.id)
    val customFontPath by settingsManager.customFontPath.collectAsState(initial = "")
    val customFontName by settingsManager.customFontName.collectAsState(initial = "")
    val activeFancyStyle by settingsManager.activeFancyFontStyle.collectAsState(initial = FancyFontStyle.NORMAL)
    val savedSignature by settingsManager.handwritingSignature.collectAsState(initial = "")
    val currentTheme by settingsManager.keyboardTheme.collectAsState(initial = KeyboardTheme.MATERIAL_YOU)
    val isDark = isSystemInDarkTheme()
    val kbColors = remember(currentTheme, isDark) {
        KeyboardThemeData.getColors(currentTheme, context, isDark)
    }

    var samplePreviewText by remember {
        mutableStateOf("আমার সোনার বাংলা, আমি তোমায় ভালোবাসি • Hello")
    }
    var signatureInput by remember(savedSignature) { mutableStateOf(savedSignature) }
    var shareFormat by remember { mutableStateOf(HandwritingCardFormat.CHAT_CARD) }

    val activeFontFamily = remember(selectedFontId, customFontPath) {
        CustomFontManager.getFontFamily(context, selectedFontId, customFontPath)
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = CustomFontManager.importFontFromUri(context, uri)
                res.onSuccess { (path, displayName) ->
                    settingsManager.setCustomFont(path, displayName)
                    settingsManager.setActiveFancyFontStyle(FancyFontStyle.HANDWRITING_CUSTOM)
                    Toast.makeText(
                        context,
                        "হাতের লেখা ফন্ট যুক্ত ও ফ্যান্সি ফন্টে অ্যাক্টিভ হয়েছে: $displayName",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "ফন্ট ফাইল লোড করা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(TGMagenta, TGViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = "Handwriting Font",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "বাংলা হাতের লেখা ফন্ট (Handwriting Font)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "কিবোর্ড কী ও সাজেশন বারের ফন্ট পরিবর্তন বা নিজস্ব .zip/.ttf যুক্ত করুন",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Keyboard + Handwriting Preview Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = kbColors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = samplePreviewText.ifBlank { "বাংলা হাতের লেখা প্রিভিউ" },
                        fontFamily = activeFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = kbColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("দ" to "ধ", "ক" to "খ", "ম" to "ঙ", "c" to "Copy", "v" to "Paste").forEach { (main, sec) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(kbColors.keyBackground)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = sec,
                                    fontSize = 7.5.sp,
                                    color = if (sec == "Copy" || sec == "Paste") kbColors.accent else kbColors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                                Text(
                                    text = main,
                                    fontFamily = activeFontFamily,
                                    fontSize = 18.sp,
                                    color = kbColors.textPrimary,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = samplePreviewText,
                onValueChange = { samplePreviewText = it },
                label = { Text("হাতের লেখা ফন্ট টেস্ট করুন") },
                textStyle = TextStyle(fontFamily = activeFontFamily, fontSize = 15.sp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TGMagenta,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("handwriting_preview_input")
            )

            // Font Presets Grid (responsive via BoxWithConstraints)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth >= 520.dp
                val presets = KeyboardFontPreset.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rows = if (twoColumns) presets.chunked(2) else presets.map { listOf(it) }
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val isSelected = selectedFontId == preset.id
                                val isCustom = preset == KeyboardFontPreset.CUSTOM_IMPORTED
                                val hasCustomFile = isCustom && customFontPath.isNotBlank()
                                val itemTitle = if (hasCustomFile && customFontName.isNotBlank()) {
                                    customFontName
                                } else {
                                    preset.displayName
                                }
                                val previewFont = remember(preset, customFontPath) {
                                    CustomFontManager.getFontFamily(context, preset.id, customFontPath)
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) TGMagenta.copy(alpha = 0.14f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) TGMagenta else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (isCustom && !hasCustomFile) {
                                                fontPickerLauncher.launch(
                                                    arrayOf(
                                                        "application/zip",
                                                        "font/ttf",
                                                        "font/otf",
                                                        "application/x-font-ttf",
                                                        "application/x-font-otf",
                                                        "application/octet-stream",
                                                        "*/*"
                                                    )
                                                )
                                            } else {
                                                scope.launch {
                                                    settingsManager.setKeyboardFontId(preset.id)
                                                    val mapped = FancyFontStyle.fromHandwritingPresetId(preset.id)
                                                    if (mapped != null) {
                                                        settingsManager.setActiveFancyFontStyle(mapped)
                                                    } else if (activeFancyStyle.isHandwritingFont) {
                                                        settingsManager.setActiveFancyFontStyle(FancyFontStyle.NORMAL)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("font_preset_${preset.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = itemTitle,
                                                fontFamily = previewFont,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isSelected) TGMagenta else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = preset.badge,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isCustom && !hasCustomFile) {
                                                "ট্যাপ করে Shahinur Rahman BanglaHandwriting-Regular.zip সিলেক্ট করুন"
                                            } else {
                                                preset.subtitle
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (hasCustomFile) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        CustomFontManager.clearCustomFont(context)
                                                        settingsManager.setCustomFont("", "")
                                                        settingsManager.setKeyboardFontId(KeyboardFontPreset.ATMA_HANDWRITING.id)
                                                        if (activeFancyStyle == FancyFontStyle.HANDWRITING_CUSTOM) {
                                                            settingsManager.setActiveFancyFontStyle(FancyFontStyle.HANDWRITING_ATMA)
                                                        }
                                                        Toast.makeText(context, "ইমপোর্ট করা ফন্ট রিসেট হয়েছে", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove imported font",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected Font",
                                                tint = TGMagenta,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (twoColumns && rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = signatureInput,
                onValueChange = {
                    signatureInput = it.take(48)
                    scope.launch { settingsManager.setHandwritingSignature(signatureInput) }
                },
                label = { Text("চিঠি / কার্ডে স্বাক্ষর (ঐচ্ছিক, যেমন: ইতি — শাহিনুর)") },
                textStyle = TextStyle(fontFamily = activeFontFamily, fontSize = 14.sp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TGMagenta,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("handwriting_signature_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HandwritingCardFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = shareFormat == fmt,
                        onClick = { shareFormat = fmt },
                        label = { Text(fmt.label, fontSize = 12.sp) }
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        fontPickerLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "font/ttf",
                                "font/otf",
                                "application/x-font-ttf",
                                "application/x-font-otf",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("import_handwriting_font_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "হাতের লেখা .zip / .ttf ফন্ট ফাইল ইমপোর্ট করুন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            val shareStyle = FancyFontStyle.fromHandwritingPresetId(selectedFontId)
                                ?: FancyFontStyle.HANDWRITING_ATMA
                            val file = HandwritingOutputGenerator.generateHandwritingPngFile(
                                context = context,
                                rawText = samplePreviewText.ifBlank { "আমার হাতের লেখা বাংলা" },
                                style = shareStyle,
                                customFontPath = customFontPath,
                                customFontName = customFontName,
                                paperTheme = HandwritingPaperTheme.NOTEBOOK,
                                format = shareFormat,
                                signature = signatureInput
                            )
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newUri(context.contentResolver, "Handwriting Output", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "হাতের লেখা শেয়ার করুন"))
                        }.onFailure {
                            Toast.makeText(context, "হাতের লেখা শেয়ার করা যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("share_handwriting_card_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = TGMagenta,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "হাতের লেখা ${shareFormat.label} শেয়ার করুন (WhatsApp/Messenger)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TGMagenta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHero(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: @Composable () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            TGMagenta.copy(alpha = 0.16f),
                            TGViolet.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TGMagenta, TGViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TGMagenta.copy(alpha = 0.18f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = TGMagenta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
