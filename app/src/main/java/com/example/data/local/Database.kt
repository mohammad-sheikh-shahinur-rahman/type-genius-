package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "learned_words")
data class LearnedWord(
    @PrimaryKey val word: String,
    val language: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "translation_memory")
data class TranslationMemoryEntry(
    @PrimaryKey val cacheKey: String,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val strategy: String,
    val confidence: Float = 1.0f,
    val isPinned: Boolean = false,
    val hitCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "text_shortcuts")
data class TextShortcut(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trigger: String,
    val expansion: String,
    val language: String = "all"
)

@Dao
interface WordDao {
    @Query("SELECT * FROM learned_words WHERE language = :lang ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopWords(lang: String, limit: Int = 30): List<LearnedWord>

    @Query("SELECT * FROM learned_words WHERE language = :lang AND word LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun searchPrefix(lang: String, prefix: String, limit: Int = 10): List<LearnedWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(word: LearnedWord)

    @Query("UPDATE learned_words SET frequency = frequency + 1, lastUsed = :now WHERE word = :word AND language = :lang")
    suspend fun incrementFrequency(word: String, lang: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM learned_words ORDER BY frequency DESC, lastUsed DESC LIMIT 200")
    fun getAllLearnedWords(): Flow<List<LearnedWord>>

    @Query("DELETE FROM learned_words WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM learned_words")
    suspend fun clearAllLearnedWords()
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC LIMIT 50")
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Update
    suspend fun update(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinned()
}

@Dao
interface TranslationMemoryDao {
    @Query("SELECT * FROM translation_memory WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun findByKey(cacheKey: String): TranslationMemoryEntry?

    @Query("SELECT * FROM translation_memory ORDER BY isPinned DESC, timestamp DESC LIMIT 40")
    fun observeHistory(): Flow<List<TranslationMemoryEntry>>

    @Query("SELECT COUNT(*) FROM translation_memory")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TranslationMemoryEntry)

    @Query("UPDATE translation_memory SET hitCount = hitCount + 1, timestamp = :now WHERE cacheKey = :cacheKey")
    suspend fun recordHit(cacheKey: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE translation_memory SET isPinned = :pinned WHERE cacheKey = :cacheKey")
    suspend fun setPinned(cacheKey: String, pinned: Boolean)

    @Query("DELETE FROM translation_memory WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM translation_memory WHERE isPinned = 0")
    suspend fun clearUnpinned()
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM text_shortcuts ORDER BY trigger ASC")
    fun observeAll(): Flow<List<TextShortcut>>

    @Query("SELECT * FROM text_shortcuts WHERE LOWER(trigger) = LOWER(:trigger) LIMIT 3")
    suspend fun findExact(trigger: String): List<TextShortcut>

    @Query("SELECT * FROM text_shortcuts WHERE LOWER(trigger) LIKE LOWER(:prefix) || '%' ORDER BY trigger ASC LIMIT 4")
    suspend fun searchPrefix(prefix: String): List<TextShortcut>

    @Query("SELECT COUNT(*) FROM text_shortcuts")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shortcut: TextShortcut): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(shortcuts: List<TextShortcut>)

    @Delete
    suspend fun delete(shortcut: TextShortcut)
}

@Database(
    entities = [LearnedWord::class, ClipboardItem::class, TranslationMemoryEntry::class, TextShortcut::class],
    version = 3,
    exportSchema = false
)
abstract class TypeGeniusDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun translationMemoryDao(): TranslationMemoryDao
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: TypeGeniusDatabase? = null

        fun getDatabase(context: Context): TypeGeniusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TypeGeniusDatabase::class.java,
                    "typegenius_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
