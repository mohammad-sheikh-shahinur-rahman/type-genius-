package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiDirectClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val preferredModels = listOf(
        "gemini-3.8-flash",
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
        "gemini-3.5-flash",
        "gemini-3.6-flash",
        "gemini-3.7-flash",
        "gemini-3-flash-preview",
        "gemini-2.5-flash",
        "gemini-2.0-flash"
    )

    private fun sanitizeKey(key: String): String {
        return key.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
            .replace(" ", "")
            .removeSurrounding("\"")
            .removeSurrounding("'")
    }

    private fun buildCandidateModels(cleanKey: String): List<String> {
        val discovered = discoverFlashModels(cleanKey)
        return (preferredModels + discovered).distinct()
    }

    private fun discoverFlashModels(cleanKey: String): List<String> {
        return runCatching {
            val req = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=50")
                .header("x-goog-api-key", cleanKey)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val raw = resp.body?.string().orEmpty()
                val arr = JSONObject(raw).optJSONArray("models") ?: return@use emptyList()
                val result = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val name = m.optString("name").removePrefix("models/")
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val supportsGen = methods != null && (0 until methods.length()).any {
                        methods.optString(it) == "generateContent"
                    }
                    if (supportsGen &&
                        name.contains("gemini") &&
                        name.contains("flash") &&
                        !name.contains("tts") &&
                        !name.contains("image") &&
                        !name.contains("live") &&
                        !name.contains("audio")
                    ) {
                        result.add(name)
                    }
                }
                result
            }
        }.getOrDefault(emptyList())
    }

    suspend fun executeDirectPrompt(
        apiKey: String,
        prompt: String,
        systemInstruction: String = "You are TypeGenius AI, a helpful keyboard assistant. Provide only the concise corrected/translated result without conversational filler or markdown quotes."
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API key provided. TypeGenius works 100% offline, or add a key from aistudio.google.com.")
            )
        }

        var lastError: Exception = Exception("Unable to reach Gemini API")
        var triedModels = preferredModels

        for (pass in 0..1) {
            val modelsToTry = if (pass == 0) {
                triedModels
            } else {
                val extra = buildCandidateModels(cleanKey).filterNot { it in triedModels }
                if (extra.isEmpty()) break
                extra
            }

            for (model in modelsToTry) {
                try {
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
                    val jsonBody = JSONObject().apply {
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                        })
                        put("contents", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                        }))
                    }

                    val request = Request.Builder()
                        .url(endpoint)
                        .header("x-goog-api-key", cleanKey)
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            val parsedMsg = parseGoogleError(response.code, bodyStr, model)
                            if (response.code == 404) {
                                lastError = Exception(parsedMsg)
                                return@use // try next model
                            }
                            return@withContext Result.failure(Exception(parsedMsg))
                        }
                        val text = extractResponseText(bodyStr)
                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                        lastError = Exception("Empty response from Gemini")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }
        Result.failure(lastError)
    }

    suspend fun executeStructuredJsonPrompt(
        apiKey: String,
        userText: String,
        systemInstruction: String,
        responseSchema: JSONObject,
        temperature: Double = 0.2
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API key provided."))
        }
        var lastError: Exception = Exception("Unable to reach Gemini API")

        for (pass in 0..1) {
            val modelsToTry = if (pass == 0) {
                preferredModels
            } else {
                val extra = buildCandidateModels(cleanKey).filterNot { it in preferredModels }
                if (extra.isEmpty()) break
                extra
            }

            for (model in modelsToTry) {
                try {
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
                    val genConfig = JSONObject()
                        .put("temperature", temperature)
                        .put("responseMimeType", "application/json")
                        .put("responseSchema", responseSchema)

                    val bodyJson = JSONObject()
                        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                        .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", userText)))))
                        .put("generationConfig", genConfig)

                    val request = Request.Builder()
                        .url(endpoint)
                        .header("x-goog-api-key", cleanKey)
                        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        val rawBody = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            val parsedMsg = parseGoogleError(response.code, rawBody, model)
                            if (response.code == 404) {
                                lastError = Exception(parsedMsg)
                                return@use // try next model
                            }
                            return@withContext Result.failure(Exception(parsedMsg))
                        }
                        val text = extractResponseText(rawBody)
                        if (!text.isNullOrBlank()) {
                            val parsed = runCatching { JSONObject(text) }.getOrNull()
                            if (parsed != null) return@withContext Result.success(parsed)
                        }
                        lastError = Exception("Could not parse structured Gemini response")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }
        Result.failure(lastError)
    }

    private fun parseGoogleError(code: Int, rawBody: String, model: String): String {
        return runCatching {
            val errObj = JSONObject(rawBody).getJSONObject("error")
            val msg = errObj.optString("message", "")
            val status = errObj.optString("status", "")
            when {
                status == "UNAUTHENTICATED" || code == 401 ->
                    "API Key is invalid or expired (401). Please copy a fresh key from aistudio.google.com/app/apikey"
                status == "PERMISSION_DENIED" || code == 403 ->
                    "Permission denied (403): Generative Language API is disabled or region restricted for this key."
                code == 404 ->
                    "Model $model unavailable (404)"
                msg.isNotBlank() -> msg
                else -> "HTTP $code: $rawBody"
            }
        }.getOrNull() ?: "HTTP $code: $rawBody"
    }

    private fun extractResponseText(rawBody: String): String? {
        return runCatching {
            val parts = JSONObject(rawBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            (0 until parts.length())
                .map { parts.getJSONObject(it) }
                .firstOrNull { it.has("text") && !it.optBoolean("thought", false) }
                ?.getString("text")
        }.getOrNull()
    }
}
