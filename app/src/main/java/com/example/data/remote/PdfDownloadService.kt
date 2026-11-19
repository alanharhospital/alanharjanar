package com.example.data.remote

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class PdfDownloadService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun pdfDir(): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun downloadLegalPdf(articleKey: String, pdfUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (pdfUrl.isBlank() || (!pdfUrl.startsWith("https://") && !pdfUrl.startsWith("http://"))) {
                    return@withContext Result.failure(IllegalArgumentException("No verified legal PDF URL available."))
                }
                val request = Request.Builder()
                    .url(pdfUrl)
                    .header("User-Agent", "AlAnharScientificJournal/1.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("Publisher returned HTTP ${response.code}"))
                    }
                    val contentType = response.header("Content-Type").orEmpty()
                    val body = response.body ?: return@withContext Result.failure(IllegalStateException("Empty PDF response"))
                    val bytes = body.bytes()
                    val hasPdfHeader = bytes.size >= 4 &&
                        bytes[0] == '%'.code.toByte() &&
                        bytes[1] == 'P'.code.toByte() &&
                        bytes[2] == 'D'.code.toByte() &&
                        bytes[3] == 'F'.code.toByte()
                    if (!contentType.contains("pdf", ignoreCase = true) && !hasPdfHeader) {
                        return@withContext Result.failure(IllegalStateException("Publisher URL did not return a valid PDF document."))
                    }
                    val safeName = articleKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") + ".pdf"
                    val outFile = File(pdfDir(), safeName)
                    outFile.writeBytes(bytes)
                    Result.success(outFile.absolutePath)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun openLocalPdf(localPath: String): Result<Unit> {
        return try {
            val file = File(localPath)
            if (!file.exists()) return Result.failure(IllegalStateException("Downloaded PDF file not found on device."))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteLocalPdf(localPath: String): Boolean {
        val file = File(localPath)
        return if (file.exists()) file.delete() else false
    }
}
