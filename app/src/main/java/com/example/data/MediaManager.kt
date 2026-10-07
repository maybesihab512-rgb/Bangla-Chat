package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object MediaManager {

    private const val TAG = "MediaManager"

    suspend fun saveMediaToGallery(
        context: Context,
        mediaUrl: String,
        fileName: String,
        isVideo: Boolean,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanName = if (fileName.isNotBlank()) fileName
            else if (isVideo) "video_${System.currentTimeMillis()}.mp4"
            else "photo_${System.currentTimeMillis()}.jpg"

            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

            val url = URL(mediaUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLength.toLong()
            val inputStream: InputStream = connection.inputStream

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = if (isVideo) Environment.DIRECTORY_MOVIES + "/CipherLink"
                    else Environment.DIRECTORY_PICTURES + "/CipherLink"
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val uri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloaded = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalBytes > 0) {
                        onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }
                outputStream.flush()
            }
            inputStream.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            onProgress(100)
            Result.success("Saved to Gallery: $cleanName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving media to gallery", e)
            Result.failure(e)
        }
    }

    suspend fun saveDocumentToDownloads(
        context: Context,
        fileUrl: String,
        fileName: String,
        mimeType: String,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeName = fileName.ifBlank { "document_${System.currentTimeMillis()}.bin" }

            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLength.toLong()
            val inputStream: InputStream = connection.inputStream

            val resolvedMime = mimeType.ifBlank {
                val ext = safeName.substringAfterLast('.', "")
                if (ext.isNotEmpty()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                else "application/octet-stream"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, resolvedMime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CipherLink")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to create Download entry"))

                resolver.openOutputStream(uri)?.use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalBytes > 0) {
                            onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                        }
                    }
                    outputStream.flush()
                }
                inputStream.close()

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "CipherLink")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, safeName)

                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalBytes > 0) {
                            onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                        }
                    }
                    output.flush()
                }
                inputStream.close()
            }

            onProgress(100)
            Result.success("Saved to Downloads: $safeName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document to downloads", e)
            Result.failure(e)
        }
    }

    suspend fun downloadDocument(
        context: Context,
        fileUrl: String,
        fileName: String,
        mimeType: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_").ifBlank { "doc_${System.currentTimeMillis()}" }
            val targetDir = File(context.cacheDir, "documents")
            if (!targetDir.exists()) targetDir.mkdirs()
            val targetFile = File(targetDir, safeName)

            // If already downloaded and valid, return cached file directly
            if (targetFile.exists() && targetFile.length() > 0) {
                onProgress(100)
                return@withContext Result.success(targetFile)
            }

            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLength.toLong()

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalBytes > 0) {
                            onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                        }
                    }
                    output.flush()
                }
            }

            onProgress(100)
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading document", e)
            Result.failure(e)
        }
    }

    fun openDocumentFile(context: Context, file: File, mimeType: String) {
        try {
            val uri: Uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val resolvedMime = mimeType.ifBlank {
                val extension = file.extension
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open document with app", e)
            Toast.makeText(context, "No app available to open this document", Toast.LENGTH_SHORT).show()
        }
    }
}
