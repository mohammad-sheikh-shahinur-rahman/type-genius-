package com.example.ui.keyboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomFontManager
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.FontCategory
import com.example.data.dictionary.UnicodeTextStyler

@Composable
fun FancyFontsPanel(
    colors: KeyboardColors,
    activeStyle: FancyFontStyle,
    sampleWord: String,
    customFontPath: String = "",
    customFontName: String = "",
    isBengaliActive: Boolean = false,
    onSelectLiveStyle: (FancyFontStyle) -> Unit,
    onTransformLastWord: (FancyFontStyle) -> Unit,
    onSendStyleCard: (FancyFontStyle) -> Unit = {},
    onOpenHandwritingImport: () -> Unit = {},
    onInsertSnippet: (String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(FontCategory.ALL) }
    val hasCustomHandwriting = remember(customFontPath) {
        CustomFontManager.hasCustomFont(customFontPath)
    }

    val previewSeed = remember(sampleWord, isBengaliActive) {
        val clean = UnicodeTextStyler.unstyleToPlain(sampleWord).trim()
        when {
            clean.isNotEmpty() -> clean
            isBengaliActive -> "আমার সোনার বাংলা"
            else -> "TypeGenius"
        }
    }

    val quickDecorTokens = listOf(
        "꧁", "꧂", "★彡", "彡★", "✦", "❖", "♡", "✿",
        "(◕‿◕)", "¯\\_(ツ)_/¯", "(づ｡◕‿‿◕｡)づ", "(♥‿♥)",
        "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "ʕ•ᴥ•ʔ", "(⌐■_■)", "✦ ━━━ ✦", "【 ★ 】"
    )

    val filteredStyles = remember(selectedCategory) {
        if (selectedCategory == FontCategory.ALL) {
            FancyFontStyle.entries
        } else {
            listOf(FancyFontStyle.NORMAL) + FancyFontStyle.entries.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(278.dp)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ফ্যান্সি ও হাতের লেখা ফন্ট",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        activeStyle == FancyFontStyle.NORMAL ->
                            "যেকোনো ফন্ট বা হাতের লেখা বেছে লিখুন • সাথে সাথে আউটপুট হবে"
                        activeStyle.isHandwritingFont ->
                            "সক্রিয়: ${if (activeStyle == FancyFontStyle.HANDWRITING_CUSTOM && customFontName.isNotBlank()) customFontName else activeStyle.displayName}"
                        else -> "সক্রিয় ফন্ট: ${activeStyle.displayName}"
                    },
                    color = colors.accent,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeStyle != FancyFontStyle.NORMAL) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent.copy(alpha = 0.2f))
                            .border(1.dp, colors.accent, RoundedCornerShape(8.dp))
                            .clickable {
                                onHaptic()
                                onSelectLiveStyle(FancyFontStyle.NORMAL)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("fancy_fonts_reset_normal")
                    ) {
                        Text(
                            text = "সাধারণ",
                            color = colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = {
                        onHaptic()
                        onClose()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("fancy_fonts_close")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = colors.textSecondary)
                }
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FontCategory.entries.forEach { category ->
                val isCatSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCatSelected) colors.accent else colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            selectedCategory = category
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("font_category_${category.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.label,
                        color = if (isCatSelected) colors.enterKeyText else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Quick symbols & Kaomoji bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickDecorTokens.forEach { token ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            val insertText = if (token.length <= 2) token else "$token "
                            onInsertSnippet(insertText)
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(token, color = colors.textPrimary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filteredStyles, key = { it.id }) { style ->
                val isSelected = activeStyle == style
                val isCustomHw = style == FancyFontStyle.HANDWRITING_CUSTOM
                val hwPresetId = style.handwritingPresetId()
                val previewFontFamily = remember(style, customFontPath) {
                    if (hwPresetId != null) {
                        CustomFontManager.getFontFamily(context, hwPresetId, customFontPath)
                    } else FontFamily.Default
                }

                val preview = remember(style, previewSeed, isCustomHw, hasCustomHandwriting) {
                    when {
                        style.isHandwritingFont -> {
                            if (previewSeed.any { it in '\u0980'..'\u09FF' }) previewSeed
                            else "আমার হাতের লেখা • $previewSeed"
                        }
                        style.supportsAllScripts -> UnicodeTextStyler.styleText(previewSeed, style)
                        else -> {
                            val seed = if (previewSeed.any { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }) {
                                previewSeed
                            } else {
                                "TypeGenius বাংলা"
                            }
                            UnicodeTextStyler.styleText(seed, style)
                        }
                    }
                }

                val rowLabel = when {
                    isCustomHw && hasCustomHandwriting && customFontName.isNotBlank() ->
                        "আমার হাতের লেখা: $customFontName"
                    isCustomHw && !hasCustomHandwriting ->
                        "আমার হাতের লেখা ফন্ট (.zip / .ttf ইমপোর্ট করুন)"
                    else -> style.displayName
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.keyBackground)
                        .then(
                            if (isSelected) Modifier.border(1.dp, colors.accent, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable {
                            onHaptic()
                            if (isCustomHw && !hasCustomHandwriting) {
                                onOpenHandwritingImport()
                            } else {
                                onSelectLiveStyle(if (isSelected) FancyFontStyle.NORMAL else style)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("fancy_font_row_${style.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (style.isHandwritingFont) {
                                Icon(
                                    imageVector = Icons.Default.Draw,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = preview,
                                fontFamily = previewFontFamily,
                                color = colors.textPrimary,
                                fontSize = if (style.isHandwritingFont) 15.5.sp else 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = rowLabel,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "সক্রিয় ফন্ট",
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isCustomHw && !hasCustomHandwriting) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accent.copy(alpha = 0.16f))
                                    .clickable {
                                        onHaptic()
                                        onOpenHandwritingImport()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("fancy_font_import_custom")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                                    Text("যুক্ত করুন", color = colors.accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (style.isHandwritingFont) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accent.copy(alpha = 0.16f))
                                    .clickable {
                                        onHaptic()
                                        onSendStyleCard(style)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("fancy_font_send_card_${style.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                                    Text("হাতের লেখায় পাঠান", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keySpecialBackground)
                                    .clickable {
                                        onHaptic()
                                        onTransformLastWord(style)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("fancy_font_restyle_${style.id}")
                            ) {
                                Text(
                                    text = if (style == FancyFontStyle.NORMAL) "সাধারণ" else "সাজান",
                                    color = colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
