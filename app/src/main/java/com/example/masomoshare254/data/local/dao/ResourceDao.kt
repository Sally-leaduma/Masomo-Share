package com.example.masomoshare254.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masomoshare254.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<ResourceEntity>)

    @Query("SELECT * FROM resources WHERE resourceId = :resourceId LIMIT 1")
    suspend fun getResourceById(resourceId: String): ResourceEntity?

    @Query("SELECT * FROM resources")
    fun observeAllResources(): Flow<List<ResourceEntity>>

    @Query("DELETE FROM resources WHERE resourceId = :resourceId")
    suspend fun deleteResourceById(resourceId: String)
}