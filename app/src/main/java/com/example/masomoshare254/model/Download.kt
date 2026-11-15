package com.example.masomoshare254.model

data class Download(
    val downloadId: String,
    val userId: String,
    val resourceId: String,
    val localFilePath: String,
    val downloadDate: Long
)