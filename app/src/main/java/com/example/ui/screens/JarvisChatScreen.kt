package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.download.AiModelDownloadManager
import com.example.ai.download.ModelDownloadStatus
import com.example.ai.engine.ChatMessage
import com.example.ai.engine.JarvisAiEngine
import com.example.service.JarvisFloatingService
import com.example.ui.components.JarvisActionConfirmCard
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisChatScreen(
    aiEngine: JarvisAiEngine,
    downloadManager: AiModelDownloadManager,
    onDataUpdated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showModelSettings by remember { mutableStateOf(false) }

    val downloadStatus by downloadManager.downloadStatus.collectAsStateWithLifecycle()
    var isFloatingActive by remember { mutableStateOf(JarvisFloatingService.isFloatingServiceEnabled(context)) }

    // Speech-to-text launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
            }
        }
    }

    // Permission overlay launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = Settings.canDrawOverlays(context)
        isFloatingActive = granted
        JarvisFloatingService.setFloatingServiceEnabled(context, granted)
    }

    // Conversation history
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = "Halo Bunda! Saya Cici, asisten pintar offline belanja keluarga ArAr TOZ 🌸✨\n\nBunda bisa minta Cici mencatat kas, belanja dapur, bayar tagihan, atau cek saldo. Semuanya diproses 100% lokal di HP!",
                actionExecutionDetails = null
            )
        )
    }

    val sendMessage: (String) -> Unit = { query ->
        val text = query.trim()
        if (text.isNotEmpty() && !isProcessing) {
            messages.add(ChatMessage(isUser = true, text = text))
            inputText = ""
            isProcessing = true

            coroutineScope.launch {
                try {
                    val response = aiEngine.processUserCommand(text)
                    messages.add(
                        ChatMessage(
                            isUser = false,
                            text = response.reply,
                            actionType = response.action,
                            actionExecutionDetails = response.executionDetails,
                            isSuccess = response.executedSuccessfully
                        )
                    )
                    onDataUpdated()
                } catch (e: Exception) {
                    messages.add(
                        ChatMessage(
                            isUser = false,
                            text = "Maaf Bunda, terjadi kendala saat memproses: ${e.message}",
                            isSuccess = false
                        )
                    )
                } finally {
                    isProcessing = false
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("jarvis_chat_screen")
    ) {
        // 1. Status Bar Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌸", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Asisten Si Cici",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            val isReady = downloadStatus is ModelDownloadStatus.Ready
                            Text(
                                text = if (isReady) "🟢 Gemma LLM Lokal Aktif" else "⚡ NLU Cepat Siap Pakai",
                                fontSize = 11.sp,
                                color = if (isReady) GreenBudgetSafe else EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showModelSettings = !showModelSettings },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Pengaturan Model AI",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quick Floating Service Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tombol Melayang di Luar Aplikasi:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isFloatingActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                    Toast.makeText(context, "Izinkan ArAr TOZ tampil di atas aplikasi lain", Toast.LENGTH_LONG).show()
                                } else {
                                    isFloatingActive = true
                                    JarvisFloatingService.setFloatingServiceEnabled(context, true)
                                }
                            } else {
                                isFloatingActive = false
                                JarvisFloatingService.setFloatingServiceEnabled(context, false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("toggle_floating_service")
                    )
                }

                // Collapsible Model Manager Details
                AnimatedVisibility(visible = showModelSettings) {
                    ModelManagementSection(
                        downloadManager = downloadManager,
                        downloadStatus = downloadStatus,
                        aiEngine = aiEngine
                    )
                }
            }
        }

        // 2. Chat Dialogue Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    // User bubble (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp),
                            color = EmeraldPrimary,
                            contentColor = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.padding(start = 48.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    // Assistant bubble (Left)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Top)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌸", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            // Show Action execution card if present
                            if (msg.actionType != null && msg.actionExecutionDetails != null) {
                                JarvisActionConfirmCard(
                                    actionType = msg.actionType,
                                    details = msg.actionExecutionDetails,
                                    isSuccess = msg.isSuccess
                                )
                            }
                        }
                    }
                }
            }

            if (isProcessing) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Si Cici sedang memproses perintah offline...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Quick Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "🥬 Catat beli telur 28rb di pasar",
                "☕ Jajan kopi susu 22000 tunai",
                "⚡ Bayar tagihan listrik",
                "🎯 Tambah target mesin cuci 2jt ke wishlist",
                "📊 Ringkasan kas hari ini"
            ).forEach { prompt ->
                Surface(
                    onClick = { sendMessage(prompt) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.testTag("quick_chip_$prompt")
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 4. Input Row (Microphone + TextField + Send Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan bicara perintah belanja kas...")
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Perangkat tidak mendukung suara Google Speech", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .testTag("voice_input_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Perintah Suara",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("jarvis_input_field"),
                placeholder = { Text("Tanya Cici atau ketik perintah kas...", fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                singleLine = false
            )

            IconButton(
                onClick = { sendMessage(inputText) },
                enabled = inputText.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (inputText.isNotBlank()) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                    .testTag("jarvis_send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ModelManagementSection(
    downloadManager: AiModelDownloadManager,
    downloadStatus: ModelDownloadStatus,
    aiEngine: JarvisAiEngine
) {
    val coroutineScope = rememberCoroutineScope()
    var customPathInput by remember { mutableStateOf(downloadManager.getCustomPath() ?: "") }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pengelola Bobot Model Gemma On-Device (0-MB APK)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            when (val status = downloadStatus) {
                is ModelDownloadStatus.Ready -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenBudgetSafe.copy(alpha = 0.15f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "✅ Model Terpasang & Siap",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GreenBudgetSafe
                            )
                            Text(
                                text = "File: ${status.file.name} (${status.file.length() / (1024 * 1024)} MB)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Path: ${status.file.absolutePath}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val ok = aiEngine.initializeLlm()
                                    if (ok) {
                                        // success
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Muat Ulang Engine", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { downloadManager.deleteDownloadedModel() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus", fontSize = 11.sp)
                        }
                    }
                }

                is ModelDownloadStatus.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mengunduh gemma-2b-it...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${status.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        LinearProgressIndicator(
                            progress = { status.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EmeraldPrimary,
                            strokeCap = StrokeCap.Round
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${status.bytesDownloaded / (1024 * 1024)} MB ${if (status.totalBytes > 0) "/ ${status.totalBytes / (1024 * 1024)} MB" else ""}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { downloadManager.cancelDownload() },
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Batal", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                is ModelDownloadStatus.NotInstalled, is ModelDownloadStatus.Error -> {
                    if (status is ModelDownloadStatus.Error) {
                        Text(
                            text = "Gagal: ${status.message}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Model belum diunduh. Aplikasi tetap berjalan sempurna dengan mesin NLU offline bawaan.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            downloadManager.startDownload(coroutineScope)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("start_download_model_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unduh Model Gemma Mandiri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ADB push tip & custom path input
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🛠️ Pengujian via ADB Push:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "adb push gemma-2b-it.bin /data/local/tmp/\nJarvis akan otomatis mendeteksi file di direktori tersebut.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customPathInput,
                        onValueChange = { customPathInput = it },
                        label = { Text("Path Custom (Opsional)", fontSize = 10.sp) },
                        placeholder = { Text("/data/local/tmp/gemma-2b-it.bin", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            downloadManager.setCustomPath(customPathInput.ifBlank { null })
                            coroutineScope.launch {
                                aiEngine.initializeLlm()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        Text("Simpan & Deteksi Path", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
