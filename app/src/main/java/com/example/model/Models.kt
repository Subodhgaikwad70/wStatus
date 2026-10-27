package com.example.model

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
    val durationMs: Long = 0L,
    val isDemo: Boolean = false
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
}
