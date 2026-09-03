# AI Language Translation Tool

An intelligent, production-ready web application built for the **CodeAlpha Artificial Intelligence Internship** program. The application translates text seamlessly across 15+ global and Indian languages using modern REST backend architecture, secure environment configuration, and a sleek, responsive interface.

---

## 📌 CodeAlpha Internship Task Reference

- **Domain:** Artificial Intelligence / Machine Learning
- **Project Name:** AI Language Translation Tool
- **Repository Name:** `CodeAlpha_LanguageTranslationTool`
- **Task Description:** Develop a functional, clean, professional, responsive, and easy-to-run web-based language translation application using Python Flask, HTML5, CSS3, JavaScript, and a real translation API service.

---

## 🌟 Key Features

1. **Multi-Language Translation:** Translate between 15+ languages including major Indian languages (Tamil, Hindi, Malayalam, Telugu, Kannada) and international languages (French, German, Spanish, Japanese, Italian, Russian, Chinese, Arabic, Korean, Portuguese).
2. **Automated Language Detection:** Intelligent "✨ Detect Language" option automatically recognizes input script and returns the detected language identity.
3. **Bi-directional Language Swapping:** Dedicated swap button (`⇄`) immediately exchanges source and target languages while dynamically updating text fields without a page reload.
4. **Live Character Counter:** Displays character limits (`0 / 5,000`) with visual warnings when approaching the limit.
5. **One-Click Clipboard Copy:** Copies translated output to clipboard with temporary visual feedback ("Copied!").
6. **Audio Speech Synthesis (TTS):** Integrated text-to-speech button allows users to listen to both source and translated text.
7. **Comprehensive Error Handling:** Gracefully handles empty inputs, network timeouts, invalid API keys, rate limits, and server issues with friendly banners.
8. **Dark / Light Theme:** Modern theme switch with persistent user preference in local storage.
9. **Responsive Mobile-First UI:** Tailored layout for desktop monitors, tablets, and smartphones.
10. **Zero-Leak Security Architecture:** Strictly isolates translation API credentials inside the Python Flask server; no secrets are ever exposed in client-side JavaScript.
11. **Keyboard Productivity Shortcut:** Press <kbd>Ctrl</kbd> + <kbd>Enter</kbd> (or <kbd>Cmd</kbd> + <kbd>Enter</kbd>) to translate instantly.

---

## 🛠️ Technologies Used

### Frontend
- **HTML5:** Semantic markup, accessible labels, and responsive meta tags.
- **CSS3:** Custom properties (CSS variables), CSS Grid, Flexbox, responsive media queries, and smooth animations.
- **JavaScript (ES6+):** Asynchronous `fetch` API, clipboard integration, DOM manipulation, and Web Speech API.

### Backend
- **Python 3.8+:** Robust server-side programming.
- **Flask (v2.3+):** Lightweight, performant micro-framework for serving the web app and REST API.
- **Requests:** HTTP library for communicating with external translation endpoints.
- **Python-dotenv:** Secure environment variable loader from `.env`.

---

## 🏛️ Application Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Browser                         │
│  [HTML5 / CSS3 / JavaScript Interface]                      │
│   • Textarea input with live character counter (5,000 max)   │
│   • Language selectors & quick popular language chips       │
│   • Swap, Copy, Listen, Clear controls                      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                      POST /translate (JSON)
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                  Flask Backend (app.py)                     │
│   • Input validation (empty check, length limit)            │
│   • API key & configuration management via .env             │
│   • Language heuristic & script detection                   │
│   • Error shielding (prevents leaking internal traces)      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                 Secure REST Call (API Key Hidden)
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                   Translation API Provider                  │
│   • Google Cloud Translation API v2 (Recommended)           │
│   • Microsoft Azure Translator API                          │
│   • LibreTranslate / Custom API                             │
│   • High-Reliability Public Demo Fallback                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🌐 Supported Languages

The application is architected so new languages can be added simply by appending a code pair to `SUPPORTED_LANGUAGES` in `app.py`:

| Language | ISO Code | Native Name | Region / Category |
|---|---|---|---|
| **English** | `en` | English | Global |
| **Tamil** | `ta` | தமிழ் | Indian (Dravidian) |
| **Hindi** | `hi` | हिन्दी | Indian (Indo-Aryan) |
| **Malayalam** | `ml` | മലയാളം | Indian (Dravidian) |
| **Telugu** | `te` | తెలుగు | Indian (Dravidian) |
| **Kannada** | `kn` | ಕನ್ನಡ | Indian (Dravidian) |
| **French** | `fr` | Français | European |
| **German** | `de` | Deutsch | European |
| **Spanish** | `es` | Español | European / Latin America |
| **Japanese** | `ja` | 日本語 | East Asian |
| **Italian** | `it` | Italiano | European |
| **Russian** | `ru` | Русский | Eurasian |
| **Chinese (Simplified)** | `zh` | 简体中文 | East Asian |
| **Arabic** | `ar` | العربية | Middle East |
| **Korean** | `ko` | 한국어 | East Asian |
| **Portuguese** | `pt` | Português | European / South America |

---

## 🔌 Translation API Configuration

The project supports official enterprise translation APIs while also including an automatic fallback engine so evaluators can run and test the project without paying for an API subscription.

### Option 1: Google Cloud Translation API (Recommended)
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project and enable the **Cloud Translation API**.
3. Generate an API Key in **APIs & Services** > **Credentials**.
4. Set in your `.env`:
   ```env
   TRANSLATION_PROVIDER=google
   TRANSLATION_API_KEY=AIzaSy...your_google_cloud_api_key...
   ```

