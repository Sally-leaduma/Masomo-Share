package com.example.masomoshare254.data.repository

import com.example.masomoshare254.data.local.dao.DownloadDao
import com.example.masomoshare254.data.local.dao.ResourceDao
import com.example.masomoshare254.data.local.dao.UserDao
import com.example.masomoshare254.data.local.mapper.toDomain
import com.example.masomoshare254.data.local.mapper.toEntity
import com.example.masomoshare254.model.Download
import com.example.masomoshare254.model.Resource
import com.example.masomoshare254.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineStorageRepositoryImpl(
    private val userDao: UserDao,
    private val resourceDao: ResourceDao,
    private val downloadDao: DownloadDao
) : OfflineStorageRepository {

    override suspend fun saveDownload(download: Download) =
        downloadDao.insertDownload(download.toEntity())

    override fun observeDownloadsByUser(userId: String): Flow<List<Download>> =
        downloadDao.observeDownloadsForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getDownloadsByUser(userId: String): List<Download> =
        downloadDao.getDownloadsForUser(userId).map { it.toDomain() }

    override suspend fun findDownloadedResource(userId: String, resourceId: String): Download? =
        downloadDao.findDownload(userId, resourceId)?.toDomain()

    override suspend fun deleteDownload(downloadId: String) =
        downloadDao.deleteDownloadById(downloadId)

    override suspend fun saveResource(resource: Resource) =
        resourceDao.insertResource(resource.toEntity())

    override suspend fun deleteResource(resourceId: String) =
        resourceDao.deleteResourceById(resourceId)

    override suspend fun upsertUser(user: User) =
        userDao.insertUser(user.toEntity())

    override suspend fun getUser(userId: String): User? =
        userDao.getUserById(userId)?.toDomain()
}