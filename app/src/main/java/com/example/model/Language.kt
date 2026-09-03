package com.example.model

/**
 * Represents a language available for online translation and offline download.
 */
data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val packSizeMb: Float = 14.5f,
    val isPreinstalled: Boolean = false
) {
    val displayName: String
        get() = if (nativeName.isNotEmpty() && nativeName != name) "$name ($nativeName)" else name
}

object LanguageCatalog {
    /**
     * Master catalog of over 100 world languages available for internet download and translation.
     */
    val ALL_LANGUAGES: List<LanguageItem> = listOf(
        // Core & Indian Languages
        LanguageItem("en", "English", "English", 12.0f, isPreinstalled = true),
        LanguageItem("ta", "Tamil", "தமிழ்", 15.4f, isPreinstalled = true),
        LanguageItem("hi", "Hindi", "हिन्दी", 16.2f, isPreinstalled = true),
        LanguageItem("ml", "Malayalam", "മലയാളം", 14.8f, isPreinstalled = true),
        LanguageItem("te", "Telugu", "తెలుగు", 15.1f, isPreinstalled = true),
        LanguageItem("kn", "Kannada", "ಕನ್ನಡ", 14.6f),
        LanguageItem("bn", "Bengali", "বাংলা", 16.8f),
        LanguageItem("mr", "Marathi", "मराठी", 15.0f),
        LanguageItem("gu", "Gujarati", "ગુજરાતી", 14.2f),
        LanguageItem("pa", "Punjabi", "ਪੰਜਾਬੀ", 14.0f),
        LanguageItem("ur", "Urdu", "اردو", 15.5f),
        LanguageItem("or", "Odia", "ଓଡ଼ିଆ", 13.9f),
        LanguageItem("as", "Assamese", "অসমীয়া", 13.5f),
        LanguageItem("ne", "Nepali", "नेपाली", 14.1f),
        LanguageItem("si", "Sinhala", "සිංහල", 14.3f),

        // Global Major Languages
        LanguageItem("es", "Spanish", "Español", 18.2f, isPreinstalled = true),
        LanguageItem("fr", "French", "Français", 17.5f, isPreinstalled = true),
        LanguageItem("de", "German", "Deutsch", 19.1f),
        LanguageItem("ja", "Japanese", "日本語", 22.4f),
        LanguageItem("zh", "Chinese (Simplified)", "简体中文", 24.0f),
        LanguageItem("zh-TW", "Chinese (Traditional)", "繁體中文", 24.5f),
        LanguageItem("ar", "Arabic", "العربية", 20.1f),
        LanguageItem("ru", "Russian", "Русский", 21.0f),
        LanguageItem("pt", "Portuguese", "Português", 17.8f),
        LanguageItem("it", "Italian", "Italiano", 16.9f),
        LanguageItem("ko", "Korean", "한국어", 21.8f),
        LanguageItem("tr", "Turkish", "Türkçe", 16.5f),
        LanguageItem("nl", "Dutch", "Nederlands", 15.7f),
        LanguageItem("pl", "Polish", "Polski", 16.3f),
        LanguageItem("sv", "Swedish", "Svenska", 15.2f),
        LanguageItem("id", "Indonesian", "Bahasa Indonesia", 14.9f),
        LanguageItem("th", "Thai", "ไทย", 17.2f),
        LanguageItem("vi", "Vietnamese", "Tiếng Việt", 16.7f),
        LanguageItem("uk", "Ukrainian", "Українська", 18.0f),
        LanguageItem("fa", "Persian", "فارسی", 16.4f),
        LanguageItem("he", "Hebrew", "עברית", 15.8f),
        LanguageItem("el", "Greek", "Ελληνικά", 17.1f),
        LanguageItem("cs", "Czech", "Čeština", 15.4f),
        LanguageItem("ro", "Romanian", "Română", 15.6f),
        LanguageItem("hu", "Hungarian", "Magyar", 16.0f),
        LanguageItem("da", "Danish", "Dansk", 14.7f),
        LanguageItem("fi", "Finnish", "Suomi", 15.3f),
        LanguageItem("no", "Norwegian", "Norsk", 14.8f),
        LanguageItem("ms", "Malay", "Bahasa Melayu", 14.5f),
        LanguageItem("tl", "Filipino (Tagalog)", "Tagalog", 15.0f),
        LanguageItem("sw", "Swahili", "Kiswahili", 13.8f),
        LanguageItem("af", "Afrikaans", "Afrikaans", 13.5f),
        LanguageItem("sq", "Albanian", "Shqip", 14.0f),
        LanguageItem("am", "Amharic", "አማርኛ", 15.2f),
        LanguageItem("hy", "Armenian", "Հայերեն", 14.7f),
        LanguageItem("az", "Azerbaijani", "Azərbaycan", 14.6f),
        LanguageItem("eu", "Basque", "Euskara", 13.2f),
        LanguageItem("be", "Belarusian", "Беларуская", 15.8f),
        LanguageItem("bs", "Bosnian", "Bosanski", 14.4f),
        LanguageItem("bg", "Bulgarian", "Български", 15.5f),
        LanguageItem("ca", "Catalan", "Català", 14.2f),
        LanguageItem("ceb", "Cebuano", "Sinugboanon", 13.7f),
        LanguageItem("hr", "Croatian", "Hrvatski", 14.6f),
        LanguageItem("eo", "Esperanto", "Esperanto", 12.5f),
        LanguageItem("et", "Estonian", "Eesti", 14.1f),
        LanguageItem("gl", "Galician", "Galego", 13.9f),
        LanguageItem("ka", "Georgian", "ქართული", 15.3f),
        LanguageItem("ht", "Haitian Creole", "Kreyòl ayisyen", 13.1f),
        LanguageItem("ha", "Hausa", "Hausa", 13.6f),
        LanguageItem("haw", "Hawaiian", "ʻŌlelo Hawaiʻi", 12.0f),
        LanguageItem("is", "Icelandic", "Íslenska", 14.3f),
        LanguageItem("ig", "Igbo", "Asụsụ Igbo", 13.4f),
        LanguageItem("ga", "Irish", "Gaeilge", 13.8f),
        LanguageItem("jw", "Javanese", "Basa Jawa", 14.2f),
        LanguageItem("kk", "Kazakh", "Қазақ тілі", 15.6f),
        LanguageItem("km", "Khmer", "ភាសាខ្មែរ", 15.9f),
        LanguageItem("rw", "Kinyarwanda", "Ikinyarwanda", 13.5f),
        LanguageItem("ku", "Kurdish", "Kurdî", 14.7f),
        LanguageItem("ky", "Kyrgyz", "Кыргызча", 14.5f),
        LanguageItem("lo", "Lao", "ພາສາລາວ", 15.0f),
        LanguageItem("la", "Latin", "Latina", 12.8f),
        LanguageItem("lv", "Latvian", "Latviešu", 14.2f),
        LanguageItem("lt", "Lithuanian", "Lietuvių", 14.4f),
        LanguageItem("lb", "Luxembourgish", "Lëtzebuergesch", 13.7f),
        LanguageItem("mk", "Macedonian", "Македонски", 14.8f),
        LanguageItem("mg", "Malagasy", "Malagasy", 13.3f),
        LanguageItem("mt", "Maltese", "Malti", 13.6f),
        LanguageItem("mi", "Maori", "Māori", 13.0f),
        LanguageItem("mn", "Mongolian", "Монгол", 15.1f),
        LanguageItem("my", "Myanmar (Burmese)", "မြန်မာစာ", 16.4f),
        LanguageItem("ps", "Pashto", "پښتو", 15.0f),
        LanguageItem("sk", "Slovak", "Slovenčina", 14.9f),
        LanguageItem("sl", "Slovenian", "Slovenščina", 14.5f),
        LanguageItem("so", "Somali", "Soomaaliga", 13.8f),
        LanguageItem("su", "Sundanese", "Basa Sunda", 13.5f),
        LanguageItem("tg", "Tajik", "Тоҷикӣ", 14.4f),
        LanguageItem("tt", "Tatar", "Татар теле", 14.0f),
        LanguageItem("tk", "Turkmen", "Türkmençe", 13.9f),
        LanguageItem("uz", "Uzbek", "Oʻzbekcha", 14.6f),
        LanguageItem("cy", "Welsh", "Cymraeg", 13.4f),
        LanguageItem("xh", "Xhosa", "isiXhosa", 13.7f),
        LanguageItem("yi", "Yiddish", "ייִדיש", 14.2f),
        LanguageItem("yo", "Yoruba", "Èdè Yorùbá", 13.5f),
        LanguageItem("zu", "Zulu", "isiZulu", 13.9f)
    )

    fun findByCode(code: String): LanguageItem? {
        return ALL_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }

    val DEFAULT_DOWNLOADED_CODES: Set<String> = setOf("en", "ta", "hi", "es", "fr")
}
