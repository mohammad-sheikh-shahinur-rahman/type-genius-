package com.example.ui.keyboard

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.data.BengaliCalendarUtils
import com.example.data.local.ClipboardItem
import com.example.data.local.SmartClipType
import com.example.data.local.SmartClipboardParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun EmojiPanel(
    colors: KeyboardColors,
    onEmojiSelected: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val categories = listOf(
        "হাসিমুখ" to listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹",
            "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
            "🫡", "🤐", "🤨", "😐", "😑", "😶", "🫥", "😏", "😒", "🙄"
        ),
        "ইশারা" to listOf(
            "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✌️", "🤞",
            "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝️", "✋",
            "🤚", "🖐️", "🖖", "👋", "✍️", "💪", "👊", "✊", "🤛", "🤜"
        ),
        "ভালোবাসা" to listOf(
            "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🤎", "🖤",
            "🩶", "🤍", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗",
            "💖", "💘", "💝", "💟", "✨", "⭐", "🌟", "💫", "🔥", "💯"
        ),
        "চিহ্ন" to listOf(
            "✔️", "❌", "❓", "❗", "‼️", "⁉️", "🆗", "🆒", "🆕", "🆓",
            "🔴", "🟢", "🟡", "🔵", "🟣", "⚫", "⚪", "🟩", "🟨", "🟥",
            "💬", "💭", "🗯️", "📢", "📣", "🔔", "🔕", "🎵", "🎶", "🎉"
        ),
        "কাওমোজি" to listOf(
            "(◕‿◕)", "¯\\_(ツ)_/¯", "(╯°□°)╯︵ ┻━┻", "(づ｡◕‿‿◕｡)づ",
            "(♥‿♥)", "(╥﹏╥)", "(⌐■_■)", "ʕ•ᴥ•ʔ",
            "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(ง •̀_•́)ง", "(｡•́︿•̀｡)", "(◠‿◠)",
            "(⌒‿⌒)", "(*^▽^*)", "(⊙_⊙)", "(¬‿¬)",
            "(づ￣ ³￣)づ", "＼(＾O＾)／", "(☞ﾟヮﾟ)☞", "(ಥ﹏ಥ)",
            "(✿◠‿◠)", "ಠ_ಠ", "✦ ━━━ ✦", "【 ★ 】"
        )
    )

    val isKaomoji = categories[selectedCategoryIndex].first == "কাওমোজি"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(colors.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = colors.suggestionBackground,
            contentColor = colors.accent,
            edgePadding = 8.dp
        ) {
            categories.forEachIndexed { index, (name, _) ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = {
                        Text(
                            text = name,
                            color = if (selectedCategoryIndex == index) colors.accent else colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isKaomoji) 110.dp else 44.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(categories[selectedCategoryIndex].second) { emoji ->
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keyBackground)
                        .clickable { onEmojiSelected(emoji) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = if (isKaomoji) 13.sp else 23.sp,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(colors.suggestionBackground)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToKeyboard) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "কীবোর্ডে ফিরুন",
                    tint = colors.textPrimary
                )
            }
            IconButton(onClick = onBackspace) {
                Text(
                    text = "⌫",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ClipboardPanel(
    items: List<ClipboardItem>,
    colors: KeyboardColors,
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    otpExpireMinutes: Int = 3,
    onToggleSearch: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    onCycleOtpExpireMinutes: () -> Unit = {},
    onItemSelected: (String) -> Unit,
    onCopyItem: (ClipboardItem) -> Unit = {},
    onTogglePin: (ClipboardItem) -> Unit,
    onDeleteItem: (ClipboardItem) -> Unit,
    onClearAll: () -> Unit,
    onClearEverything: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterIndex by remember { mutableIntStateOf(0) } // 0=সব, 1=OTP, 2=লিংক, 3=পিন
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showClearConfirmBar by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val classified = remember(items) {
        items.map { it to SmartClipboardParser.classify(it.content) }
    }
    val activeItems = remember(classified, nowMs) {
        classified.filter { (item, _) ->
            item.isPinned || item.expiresAt == null || item.expiresAt > nowMs
        }
    }
    val pinnedCount = remember(activeItems) { activeItems.count { it.first.isPinned } }
    val otpCount = remember(activeItems) { activeItems.count { it.second.type == SmartClipType.OTP || it.first.isSensitive } }
    val linkCount = remember(activeItems) { activeItems.count { it.second.type == SmartClipType.LINK } }

    val filtered = remember(activeItems, filterIndex, searchQuery) {
        activeItems.filter { (item, info) ->
            val matchesTab = when (filterIndex) {
                1 -> info.type == SmartClipType.OTP || item.isSensitive
                2 -> info.type == SmartClipType.LINK
                3 -> item.isPinned
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.content.contains(q, ignoreCase = true) ||
                info.extractedValue.contains(q, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    val listHeight = if (isSearchActive) 186.dp else 274.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(listHeight)
            .background(colors.background)
    ) {
        // Elevated Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.suggestionBackground,
                            colors.accent.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = "স্মার্ট ক্লিপবোর্ড",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (activeItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.keySpecialBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val countStr = BengaliCalendarUtils.toBengaliDigits(activeItems.size.toString())
                        val pinStr = if (pinnedCount > 0) {
                            " • ${BengaliCalendarUtils.toBengaliDigits(pinnedCount.toString())} পিন"
                        } else ""
                        Text(
                            text = "$countStr টি$pinStr",
                            color = colors.textSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // OTP Auto-Expire timer chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (otpExpireMinutes > 0) colors.accent.copy(alpha = 0.16f)
                            else colors.keySpecialBackground
                        )
                        .border(
                            0.8.dp,
                            if (otpExpireMinutes > 0) colors.accent.copy(alpha = 0.45f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onCycleOtpExpireMinutes() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("clipboard_otp_timer_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "OTP অটো-ডিলিট সময়",
                        tint = if (otpExpireMinutes > 0) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (otpExpireMinutes > 0) {
                            "OTP ${BengaliCalendarUtils.toBengaliDigits(otpExpireMinutes.toString())} মি."
                        } else {
                            "OTP বন্ধ"
                        },
                        color = if (otpExpireMinutes > 0) colors.accent else colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (activeItems.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmBar = !showClearConfirmBar },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("clipboard_clear_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "ক্লিপবোর্ড পরিষ্কার করুন",
                            tint = if (showClearConfirmBar) Color(0xFFEF5350) else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("clipboard_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Inline Two-Step Clear Confirmation Bar (protects pinned clips!)
        AnimatedVisibility(visible = showClearConfirmBar && activeItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEF5350).copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ক্লিপ মুছুন:",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFEF5350))
                            .clickable {
                                showClearConfirmBar = false
                                onClearAll()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("clipboard_confirm_clear_unpinned")
                    ) {
                        Text("অ-পিন মুছুন", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFB71C1C))
                            .clickable {
                                showClearConfirmBar = false
                                onClearEverything()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("clipboard_confirm_clear_everything")
                    ) {
                        Text("সব মুছুন", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.keySpecialBackground)
                            .clickable { showClearConfirmBar = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("বাতিল", color = colors.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Search Bar & Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.keyBackground)
                .border(
                    width = if (isSearchActive) 1.5.dp else 1.dp,
                    color = if (isSearchActive) colors.accent else colors.keySpecialBackground,
                    shape = RoundedCornerShape(11.dp)
                )
                .clickable { onToggleSearch() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("clipboard_search_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "ক্লিপবোর্ড খুঁজুন",
                tint = if (isSearchActive) colors.accent else colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = when {
                    searchQuery.isNotEmpty() -> searchQuery
                    isSearchActive -> "নিচের কীবোর্ড দিয়ে লিখে খুঁজুন..."
                    else -> "সেভ থাকা টেক্সট, OTP বা লিংক খুঁজুন..."
                },
                color = if (searchQuery.isNotEmpty()) colors.textPrimary else colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "সার্চ মুছুন",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClearSearch() }
                        .testTag("clipboard_search_clear")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                Triple("সব (${BengaliCalendarUtils.toBengaliDigits(activeItems.size.toString())})", 0, activeItems.size),
                Triple("🔑 OTP (${BengaliCalendarUtils.toBengaliDigits(otpCount.toString())})", 1, otpCount),
                Triple("🔗 লিংক (${BengaliCalendarUtils.toBengaliDigits(linkCount.toString())})", 2, linkCount),
                Triple("📌 পিন (${BengaliCalendarUtils.toBengaliDigits(pinnedCount.toString())})", 3, pinnedCount)
            )
            tabs.forEach { (label, idx, _) ->
                val selected = filterIndex == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) colors.accent else colors.keySpecialBackground)
                        .clickable { filterIndex = idx }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("clipboard_filter_$idx")
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.keySpecialBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (activeItems.isEmpty()) {
                            "ক্লিপবোর্ড এখনো খালি"
                        } else {
                            "এই ক্যাটাগরিতে কিছু পাওয়া যায়নি"
                        },
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (activeItems.isEmpty()) {
                            "কোনো লেখা, OTP বা লিংক কপি করলে এখানে জমা হবে। পিন ও ডিলিট এক ট্যাপেই!"
                        } else {
                            "অন্য ট্যাব বা সার্চ পরিবর্তন করে দেখুন"
                        },
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(filtered, key = { it.first.id }) { (item, clipInfo) ->
                    val remainingSec = if (!item.isPinned && item.expiresAt != null) {
                        ((item.expiresAt - nowMs) / 1000L).coerceAtLeast(0L)
                    } else null
                    val stripeColor = when {
                        item.isPinned -> colors.accent
                        remainingSec != null -> Color(0xFFFF9100)
                        clipInfo.type == SmartClipType.LINK -> Color(0xFF29B6F6)
                        else -> colors.textSecondary.copy(alpha = 0.28f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (item.isPinned) {
                                    colors.accent.copy(alpha = 0.09f)
                                } else {
                                    colors.keyBackground
                                }
                            )
                            .border(
                                width = if (item.isPinned) 1.dp else 0.7.dp,
                                color = if (item.isPinned) {
                                    colors.accent.copy(alpha = 0.55f)
                                } else {
                                    colors.keySpecialBackground
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onItemSelected(item.content) }
                            .testTag("clipboard_item_card_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Status Accent Bar
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(stripeColor)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 9.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Top Badges Row (Smart type + Relative Time + OTP Countdown)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (item.isPinned) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(colors.accent.copy(alpha = 0.18f))
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "📌 পিন করা",
                                            color = colors.accent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (clipInfo.type != SmartClipType.TEXT) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(colors.accent.copy(alpha = 0.18f))
                                            .clickable { onItemSelected(clipInfo.extractedValue) }
                                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = if (clipInfo.type == SmartClipType.OTP) {
                                                "🔑 OTP ${clipInfo.extractedValue}"
                                            } else {
                                                clipInfo.badgeLabel
                                            },
                                            color = colors.accent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (remainingSec != null) {
                                    val mins = remainingSec / 60
                                    val secs = remainingSec % 60
                                    val countdownStr = BengaliCalendarUtils.toBengaliDigits(
                                        String.format("%d:%02d", mins, secs)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFFFF6D00).copy(alpha = 0.16f))
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "⏱️ $countdownStr",
                                            color = Color(0xFFFF9100),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = formatRelativeBanglaTime(nowMs - item.timestamp),
                                    color = colors.textSecondary.copy(alpha = 0.78f),
                                    fontSize = 9.5.sp,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = item.content,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                fontWeight = if (item.isPinned) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Action Buttons Cluster (Copy / Pin / Delete)
                        Row(
                            modifier = Modifier.padding(end = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.keySpecialBackground.copy(alpha = 0.65f))
                                    .clickable { onCopyItem(item) }
                                    .testTag("clipboard_copy_btn_${item.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "কপি করুন",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (item.isPinned) colors.accent.copy(alpha = 0.22f)
                                        else colors.keySpecialBackground.copy(alpha = 0.65f)
                                    )
                                    .clickable { onTogglePin(item) }
                                    .testTag("clipboard_pin_btn_${item.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = if (item.isPinned) "আনপিন করুন" else "পিন করুন",
                                    tint = if (item.isPinned) colors.accent else colors.textSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer {
                                            rotationZ = if (item.isPinned) 0f else 35f
                                        }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF5350).copy(alpha = 0.14f))
                                    .clickable { onDeleteItem(item) }
                                    .testTag("clipboard_delete_btn_${item.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "ডিলিট করুন",
                                    tint = Color(0xFFEF5350),
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

private fun formatRelativeBanglaTime(deltaMs: Long): String {
    val sec = (deltaMs / 1000L).coerceAtLeast(0L)
    return when {
        sec < 45L -> "এইমাত্র"
        sec < 3600L -> "${BengaliCalendarUtils.toBengaliDigits((sec / 60L).coerceAtLeast(1L).toString())} মি. আগে"
        sec < 86_400L -> "${BengaliCalendarUtils.toBengaliDigits((sec / 3600L).toString())} ঘ. আগে"
        else -> "${BengaliCalendarUtils.toBengaliDigits((sec / 86_400L).toString())} দিন আগে"
    }
}

@Composable
fun GifPanel(
    colors: KeyboardColors,
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    onSendGifFile: (file: File, shareOnly: Boolean, fallbackText: String) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("সব") }

    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val filteredPresets = remember(selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        AnimatedGifBuilder.presets.filter { item ->
            val catOk = selectedCategory == "সব" || item.category == selectedCategory
            val qOk = q.isEmpty() ||
                item.title.contains(q, ignoreCase = true) ||
                item.subtitle.contains(q, ignoreCase = true) ||
                item.keywords.any { it.contains(q, ignoreCase = true) }
            catOk && qOk
        }
    }

    val panelHeight = if (isSearchActive) 186.dp else 266.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("GIF", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    text = "অ্যানিমেটেড বাংলা GIF",
                    color = colors.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { onHaptic(); onClose() },
                modifier = Modifier.size(28.dp).testTag("close_gif_panel")
            ) {
                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // Search & Custom GIF creation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .border(
                        width = if (isSearchActive) 1.5.dp else 1.dp,
                        color = if (isSearchActive) colors.accent else colors.keySpecialBackground,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onHaptic()
                        onToggleSearch()
                    }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("gif_search_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "GIF খুঁজুন",
                    tint = if (isSearchActive) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = when {
                        searchQuery.isNotEmpty() -> searchQuery
                        isSearchActive -> "লিখে GIF খুঁজুন বা বানান..."
                        else -> "GIF খুঁজুন (যেমন: অস্থির, love, ঈদ)"
                    },
                    color = if (searchQuery.isNotEmpty()) colors.textPrimary else colors.textSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "মুছুন",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp).clickable { onClearSearch() }
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent)
                        .clickable {
                            onHaptic()
                            val customGif = AnimatedGifBuilder.createCustomTextGif(context, searchQuery)
                            onSendGifFile(customGif, false, searchQuery)
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("gif_create_custom_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("GIF বানান", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Categories Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AnimatedGifBuilder.categories.forEach { cat ->
                val selected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selected) colors.accent.copy(alpha = 0.22f) else colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            selectedCategory = cat
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (selected) colors.accent else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Animated GIF Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredPresets, key = { it.id }) { item ->
                val gifFile by produceState<File?>(initialValue = null, key1 = item.id) {
                    value = withContext(Dispatchers.IO) {
                        AnimatedGifBuilder.getOrCreatePresetGif(context, item)
                    }
                }

                Box(
                    modifier = Modifier
                        .height(82.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(item.startColor).copy(alpha = 0.45f))
                        .border(1.dp, colors.keySpecialBackground, RoundedCornerShape(12.dp))
                        .clickable {
                            onHaptic()
                            val readyFile = gifFile ?: AnimatedGifBuilder.getOrCreatePresetGif(context, item)
                            onSendGifFile(readyFile, false, "${item.title} ${item.emoji}")
                        }
                        .testTag("gif_card_${item.id}")
                ) {
                    if (gifFile != null) {
                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(gifFile)
                                .crossfade(false)
                                .build(),
                            imageLoader = gifImageLoader
                        )
                        Image(
                            painter = painter,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(item.emoji, fontSize = 20.sp)
                            Text(
                                item.title,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Bottom-right quick share button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                onHaptic()
                                val readyFile = gifFile ?: AnimatedGifBuilder.getOrCreatePresetGif(context, item)
                                onSendGifFile(readyFile, true, item.title)
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "GIF শেয়ার করুন",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
