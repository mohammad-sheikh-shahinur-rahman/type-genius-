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
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash"
    )

    suspend fun executeDirectPrompt(
        apiKey: String,
        prompt: String,
        systemInstruction: String = "You are TypeGenius AI, a helpful keyboard assistant. Provide only the concise corrected/translated result without conversational filler or markdown quotes."
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API key provided. Add your key in TypeGenius Settings for optional AI features.")
            )
        }

        val cleanKey = apiKey.trim()
        var lastError: Exception = Exception("Unable to reach Gemini API")

        for (model in preferredModels) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
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
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.code == 404) {
                        lastError = Exception("Model $model unavailable (404)")
                        return@use
                    }
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Gemini API Error (${response.code}): $bodyStr"))
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
        Result.failure(lastError)
    }

    suspend fun executeStructuredJsonPrompt(
        apiKey: String,
        userText: String,
        systemInstruction: String,
        responseSchema: JSONObject,
        temperature: Double = 0.2
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API key provided."))
        }
        val cleanKey = apiKey.trim()
        var lastError: Exception = Exception("Unable to reach Gemini API")

        for (model in preferredModels) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
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
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string() ?: ""
                    if (response.code == 404) {
                        lastError = Exception("Model $model unavailable")
                        return@use
                    }
                    if (!response.isSuccessful) {
                        val msg = runCatching {
                            JSONObject(rawBody).getJSONObject("error").getString("message")
                        }.getOrNull() ?: "HTTP ${response.code}"
                        return@withContext Result.failure(Exception(msg))
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
        Result.failure(lastError)
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
