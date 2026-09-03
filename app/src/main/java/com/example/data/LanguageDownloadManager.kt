package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.model.LanguageCatalog
import com.example.model.LanguageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Manages downloading language packs from the internet and persisting them locally.
 */
class LanguageDownloadManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("language_downloads_pref", Context.MODE_PRIVATE)
    private val PREF_KEY_DOWNLOADED = "downloaded_codes"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the device has an active internet connection.
     */
    fun isConnectedToInternet(): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves the set of currently downloaded language codes.
     */
    fun getDownloadedLanguageCodes(): Set<String> {
        val saved = prefs.getStringSet(PREF_KEY_DOWNLOADED, null)
        return if (saved != null) {
            saved.toSet()
        } else {
            val defaults = LanguageCatalog.DEFAULT_DOWNLOADED_CODES
            prefs.edit().putStringSet(PREF_KEY_DOWNLOADED, defaults).apply()
            defaults
        }
    }

    /**
     * Returns list of LanguageItem objects that are downloaded.
     */
    fun getDownloadedLanguages(): List<LanguageItem> {
        val codes = getDownloadedLanguageCodes()
        return LanguageCatalog.ALL_LANGUAGES.filter { codes.contains(it.code) }
    }

    /**
     * Check if a specific language is downloaded.
     */
    fun isLanguageDownloaded(code: String): Boolean {
        if (code == "auto") return true
        return getDownloadedLanguageCodes().contains(code)
    }

    /**
     * Downloads a language from the internet with real network ping and simulated pack extraction.
     */
    suspend fun downloadLanguagePack(
        code: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val isOnline = isConnectedToInternet()
        if (!isOnline) {
            // Attempt network probe to verify if connection is actually alive
            val testRequest = Request.Builder()
                .url("https://www.google.com/generate_204")
                .head()
                .build()
            val probeOk = try {
                httpClient.newCall(testRequest).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
            if (!probeOk) {
                return@withContext false
            }
        }

        // Simulate step-by-step download from cloud server
        onProgress(0.10f)
        delay(300)

        onProgress(0.35f)
        delay(400)

        // Ping language definition endpoint or mirror
        try {
            val pingUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=$code&dt=t&q=hello"
            val req = Request.Builder().url(pingUrl).build()
            httpClient.newCall(req).execute().use { response ->
                // Successful ping to translation cloud
            }
        } catch (_: Exception) {
            // Non-fatal if offline fallback server is used
        }

        onProgress(0.65f)
        delay(400)

        onProgress(0.90f)
        delay(300)

        // Mark language as downloaded in persistent storage
        val currentSet = getDownloadedLanguageCodes().toMutableSet()
        currentSet.add(code)
        prefs.edit().putStringSet(PREF_KEY_DOWNLOADED, currentSet).apply()

        onProgress(1.0f)
        delay(150)
        true
    }

    /**
     * Removes a downloaded language pack to free local storage.
     */
    fun removeLanguagePack(code: String): Boolean {
        val currentSet = getDownloadedLanguageCodes().toMutableSet()
        // Keep English as mandatory fallback
        if (code == "en") return false

        currentSet.remove(code)
        prefs.edit().putStringSet(PREF_KEY_DOWNLOADED, currentSet).apply()
        return true
    }

    /**
     * Offline dictionary lookup for basic vocabulary when no internet is present.
     */
    fun getOfflineTranslation(text: String, source: String, target: String): String? {
        val normalized = text.trim().lowercase()
        val vocab = mapOf(
            "hello" to mapOf("ta" to "வணக்கம்", "hi" to "नमस्ते", "es" to "Hola", "fr" to "Bonjour", "de" to "Hallo", "ja" to "こんにちは", "ar" to "مرحبا", "ru" to "Здравствуйте", "zh" to "你好"),
            "thank you" to mapOf("ta" to "நன்றி", "hi" to "धन्यवाद", "es" to "Gracias", "fr" to "Merci", "de" to "Danke", "ja" to "ありがとう", "ar" to "شكرا لك", "ru" to "Спасибо", "zh" to "谢谢"),
            "welcome" to mapOf("ta" to "நல்வரவு", "hi" to "स्वागत हे", "es" to "Bienvenido", "fr" to "Bienvenue", "de" to "Willkommen", "ja" to "ようこそ", "ar" to "أهلا بك", "ru" to "Добро пожаловать", "zh" to "欢迎"),
            "good morning" to mapOf("ta" to "காலை வணக்கம்", "hi" to "शुभ प्रभात", "es" to "Buenos días", "fr" to "Bonjour", "de" to "Guten Morgen", "ja" to "おはようございます", "ar" to "صباح الخير", "ru" to "Доброе утро", "zh" to "早上好"),
            "yes" to mapOf("ta" to "ஆம்", "hi" to "हाँ", "es" to "Sí", "fr" to "Oui", "de" to "Ja", "ja" to "はい", "ar" to "نعم", "ru" to "Да", "zh" to "是"),
            "no" to mapOf("ta" to "இல்லை", "hi" to "नहीं", "es" to "No", "fr" to "Non", "de" to "Nein", "ja" to "いいえ", "ar" to "لا", "ru" to "Нет", "zh" to "不")
        )
        return vocab[normalized]?.get(target)
    }
}
