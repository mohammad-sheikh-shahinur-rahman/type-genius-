package com.example.ui.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

enum class StickerTab(val title: String) {
    BANGLA_STICKERS("বাংলা স্টিকার"),
    CUSTOM_MAKER("স্টিকার তৈরি"),
    KAOMOJI("কাওমোজি")
}

data class StickerItem(
    val category: String,
    val text: String,
    val previewEmoji: String = ""
)

object StickerRepository {
    val banglaStickers: List<StickerItem> = listOf(
        // সম্ভাষণ
        StickerItem("সম্ভাষণ", "শুভ সকাল ☀️"),
        StickerItem("সম্ভাষণ", "শুভ রাত্রি 🌙"),
        StickerItem("সম্ভাষণ", "আসসালামু আলাইকুম 🤲"),
        StickerItem("সম্ভাষণ", "ওয়ালাইকুমুস সালাম 🤍"),
        StickerItem("সম্ভাষণ", "কী অবস্থা দোস্ত? 👀"),
        StickerItem("সম্ভাষণ", "ভালো আছেন তো? 😊"),
        StickerItem("সম্ভাষণ", "আফনে ভালা আছেননি? ☕"),
        StickerItem("সম্ভাষণ", "কেমন চলছে দিনকাল? ✨"),

        // অনুভূতি
        StickerItem("অনুভূতি", "অস্থির হইছে! 🔥"),
        StickerItem("অনুভূতি", "ধুর ছাই! 🤦‍♂️"),
        StickerItem("অনুভূতি", "একদম ঠিক কথা! 💯"),
        StickerItem("অনুভূতি", "আরে বাহ্! 👏"),
        StickerItem("অনুভূতি", "ভালোবাসা অবিরাম ❤️"),
        StickerItem("অনুভূতি", "মাথা নষ্ট! 🤯"),
        StickerItem("অনুভূতি", "ব্যাপার না, চিল 😎"),
        StickerItem("অনুভূতি", "হাসতে হাসতে শেষ! 😂"),
        StickerItem("অনুভূতি", "অনেক অনেক ধন্যবাদ 🙏"),
        StickerItem("অনুভূতি", "স্যালুট বস! 🫡"),

        // দোয়া ও শুভেচ্ছা
        StickerItem("দোয়া ও শুভেচ্ছা", "শুভ জন্মদিন! 🎂🎉"),
        StickerItem("দোয়া ও শুভেচ্ছা", "ঈদ মোবারক 🌙✨"),
        StickerItem("দোয়া ও শুভেচ্ছা", "ইনশাআল্লাহ 🤲"),
        StickerItem("দোয়া ও শুভেচ্ছা", "মাশাআল্লাহ ✨"),
        StickerItem("দোয়া ও শুভেচ্ছা", "আলহামদুলিল্লাহ 🤍"),
        StickerItem("দোয়া ও শুভেচ্ছা", "জাযাকাল্লাহু খাইরান 🤲"),
        StickerItem("দোয়া ও শুভেচ্ছা", "অনেক দোয়া রইলো 🌸"),
        StickerItem("দোয়া ও শুভেচ্ছা", "আল্লাহ ভরসা 🤲"),

        // দৈনিক চ্যাট
        StickerItem("দৈনিক চ্যাট", "জরুরি কথা আছে 📞"),
        StickerItem("দৈনিক চ্যাট", "একটু পর কল দিচ্ছি 📱"),
        StickerItem("দৈনিক চ্যাট", "পৌঁছে মেসেজ দিও 🚗"),
        StickerItem("দৈনিক চ্যাট", "চলো চা খাই! ☕"),
        StickerItem("দৈনিক চ্যাট", "ভাত খাইছো? 🍛"),
        StickerItem("দৈনিক চ্যাট", "ঘুম পাচ্ছে, পরে কথা বলি 😴"),
        StickerItem("দৈনিক চ্যাট", "থ্যাংক ইউ সো মাচ! 💖"),
        StickerItem("দৈনিক চ্যাট", "দেখা হচ্ছে শীঘ্রই! 👋")
    )

