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
        isVideo: Boolean
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
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
            inputStream.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Result.success("Saved to Gallery: $cleanName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving media to gallery", e)
            Result.failure(e)
        }
    }

    suspend fun downloadDocument(
        context: Context,
        fileUrl: String,
        fileName: String,
        mimeType: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetDir = File(context.cacheDir, "documents")
            if (!targetDir.exists()) targetDir.mkdirs()
            val targetFile = File(targetDir, safeName)

            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

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
