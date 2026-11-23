
package com.example.masomoshare254.model

data class Resource(
    val resourceId: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val educationLevel: String = "",
    val topic: String = "",
    val resourceType: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val fileType: String = "",
    val storagePath: String = "",
    val contributorId: String = "",
    val uploaderId: String = "",
    val uploadedAt: Long = 0L
)
