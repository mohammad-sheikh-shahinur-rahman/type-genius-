package com.example.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class KeyboardFontPreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val badge: String
) {
    ATMA_HANDWRITING(
        id = "atma",
        displayName = "বাংলা হাতের লেখা (Atma)",
        subtitle = "স্বাভাবিক হাতে লেখা বাংলা ও ইংরেজি স্ক্রিপ্ট",
        badge = "হাতের লেখা"
    ),
    GALADA_CALLIGRAPHY(
        id = "galada",
        displayName = "বাংলা ক্যালিগ্রাফি (Galada)",
        subtitle = "শৈল্পিক তুলি ও হাতের লেখার বাংলা ফন্ট",
        badge = "ক্যালিগ্রাফি"
    ),
    CUSTOM_IMPORTED(
        id = "custom",
        displayName = "আমার ইমপোর্ট করা ফন্ট (.zip / .ttf)",
        subtitle = "BanglaHandwriting-Regular.zip বা যেকোনো ফন্ট",
        badge = "CUSTOM"
    ),
    SYSTEM(
        id = "system",
        displayName = "System Default",
        subtitle = "ডিভাইসের ডিফল্ট স্ট্যান্ডার্ড ফন্ট",
        badge = "DEFAULT"
    );

    companion object {
        fun fromId(id: String): KeyboardFontPreset =
            entries.firstOrNull { it.id == id } ?: ATMA_HANDWRITING
    }
}

object CustomFontManager {

    private var cachedKey: String? = null
    private var cachedFontFamily: FontFamily = FontFamily.Default

    private val galadaFontFamily: FontFamily by lazy {
        FontFamily(Font(R.font.galada, FontWeight.Normal))
    }

    private val atmaFontFamily: FontFamily by lazy {
        FontFamily(Font(R.font.atma, FontWeight.Normal))
    }

    fun getFontFamily(context: Context, fontId: String, customFontPath: String): FontFamily {
        val key = "$fontId|$customFontPath"
        if (cachedKey == key) return cachedFontFamily

        val resolved = when (KeyboardFontPreset.fromId(fontId)) {
            KeyboardFontPreset.SYSTEM -> FontFamily.Default
            KeyboardFontPreset.GALADA_CALLIGRAPHY -> galadaFontFamily
            KeyboardFontPreset.ATMA_HANDWRITING -> atmaFontFamily
            KeyboardFontPreset.CUSTOM_IMPORTED -> {
                loadCustomFileFontFamily(customFontPath) ?: atmaFontFamily
            }
        }
        cachedKey = key
        cachedFontFamily = resolved
        return resolved
    }

    fun hasCustomFont(customFontPath: String): Boolean {
        if (customFontPath.isBlank()) return false
        return runCatching {
            val f = File(customFontPath)
            f.exists() && f.length() >= 128L
        }.getOrDefault(false)
    }

    fun getTypeface(context: Context, fontId: String, customFontPath: String): Typeface {
        return when (KeyboardFontPreset.fromId(fontId)) {
            KeyboardFontPreset.SYSTEM -> Typeface.DEFAULT
            KeyboardFontPreset.GALADA_CALLIGRAPHY -> {
                runCatching { androidx.core.content.res.ResourcesCompat.getFont(context, R.font.galada) }.getOrNull()
                    ?: Typeface.DEFAULT
            }
            KeyboardFontPreset.ATMA_HANDWRITING -> {
                runCatching { androidx.core.content.res.ResourcesCompat.getFont(context, R.font.atma) }.getOrNull()
                    ?: Typeface.DEFAULT
            }
            KeyboardFontPreset.CUSTOM_IMPORTED -> {
                loadCustomTypeface(customFontPath)
                    ?: runCatching { androidx.core.content.res.ResourcesCompat.getFont(context, R.font.atma) }.getOrNull()
                    ?: Typeface.DEFAULT
            }
        }
    }

