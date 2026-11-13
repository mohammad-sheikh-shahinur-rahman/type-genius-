package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class StickerTab(val title: String) {
    BANGLA_STICKERS("বাংলা স্টিকার"),
    KAOMOJI("কাওমোজি (Kaomoji)")
}

data class StickerItem(
    val category: String,
    val text: String,
    val previewEmoji: String = ""
)

object StickerRepository {
    val banglaStickers: List<StickerItem> = listOf(
        // Greetings
        StickerItem("সম্ভাষণ", "শুভ সকাল ☀️"),
        StickerItem("সম্ভাষণ", "শুভ রাত্রি 🌙"),
        StickerItem("সম্ভাষণ", "আসসালামু আলাইকুম 🤲"),
        StickerItem("সম্ভাষণ", "ওয়ালাইকুমুস সালাম 🤍"),
        StickerItem("সম্ভাষণ", "কী অবস্থা দোস্ত? 👀"),
        StickerItem("সম্ভাষণ", "ভালো আছেন তো? 😊"),
        StickerItem("সম্ভাষণ", "আফনে ভালা আছেননি? ☕"),
        StickerItem("সম্ভাষণ", "কেমন চলছে দিনকাল? ✨"),

        // Reactions
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

        // Wishes & Dua
        StickerItem("দোয়া ও শুভেচ্ছা", "শুভ জন্মদিন! 🎂🎉"),
        StickerItem("দোয়া ও শুভেচ্ছা", "ঈদ মোবারক 🌙✨"),
        StickerItem("দোয়া ও শুভেচ্ছা", "ইনশাআল্লাহ 🤲"),
        StickerItem("দোয়া ও শুভেচ্ছা", "মাশাআল্লাহ ✨"),
        StickerItem("দোয়া ও শুভেচ্ছা", "আলহামদুলিল্লাহ 🤍"),
        StickerItem("দোয়া ও শুভেচ্ছা", "জাযাকাল্লাহু খাইরান 🤲"),
        StickerItem("দোয়া ও শুভেচ্ছা", "অনেক দোয়া রইলো 🌸"),
        StickerItem("দোয়া ও শুভেচ্ছা", "আল্লাহ ভরসা 🤲"),

        // Daily Chat
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
        // Happy
        StickerItem("আনন্দ", "(⁠^⁠^⁠)"),
        StickerItem("আনন্দ", "(⁠•⁠‿⁠•⁠)"),
        StickerItem("আনন্দ", "(⁠≧⁠▽⁠≦⁠)"),
        StickerItem("আনন্দ", "٩(◕‿◕)۶"),
        StickerItem("আনন্দ", "(⁠◕⁠ᴗ⁠◕⁠✿⁠)"),
        StickerItem("আনন্দ", "(* ^ ω ^)"),
        StickerItem("আনন্দ", "(o^▽^o)"),
        StickerItem("আনন্দ", "(´ ∀ ` *)"),

        // Love
        StickerItem("ভালোবাসা", "(⁠♡⁠‿⁠♡⁠)"),
        StickerItem("ভালোবাসা", "(⁠｡⁠♡⁠‿⁠♡⁠｡⁠)"),
        StickerItem("ভালোবাসা", "(⁠っ⁠˘⁠з⁠(⁠˘⁠⌣⁠˘⁠ ⁠)"),
        StickerItem("ভালোবাসা", "(⁠ ⁠˘⁠ ⁠³⁠˘⁠)⁠♥"),
        StickerItem("ভালোবাসা", "(⁠人⁠ ⁠•͈⁠ᴗ⁠•͈⁠)"),
        StickerItem("ভালোবাসা", "(❤ω❤)"),
        StickerItem("ভালোবাসা", "(´,,•ω•,,)♡"),

        // Shrug
        StickerItem("ভাব/শ্রাগ", "¯\\_(ツ)_/¯"),
        StickerItem("ভাব/শ্রাগ", "╮(︶︿︶)╭"),
        StickerItem("ভাব/শ্রাগ", "¯\\(°_o)/¯"),
        StickerItem("ভাব/শ্রাগ", "(￢_￢)"),
        StickerItem("ভাব/শ্রাগ", "( ˘︹˘ )"),

        // Sad
        StickerItem("দুঃখ", "(⁠╥⁠﹏⁠╥⁠)"),
        StickerItem("দুঃখ", "(⁠╯⁠︵⁠╰⁠,⁠)"),
        StickerItem("দুঃখ", "(⁠T⁠_⁠T⁠)"),
        StickerItem("দুঃখ", "(⁠´⁠;⁠︵⁠;⁠`⁠)"),
        StickerItem("দুঃখ", "(⁠っ⁠-⁠ ⁠|⁠)"),
        StickerItem("দুঃখ", "(;﹏;)"),

        // Angry
        StickerItem("রাগ", "(⁠╯⁠°⁠□⁠°⁠）⁠╯⁠︵⁠ ⁠┻⁠━⁠┻"),
        StickerItem("রাগ", "(⁠ಠ⁠_⁠ಠ⁠)"),
        StickerItem("রাগ", "凸(｀0´)凸"),
        StickerItem("রাগ", "(⁠ノ⁠ಥ⁠益⁠ಥ⁠)⁠ノ"),
        StickerItem("রাগ", "(‡▼益▼)"),

        // Cool / Animals
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
    onInsertText: (String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(StickerTab.BANGLA_STICKERS) }
    var selectedCategory by remember { mutableStateOf("সব (All)") }

    val rawList = if (selectedTab == StickerTab.BANGLA_STICKERS) {
        StickerRepository.banglaStickers
    } else {
        StickerRepository.kaomojis
    }

    val categories = remember(rawList) {
        listOf("সব (All)") + rawList.map { it.category }.distinct()
    }

    val filteredList = remember(rawList, selectedCategory) {
        if (selectedCategory == "সব (All)") rawList
        else rawList.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                selectedCategory = "সব (All)"
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("sticker_tab_${tab.name.lowercase()}")
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else colors.textPrimary,
                            fontSize = 12.sp,
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
                    contentDescription = "Close Stickers",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
