package com.example.data

import com.example.model.LanguageCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TranslationResult(
    val translatedText: String,
    val detectedSourceLanguage: String? = null,
    val provider: String = "Internet Cloud API"
)

class OnlineTranslationService(private val downloadManager: LanguageDownloadManager) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun detectScriptLanguage(text: String): String {
        return when {
            text.any { it in '\u0B80'..'\u0BFF' } -> "ta" // Tamil
            text.any { it in '\u0900'..'\u097F' } -> "hi" // Hindi / Marathi / Nepali
            text.any { it in '\u0D00'..'\u0D7F' } -> "ml" // Malayalam
            text.any { it in '\u0C00'..'\u0C7F' } -> "te" // Telugu
            text.any { it in '\u0C80'..'\u0CFF' } -> "kn" // Kannada
            text.any { it in '\u0980'..'\u09FF' } -> "bn" // Bengali
            text.any { it in '\u0A80'..'\u0AFF' } -> "gu" // Gujarati
            text.any { it in '\u0A00'..'\u0A7F' } -> "pa" // Punjabi
            text.any { it in '\u0B00'..'\u0B7F' } -> "or" // Odia
            text.any { it in '\u3040'..'\u30FF' } -> "ja" // Japanese Kana
            text.any { it in '\u4E00'..'\u9FFF' } -> "zh" // Chinese / Kanji
            text.any { it in '\uAC00'..'\uD7AF' } -> "ko" // Korean
            text.any { it in '\u0600'..'\u06FF' } -> "ar" // Arabic
            text.any { it in '\u0400'..'\u04FF' } -> "ru" // Cyrillic / Russian
            text.any { it in '\u0590'..'\u05FF' } -> "he" // Hebrew
            text.any { it in '\u0370'..'\u03FF' } -> "el" // Greek
            text.any { it in '\u0E00'..'\u0E7F' } -> "th" // Thai
            else -> "en"
        }
    }

    suspend fun translate(
        text: String,
        sourceCode: String,
        targetCode: String
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Text cannot be empty"))
        }

        val effectiveSource = if (sourceCode == "auto") "auto" else sourceCode

        // If source matches target
        if (effectiveSource == targetCode && effectiveSource != "auto") {
            return@withContext Result.success(TranslationResult(trimmed, effectiveSource, "Direct"))
        }

        val encoded = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        // 1. Primary Engine: Google Translate Web API
        try {
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$effectiveSource&tl=$targetCode&dt=t&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val jsonArray = JSONArray(body)
                val sentences = jsonArray.optJSONArray(0)
                val detected = jsonArray.optString(2, "")

                if (sentences != null && sentences.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val part = sentences.optJSONArray(i)
                        if (part != null && part.length() > 0) {
                            sb.append(part.optString(0, ""))
                        }
                    }
                    val translated = sb.toString()
                    if (translated.isNotBlank()) {
                        return@withContext Result.success(
                            TranslationResult(
                                translatedText = translated,
                                detectedSourceLanguage = if (detected.isNotBlank()) detected else null,
                                provider = "Google Cloud Gateway"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to MyMemory
        }

        // 2. Secondary Engine: MyMemory Public API
        try {
            val srcForMyMemory = if (effectiveSource == "auto") detectScriptLanguage(trimmed) else effectiveSource
            val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$srcForMyMemory|$targetCode"
            val request = Request.Builder().url(url).build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val status = json.optInt("responseStatus")
                if (status == 200) {
                    val data = json.optJSONObject("responseData")
                    val translated = data?.optString("translatedText") ?: ""
                    if (translated.isNotBlank()) {
                        return@withContext Result.success(
                            TranslationResult(
                                translatedText = translated,
                                detectedSourceLanguage = srcForMyMemory,
                                provider = "MyMemory Gateway"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to offline dictionary
        }

        // 3. Offline Fallback: Check local downloaded dictionary
        val offlineResolved = downloadManager.getOfflineTranslation(trimmed, effectiveSource, targetCode)
        if (offlineResolved != null) {
            return@withContext Result.success(
                TranslationResult(
                    translatedText = offlineResolved,
                    detectedSourceLanguage = if (effectiveSource == "auto") "en" else effectiveSource,
                    provider = "Offline Downloaded Pack"
                )
            )
        }

        Result.failure(Exception("Unable to translate. Please check your internet connection or download the $targetCode language pack."))
    }
}
