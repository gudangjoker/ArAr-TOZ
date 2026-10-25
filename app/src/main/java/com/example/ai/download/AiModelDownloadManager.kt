package com.example.ai.download

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class ModelDownloadStatus {
    object NotInstalled : ModelDownloadStatus()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ModelDownloadStatus()
    data class Ready(val file: File, val isExternalPath: Boolean = false) : ModelDownloadStatus()
    data class Error(val message: String) : ModelDownloadStatus()
}

class AiModelDownloadManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)

    private val _downloadStatus = MutableStateFlow<ModelDownloadStatus>(ModelDownloadStatus.NotInstalled)
    val downloadStatus: StateFlow<ModelDownloadStatus> = _downloadStatus.asStateFlow()

    private var downloadJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    val modelsDir: File
        get() {
            val dir = File(context.filesDir, "models")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    val defaultModelFile: File
        get() = File(modelsDir, DEFAULT_MODEL_NAME)

    init {
        checkModelAvailability()
    }

    /**
     * Checks whether a model exists in internal storage, custom path, or ADB push path (/data/local/tmp/).
     */
    fun checkModelAvailability(): ModelDownloadStatus {
        val customPath = getCustomPath()
        if (!customPath.isNullOrBlank()) {
            val customFile = File(customPath)
            if (customFile.exists() && customFile.canRead() && customFile.length() > 0) {
                val ready = ModelDownloadStatus.Ready(customFile, isExternalPath = true)
                _downloadStatus.value = ready
                return ready
            }
        }

        // Check common ADB push paths: /data/local/tmp/gemma-2b-it.bin or .task
        val adbCandidates = listOf(
            File("/data/local/tmp/$DEFAULT_MODEL_NAME"),
            File("/data/local/tmp/gemma-2b-it.bin"),
            File("/data/local/tmp/gemma-2b-it-cpu.task"),
            File("/data/local/tmp/gemma-2b-it-gpu.bin")
        )
        for (adbFile in adbCandidates) {
            try {
                if (adbFile.exists() && adbFile.canRead() && adbFile.length() > 0) {
                    val ready = ModelDownloadStatus.Ready(adbFile, isExternalPath = true)
                    _downloadStatus.value = ready
                    return ready
                }
            } catch (_: Exception) {
                // Ignore permission restrictions on some sandboxes
            }
        }

        // Check internal filesDir
        if (defaultModelFile.exists() && defaultModelFile.length() > 0) {
            val ready = ModelDownloadStatus.Ready(defaultModelFile, isExternalPath = false)
            _downloadStatus.value = ready
            return ready
        }

        val notInstalled = ModelDownloadStatus.NotInstalled
        _downloadStatus.value = notInstalled
        return notInstalled
    }

    fun getActiveModelFile(): File? {
        val status = checkModelAvailability()
        return if (status is ModelDownloadStatus.Ready) status.file else null
    }

    fun isModelReady(): Boolean {
        return checkModelAvailability() is ModelDownloadStatus.Ready
    }

    fun getCustomPath(): String? {
        return prefs.getString(KEY_CUSTOM_MODEL_PATH, null)
    }

    fun setCustomPath(path: String?) {
        prefs.edit().putString(KEY_CUSTOM_MODEL_PATH, path?.trim()).apply()
        checkModelAvailability()
    }

    /**
     * Starts download from custom or default URL into internal filesDir without bloating the APK.
     */
    fun startDownload(
        scope: CoroutineScope,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        targetFileName: String = DEFAULT_MODEL_NAME
    ) {
        if (_downloadStatus.value is ModelDownloadStatus.Downloading) return

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            _downloadStatus.value = ModelDownloadStatus.Downloading(
                progressPercent = 0,
                bytesDownloaded = 0L,
                totalBytes = -1L
            )

            val targetFile = File(modelsDir, targetFileName)
            val tempFile = File(modelsDir, "$targetFileName.download")

            try {
                val request = Request.Builder().url(downloadUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Gagal mengunduh model. Kode HTTP: ${response.code}")
                }

                val body = response.body ?: throw Exception("Respons server kosong")
                val totalBytes = body.contentLength()
                var bytesReadTotal = 0L

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var lastProgressUpdate = System.currentTimeMillis()

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesReadTotal += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 300 || bytesReadTotal == totalBytes) {
                                lastProgressUpdate = now
                                val progress = if (totalBytes > 0) {
                                    ((bytesReadTotal.toDouble() / totalBytes.toDouble()) * 100).toInt()
                                } else 0

                                _downloadStatus.value = ModelDownloadStatus.Downloading(
                                    progressPercent = progress.coerceIn(0, 100),
                                    bytesDownloaded = bytesReadTotal,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                    }
                }

                if (tempFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = tempFile.renameTo(targetFile)
                    if (!renamed) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }
                }

                _downloadStatus.value = ModelDownloadStatus.Ready(targetFile, isExternalPath = false)

            } catch (e: CancellationException) {
                tempFile.delete()
                _downloadStatus.value = ModelDownloadStatus.NotInstalled
            } catch (e: Exception) {
                tempFile.delete()
                _downloadStatus.value = ModelDownloadStatus.Error(
                    e.localizedMessage ?: "Terjadi kesalahan saat mengunduh bobot model Gemma"
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val tempFile = File(modelsDir, "$DEFAULT_MODEL_NAME.download")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        checkModelAvailability()
    }

    fun deleteDownloadedModel(): Boolean {
        cancelDownload()
        var deleted = false
        if (defaultModelFile.exists()) {
            deleted = defaultModelFile.delete()
        }
        setCustomPath(null)
        checkModelAvailability()
        return deleted
    }

    companion object {
        const val DEFAULT_MODEL_NAME = "gemma-2b-it.bin"
        const val DEFAULT_DOWNLOAD_URL = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma-2b-it-gpu.bin"
        private const val KEY_CUSTOM_MODEL_PATH = "key_custom_model_path"
    }
}
