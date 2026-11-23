package com.example.masomoshare254.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["resourceId"]),
        Index(value = ["userId", "resourceId"], unique = true)
    ]
)
data class DownloadEntity(
    @PrimaryKey val downloadId: String,
    val userId: String,
    val resourceId: String,
    val localFilePath: String = "",
    val downloadDate: Long = 0L
)