"""
Unit tests for AI Language Translation Tool (Flask Backend)
Run with: python -m unittest test_app.py
"""

import unittest
import json
from app import app, SUPPORTED_LANGUAGES, MAX_CHAR_LIMIT


class TranslationAppTestCase(unittest.TestCase):
    def setUp(self):
        app.config["TESTING"] = True
        self.client = app.test_client()

    def test_home_page(self):
        """Test that the homepage renders successfully."""
        response = self.client.get("/")
        self.assertEqual(response.status_code, 200)
        self.assertIn(b"AI Language Translator", response.data)

    def test_languages_endpoint(self):
        """Test that the /api/languages endpoint returns required languages."""
        response = self.client.get("/api/languages")
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data["success"])
        langs = data["languages"]

        # Ensure all core required languages are present
        required_langs = ["en", "ta", "hi", "ml", "te", "kn", "fr", "de", "es", "ja"]
        for lang_code in required_langs:
            self.assertIn(lang_code, langs)

    def test_translate_empty_text(self):
        """Test that translation with empty input returns 400."""
        payload = {"text": "   ", "source_language": "en", "target_language": "ta"}
        response = self.client.post(
            "/translate",
            data=json.dumps(payload),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 400)
        data = response.get_json()
        self.assertFalse(data["success"])
        self.assertIn("enter text", data["error"].lower())

    def test_translate_character_limit_exceeded(self):
        """Test that exceeding maximum character limit returns 400."""
        payload = {
            "text": "a" * (MAX_CHAR_LIMIT + 10),
            "source_language": "en",
            "target_language": "ta",
        }
        response = self.client.post(
            "/translate",
            data=json.dumps(payload),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 400)
        data = response.get_json()
        self.assertFalse(data["success"])
        self.assertIn("limit exceeded", data["error"].lower())

    def test_translate_unsupported_target(self):
        """Test that unsupported target language returns 400."""
        payload = {"text": "Hello", "source_language": "en", "target_language": "klingon"}
        response = self.client.post(
            "/translate",
            data=json.dumps(payload),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 400)
        data = response.get_json()
        self.assertFalse(data["success"])

    def test_translate_same_source_and_target(self):
        """Test that identical source and target returns identical text."""
        payload = {"text": "Bonjour le monde", "source_language": "fr", "target_language": "fr"}
        response = self.client.post(
            "/translate",
            data=json.dumps(payload),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data["success"])
        self.assertEqual(data["translation"], "Bonjour le monde")


if __name__ == "__main__":
    unittest.main()