### Option 2: Microsoft Azure Translator
1. Create an Azure Translator resource in the [Azure Portal](https://portal.azure.com/).
2. Copy your Key and Region.
3. Set in your `.env`:
   ```env
   TRANSLATION_PROVIDER=azure
   TRANSLATION_API_KEY=your_azure_subscription_key
   AZURE_TRANSLATOR_REGION=eastus
   ```

### Option 3: LibreTranslate or Self-Hosted Instance
1. Set the URL of your LibreTranslate instance in `.env`:
   ```env
   TRANSLATION_PROVIDER=libretranslate
   TRANSLATION_API_URL=https://libretranslate.com/translate
   TRANSLATION_API_KEY=optional_key_if_required
   ```

### Option 4: Zero-Setup Evaluation Mode
If `TRANSLATION_API_KEY` is left blank in `.env`, the tool automatically routes through a high-reliability public translation engine (Google Translate Web API & MyMemory). This ensures the evaluator can immediately verify the UI, Tamil, Hindi, French, Spanish, Japanese, and language swapping out of the box!

---

## 📁 Project Structure

```
CodeAlpha_LanguageTranslationTool/
│
├── app.py                  # Main Flask application and translation routes
├── requirements.txt        # Python package dependencies
├── .env.example            # Template for environment variables
├── .env                    # Real credentials (gitignored, never committed)
├── .gitignore              # Git ignore rules for Python, virtualenv & secrets
├── README.md               # Comprehensive project documentation
│
├── templates/
│   └── index.html          # Semantic HTML5 single-page application
│
└── static/
    ├── style.css           # Modern CSS styling with dark/light themes
    └── script.js           # Responsive client-side logic and event listeners
```

---

## 💻 Windows Setup Instructions

Follow these exact steps to run the application on Windows:

### 1. Clone or Extract the Repository
Open PowerShell or Command Prompt:
```cmd
git clone https://github.com/your-username/CodeAlpha_LanguageTranslationTool.git
cd CodeAlpha_LanguageTranslationTool
```

### 2. Create a Python Virtual Environment
```cmd
python -m venv .venv
```

### 3. Activate the Virtual Environment
**PowerShell:**
```powershell
.venv\Scripts\Activate.ps1
```
*(If you receive an ExecutionPolicy message, run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` and rerun the activate command).*

**Command Prompt (cmd):**
```cmd
.venv\Scripts\activate.bat
```

### 4. Install Dependencies
```cmd
pip install -r requirements.txt
```

### 5. Configure Environment Variables
Copy `.env.example` to `.env`:
```cmd
copy .env.example .env
```
Open `.env` in Notepad or VS Code and add your API credentials if available (or leave blank to use the built-in testing engine).

### 6. Start the Application
```cmd
python app.py
```

### 7. Open the App in Your Browser
Open your browser and navigate to:
```
http://127.0.0.1:5000
```

---

## 🐧 macOS / Linux Setup Instructions

```bash
# 1. Create and activate virtual environment
python3 -m venv .venv
source .venv/bin/activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Configure .env
cp .env.example .env

# 4. Run application
python3 app.py
```

---

## 📖 How to Use the Application

1. **Enter Text:** Type or paste your text in the left input card (e.g., `"Hello, welcome to our AI project"`).
2. **Select Languages:**
   - Keep source on `"✨ Detect Language"` or pick a specific language.
   - Choose your desired target language (e.g., `"Tamil (தமிழ்)"` or `"Spanish"`).
3. **Translate:** Click the **Translate** button or press <kbd>Ctrl</kbd> + <kbd>Enter</kbd>.
4. **Copy Output:** Click **Copy** to copy the translated result with visual confirmation.
5. **Swap:** Click the **⇄** swap button to quickly reverse translation direction.
6. **Audio Readout:** Click **Listen** to hear pronunciation via speech synthesis.
7. **Clear:** Click **Clear All** to reset the input and output.

---

## 🛡️ Security Considerations

- **Server-Side API Key Storage:** All API keys reside strictly inside `.env` on the backend server and are read into memory via `os.getenv()`.
- **Zero Frontend Secret Exposure:** The browser only interacts with the `/translate` Flask endpoint. The translation API key is never rendered in HTML or accessible in JavaScript.
- **Git Protection:** `.env` is explicitly included in `.gitignore` to prevent leaking private credentials to public GitHub repositories.
- **Input Sanitization & Length Bounds:** Text input is capped at 5,000 characters and validated against null/empty payloads to prevent denial-of-service abuse.
- **Provider Exception Shielding:** Upstream error details from third-party APIs are logged internally; clients only receive user-friendly error messages.

---

## 📸 Screenshots Section

*(Add screenshots here after running the application)*

| Desktop Light View | Desktop Dark View |
|:---:|:---:|
| ![Desktop Light](https://via.placeholder.com/600x380?text=Desktop+Light+View) | ![Desktop Dark](https://via.placeholder.com/600x380?text=Desktop+Dark+View) |

| Mobile View | Language Swapping & Copy |
|:---:|:---:|
| ![Mobile View](https://via.placeholder.com/300x550?text=Mobile+View) | ![Action Feedback](https://via.placeholder.com/300x550?text=Copied+Feedback) |

---

## 🚀 Future Improvements

- [ ] Add document translation for `.txt`, `.docx`, and `.pdf` files.
- [ ] Implement translation history using SQLite or IndexedDB.
- [ ] Integrate voice input with Web Speech Recognition for speech-to-text.
- [ ] Add real-time side-by-side dictionary and synonym exploration.

---

## 👤 Author

- **Intern:** CodeAlpha AI Intern
- **Task:** AI Language Translation Tool
- **Track:** Artificial Intelligence Internship
- **Organization:** CodeAlpha

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
