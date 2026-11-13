package com.example.data.translator

import android.content.Context
import com.example.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TranslationPackInstallResult(
    val langCode: String,
    val entryCount: Int,
    val sizeBytes: Long
)

/**
 * Manages on-demand offline language pack installation & removal inside private app storage
 * (`filesDir/translation_packs/<langCode>.json`). Zero network servers required: packs are
 * extracted into real per-language SQL/JSON pack files on disk and loaded dynamically into
 * [LocalTranslationEngine].
 */
object TranslationPackManager {

    private const val PACK_DIR_NAME = "translation_packs"
    private const val EXTRA_ASSET_FILE = "typegenius_extra_packs.json"

    val downloadableCodes: Set<String> = setOf(
        "ta", "te", "gu", "pa", "ml", "kn", "mr", "sa",
        "he", "th", "vi", "nl", "pl", "uk", "sw", "tl"
    )

    fun getPackDir(context: Context): File {
        val dir = File(context.filesDir, PACK_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPackFile(context: Context, langCode: String): File {
        return File(getPackDir(context), "$langCode.json")
    }

    fun getInstalledCodesFromDisk(context: Context): Set<String> {
        val dir = File(context.filesDir, PACK_DIR_NAME)
        if (!dir.exists() || !dir.isDirectory) return emptySet()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "json" && it.length() > 0L }
            ?.map { it.nameWithoutExtension }
            ?.toSet()
            ?: emptySet()
    }

    fun isPackReady(context: Context?, langCode: String, downloadedSet: Set<String> = emptySet()): Boolean {
        val lang = OfflineTranslator.supportedLanguages.find { it.code == langCode }
        if (lang?.isBundledCore == true) return true
        if (langCode in downloadedSet) return true
        return context != null && getPackFile(context, langCode).let { it.exists() && it.length() > 0L }
    }

    fun getPackSizeLabel(context: Context, langCode: String): String {
        val lang = OfflineTranslator.supportedLanguages.find { it.code == langCode }
        if (lang?.isBundledCore == true) return "Built-in"
        val file = getPackFile(context, langCode)
        if (!file.exists() || file.length() <= 0L) return "Offline Pack (~3 KB)"
        val bytes = file.length()
        return if (bytes >= 1024L) {
            String.format("%.1f KB", bytes / 1024.0)
        } else {
            "$bytes B"
        }
    }

    suspend fun downloadLanguagePack(
        context: Context,
        langCode: String
    ): TranslationPackInstallResult = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val rawExtra = appCtx.assets.open(EXTRA_ASSET_FILE)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(rawExtra)

        val outPhrases = JSONArray()
        val phrasesArr = root.optJSONArray("phrases")
        if (phrasesArr != null) {
            for (i in 0 until phrasesArr.length()) {
                val obj = phrasesArr.optJSONObject(i) ?: continue
                val valTarget = obj.optString(langCode)
                if (valTarget.isNotBlank()) {
                    val cleanRow = JSONObject()
                    obj.optString("en").takeIf { it.isNotBlank() }?.let { cleanRow.put("en", it) }
                    obj.optString("bn").takeIf { it.isNotBlank() }?.let { cleanRow.put("bn", it) }
                    cleanRow.put(langCode, valTarget)
                    outPhrases.put(cleanRow)
                }
            }
        }

        val outLexicon = JSONArray()
        val lexArr = root.optJSONArray("lexicon")
        if (lexArr != null) {
            for (i in 0 until lexArr.length()) {
                val obj = lexArr.optJSONObject(i) ?: continue
                val valTarget = obj.optString(langCode)
                if (valTarget.isNotBlank()) {
                    val cleanRow = JSONObject()
                    cleanRow.put("pos", obj.optString("pos", "OTHER"))
                    obj.optString("en").takeIf { it.isNotBlank() }?.let { cleanRow.put("en", it) }
                    obj.optString("bn").takeIf { it.isNotBlank() }?.let { cleanRow.put("bn", it) }
                    cleanRow.put(langCode, valTarget)
                    outLexicon.put(cleanRow)
                }
            }
        }

        val packJson = JSONObject().apply {
            put("langCode", langCode)
            put("version", root.optString("packVersion", "typegenius-extra-packs-v1"))
            put("installedAt", System.currentTimeMillis())
            put("phrases", outPhrases)
            put("lexicon", outLexicon)
        }

        val outFile = getPackFile(appCtx, langCode)
        outFile.writeText(packJson.toString(), Charsets.UTF_8)

        SettingsManager.getInstance(appCtx).addDownloadedTranslatorPack(langCode)
        LocalTranslationEngine.reloadDownloadedPacks(appCtx)

        TranslationPackInstallResult(
            langCode = langCode,
            entryCount = outPhrases.length() + outLexicon.length(),
            sizeBytes = outFile.length()
        )
    }

    suspend fun downloadAllExtraPacks(context: Context): List<TranslationPackInstallResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TranslationPackInstallResult>()
        for (code in downloadableCodes) {
            results.add(downloadLanguagePack(context, code))
        }
        results
    }

    suspend fun removeLanguagePack(context: Context, langCode: String) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val file = getPackFile(appCtx, langCode)
        if (file.exists()) file.delete()
        SettingsManager.getInstance(appCtx).removeDownloadedTranslatorPack(langCode)
        LocalTranslationEngine.reloadDownloadedPacks(appCtx)
    }
}
