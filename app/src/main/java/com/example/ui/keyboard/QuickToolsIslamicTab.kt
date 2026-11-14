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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
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
import com.example.data.BengaliCalendarUtils
import com.example.data.IslamicToolkit
import java.util.Date

@Composable
fun IslamicToolsTab(
    colors: KeyboardColors,
    tasbihCount: Int,
    tasbihDhikrIndex: Int,
    onIncrementTasbih: () -> Unit,
    onResetTasbih: () -> Unit,
    onCycleTasbihDhikr: () -> Unit,
    onInsertSnippet: (String) -> Unit,
    onHaptic: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("সব") }
    val hijri = remember { IslamicToolkit.toHijriDate(Date()) }
    val activeDhikr = IslamicToolkit.tasbihPresets[tasbihDhikrIndex.coerceIn(0, IslamicToolkit.tasbihPresets.lastIndex)]
    val filteredSnippets = remember(selectedCategory) {
        if (selectedCategory == "সব") IslamicToolkit.snippets
        else IslamicToolkit.snippets.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // 1. Live Hijri Date + Calligraphic Symbols Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.accent.copy(alpha = 0.16f))
                    .clickable {
                        onHaptic()
                        onInsertSnippet(hijri.formattedBn)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("islamic_hijri_date_chip")
            ) {
                Text(
                    text = "🌙 ${hijri.formattedBn}",
                    color = colors.accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IslamicToolkit.honorificAndSymbols.forEachIndexed { idx, (label, insertText) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.keyBackground)
                        .clickable {
                            onHaptic()
                            onInsertSnippet(insertText)
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("islamic_symbol_$idx")
                ) {
                    Text(
                        text = label,
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Digital Tasbih Counter Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(colors.keyBackground)
                .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onHaptic()
                        onCycleTasbihDhikr()
                    }
                    .testTag("tasbih_cycle_dhikr")
            ) {
                Text(
                    text = "ডিজিটাল তসবিহ • ট্যাপ করে পরিবর্তন করুন",
                    color = colors.textSecondary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${activeDhikr.titleBn} (${activeDhikr.arabic})",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "${BengaliCalendarUtils.toBengaliDigits(tasbihCount.toString())}/${BengaliCalendarUtils.toBengaliDigits(activeDhikr.target.toString())}",
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("tasbih_count_label")
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable {
                            onHaptic()
                            onIncrementTasbih()
                        }
                        .testTag("tasbih_increment_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "তসবিহ গণনা", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                IconButton(
                    onClick = {
                        onHaptic()
                        onResetTasbih()
                    },
                    modifier = Modifier.size(26.dp).testTag("tasbih_reset_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "রিসেট", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            onInsertSnippet("${activeDhikr.arabic} (${activeDhikr.titleBn})")
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("tasbih_insert_btn")
                ) {
                    Text("বসান", color = colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            IslamicToolkit.islamicCategories.forEach { cat ->
                val selected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) colors.accent else colors.keySpecialBackground)
                        .clickable {
                            onHaptic()
                            selectedCategory = cat
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (selected) Color.White else colors.textPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Snippets List
        filteredSnippets.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .clickable {
                        onHaptic()
                        onInsertSnippet(item.text)
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("islamic_snippet_${item.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = colors.accent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.text,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
