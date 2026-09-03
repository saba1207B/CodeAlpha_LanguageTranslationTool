/**
 * AI Language Translation Tool - Frontend Application Script
 * Developed for CodeAlpha AI Internship Task
 *
 * Handles:
 * - Real-time translation requests via Flask REST API
 * - Automatic source language detection display
 * - Smooth language swapping & text exchange
 * - Clipboard copy with temporary feedback
 * - Character count monitoring & limits
 * - Form validation and friendly error handling
 * - Audio speech synthesis playback (bonus accessibility)
 * - Light/Dark theme persistence
 * - Android WebView bridge integration for emulator preview
 */

document.addEventListener("DOMContentLoaded", () => {
    // DOM Elements
    const sourceSelect = document.getElementById("sourceLanguageSelect");
    const targetSelect = document.getElementById("targetLanguageSelect");
    const sourceInput = document.getElementById("sourceTextInput");
    const targetOutput = document.getElementById("targetTextOutput");
    const emptyPlaceholder = document.getElementById("emptyOutputPlaceholder");
    const charCounter = document.getElementById("charCounter");
    const outputCharCounter = document.getElementById("outputCharCounter");
    const detectedTag = document.getElementById("detectedLanguageTag");
    const detectedName = document.getElementById("detectedLanguageName");
    const providerPill = document.getElementById("providerPill");
    const providerName = document.getElementById("providerName");

    // Buttons
    const translateBtn = document.getElementById("translateBtn");
    const translateBtnText = document.getElementById("translateBtnText");
    const btnSpinner = document.getElementById("btnSpinner");
    const translateIcon = translateBtn.querySelector(".translate-icon");
    const swapBtn = document.getElementById("swapLanguagesBtn");
    const copyBtn = document.getElementById("copyOutputBtn");
    const copyBtnText = document.getElementById("copyBtnText");
    const clearInputBtn = document.getElementById("clearInputBtn");
    const clearAllBtn = document.getElementById("clearAllBtn");
    const speakSourceBtn = document.getElementById("speakSourceBtn");
    const speakTargetBtn = document.getElementById("speakTargetBtn");
    const themeToggleBtn = document.getElementById("themeToggleBtn");
    const loadingOverlay = document.getElementById("loadingOverlay");

    // Notification Banner
    const notificationBox = document.getElementById("notificationBox");
    const notificationMessage = document.getElementById("notificationMessage");
    const notificationCloseBtn = document.getElementById("notificationCloseBtn");

    // Quick chips
    const langChips = document.querySelectorAll(".lang-chip");
    const sampleBtns = document.querySelectorAll(".sample-btn");

    const MAX_CHARS = 5000;
    let isTranslating = false;

    // =========================================================================
    // Theme Management (Light / Dark)
    // =========================================================================
    const savedTheme = localStorage.getItem("codealpha_translator_theme") ||
        (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    document.documentElement.setAttribute("data-theme", savedTheme);

    themeToggleBtn.addEventListener("click", () => {
        const currentTheme = document.documentElement.getAttribute("data-theme");
        const newTheme = currentTheme === "dark" ? "light" : "dark";
        document.documentElement.setAttribute("data-theme", newTheme);
        localStorage.setItem("codealpha_translator_theme", newTheme);
    });

    // =========================================================================
    // Notification Banner Helper
    // =========================================================================
    function showNotification(message, type = "error") {
        notificationBox.className = `notification-container ${type}`;
        notificationMessage.textContent = message;
        notificationBox.style.display = "flex";

        // Auto-dismiss after 6 seconds if success
        if (type === "success") {
            setTimeout(() => {
                hideNotification();
            }, 6000);
        }
    }

    function hideNotification() {
        notificationBox.style.display = "none";
    }

    notificationCloseBtn.addEventListener("click", hideNotification);

    // =========================================================================
    // Character Counter
    // =========================================================================
    function updateCharCount() {
        const count = sourceInput.value.length;
        charCounter.textContent = `${count.toLocaleString()} / ${MAX_CHARS.toLocaleString()}`;

        charCounter.classList.remove("warning", "limit");
        if (count >= MAX_CHARS) {
            charCounter.classList.add("limit");
        } else if (count >= MAX_CHARS * 0.85) {
            charCounter.classList.add("warning");
        }
    }

    sourceInput.addEventListener("input", () => {
        updateCharCount();
        hideNotification();
    });

    // =========================================================================
    // Quick Language Chips
    // =========================================================================
    function syncActiveChip() {
        const currentTarget = targetSelect.value;
        langChips.forEach(chip => {
            if (chip.dataset.lang === currentTarget) {
                chip.classList.add("active");
            } else {
                chip.classList.remove("active");
            }
        });
    }

    langChips.forEach(chip => {
        chip.addEventListener("click", () => {
            const targetLang = chip.dataset.lang;
            // Prevent source and target being identical
            if (sourceSelect.value === targetLang) {
                sourceSelect.value = "auto";
            }
            targetSelect.value = targetLang;
            syncActiveChip();

            // If text is already entered, trigger translation
            if (sourceInput.value.trim().length > 0) {
                handleTranslate();
            }
        });
    });

    targetSelect.addEventListener("change", syncActiveChip);
    syncActiveChip();

    // Sample prompts
    sampleBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            sourceInput.value = btn.dataset.sample;
            updateCharCount();
            handleTranslate();
        });
    });

    // =========================================================================
    // Translation Execution
    // =========================================================================
    async function handleTranslate() {
        const text = sourceInput.value.trim();
        const sourceLang = sourceSelect.value;
        const targetLang = targetSelect.value;

        // 1. Validation: Check empty input
        if (!text) {
            showNotification("Please enter some text in the input area before translating.", "error");
            sourceInput.focus();
            return;
        }

        // 2. Validation: Length limit
        if (text.length > MAX_CHARS) {
            showNotification(`Text cannot exceed ${MAX_CHARS.toLocaleString()} characters.`, "error");
            return;
        }

        if (isTranslating) return;

        setLoading(true);
        hideNotification();

        try {
            let result;

            // Check for native Android WebView bridge if running in streaming emulator
            if (window.AndroidTranslationBridge && typeof window.AndroidTranslationBridge.translate === "function") {
                const responseStr = await new Promise((resolve) => {
                    window.__androidTranslationCallback = (jsonStr) => resolve(jsonStr);
                    window.AndroidTranslationBridge.translate(text, sourceLang, targetLang);
                });
                result = JSON.parse(responseStr);
            } else {
                // Standard Flask Backend REST API Call
                const response = await fetch("/translate", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify({
                        text: text,
                        source_language: sourceLang,
                        target_language: targetLang
                    })
                });

                result = await response.json();
            }

            if (result.success) {
                displayTranslationResult(result);
            } else {
                showNotification(result.error || "Translation failed. Please try again.", "error");
            }
        } catch (error) {
            console.error("Translation error:", error);
            showNotification("Unable to reach the translation server. Please check your internet connection.", "error");
        } finally {
            setLoading(false);
        }
    }

    function displayTranslationResult(data) {
        // Show result textarea and hide empty placeholder
        emptyPlaceholder.style.display = "none";
        targetOutput.style.display = "block";
        targetOutput.value = data.translation;

        // Enable action buttons
        copyBtn.disabled = false;
        speakTargetBtn.disabled = false;

        // Update output count
        outputCharCounter.textContent = `${data.translation.length.toLocaleString()} chars`;

        // Update Auto-Detected Language Tag
        if (data.detected_language && sourceSelect.value === "auto") {
            detectedName.textContent = data.detected_language_name || data.detected_language;
            detectedTag.style.display = "inline-flex";
        } else {
            detectedTag.style.display = "none";
        }

        // Update Provider Pill
        if (data.provider) {
            providerName.textContent = data.provider;
            providerPill.style.display = "flex";
        }
    }

    function setLoading(isLoading) {
        isTranslating = isLoading;
        if (isLoading) {
            loadingOverlay.style.display = "flex";
            btnSpinner.style.display = "inline-block";
            translateIcon.style.display = "none";
            translateBtnText.textContent = "Translating...";
            translateBtn.disabled = true;
        } else {
            loadingOverlay.style.display = "none";
            btnSpinner.style.display = "none";
            translateIcon.style.display = "inline-block";
            translateBtnText.textContent = "Translate";
            translateBtn.disabled = false;
        }
    }

    translateBtn.addEventListener("click", handleTranslate);

    // =========================================================================
    // Keyboard Shortcut: Ctrl+Enter / Cmd+Enter to Translate
    // =========================================================================
    sourceInput.addEventListener("keydown", (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
            e.preventDefault();
            handleTranslate();
        }
    });

    // =========================================================================
    // Swap Languages (⇄)
    // =========================================================================
    swapBtn.addEventListener("click", () => {
        let currentSource = sourceSelect.value;
        let currentTarget = targetSelect.value;

        // Animate swap button icon rotation
        swapBtn.style.transform = "rotate(180deg) scale(1.1)";
        setTimeout(() => {
            swapBtn.style.transform = "";
        }, 300);

        // If source was "auto", swap target with detected language or fallback
        if (currentSource === "auto") {
            const detectedCode = detectedTag.style.display !== "none" ?
                getLanguageCodeByName(detectedName.textContent) : "en";
            currentSource = detectedCode || "en";
        }

        // Swap select values
        sourceSelect.value = currentTarget;
        targetSelect.value = currentSource;

        // Exchange text if output exists
        const currentInputText = sourceInput.value;
        const currentOutputText = targetOutput.value;

        if (currentOutputText && currentOutputText.trim()) {
            sourceInput.value = currentOutputText;
            targetOutput.value = currentInputText;
            updateCharCount();
            outputCharCounter.textContent = `${currentInputText.length.toLocaleString()} chars`;
        }

        // Reset detected badge
        detectedTag.style.display = "none";
        syncActiveChip();
        hideNotification();
    });

    function getLanguageCodeByName(name) {
        for (let i = 0; i < sourceSelect.options.length; i++) {
            const opt = sourceSelect.options[i];
            if (opt.text.toLowerCase().includes(name.toLowerCase())) {
                return opt.value;
            }
        }
        return "en";
    }

    // =========================================================================
    // Copy Translated Output with Temporary Feedback
    // =========================================================================
    copyBtn.addEventListener("click", async () => {
        const textToCopy = targetOutput.value;
        if (!textToCopy) return;

        try {
            await navigator.clipboard.writeText(textToCopy);
            showCopiedFeedback();
        } catch (err) {
            // Fallback for older browsers
            targetOutput.select();
            document.execCommand("copy");
            showCopiedFeedback();
        }
    });

    function showCopiedFeedback() {
        copyBtn.classList.add("copied");
        copyBtnText.textContent = "Copied!";

        setTimeout(() => {
            copyBtn.classList.remove("copied");
            copyBtnText.textContent = "Copy";
        }, 2200);
    }

    // =========================================================================
    // Clear Input / Clear All
    // =========================================================================
    function clearAll() {
        sourceInput.value = "";
        targetOutput.value = "";
        targetOutput.style.display = "none";
        emptyPlaceholder.style.display = "flex";
        detectedTag.style.display = "none";
        providerPill.style.display = "none";
        outputCharCounter.textContent = "";
        copyBtn.disabled = true;
        speakTargetBtn.disabled = true;
        updateCharCount();
        hideNotification();
        sourceInput.focus();
    }

    clearInputBtn.addEventListener("click", () => {
        sourceInput.value = "";
        updateCharCount();
        sourceInput.focus();
    });

    clearAllBtn.addEventListener("click", clearAll);

    // =========================================================================
    // Speech Synthesis (Audio Readout Accessibility)
    // =========================================================================
    if ("speechSynthesis" in window) {
        speakSourceBtn.addEventListener("click", () => {
            const text = sourceInput.value.trim();
            if (!text) return;
            speakText(text, sourceSelect.value);
        });

        speakTargetBtn.addEventListener("click", () => {
            const text = targetOutput.value.trim();
            if (!text) return;
            speakText(text, targetSelect.value);
        });
    } else {
        speakSourceBtn.style.display = "none";
        speakTargetBtn.style.display = "none";
    }

    function speakText(text, langCode) {
        window.speechSynthesis.cancel(); // Stop ongoing speech
        const utterance = new SpeechSynthesisUtterance(text);
        if (langCode && langCode !== "auto") {
            utterance.lang = langCode;
        }
        window.speechSynthesis.speak(utterance);
    }

    // Check backend health on initial load
    fetch("/health")
        .then(res => res.json())
        .then(data => {
            const badge = document.getElementById("apiStatusBadge");
            if (data && data.status === "healthy") {
                badge.textContent = data.provider_configured ? "API Configured" : "Service Ready";
            }
        })
        .catch(() => {
            // Quietly fallback for offline/isolated mode
        });
});
