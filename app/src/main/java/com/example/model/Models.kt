package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class WhatsAppAppType(
    val displayName: String,
    val packageName: String,
    val relativePath: String,
    val shortName: String
) {
    WHATSAPP(
        displayName = "WhatsApp",
        packageName = "com.whatsapp",
        relativePath = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        shortName = "WA"
    ),
    WHATSAPP_BUSINESS(
        displayName = "WA Business",
        packageName = "com.whatsapp.w4b",
        relativePath = "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        shortName = "WAB"
    ),
    GB_WHATSAPP(
        displayName = "GB WhatsApp",
        packageName = "com.gbwhatsapp",
        relativePath = "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
        shortName = "GB"
    ),
    DUAL_WHATSAPP(
        displayName = "Dual Space / Clone",
        packageName = "com.whatsapp.dual",
        relativePath = "Android/media/com.whatsapp.clone/Media/.Statuses",
        shortName = "Dual"
    )
}

enum class ThemeMode(val title: String) {
    SYSTEM("Follow System"),
    DARK("Dark Theme"),
    LIGHT("Light Theme")
}

enum class SortOption(val title: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    LARGEST_SIZE("Largest Size"),
    SMALLEST_SIZE("Smallest Size")
}

data class StatusItem(
    val id: String,
    val uriString: String,
    val filePath: String? = null,
    val title: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val dateModified: Long,
    val appType: WhatsAppAppType,
    val isSaved: Boolean = false,
    val isFavorite: Boolean = false,
    val albumTag: String = "General",
    val durationMs: Long = 0L
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 KB"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedDateTime: String
        get() {
            if (dateModified <= 0) return "Recent"
            val now = System.currentTimeMillis()
            val diff = now - dateModified
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = timeFormat.format(Date(dateModified))

            val oneDayMs = 24 * 60 * 60 * 1000L
            return when {
                diff in 0..oneDayMs -> "Today, $timeStr"
                diff in oneDayMs..(2 * oneDayMs) -> "Yesterday, $timeStr"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                    dateFormat.format(Date(dateModified))
                }
            }
        }

    val formattedDate: String
        get() {
            if (dateModified <= 0) return ""
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return dateFormat.format(Date(dateModified))
        }

    val formattedTime: String
        get() {
            if (dateModified <= 0) return ""
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return timeFormat.format(Date(dateModified))
        }
}
