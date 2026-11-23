package com.example.masomoshare254.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey val resourceId: String,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val educationLevel: String = "",
    val topic: String = "",
    val resourceType: String = "",
    val fileUrl: String = "",
    val contributorId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)