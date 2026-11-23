package com.example.masomoshare254.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.masomoshare254.data.local.dao.DownloadDao
import com.example.masomoshare254.data.local.dao.ResourceDao
import com.example.masomoshare254.data.local.dao.UserDao
import com.example.masomoshare254.data.local.entity.DownloadEntity
import com.example.masomoshare254.data.local.entity.ResourceEntity
import com.example.masomoshare254.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ResourceEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun resourceDao(): ResourceDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "masomoshare254_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}