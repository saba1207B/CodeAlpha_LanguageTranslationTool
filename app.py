"""
AI Language Translation Tool - Backend Server
Developed for CodeAlpha AI Internship Task

Architecture:
- Flask REST API
- Communicates securely with Translation APIs (Google Cloud Translation v2, Microsoft Translator, LibreTranslate)
- Includes fallback translation support for out-of-the-box local testing
- All secret credentials are held in server-side environment variables (.env)
"""

import os
import re
import json
import logging
import urllib.parse
from flask import Flask, render_template, request, jsonify
import requests

# Load environment variables if python-dotenv is present
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Configuration
TRANSLATION_API_KEY = os.getenv("TRANSLATION_API_KEY", "").strip()
TRANSLATION_API_URL = os.getenv("TRANSLATION_API_URL", "").strip()
TRANSLATION_PROVIDER = os.getenv("TRANSLATION_PROVIDER", "auto").strip().lower()
AZURE_TRANSLATOR_REGION = os.getenv("AZURE_TRANSLATOR_REGION", "global").strip()

MAX_CHARACTERS = 5000

# Supported Languages Directory (easily extensible)
SUPPORTED_LANGUAGES = [
    {"code": "en", "name": "English", "native": "English", "flag": "🇺🇸"},
    {"code": "ta", "name": "Tamil", "native": "தமிழ்", "flag": "🇮🇳"},
    {"code": "hi", "name": "Hindi", "native": "हिन्दी", "flag": "🇮🇳"},
    {"code": "ml", "name": "Malayalam", "native": "മലയാളം", "flag": "🇮🇳"},
    {"code": "te", "name": "Telugu", "native": "తెలుగు", "flag": "🇮🇳"},
    {"code": "kn", "name": "Kannada", "native": "ಕನ್ನಡ", "flag": "🇮🇳"},
    {"code": "fr", "name": "French", "native": "Français", "flag": "🇫🇷"},
    {"code": "de", "name": "German", "native": "Deutsch", "flag": "🇩🇪"},
    {"code": "es", "name": "Spanish", "native": "Español", "flag": "🇪🇸"},
    {"code": "ja", "name": "Japanese", "native": "日本語", "flag": "🇯🇵"},
    {"code": "it", "name": "Italian", "native": "Italiano", "flag": "🇮🇹"},
    {"code": "ru", "name": "Russian", "native": "Русский", "flag": "🇷🇺"},
    {"code": "zh", "name": "Chinese (Simplified)", "native": "简体中文", "flag": "🇨🇳"},
    {"code": "ar", "name": "Arabic", "native": "العربية", "flag": "🇸🇦"},
    {"code": "ko", "name": "Korean", "native": "한국어", "flag": "🇰🇷"},
    {"code": "pt", "name": "Portuguese", "native": "Português", "flag": "🇵🇹"}
]

LANGUAGE_CODE_MAP = {lang["code"]: lang["name"] for lang in SUPPORTED_LANGUAGES}


def detect_language_heuristic(text: str) -> str:
    """Fast local script-based heuristic for Indian & East Asian scripts and Latin."""
    # Tamil Unicode block: 0B80 - 0BFF
    if re.search(r'[\u0B80-\u0BFF]', text):
        return "ta"
    # Malayalam Unicode block: 0D00 - 0D7F
    if re.search(r'[\u0D00-\u0D7F]', text):
        return "ml"
    # Telugu Unicode block: 0C00 - 0C7F
    if re.search(r'[\u0C00-\u0C7F]', text):
        return "te"
    # Kannada Unicode block: 0C80 - 0CFF
    if re.search(r'[\u0C80-\u0CFF]', text):
        return "kn"
    # Devanagari (Hindi): 0900 - 097F
    if re.search(r'[\u0900-\u097F]', text):
        return "hi"
    # Japanese (Hiragana, Katakana, Kanji): 3040-309F, 30A0-30FF, 4E00-9FFF
    if re.search(r'[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]', text):
        return "ja"
    # Arabic: 0600 - 06FF
    if re.search(r'[\u0600-\u06FF]', text):
        return "ar"
    # Cyrillic (Russian): 0400 - 04FF
    if re.search(r'[\u0400-\u04FF]', text):
        return "ru"
    return "en"


def translate_with_google_cloud(text: str, source_lang: str, target_lang: str, api_key: str):
    """Google Cloud Translation API v2."""
    url = "https://translation.googleapis.com/language/translate/v2"
    params = {
        "q": text,
        "target": target_lang,
        "key": api_key,
        "format": "text"
    }
    if source_lang and source_lang != "auto":
        params["source"] = source_lang

    response = requests.post(url, data=params, timeout=10)
    if response.status_code == 200:
        data = response.json()
        translations = data.get("data", {}).get("translations", [])
        if translations:
            item = translations[0]
            detected = item.get("detectedSourceLanguage", source_lang)
            return item.get("translatedText", ""), detected, "Google Cloud Translation API"
    elif response.status_code == 400 or response.status_code == 403:
        logger.error(f"Google Cloud Translation error: {response.text}")
        raise ValueError("Invalid Google Cloud Translation API credentials or billing not enabled.")
    response.raise_for_status()