    fun getTypefaceForFancyStyle(
        context: Context,
        style: com.example.data.dictionary.FancyFontStyle,
        customFontPath: String
    ): Typeface {
        val presetId = style.handwritingPresetId()
        if (presetId != null) {
            return getTypeface(context, presetId, customFontPath)
        }
        return when (style) {
            com.example.data.dictionary.FancyFontStyle.BOLD_SANS,
            com.example.data.dictionary.FancyFontStyle.SERIF_BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            com.example.data.dictionary.FancyFontStyle.ITALIC_SANS,
            com.example.data.dictionary.FancyFontStyle.SERIF_ITALIC -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            com.example.data.dictionary.FancyFontStyle.BOLD_ITALIC -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            com.example.data.dictionary.FancyFontStyle.MONOSPACE -> Typeface.MONOSPACE
            else -> runCatching { androidx.core.content.res.ResourcesCompat.getFont(context, R.font.atma) }.getOrNull()
                ?: Typeface.DEFAULT
        }
    }

    private fun loadCustomTypeface(path: String): Typeface? {
        if (path.isBlank()) return null
        return try {
            val file = File(path)
            if (!file.exists() || file.length() < 128L) return null
            Typeface.createFromFile(file)
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadCustomFileFontFamily(path: String): FontFamily? {
        val typeface = loadCustomTypeface(path) ?: return null
        return FontFamily(typeface)
    }

    suspend fun importFontFromUri(context: Context, uri: Uri): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val originalName = queryDisplayName(context, uri) ?: "CustomFont"
                val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
                fontsDir.listFiles()?.forEach { it.delete() }

                val rawBytes = context.contentResolver.openInputStream(uri)?.use {
                    BufferedInputStream(it).readBytes()
                } ?: return@withContext Result.failure(IllegalStateException("ফাইলটি পড়া যায়নি"))

                val isZip = originalName.endsWith(".zip", ignoreCase = true) ||
                    (rawBytes.size >= 4 &&
                        rawBytes[0] == 0x50.toByte() &&
                        rawBytes[1] == 0x4B.toByte() &&
                        rawBytes[2] == 0x03.toByte() &&
                        rawBytes[3] == 0x04.toByte())

                var extractedBytes: ByteArray? = null
                var resolvedName = originalName.substringBeforeLast(".")
                var ext = "ttf"

                if (isZip) {
                    val candidates = mutableListOf<Triple<String, String, ByteArray>>()
                    ZipInputStream(rawBytes.inputStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            val lower = name.lowercase()
                            if (!entry.isDirectory &&
                                !lower.contains("__macosx") &&
                                !name.substringAfterLast("/").startsWith("._") &&
                                (lower.endsWith(".ttf") || lower.endsWith(".otf"))
                            ) {
                                val bytes = zis.readBytes()
                                val fileExt = if (lower.endsWith(".otf")) "otf" else "ttf"
                                candidates.add(Triple(name.substringAfterLast("/"), fileExt, bytes))
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }

                    val chosen = candidates.firstOrNull { (name, _, _) ->
                        name.contains("regular", ignoreCase = true)
                    } ?: candidates.firstOrNull()

                    if (chosen == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("ZIP ফাইলের ভেতরে কোনো .ttf বা .otf ফন্ট পাওয়া যায়নি")
                        )
                    }
                    resolvedName = chosen.first.substringBeforeLast(".")
                    ext = chosen.second
                    extractedBytes = chosen.third
                } else {
                    val lower = originalName.lowercase()
                    if (lower.endsWith(".otf")) ext = "otf"
                    extractedBytes = rawBytes
                }

                val outFile = File(fontsDir, "custom_font_${System.currentTimeMillis()}.$ext")
                FileOutputStream(outFile).use { fos -> fos.write(extractedBytes) }

                // Verify Android can parse this font file safely before persisting
                val tf = Typeface.createFromFile(outFile)
                if (tf == null) {
                    outFile.delete()
                    return@withContext Result.failure(IllegalArgumentException("ফন্ট ফাইলটি সঠিক .ttf/.otf ফরম্যাটে নেই"))
                }

                cachedKey = null
                Result.success(outFile.absolutePath to resolvedName.ifBlank { "Custom Handwriting" })
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    fun clearCustomFont(context: Context) {
        try {
            File(context.filesDir, "custom_fonts").listFiles()?.forEach { it.delete() }
            cachedKey = null
        } catch (_: Exception) {}
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
