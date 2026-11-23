package com.example.masomoshare254.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masomoshare254.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY downloadDate DESC")
    fun observeDownloadsForUser(userId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY downloadDate DESC")
    suspend fun getDownloadsForUser(userId: String): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE userId = :userId AND resourceId = :resourceId LIMIT 1")
    suspend fun findDownload(userId: String, resourceId: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE downloadId = :downloadId")
    suspend fun deleteDownloadById(downloadId: String)

    @Query("DELETE FROM downloads WHERE userId = :userId AND resourceId = :resourceId")
    suspend fun deleteDownloadForUserResource(userId: String, resourceId: String)
}