def translate_with_microsoft_azure(text: str, source_lang: str, target_lang: str, api_key: str, region: str):
    """Microsoft Azure Translator API."""
    endpoint = "https://api.cognitive.microsofttranslator.com/translate"
    params = {
        "api-version": "3.0",
        "to": target_lang
    }
    if source_lang and source_lang != "auto":
        params["from"] = source_lang

    headers = {
        "Ocp-Apim-Subscription-Key": api_key,
        "Ocp-Apim-Subscription-Region": region,
        "Content-Type": "application/json"
    }
    body = [{"text": text}]
    response = requests.post(endpoint, params=params, headers=headers, json=body, timeout=10)
    if response.status_code == 200:
        data = response.json()
        if data and "translations" in data[0]:
            item = data[0]
            detected = item.get("detectedLanguage", {}).get("language", source_lang)
            translated = item["translations"][0]["text"]
            return translated, detected, "Microsoft Azure Translator"
    elif response.status_code in (401, 403):
        raise ValueError("Invalid Microsoft Azure Translator subscription key or region.")
    response.raise_for_status()


def translate_with_libretranslate(text: str, source_lang: str, target_lang: str, api_url: str, api_key: str = ""):
    """LibreTranslate REST API."""
    url = api_url.rstrip("/")
    if not url.endswith("/translate"):
        url = f"{url}/translate"

    payload = {
        "q": text,
        "source": "auto" if (not source_lang or source_lang == "auto") else source_lang,
        "target": target_lang,
        "format": "text"
    }
    if api_key:
        payload["api_key"] = api_key

    response = requests.post(url, json=payload, headers={"Content-Type": "application/json"}, timeout=10)
    if response.status_code == 200:
        data = response.json()
        detected = data.get("detectedLanguage", {}).get("language", source_lang)
        return data.get("translatedText", ""), detected, "LibreTranslate API"
    response.raise_for_status()


def translate_with_reliable_free_api(text: str, source_lang: str, target_lang: str):
    """
    High-reliability public translation engine for zero-setup internship evaluation.
    Tries Google Translate public web API first, then MyMemory Translation API.
    """
    detected_lang = source_lang

    # Attempt 1: Google Translate Public Web API
    try:
        sl = "auto" if (not source_lang or source_lang == "auto") else source_lang
        encoded_text = urllib.parse.quote(text)
        gt_url = f"https://translate.googleapis.com/translate_a/single?client=gtx&sl={sl}&tl={target_lang}&dt=t&q={encoded_text}"
        headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
        res = requests.get(gt_url, headers=headers, timeout=8)
        if res.status_code == 200:
            data = res.json()
            if isinstance(data, list) and len(data) > 0 and isinstance(data[0], list):
                translated_parts = [segment[0] for segment in data[0] if segment and len(segment) > 0 and segment[0]]
                translated_text = "".join(translated_parts)
                if len(data) > 2 and isinstance(data[2], str):
                    detected_lang = data[2]
                return translated_text, detected_lang, "Google Translate API (Public Service)"
    except Exception as e:
        logger.warning(f"Google public endpoint fallback note: {e}")

    # Attempt 2: MyMemory Translation API
    try:
        from_lang = source_lang
        if not from_lang or from_lang == "auto":
            from_lang = detect_language_heuristic(text)
            detected_lang = from_lang

        lang_pair = f"{from_lang}|{target_lang}"
        mymemory_url = "https://api.mymemory.translated.net/get"
        params = {"q": text, "langpair": lang_pair}
        res = requests.get(mymemory_url, params=params, timeout=8)
        if res.status_code == 200:
            data = res.json()
            if data.get("responseStatus") == 200:
                translated_text = data.get("responseData", {}).get("translatedText", "")
                if translated_text and not translated_text.startswith("MYMEMORY WARNING"):
                    return translated_text, detected_lang, "MyMemory Translation API"
    except Exception as e:
        logger.warning(f"MyMemory fallback note: {e}")

    raise RuntimeError("All translation service endpoints were temporarily unreachable. Please check your internet connection.")


