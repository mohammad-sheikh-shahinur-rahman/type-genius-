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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.FontCategory
import com.example.data.dictionary.UnicodeTextStyler

@Composable
fun FancyFontsPanel(
    colors: KeyboardColors,
    activeStyle: FancyFontStyle,
    sampleWord: String,
    isBengaliActive: Boolean = false,
    onSelectLiveStyle: (FancyFontStyle) -> Unit,
    onTransformLastWord: (FancyFontStyle) -> Unit,
    onInsertSnippet: (String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(FontCategory.ALL) }
    val previewSeed = remember(sampleWord, isBengaliActive) {
        val clean = UnicodeTextStyler.unstyleToPlain(sampleWord).trim()
        when {
            clean.isNotEmpty() -> clean
            isBengaliActive -> "বাংলা TypeGenius"
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
            .height(272.dp)
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
                    text = "ফন্ট ও স্টাইলিশ টেক্সট (Fonts & Decor)",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (activeStyle == FancyFontStyle.NORMAL) {
                        "যেকোনো ফন্টে ট্যাপ করে সরাসরি লিখুন • 'Restyle' শেষ শব্দ সাজাবে"
                    } else {
                        "Live Font Active: ${activeStyle.displayName}"
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
                            text = "Normal",
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
                    Icon(Icons.Default.Close, contentDescription = "Close Fancy Fonts", tint = colors.textSecondary)
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
                val previewInput = if (style.supportsAllScripts) previewSeed else {
                    if (previewSeed.any { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }) previewSeed else "TypeGenius"
                }
                val preview = UnicodeTextStyler.styleText(previewInput, style)

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
                            onSelectLiveStyle(if (isSelected) FancyFontStyle.NORMAL else style)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("fancy_font_row_${style.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = preview,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = style.displayName,
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Font",
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                                text = if (style == FancyFontStyle.NORMAL) "Plain" else "Restyle",
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
