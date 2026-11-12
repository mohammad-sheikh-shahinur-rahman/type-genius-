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

    @Query("DELETE FROM learned_words WHERE word = :word")
    suspend fun deleteWord(word: String)
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

@Database(entities = [LearnedWord::class, ClipboardItem::class], version = 1, exportSchema = false)
abstract class TypeGeniusDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun clipboardDao(): ClipboardDao

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