def perform_translation(text: str, source_lang: str, target_lang: str):
    """
    Orchestrates translation according to configured environment variables,
    falling back seamlessly to provide a 100% working demo for internship review.
    """
    # 1. Google Cloud Translation API if configured
    if TRANSLATION_API_KEY and (TRANSLATION_PROVIDER in ("google", "auto") or not TRANSLATION_API_URL):
        try:
            return translate_with_google_cloud(text, source_lang, target_lang, TRANSLATION_API_KEY)
        except Exception as e:
            logger.error(f"Configured Google Cloud Translation failed: {e}")
            if TRANSLATION_PROVIDER == "google":
                raise e

    # 2. Microsoft Azure Translator if configured
    if TRANSLATION_API_KEY and TRANSLATION_PROVIDER == "azure":
        try:
            return translate_with_microsoft_azure(text, source_lang, target_lang, TRANSLATION_API_KEY, AZURE_TRANSLATOR_REGION)
        except Exception as e:
            logger.error(f"Configured Microsoft Azure Translator failed: {e}")
            raise e

    # 3. Custom / LibreTranslate API if configured
    if TRANSLATION_API_URL:
        try:
            return translate_with_libretranslate(text, source_lang, target_lang, TRANSLATION_API_URL, TRANSLATION_API_KEY)
        except Exception as e:
            logger.error(f"Configured LibreTranslate failed: {e}")
            if TRANSLATION_PROVIDER == "libretranslate":
                raise e

    # 4. Built-in zero-setup translation engine (Google Web API + MyMemory)
    return translate_with_reliable_free_api(text, source_lang, target_lang)


@app.route("/")
def index():
    """Renders the single-page application."""
    return render_template("index.html", languages=SUPPORTED_LANGUAGES)


@app.route("/languages", methods=["GET"])
def get_languages():
    """Returns the JSON list of supported languages."""
    return jsonify({
        "success": True,
        "languages": SUPPORTED_LANGUAGES
    })


@app.route("/translate", methods=["POST"])
def translate():
    """
    REST API Translation Endpoint
    Accepts JSON:
    {
        "text": "Hello world",
        "source_language": "en",  // or "auto"
        "target_language": "ta"
    }
    """
    try:
        data = request.get_json(silent=True)
        if not data:
            return jsonify({
                "success": False,
                "error": "Invalid request. JSON body is required."
            }), 400

        text = data.get("text", "")
        source_lang = data.get("source_language", "auto").strip().lower()
        target_lang = data.get("target_language", "").strip().lower()

        # 1. Validation: Empty input
        if not text or not text.strip():
            return jsonify({
                "success": False,
                "error": "Please enter text to translate."
            }), 400

        text = text.strip()

        # 2. Validation: Length limit
        if len(text) > MAX_CHARACTERS:
            return jsonify({
                "success": False,
                "error": f"Text exceeds maximum limit of {MAX_CHARACTERS:,} characters (current: {len(text):,})."
            }), 400

        # 3. Validation: Target language required
        if not target_lang:
            return jsonify({
                "success": False,
                "error": "Please select a target language."
            }), 400

        if target_lang not in LANGUAGE_CODE_MAP:
            return jsonify({
                "success": False,
                "error": f"Target language '{target_lang}' is currently unsupported."
            }), 400

        # 4. Edge case: source == target
        if source_lang == target_lang and source_lang != "auto":
            return jsonify({
                "success": True,
                "translation": text,
                "detected_language": source_lang,
                "detected_language_name": LANGUAGE_CODE_MAP.get(source_lang, source_lang),
                "source_language": source_lang,
                "target_language": target_lang,
                "provider": "Identity (source matches target)"
            })

        # 5. Perform Translation
        translated_text, detected_code, provider_name = perform_translation(text, source_lang, target_lang)

        detected_name = LANGUAGE_CODE_MAP.get(detected_code, detected_code.title() if detected_code else "Unknown")

        return jsonify({
            "success": True,
            "translation": translated_text,
            "detected_language": detected_code,
            "detected_language_name": detected_name,
            "source_language": source_lang,
            "target_language": target_lang,
            "provider": provider_name
        })

    except ValueError as ve:
        logger.warning(f"Validation / API credential error: {ve}")
        return jsonify({
            "success": False,
            "error": str(ve)
        }), 400
    except requests.exceptions.Timeout:
        logger.error("Translation API timeout")
        return jsonify({
            "success": False,
            "error": "The translation service timed out. Please try again in a few moments."
        }), 504
    except requests.exceptions.ConnectionError:
        logger.error("Translation API connection error")
        return jsonify({
            "success": False,
            "error": "Network connection error. Please verify your internet connection."
        }), 502
    except Exception as ex:
        logger.exception("Unexpected translation error")
        return jsonify({
            "success": False,
            "error": "An error occurred while processing your translation. Please try again."
        }), 500


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint for monitors & deployment platforms."""
    return jsonify({
        "status": "healthy",
        "app": "AI Language Translation Tool",
        "version": "1.0.0",
        "provider_configured": bool(TRANSLATION_API_KEY or TRANSLATION_API_URL)
    })


if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    debug_mode = os.getenv("DEBUG", "True").lower() in ("true", "1", "yes")
    print("\n" + "=" * 60)
    print(" 🚀 AI Language Translation Tool - CodeAlpha Task")
    print(f" 🌐 Running on: http://127.0.0.1:{port}")
    print(f" 🔑 API Key Status: {'Configured' if TRANSLATION_API_KEY else 'Zero-setup Fallback Active'}")
    print("=" * 60 + "\n")
    app.run(host="0.0.0.0", port=port, debug=debug_mode)
