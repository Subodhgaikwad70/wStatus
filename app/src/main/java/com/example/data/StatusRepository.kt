package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.model.MediaType
import com.example.model.StatusItem
import com.example.model.WhatsAppAppType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class StatusRepository(private val context: Context) {
    private val preferences = UserPreferences(context)
    private val TAG = "StatusRepository"

    suspend fun getStatuses(
        appType: WhatsAppAppType,
        forceDemo: Boolean = false
    ): List<StatusItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<StatusItem>()
        val savedFileNames = getSavedFileNames()
        val favorites = preferences.favoriteIdsFlow.first()
        val tags = preferences.allAlbumTagsFlow.first()

// 1. Try to read from SAF tree URI if granted
        val safUriString = preferences.getSafUriFlow(appType).first()
        if (!safUriString.isNullOrEmpty()) {
            try {
                val treeUri = Uri.parse(safUriString)
                val documentId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        if (name.startsWith(".nomedia")) continue
                        val mime = cursor.getString(mimeCol) ?: ""
                        val isVideo = mime.startsWith("video/") || name.endsWith(".mp4", ignoreCase = true)
                        val isImage = mime.startsWith("image/") ||
                                name.endsWith(".jpg", ignoreCase = true) ||
                                name.endsWith(".jpeg", ignoreCase = true) ||
                                name.endsWith(".png", ignoreCase = true)

                        if (isImage || isVideo) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val size = cursor.getLong(sizeCol)
                            val date = cursor.getLong(dateCol)
                            val id = "saf_${appType.name}_${docId.hashCode()}"
                            val isSaved = savedFileNames.contains(name)
                            results.add(
                                StatusItem(
                                    id = id,
                                    uriString = fileUri.toString(),
                                    filePath = null,
                                    title = name,
                                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                                    sizeBytes = size,
                                    dateModified = date,
                                    appType = appType,
                                    isSaved = isSaved,
                                    isFavorite = favorites.contains(id),
                                    albumTag = tags[id] ?: "General",
                                    isDemo = false
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read from SAF tree URI", e)
            }
        }

        // 2. Try to read from direct standard filesystem paths
        if (results.isEmpty()) {
            val possiblePaths = listOf(
                File(Environment.getExternalStorageDirectory(), appType.relativePath),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
                File(Environment.getExternalStorageDirectory(), "WhatsApp Business/Media/.Statuses"),
                File(Environment.getExternalStorageDirectory(), "GBWhatsApp/Media/.Statuses")
            )

            for (dir in possiblePaths) {
                if (dir.exists() && dir.isDirectory && dir.canRead()) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        if (file.name.startsWith(".nomedia")) continue
                        val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                        val isImage = file.name.endsWith(".jpg", ignoreCase = true) ||
                                file.name.endsWith(".jpeg", ignoreCase = true) ||
                                file.name.endsWith(".png", ignoreCase = true)
                        if (isImage || isVideo) {
                            val id = "file_${file.absolutePath.hashCode()}"
                            val isSaved = savedFileNames.contains(file.name)
                            results.add(
                                StatusItem(
                                    id = id,
                                    uriString = Uri.fromFile(file).toString(),
                                    filePath = file.absolutePath,
                                    title = file.name,
                                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                                    sizeBytes = file.length(),
                                    dateModified = file.lastModified(),
                                    appType = appType,
                                    isSaved = isSaved,
                                    isFavorite = favorites.contains(id),
                                    albumTag = tags[id] ?: "General",
                                    isDemo = false
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Fallback to / include demo statuses if folder is empty or forceDemo is enabled
        if (results.isEmpty() || forceDemo) {
            val demoList = DemoMediaGenerator.generateDemoStatuses(context)
            val filteredDemo = demoList.filter { it.appType == appType || forceDemo }
            for (demo in filteredDemo) {
                val isSaved = savedFileNames.contains(File(demo.filePath ?: "").name)
                results.add(
                    demo.copy(
                        isSaved = isSaved,
                        isFavorite = favorites.contains(demo.id),
                        albumTag = tags[demo.id] ?: demo.albumTag
                    )
                )
            }
        }

        results.sortedByDescending { it.dateModified }
    }

    suspend fun getSavedStatuses(): List<StatusItem> = withContext(Dispatchers.IO) {
        val savedItems = mutableListOf<StatusItem>()
        val favorites = preferences.favoriteIdsFlow.first()
        val tags = preferences.allAlbumTagsFlow.first()

        // Check app internal saved statuses
        val appSavedDir = File(context.getExternalFilesDir(null), "SavedStatuses")
        if (appSavedDir.exists()) {
            appSavedDir.listFiles()?.forEach { file ->
                val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                val id = "saved_${file.absolutePath.hashCode()}"
                savedItems.add(
                    StatusItem(
                        id = id,
                        uriString = Uri.fromFile(file).toString(),
                        filePath = file.absolutePath,
                        title = file.name,
                        mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                        sizeBytes = file.length(),
                        dateModified = file.lastModified(),
                        appType = parseAppTypeFromFileName(file.name),
                        isSaved = true,
                        isFavorite = favorites.contains(id),
                        albumTag = tags[id] ?: "General",
                        isDemo = false
                    )
                )
            }
        }

        // Query MediaStore for saved statuses in Pictures/StatusSaver and Movies/StatusSaver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        // Query images
        try {
            val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else {
                null
            }
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf("%StatusSaver%")
            } else {
                null
            }

            context.contentResolver.query(imageUri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val idVal = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Saved_Image.jpg"
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val contentUri = Uri.withAppendedPath(imageUri, idVal.toString())
                    val itemId = "ms_img_$idVal"

                    // Avoid duplicate if already found in appSavedDir
                    if (savedItems.none { it.title == name }) {
                        savedItems.add(
                            StatusItem(
                                id = itemId,
                                uriString = contentUri.toString(),
                                filePath = null,
                                title = name,
                                mediaType = MediaType.IMAGE,
                                sizeBytes = size,
                                dateModified = date,
                                appType = parseAppTypeFromFileName(name),
                                isSaved = true,
                                isFavorite = favorites.contains(itemId),
                                albumTag = tags[itemId] ?: "General",
                                isDemo = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore images", e)
        }

        // Query videos
        try {
            val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else {
                null
            }
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf("%StatusSaver%")
            } else {
                null
            }

            context.contentResolver.query(videoUri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val idVal = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Saved_Video.mp4"
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val contentUri = Uri.withAppendedPath(videoUri, idVal.toString())
                    val itemId = "ms_vid_$idVal"

                    if (savedItems.none { it.title == name }) {
                        savedItems.add(
                            StatusItem(
                                id = itemId,
                                uriString = contentUri.toString(),
                                filePath = null,
                                title = name,
                                mediaType = MediaType.VIDEO,
                                sizeBytes = size,
                                dateModified = date,
                                appType = parseAppTypeFromFileName(name),
                                isSaved = true,
                                isFavorite = favorites.contains(itemId),
                                albumTag = tags[itemId] ?: "General",
                                isDemo = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore videos", e)
        }

        savedItems.sortedByDescending { it.dateModified }
    }

    suspend fun deleteSavedStatus(item: StatusItem): Boolean = withContext(Dispatchers.IO) {
        var deleted = false
        try {
            if (item.filePath != null) {
                val f = File(item.filePath)
                if (f.exists()) {
                    deleted = f.delete()
                }
            }
            val uri = Uri.parse(item.uriString)
            if (uri.scheme == "content") {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deleted = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting saved status", e)
        }
        deleted
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val saved = getSavedStatuses()
        val totalBytes = saved.sumOf { it.sizeBytes }
        val imageCount = saved.count { it.mediaType == MediaType.IMAGE }
        val videoCount = saved.count { it.mediaType == MediaType.VIDEO }

        val cacheDir = context.cacheDir
        val cacheBytes = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        StorageStats(
            totalSavedCount = saved.size,
            imageCount = imageCount,
            videoCount = videoCount,
            totalSizeBytes = totalBytes,
            cacheSizeBytes = cacheBytes
        )
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            false
        }
    }

    private fun getSavedFileNames(): Set<String> {
        val set = mutableSetOf<String>()
        val appSavedDir = File(context.getExternalFilesDir(null), "SavedStatuses")
        if (appSavedDir.exists()) {
            appSavedDir.listFiles()?.forEach { set.add(it.name) }
        }
        return set
    }

    private fun parseAppTypeFromFileName(name: String): WhatsAppAppType {
        return when {
            name.contains("_WAB_") -> WhatsAppAppType.WHATSAPP_BUSINESS
            name.contains("_GB_") -> WhatsAppAppType.GB_WHATSAPP
            name.contains("_Dual_") -> WhatsAppAppType.DUAL_WHATSAPP
            else -> WhatsAppAppType.WHATSAPP
        }
    }
}

data class StorageStats(
    val totalSavedCount: Int,
    val imageCount: Int,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val cacheSizeBytes: Long
) {
    val formattedTotalSize: String
        get() = formatBytes(totalSizeBytes)

    val formattedCacheSize: String
        get() = formatBytes(cacheSizeBytes)

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
