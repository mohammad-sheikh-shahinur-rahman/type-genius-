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
import com.example.data.dictionary.UnicodeTextStyler

@Composable
fun FancyFontsPanel(
    colors: KeyboardColors,
    activeStyle: FancyFontStyle,
    sampleWord: String,
    onSelectLiveStyle: (FancyFontStyle) -> Unit,
    onTransformLastWord: (FancyFontStyle) -> Unit,
    onInsertSnippet: (String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewSeed = sampleWord.ifBlank { "TypeGenius" }
    val quickKaomoji = listOf(
        "(◕‿◕)", "¯\\_(ツ)_/¯", "(づ｡◕‿‿◕｡)づ", "(♥‿♥)", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧",
        "ʕ•ᴥ•ʔ", "(⌐■_■)", "(ಥ﹏ಥ)", "✦ ━━━ ✦", "【 ★ 】"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Stylish Fonts & Text Decor",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeStyle == FancyFontStyle.NORMAL) {
                        "Tap style for live typing • 'Restyle' converts last word"
                    } else {
                        "Live typing: ${activeStyle.displayName}"
                    },
                    color = colors.accent,
                    fontSize = 10.sp
                )
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

        // Quick Kaomoji & aesthetic dividers bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickKaomoji.forEach { kaomoji ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            onInsertSnippet("$kaomoji ")
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(kaomoji, color = colors.textPrimary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(FancyFontStyle.entries, key = { it.id }) { style ->
                val isSelected = activeStyle == style
                val preview = if (style == FancyFontStyle.DECORATED_STAR || style == FancyFontStyle.DECORATED_BRACKET) {
                    UnicodeTextStyler.styleText(previewSeed, style)
                } else {
                    UnicodeTextStyler.styleText(previewSeed, style)
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
                            if (style == FancyFontStyle.DECORATED_STAR || style == FancyFontStyle.DECORATED_BRACKET) {
                                onTransformLastWord(style)
                            } else {
                                onSelectLiveStyle(if (isSelected) FancyFontStyle.NORMAL else style)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                            fontSize = 10.sp
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
                        if (style != FancyFontStyle.NORMAL) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keySpecialBackground)
                                    .clickable {
                                        onHaptic()
                                        onTransformLastWord(style)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Restyle",
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
