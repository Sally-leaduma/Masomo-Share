package com.example.masomoshare254.data.repository

import com.example.masomoshare254.model.Download
import com.example.masomoshare254.model.Resource
import com.example.masomoshare254.model.User
import kotlinx.coroutines.flow.Flow

interface OfflineStorageRepository {
    suspend fun saveDownload(download: Download)
    fun observeDownloadsByUser(userId: String): Flow<List<Download>>
    suspend fun getDownloadsByUser(userId: String): List<Download>
    suspend fun findDownloadedResource(userId: String, resourceId: String): Download?
    suspend fun deleteDownload(downloadId: String)

    suspend fun saveResource(resource: Resource)
    suspend fun deleteResource(resourceId: String)
    suspend fun upsertUser(user: User)
    suspend fun getUser(userId: String): User?
}