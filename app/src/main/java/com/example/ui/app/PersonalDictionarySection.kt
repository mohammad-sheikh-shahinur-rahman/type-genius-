package com.example.ui.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dictionary.DictionaryManager
import com.example.ui.theme.TGMagenta
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalDictionaryCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dictManager = remember { DictionaryManager(context) }
    val learnedWords by dictManager.learnedWordsFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var newWordInput by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("bn_probhat") }

    val langChoices = listOf(
        "bn_probhat" to "বাংলা",
        "bn_phonetic" to "Banglish",
        "en" to "English",
        "syl" to "সিলেটি"
    )

    val filteredWords = remember(learnedWords, searchQuery) {
        if (searchQuery.isBlank()) learnedWords
        else learnedWords.filter { it.word.contains(searchQuery.trim(), ignoreCase = true) }
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("personal_dictionary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = TGMagenta)
                    Column {
                        Text(
                            "কাস্টম শব্দকোষ ও শেখা শব্দ (${learnedWords.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "কীবোর্ডের অটো-লার্নড ও আপনার নিজস্ব যুক্ত করা শব্দসমূহ",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (learnedWords.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                dictManager.clearAllLearnedWords()
                                Toast.makeText(context, "সব শেখা শব্দ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("clear_learned_words_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Words", tint = Color(0xFFD32F2F))
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                langChoices.forEach { (code, title) ->
                    FilterChip(
                        selected = selectedLang == code,
                        onClick = { selectedLang = code },
                        label = { Text(title, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TGMagenta.copy(alpha = 0.22f),
                            selectedLabelColor = TGMagenta
                        )
                    )
                }
            }

            // Add new custom word row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newWordInput,
                    onValueChange = { newWordInput = it },
                    placeholder = { Text("নতুন শব্দ লিখুন...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_word_input"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val word = newWordInput.trim()
                        if (word.isNotBlank()) {
                            scope.launch {
                                dictManager.addCustomWord(word, selectedLang)
                                newWordInput = ""
                                Toast.makeText(context, "'$word' শব্দকোষে যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TGMagenta),
                    shape = RoundedCornerShape(12.dp),
                    enabled = newWordInput.isNotBlank(),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("add_custom_word_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Word", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("যোগ")
                }
            }

            // Search filter if multiple words exist
            if (learnedWords.size > 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("শব্দ খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_learned_words_input"),
                    singleLine = true
                )
            }

            // Word chips
            if (filteredWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "কোনো শব্দ খুঁজে পাওয়া যায়নি" else "কীবোর্ডে টাইপ করার সাথে সাথে নতুন শব্দ এখানে অটোমেটিক সেভ হবে",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredWords.forEach { item ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.word,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        dictManager.removeLearnedWord(item.word)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete Word",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
