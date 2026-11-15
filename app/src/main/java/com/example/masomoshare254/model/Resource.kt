package com.example.masomoshare254.model

data class Resource(
    val resourceId: String,
    val title: String,
    val description: String,
    val subject: String,
    val educationLevel: String,
    val topic: String,
    val resourceType: String,
    val fileUrl: String,
    val contributorId: String
)