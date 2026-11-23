package com.example.masomoshare254

import android.content.Context
import java.io.File

object FileStorage {
    fun isValid(file: File): Boolean {
        return file.exists() && file.length() > 0
    }
}

class LibraryStore {
    suspend fun getDownloadsForUser(userId: String): List<DownloadRecord> {
        return emptyList()
    }
    suspend fun delete(userId: String, resourceId: String) {}
}

object LibraryProvider {
    val store = LibraryStore()
}

class DownloadService(val context: Context) {
    suspend fun deleteDownload(record: DownloadRecord) {
        LibraryProvider.store.delete(record.userId, record.resourceId)
    }
}
