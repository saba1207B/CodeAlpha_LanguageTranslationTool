package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LanguageDownloadManager
import com.example.data.OnlineTranslationService
import com.example.model.LanguageCatalog
import com.example.model.LanguageItem
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TranslationAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val downloadManager = remember { LanguageDownloadManager(context) }
    val translationService = remember { OnlineTranslationService(downloadManager) }

    // State variables
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var sourceLangCode by remember { mutableStateOf("auto") }
    var targetLangCode by remember { mutableStateOf("ta") }
    var isLoading by remember { mutableStateOf(false) }
    var detectedLanguageText by remember { mutableStateOf<String?>(null) }
    var activeProviderText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Connectivity state
    var isConnected by remember { mutableStateOf(true) }

    // Downloaded language set
    var downloadedCodes by remember { mutableStateOf(downloadManager.getDownloadedLanguageCodes()) }

    // Language Download Dialog state
    var showDownloadDialog by remember { mutableStateOf(false) }
    val downloadProgressMap = remember { mutableStateMapOf<String, Float>() }

    val maxChars = 5000

    // Refresh connectivity on launch
    LaunchedEffect(Unit) {
        isConnected = downloadManager.isConnectedToInternet()
    }

    fun refreshConnectivity() {
        coroutineScope.launch {
            isConnected = downloadManager.isConnectedToInternet()
            val status = if (isConnected) "Connected to Internet" else "Offline Mode"
            snackbarHostState.showSnackbar(status)
        }
    }

    // Trigger translation
    fun executeTranslation() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Please enter text to translate."
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please enter text to translate.")
            }
            return
        }

        if (trimmed.length > maxChars) {
            errorMessage = "Text exceeds the 5,000 character limit."
            return
        }

        errorMessage = null
        isLoading = true

        coroutineScope.launch {
            val result = translationService.translate(
                text = trimmed,
                sourceCode = sourceLangCode,
                targetCode = targetLangCode
            )

            result.onSuccess { data ->
                outputText = data.translatedText
                activeProviderText = data.provider
                if (data.detectedSourceLanguage != null) {
                    val langObj = LanguageCatalog.findByCode(data.detectedSourceLanguage)
                    val langName = langObj?.name ?: data.detectedSourceLanguage.uppercase()
                    detectedLanguageText = "Detected: $langName"
                } else {
                    detectedLanguageText = null
                }
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Translation failed. Check internet connection."
                snackbarHostState.showSnackbar(errorMessage ?: "Translation failed.")
            }

            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Language Translator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Connect to internet & download 100+ languages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Internet Status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isConnected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                        .clickable { refreshConnectivity() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = if (isConnected) "Online" else "Offline",
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isConnected) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent "Download Languages from Internet" Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDownloadDialog = true }
                    .testTag("download_languages_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download Languages Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Languages from Internet",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${downloadedCodes.size} Downloaded • 100+ Global Languages Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = { showDownloadDialog = true },
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_download_center_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Browse", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Translation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translation_main_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Source Language Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LanguageDropdown(
                            label = "From",
                            selectedCode = sourceLangCode,
                            includeAuto = true,
                            downloadedCodes = downloadedCodes,
                            onLanguageSelected = {
                                sourceLangCode = it
                                detectedLanguageText = null
                            },
                            onOpenDownloadManager = { showDownloadDialog = true }
                        )

                        if (detectedLanguageText != null) {
                            Text(
                                text = detectedLanguageText!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Text Area
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            if (it.length <= maxChars) {
                                inputText = it
                                errorMessage = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("input_text_field"),
                        placeholder = { Text("Type or paste text to translate...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Character Counter & Clear Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${inputText.length} / $maxChars",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (inputText.length > maxChars * 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )

                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    inputText = ""
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("clear_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Input",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Central Swap Languages Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val oldSource = sourceLangCode
                                val oldTarget = targetLangCode
                                val newTarget = if (oldSource == "auto") "en" else oldSource
                                sourceLangCode = oldTarget
                                targetLangCode = newTarget

                                if (outputText.isNotEmpty()) {
                                    val oldOut = outputText
                                    outputText = inputText
                                    inputText = oldOut
                                }
                                Toast.makeText(context, "Languages swapped", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("swap_languages_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Languages",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Target Language Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LanguageDropdown(
                            label = "To",
                            selectedCode = targetLangCode,
                            includeAuto = false,
                            downloadedCodes = downloadedCodes,
                            onLanguageSelected = {
                                targetLangCode = it
                                if (inputText.isNotEmpty()) {
                                    executeTranslation()
                                }
                            },
                            onOpenDownloadManager = { showDownloadDialog = true }
                        )

                        Text(
                            text = if (downloadedCodes.contains(targetLangCode)) "✓ Offline Ready" else "Online API",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (downloadedCodes.contains(targetLangCode)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Output Text Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .testTag("output_container")
                    ) {
                        if (isLoading) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Translating with AI...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            Text(
                                text = if (outputText.isEmpty()) "Translation will appear here..." else outputText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (outputText.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("output_text")
                            )
                        }
                    }

                    // Output Controls: Copy Button & Provider Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeProviderText != null) {
                            Text(
                                text = "Via: $activeProviderText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                if (outputText.isNotEmpty()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Translated Text", outputText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No translation to copy", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Icon",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            // Error Display if any
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Clear All & Translate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        inputText = ""
                        outputText = ""
                        errorMessage = null
                        detectedLanguageText = null
                        activeProviderText = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All Icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear")
                }

                Button(
                    onClick = { executeTranslation() },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("translate_button"),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isLoading) "Translating..." else "Translate")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Target Language Chips (Uses downloaded + popular languages)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Select Target:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+ Download More",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showDownloadDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val availableForQuickChips = remember(downloadedCodes) {
                    LanguageCatalog.ALL_LANGUAGES.filter { downloadedCodes.contains(it.code) }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableForQuickChips) { lang ->
                        val isSelected = targetLangCode == lang.code
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    targetLangCode = lang.code
                                    if (inputText.isNotEmpty()) {
                                        executeTranslation()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "100+ Internet Languages Ready • CodeAlpha AI Internship",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    // ==========================================
    // Language Download Manager Dialog (Full Modal)
    // ==========================================
    if (showDownloadDialog) {
        LanguageDownloadCenterDialog(
            downloadManager = downloadManager,
            downloadedCodes = downloadedCodes,
            downloadProgressMap = downloadProgressMap,
            onDismiss = { showDownloadDialog = false },
            onLanguageDownloaded = { code ->
                downloadedCodes = downloadManager.getDownloadedLanguageCodes()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Downloaded language pack: ${LanguageCatalog.findByCode(code)?.name ?: code}")
                }
            },
            onLanguageRemoved = { code ->
                downloadedCodes = downloadManager.getDownloadedLanguageCodes()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Removed ${LanguageCatalog.findByCode(code)?.name ?: code} pack")
                }
            }
        )
    }
}

/**
 * Dropdown selector showing downloaded languages and an option to download more.
 */
@Composable
fun LanguageDropdown(
    label: String,
    selectedCode: String,
    includeAuto: Boolean,
    downloadedCodes: Set<String>,
    onLanguageSelected: (String) -> Unit,
    onOpenDownloadManager: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val currentDisplayName = if (selectedCode == "auto") {
        "Detect language"
    } else {
        LanguageCatalog.findByCode(selectedCode)?.displayName ?: selectedCode
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = currentDisplayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (includeAuto) {
                DropdownMenuItem(
                    text = { Text("✨ Detect language automatically") },
                    onClick = {
                        onLanguageSelected("auto")
                        expanded = false
                    }
                )
                HorizontalDivider()
            }

            val downloadedLanguages = LanguageCatalog.ALL_LANGUAGES.filter { downloadedCodes.contains(it.code) }
            downloadedLanguages.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.displayName)
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        onLanguageSelected(lang.code)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download More",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download more from Internet (100+)...",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onOpenDownloadManager()
                }
            )
        }
    }
}

/**
 * Full Download Center Dialog where users can search from 100+ languages
 * and download any language pack from the internet to their device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDownloadCenterDialog(
    downloadManager: LanguageDownloadManager,
    downloadedCodes: Set<String>,
    downloadProgressMap: MutableMap<String, Float>,
    onDismiss: () -> Unit,
    onLanguageDownloaded: (String) -> Unit,
    onLanguageRemoved: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "downloaded", "available"

    val allLanguages = LanguageCatalog.ALL_LANGUAGES

    val filteredList = remember(searchQuery, selectedFilter, downloadedCodes) {
        allLanguages.filter { lang ->
            val matchesSearch = lang.name.contains(searchQuery, ignoreCase = true) ||
                    lang.nativeName.contains(searchQuery, ignoreCase = true) ||
                    lang.code.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "downloaded" -> downloadedCodes.contains(lang.code)
                "available" -> !downloadedCodes.contains(lang.code)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("download_center_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download Center",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Language Download Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Download any language from internet for offline use",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close Dialog"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_search_input"),
                    placeholder = { Text("Search 100+ languages (e.g. Russian, Arabic, Japanese)...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("All (${allLanguages.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "downloaded",
                        onClick = { selectedFilter = "downloaded" },
                        label = { Text("Downloaded (${downloadedCodes.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "available",
                        onClick = { selectedFilter = "available" },
                        label = { Text("Available (${allLanguages.size - downloadedCodes.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                // Languages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredList, key = { it.code }) { lang ->
                        val isDownloaded = downloadedCodes.contains(lang.code)
                        val currentProgress = downloadProgressMap[lang.code]

                        LanguageItemRow(
                            language = lang,
                            isDownloaded = isDownloaded,
                            downloadProgress = currentProgress,
                            onDownloadClick = {
                                downloadProgressMap[lang.code] = 0.05f
                                coroutineScope.launch {
                                    val success = downloadManager.downloadLanguagePack(lang.code) { progress ->
                                        downloadProgressMap[lang.code] = progress
                                    }
                                    downloadProgressMap.remove(lang.code)
                                    if (success) {
                                        onLanguageDownloaded(lang.code)
                                    }
                                }
                            },
                            onRemoveClick = {
                                downloadManager.removeLanguagePack(lang.code)
                                onLanguageRemoved(lang.code)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // Dialog Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

/**
 * Individual row in the language catalog list.
 */
@Composable
fun LanguageItemRow(
    language: LanguageItem,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onDownloadClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Language Avatar & Names
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDownloaded) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.code.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${language.nativeName} • ${language.packSizeMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (downloadProgress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Action Status / Button
        if (downloadProgress != null) {
            Text(
                text = "${(downloadProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else if (isDownloaded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (language.code != "en") {
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Language Pack",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Language",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Retained for test compatibility with GreetingScreenshotTest
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
