package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TextShortcutRepository(context: Context) {

    private val shortcutDao = TypeGeniusDatabase.getDatabase(context).shortcutDao()

    val allShortcuts: Flow<List<TextShortcut>> = shortcutDao.observeAll()

    suspend fun ensureSeededDefaults() = withContext(Dispatchers.IO) {
        try {
            if (shortcutDao.countAll() == 0) {
                shortcutDao.insertAll(
                    listOf(
                        TextShortcut(trigger = "gm", expansion = "শুভ সকাল ☀️"),
                        TextShortcut(trigger = "gn", expansion = "শুভ রাত্রি 🌙"),
                        TextShortcut(trigger = "slm", expansion = "আসসালামু আলাইকুম"),
                        TextShortcut(trigger = "ws", expansion = "ওয়ালাইকুমুস সালাম"),
                        TextShortcut(trigger = "dhn", expansion = "ধন্যবাদ"),
                        TextShortcut(trigger = "ty", expansion = "Thank you!"),
                        TextShortcut(trigger = "brb", expansion = "Be right back"),
                        TextShortcut(trigger = "syl", expansion = "আফনে ভালা আছইননি?")
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun findMatchingExpansions(token: String): List<String> = withContext(Dispatchers.IO) {
        val clean = token.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        return@withContext try {
            val exact = shortcutDao.findExact(clean).map { it.expansion }
            val prefix = if (clean.length >= 2) {
                shortcutDao.searchPrefix(clean).map { it.expansion }
            } else {
                emptyList()
            }
            (exact + prefix).distinct().take(3)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addOrUpdateShortcut(trigger: String, expansion: String, language: String = "all") =
        withContext(Dispatchers.IO) {
            val cleanTrigger = trigger.trim().lowercase()
            val cleanExpansion = expansion.trim()
            if (cleanTrigger.isEmpty() || cleanExpansion.isEmpty()) return@withContext
            val existing = shortcutDao.findExact(cleanTrigger).firstOrNull()
            shortcutDao.upsert(
                TextShortcut(
                    id = existing?.id ?: 0,
                    trigger = cleanTrigger,
                    expansion = cleanExpansion,
                    language = language
                )
            )
        }

    suspend fun deleteShortcut(shortcut: TextShortcut) = withContext(Dispatchers.IO) {
        shortcutDao.delete(shortcut)
    }
}
