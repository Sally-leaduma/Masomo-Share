package com.example.masomoshare254.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val email: String,
    val role: String = "",
    val institution: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)