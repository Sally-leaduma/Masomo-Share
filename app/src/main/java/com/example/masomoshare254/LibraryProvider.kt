
package com.example.masomoshare254

import android.content.Context
import android.net.Uri
import com.example.masomoshare254.data.local.AppDatabase
import com.example.masomoshare254.data.local.entity.DownloadEntity
import com.example.masomoshare254.data.local.entity.UserEntity
import com.example.masomoshare254.data.local.mapper.toEntity
import com.example.masomoshare254.model.Resource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID

object FileStorage {
    fun isValid(file: File): Boolean {
        return file.exists() && file.isFile && file.length() > 0
    }
}

object LibraryProvider {
    @Volatile
    private var database: AppDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    database = AppDatabase.getInstance(context.applicationContext)
                }
            }
        }
    }

    val store: LibraryStore
        get() = LibraryStore(
            database ?: throw IllegalStateException(
                "LibraryProvider has not been initialized."
            )
        )
}

class LibraryStore(private val db: AppDatabase) {

    suspend fun getDownloadsForUser(userId: String): List<DownloadRecord> {
        val downloads = db.downloadDao().getDownloadsForUser(userId)

        return downloads.mapNotNull { download ->
            val resource = db.resourceDao()
                .getResourceById(download.resourceId)
                ?: return@mapNotNull null

            val file = File(download.localFilePath)

            DownloadRecord(
                userId = download.userId,
                resourceId = download.resourceId,
                title = resource.title,
                subject = resource.subject,
                educationLevel = resource.educationLevel,
                localPath = download.localFilePath,
                downloadedAt = download.downloadDate,
                fileSizeBytes = if (file.exists()) file.length() else 0L
            )
        }
    }

    suspend fun delete(userId: String, resourceId: String) {
        db.downloadDao().deleteDownloadForUserResource(userId, resourceId)
    }
}

class DownloadService(private val context: Context) {

    init {
        LibraryProvider.initialize(context)
    }

    suspend fun download(resource: Resource): DownloadRecord =
        withContext(Dispatchers.IO) {

            val user = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("Please log in before downloading.")

            require(resource.resourceId.isNotBlank()) {
                "Invalid resource."
            }

            require(resource.fileUrl.isNotBlank()) {
                "This resource has no downloadable file."
            }

            val email = user.email ?: ""
            val fullName = user.displayName
                ?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@")

            val folder = File(context.filesDir, "downloads/${user.uid}")

            if (!folder.exists() && !folder.mkdirs()) {
                throw IllegalStateException("Could not create download folder.")
            }

            val originalName = resource.fileName
                .ifBlank {
                    Uri.parse(resource.fileUrl).lastPathSegment
                        ?.let { Uri.decode(it) }
                        .orEmpty()
                }
                .ifBlank { "${resource.resourceId}.pdf" }

            val safeName = originalName
                .substringAfterLast("/")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")

            val file = File(folder, "${UUID.randomUUID()}_$safeName")
            val tempFile = File(folder, "${file.name}.part")

            try {
                val connection = URL(resource.fileUrl).openConnection().apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                }

                connection.getInputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!FileStorage.isValid(tempFile)) {
                    throw IllegalStateException("The downloaded file is empty.")
                }

                if (!tempFile.renameTo(file)) {
                    throw IllegalStateException("Could not save downloaded file.")
                }

                val db = AppDatabase.getInstance(context)

                // Save the parent records before the download record.
                db.userDao().insertUser(
                    UserEntity(
                        userId = user.uid,
                        fullName = fullName,
                        email = email
                    )
                )

                db.resourceDao().insertResource(resource.toEntity())

                db.downloadDao().insertDownload(
                    DownloadEntity(
                        downloadId = UUID.randomUUID().toString(),
                        userId = user.uid,
                        resourceId = resource.resourceId,
                        localFilePath = file.absolutePath,
                        downloadDate = System.currentTimeMillis()
                    )
                )

                DownloadRecord(
                    userId = user.uid,
                    resourceId = resource.resourceId,
                    title = resource.title,
                    subject = resource.subject,
                    educationLevel = resource.educationLevel,
                    localPath = file.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                    fileSizeBytes = file.length()
                )

            } catch (e: Exception) {
                tempFile.delete()
                file.delete()
                throw e
            }
        }

    suspend fun deleteDownload(record: DownloadRecord) {
        withContext(Dispatchers.IO) {
            File(record.localPath).delete()
            LibraryProvider.store.delete(record.userId, record.resourceId)
        }
    }
}