    val kaomojis: List<StickerItem> = listOf(
        StickerItem("আনন্দ", "(⁠^⁠^⁠)"),
        StickerItem("আনন্দ", "(⁠•⁠‿⁠•⁠)"),
        StickerItem("আনন্দ", "(⁠≧⁠▽⁠≦⁠)"),
        StickerItem("আনন্দ", "٩(◕‿◕)۶"),
        StickerItem("আনন্দ", "(⁠◕⁠ᴗ⁠◕⁠✿⁠)"),
        StickerItem("আনন্দ", "(* ^ ω ^)"),
        StickerItem("আনন্দ", "(o^▽^o)"),
        StickerItem("আনন্দ", "(´ ∀ ` *)"),
        StickerItem("ভালোবাসা", "(⁠♡⁠‿⁠♡⁠)"),
        StickerItem("ভালোবাসা", "(⁠｡⁠♡⁠‿⁠♡⁠｡⁠)"),
        StickerItem("ভালোবাসা", "(⁠っ⁠˘⁠з⁠(⁠˘⁠⌣⁠˘⁠ ⁠)"),
        StickerItem("ভালোবাসা", "(⁠ ⁠˘⁠ ⁠³⁠˘⁠)⁠♥"),
        StickerItem("ভালোবাসা", "(⁠人⁠ ⁠•͈⁠ᴗ⁠•͈⁠)"),
        StickerItem("ভালোবাসা", "(❤ω❤)"),
        StickerItem("ভালোবাসা", "(´,,•ω•,,)♡"),
        StickerItem("ভাব/শ্রাগ", "¯\\_(ツ)_/¯"),
        StickerItem("ভাব/শ্রাগ", "╮(︶︿︶)╭"),
        StickerItem("ভাব/শ্রাগ", "¯\\(°_o)/¯"),
        StickerItem("ভাব/শ্রাগ", "(￢_￢)"),
        StickerItem("ভাব/শ্রাগ", "( ˘︹˘ )"),
        StickerItem("দুঃখ", "(⁠╥⁠﹏⁠╥⁠)"),
        StickerItem("দুঃখ", "(⁠╯⁠︵⁠╰⁠,⁠)"),
        StickerItem("দুঃখ", "(⁠T⁠_⁠T⁠)"),
        StickerItem("দুঃখ", "(⁠´⁠;⁠︵⁠;⁠`⁠)"),
        StickerItem("দুঃখ", "(⁠っ⁠-⁠ ⁠|⁠)"),
        StickerItem("দুঃখ", "(;﹏;)"),
        StickerItem("রাগ", "(⁠╯⁠°⁠□⁠°⁠）⁠╯⁠︵⁠ ⁠┻⁠━⁠┻"),
        StickerItem("রাগ", "(⁠ಠ⁠_⁠ಠ⁠)"),
        StickerItem("রাগ", "凸(｀0´)凸"),
        StickerItem("রাগ", "(⁠ノ⁠ಥ⁠益⁠ಥ⁠)⁠ノ"),
        StickerItem("রাগ", "(‡▼益▼)"),
        StickerItem("কুল ও কিউট", "(⁠⌐⁠■⁠-⁠■⁠)"),
        StickerItem("কুল ও কিউট", "(⁠ ⁠•⁠_⁠•⁠ ⁠)"),
        StickerItem("কুল ও কিউট", "(=^･ω･^=)"),
        StickerItem("কুল ও কিউট", "(=^･ｪ･^=)"),
        StickerItem("কুল ও কিউট", "ʕ •ᴥ•ʔ"),
        StickerItem("কুল ও কিউট", "(ᵔᴥᵔ)")
    )
}

