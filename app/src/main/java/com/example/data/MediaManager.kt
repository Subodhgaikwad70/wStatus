package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.MediaType
import com.example.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object MediaManager {
    private const val TAG = "MediaManager"

    suspend fun saveToGallery(context: Context, item: StatusItem): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val isVideo = item.mediaType == MediaType.VIDEO
            val extension = if (isVideo) ".mp4" else ".jpg"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            val timestamp = System.currentTimeMillis()
            val fileName = "Status_${item.appType.shortName}_${timestamp}$extension"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val folder = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/StatusSaver")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val targetUri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Unable to create MediaStore entry"))

            // Stream media content into the created MediaStore entry
            val inputStream: InputStream? = if (item.filePath != null && File(item.filePath).exists()) {
                FileInputStream(File(item.filePath))
            } else {
                resolver.openInputStream(Uri.parse(item.uriString))
            }

            if (inputStream == null) {
                resolver.delete(targetUri, null, null)
                return@withContext Result.failure(Exception("Could not open source media stream"))
            }

            resolver.openOutputStream(targetUri)?.use { out ->
                inputStream.use { inStream ->
                    inStream.copyTo(out)
                }
            } ?: run {
                resolver.delete(targetUri, null, null)
                return@withContext Result.failure(Exception("Could not open output stream for writing"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }

            // Also copy to app internal saved media directory for file manager organization
            val savedDir = File(context.getExternalFilesDir(null), "SavedStatuses").apply { mkdirs() }
            val localSavedFile = File(savedDir, fileName)
            if (item.filePath != null && File(item.filePath).exists()) {
                File(item.filePath).copyTo(localSavedFile, overwrite = true)
            }

            Result.success(targetUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving status to gallery", e)
            Result.failure(e)
        }
    }

    fun getShareableUri(context: Context, item: StatusItem): Uri? {
        return try {
            if (item.filePath != null) {
                val file = File(item.filePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    Uri.parse(item.uriString)
                }
            } else {
                val uri = Uri.parse(item.uriString)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: return null)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    uri
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating shareable URI", e)
            null
        }
    }

    fun shareStatus(context: Context, item: StatusItem) {
        try {
            val shareUri = getShareableUri(context, item) ?: run {
                Toast.makeText(context, "Could not prepare file for sharing", Toast.LENGTH_SHORT).show()
                return
            }
            val mimeType = if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Status via"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing status", e)
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMultiple(context: Context, items: List<StatusItem>) {
        if (items.isEmpty()) return
        try {
            val uris = ArrayList<Uri>()
            for (item in items) {
                val uri = getShareableUri(context, item)
                if (uri != null) {
                    uris.add(uri)
                }
            }
            if (uris.isEmpty()) {
                Toast.makeText(context, "No media files available to share", Toast.LENGTH_SHORT).show()
                return
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${items.size} Statuses"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing multiple statuses", e)
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun repostToWhatsApp(context: Context, item: StatusItem) {
        try {
            val shareUri = getShareableUri(context, item) ?: return
            val mimeType = if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                `package` = item.appType.packageName
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If target WhatsApp package is not installed, fallback to standard share chooser
            shareStatus(context, item)
        }
    }
}
