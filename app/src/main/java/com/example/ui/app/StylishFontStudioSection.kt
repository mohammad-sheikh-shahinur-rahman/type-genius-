package com.example.ui.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.CustomFontManager
import com.example.data.SettingsManager
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.FontCategory
import com.example.data.dictionary.UnicodeTextStyler
import com.example.ui.keyboard.HandwritingOutputGenerator
import com.example.ui.keyboard.HandwritingPaperTheme
import com.example.ui.theme.TGMagenta
import kotlinx.coroutines.launch

@Composable
fun StylishFontStudioCard(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeFont by settingsManager.activeFancyFontStyle.collectAsState(initial = FancyFontStyle.NORMAL)
    val customFontPath by settingsManager.customFontPath.collectAsState(initial = "")
    val customFontName by settingsManager.customFontName.collectAsState(initial = "")
    val hasCustomFont = remember(customFontPath) { CustomFontManager.hasCustomFont(customFontPath) }

    var inputText by remember { mutableStateOf("TypeGenius আমার বাংলা") }
    var selectedCategory by remember { mutableStateOf(FontCategory.ALL) }
    var showAll by remember { mutableStateOf(false) }

    val filteredStyles = remember(selectedCategory) {
        if (selectedCategory == FontCategory.ALL) {
            FancyFontStyle.entries.filter { it != FancyFontStyle.NORMAL }
        } else {
            FancyFontStyle.entries.filter { it.category == selectedCategory }
        }
    }
    val visibleStyles = if (showAll) filteredStyles else filteredStyles.take(10)

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TextFormat, contentDescription = null, tint = TGMagenta)
                    Column {
                        Text(
                            text = "ফন্ট স্টুডিও (Stylish Fonts)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active Keyboard Font: ${activeFont.displayName}",
                            fontSize = 11.sp,
                            color = TGMagenta,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (activeFont != FancyFontStyle.NORMAL) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TGMagenta.copy(alpha = 0.16f))
                            .clickable {
                                scope.launch { settingsManager.setActiveFancyFontStyle(FancyFontStyle.NORMAL) }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("studio_reset_normal_font")
                    ) {
                        Text("Reset Normal", color = TGMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("যেকোনো বাংলা বা English টেক্সট লিখুন") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TGMagenta,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("font_studio_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FontCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TGMagenta.copy(alpha = 0.22f),
                            selectedLabelColor = TGMagenta
                        )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                visibleStyles.forEach { style ->
                    val isActive = activeFont == style
                    val rendered = UnicodeTextStyler.styleText(inputText.ifBlank { "TypeGenius" }, style)
                    val presetId = style.handwritingPresetId()
                    val rowFontFamily = remember(presetId, customFontPath) {
                        if (presetId != null) CustomFontManager.getFontFamily(context, presetId, customFontPath)
                        else androidx.compose.ui.text.font.FontFamily.Default
                    }
                    val displayLabel = if (style == FancyFontStyle.HANDWRITING_CUSTOM && hasCustomFont && customFontName.isNotBlank()) {
                        customFontName
                    } else {
                        style.displayName
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) TGMagenta.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .then(
                                if (isActive) Modifier.border(1.dp, TGMagenta, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .clickable {
                                if (style == FancyFontStyle.HANDWRITING_CUSTOM && !hasCustomFont) {
                                    Toast.makeText(
                                        context,
                                        "উপরে 'হাতের লেখা ফন্ট' কার্ড থেকে আপনার .zip বা .ttf ফন্ট ইমপোর্ট করুন",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@clickable
                                }
                                scope.launch {
                                    val nextStyle = if (isActive) FancyFontStyle.NORMAL else style
                                    settingsManager.setActiveFancyFontStyle(nextStyle)
                                    if (!isActive && presetId != null) {
                                        settingsManager.setKeyboardFontId(presetId)
                                    }
                                    Toast.makeText(
                                        context,
                                        if (isActive) "নরমাল ফন্টে ফিরে এসেছে" else "কিবোর্ড ফন্ট সেট: $displayLabel",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("studio_font_${style.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = rendered,
                                fontFamily = rowFontFamily,
                                fontSize = if (style.isHandwritingFont) 17.sp else 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = displayLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (style == FancyFontStyle.HANDWRITING_CUSTOM && !hasCustomFont) {
                                    Text(
                                        text = "• ইমপোর্ট প্রয়োজন",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else if (isActive) {
                                    Text(
                                        text = "• ACTIVE ON KEYBOARD",
                                        fontSize = 9.sp,
                                        color = TGMagenta,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = TGMagenta,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Styled Text", rendered))
                                    Toast.makeText(context, "কপি হয়েছে: $rendered", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Styled Text",
                                    tint = TGMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (style.isHandwritingFont) {
                                        if (style == FancyFontStyle.HANDWRITING_CUSTOM && !hasCustomFont) {
                                            Toast.makeText(context, "আগে কাস্টম ফন্ট ইমপোর্ট করুন", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        runCatching {
                                            val file = HandwritingOutputGenerator.generateHandwritingPngFile(
                                                context = context,
                                                rawText = rendered,
                                                style = style,
                                                customFontPath = customFontPath,
                                                customFontName = customFontName,
                                                paperTheme = HandwritingPaperTheme.NOTEBOOK
                                            )
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                clipData = ClipData.newUri(context.contentResolver, "Handwriting Output", uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(send, "হাতের লেখায় শেয়ার করুন"))
                                        }
                                    } else {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, rendered)
                                        }
                                        context.startActivity(Intent.createChooser(send, "Share Styled Text"))
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Styled Text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (filteredStyles.size > 10) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TGMagenta.copy(alpha = 0.1f))
                        .clickable { showAll = !showAll }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showAll) "কম দেখান" else "সব ${filteredStyles.size}টি ফন্ট দেখুন →",
                        color = TGMagenta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