@Composable
fun StickersAndKaomojiPanel(
    colors: KeyboardColors,
    customStickerText: String = "অস্থির হইছে বস!",
    isEditingCustomText: Boolean = false,
    onCustomStickerTextChange: (String) -> Unit = {},
    onToggleCustomTextKeyboard: () -> Unit = {},
    onInsertText: (String) -> Unit,
    onSendStickerImage: (File, shareOnly: Boolean) -> Unit = { _, _ -> },
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(StickerTab.BANGLA_STICKERS) }
    var selectedCategory by remember { mutableStateOf("সব") }

    // Custom Sticker Maker state
    var selectedGradientIdx by remember { mutableIntStateOf(0) }
    var selectedFontStyle by remember { mutableStateOf(StickerFontStyle.ATMA) }
    var selectedEmoji by remember { mutableStateOf("🔥") }

    val gradientPresets = StickerImageGenerator.gradientPresets
    val activeGradient = gradientPresets.getOrElse(selectedGradientIdx) { gradientPresets.first() }
    val emojiOptions = listOf("🔥", "❤️", "😂", "🤲", "☕", "✨", "🎉", "😎", "🌸", "")

    val previewBitmap = remember(customStickerText, selectedEmoji, activeGradient, selectedFontStyle) {
        StickerImageGenerator.renderStickerBitmap(
            context = context,
            text = customStickerText,
            emoji = selectedEmoji,
            gradient = activeGradient,
            fontStyle = selectedFontStyle,
            sizePx = 280
        ).asImageBitmap()
    }

    val rawList = if (selectedTab == StickerTab.BANGLA_STICKERS) {
        StickerRepository.banglaStickers
    } else {
        StickerRepository.kaomojis
    }

    val categories = remember(rawList) {
        listOf("সব") + rawList.map { it.category }.distinct()
    }

    val filteredList = remember(rawList, selectedCategory) {
        if (selectedCategory == "সব") rawList
        else rawList.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Header Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StickerTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accent else colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                selectedTab = tab
                                selectedCategory = "সব"
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("sticker_tab_${tab.name.lowercase()}")
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else colors.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            IconButton(
                onClick = { onHaptic(); onClose() },
                modifier = Modifier.size(30.dp).testTag("close_stickers_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "বন্ধ করুন",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (selectedTab == StickerTab.CUSTOM_MAKER) {
            val makerHeight = if (isEditingCustomText) 150.dp else 218.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(makerHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live sticker card preview
                    Box(
                        modifier = Modifier
                            .size(if (isEditingCustomText) 88.dp else 108.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.keyBackground)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "বাংলা স্টিকার প্রিভিউ",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Text edit trigger (opens compact keyboard below for typing)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isEditingCustomText) colors.accent.copy(alpha = 0.16f)
                                    else colors.keyBackground
                                )
                                .border(
                                    1.dp,
                                    if (isEditingCustomText) colors.accent else colors.keySpecialBackground,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onHaptic()
                                    onToggleCustomTextKeyboard()
                                }
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                                .testTag("sticker_maker_edit_text_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = customStickerText.ifBlank { "বাংলা টেক্সট লিখুন..." },
                                color = if (customStickerText.isBlank()) colors.textSecondary else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isEditingCustomText) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "লিখুন",
                                tint = colors.accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Gradient swatches
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            gradientPresets.forEachIndexed { idx, preset ->
                                val selected = idx == selectedGradientIdx
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.startColor))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onHaptic()
                                            selectedGradientIdx = idx
                                        }
                                        .testTag("sticker_gradient_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // Action buttons: Send via commitContent & Share via Intent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.accent)
                                    .clickable {
                                        onHaptic()
                                        val file = StickerImageGenerator.generateStickerPngFile(
                                            context = context,
                                            text = customStickerText,
                                            emoji = selectedEmoji,
                                            gradient = activeGradient,
                                            fontStyle = selectedFontStyle
                                        )
                                        onSendStickerImage(file, false)
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("sticker_send_commit_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("চ্যাটে পাঠান", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.keySpecialBackground)
                                    .clickable {
                                        onHaptic()
                                        val file = StickerImageGenerator.generateStickerPngFile(
                                            context = context,
                                            text = customStickerText,
                                            emoji = selectedEmoji,
                                            gradient = activeGradient,
                                            fontStyle = selectedFontStyle
                                        )
                                        onSendStickerImage(file, true)
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("sticker_share_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(13.dp))
                                    Text("শেয়ার", color = colors.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Quick text presets + Font style + Emoji row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StickerFontStyle.entries.forEach { font ->
                        val active = selectedFontStyle == font
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (active) colors.accent.copy(alpha = 0.22f) else colors.keyBackground)
                                .clickable {
                                    onHaptic()
                                    selectedFontStyle = font
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = font.label,
                                color = if (active) colors.accent else colors.textPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    emojiOptions.forEach { em ->
                        val active = selectedEmoji == em
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (active) colors.accent.copy(alpha = 0.22f) else colors.keyBackground)
                                .clickable {
                                    onHaptic()
                                    selectedEmoji = em
                                }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = em.ifEmpty { "ইমোজি নেই" },
                                color = colors.textPrimary,
                                fontSize = if (em.isEmpty()) 10.sp else 13.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    StickerImageGenerator.quickStickersTextPresets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.keySpecialBackground)
                                .clickable {
                                    onHaptic()
                                    onCustomStickerTextChange(preset)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(preset, color = colors.textPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Categories Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) colors.accent.copy(alpha = 0.22f)
                                else colors.keyBackground
                            )
                            .clickable {
                                onHaptic()
                                selectedCategory = cat
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grid of items
            LazyVerticalGrid(
                columns = if (selectedTab == StickerTab.BANGLA_STICKERS) GridCells.Fixed(2) else GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(filteredList) { item ->
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.keySpecialBackground)
                            .clickable {
                                onHaptic()
                                onInsertText(item.text)
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.text,
                            color = colors.textPrimary,
                            fontSize = if (selectedTab == StickerTab.BANGLA_STICKERS) 13.sp else 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
