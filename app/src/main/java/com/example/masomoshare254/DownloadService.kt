package com.example.masomoshare254

import android.content.Context
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

sealed class DownloadOutcome {
    data class Downloaded(val record: DownloadRecord) : DownloadOutcome()
    data class AlreadyDownloaded(val record: DownloadRecord) : DownloadOutcome()
    object InProgress : DownloadOutcome()                     // same resource is already downloading
    data class Failed(val message: String) : DownloadOutcome()
    // Cancellation: cancel the coroutine Job that called downloadResource(); it throws CancellationException.
}

/**
 * Flow: duplicate check -> Firebase Storage download (progress, cancellable) -> save DownloadRecord.
 * Uses the shared persistence layer (LibraryProvider.store = Member 6's repository), NOT its own database.
 */
class DownloadService(
    private val context: Context,
    private val store: DownloadStore = LibraryProvider.store,
    private val downloader: FileDownloader = FileDownloader()
) {

    companion object {
        // Resources being downloaded right now ("userId:resourceId"), to block double-taps
        private val active = mutableSetOf<String>()
        fun isActive(userId: String, resourceId: String) =
            synchronized(active) { active.contains("$userId:$resourceId") }
    }

    suspend fun downloadResource(
        userId: String,
        resource: ResourceInfo,
        onProgress: (Int) -> Unit = {}
    ): DownloadOutcome {
        val key = "$userId:${resource.resourceId}"
        if (!synchronized(active) { active.add(key) }) return DownloadOutcome.InProgress

        try {
            // 1. Already downloaded? (record exists AND the file really exists)
            val existing = store.find(userId, resource.resourceId)
            if (existing != null && FileStorage.isValid(File(existing.localPath))) {
                return DownloadOutcome.AlreadyDownloaded(existing)
            }
            if (existing != null) store.delete(userId, resource.resourceId)   // stale record

            if (!NetworkUtils.isOnline(context)) {
                return DownloadOutcome.Failed("No internet connection. Connect and try again.")
            }
            if (resource.fileUrl.isBlank()) return DownloadOutcome.Failed("This resource has no file.")

            // 2. Download into private internal storage
            val target = FileStorage.fileFor(context, userId, resource.resourceId)
            val result = downloader.download(resource.fileUrl, target, onProgress)
            if (result is FileResult.Failure) return DownloadOutcome.Failed(result.message)
            val file = (result as FileResult.Success).file

            // 3. Save the record. NonCancellable so a late cancel can't leave a file with no record.
            val record = DownloadRecord(
                userId = userId,
                resourceId = resource.resourceId,
                title = resource.title,
                subject = resource.subject,
                educationLevel = resource.educationLevel,
                fileUrl = resource.fileUrl,
                localPath = file.absolutePath,
                fileSizeBytes = file.length(),
                downloadedAt = System.currentTimeMillis()
            )
            return try {
                withContext(NonCancellable) { store.save(record) }
                DownloadOutcome.Downloaded(record)
            } catch (e: Exception) {
                file.delete()                                   // keep file + record in sync
                DownloadOutcome.Failed("Could not save download record.")
            }
        } finally {
            synchronized(active) { active.remove(key) }
        }
    }

    /** Removes BOTH the local file and its database record. */
    suspend fun deleteDownload(record: DownloadRecord) {
        File(record.localPath).delete()
        store.delete(record.userId, record.resourceId)
    }
}
