package com.example.masomoshare254

/**
 * DownloadRecord entity/model representing offline downloaded resources.
 */
data class DownloadRecord(
    val userId: String = "",
    val resourceId: String = "",
    val title: String = "",
    val subject: String = "",
    val educationLevel: String = "",
    val localPath: String = "",
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
)
