package com.example.masomoshare254   // TODO: change to YOUR package name

/**
 * CONTRACT WITH MEMBER 6.
 * Member 6's Room 'Download' entity should hold at least these fields.
 * If their entity is named differently, map it to/from this class inside their repository.
 * The record and the file on the phone MUST correspond: localPath points to a real file.
 */
data class DownloadRecord(
    val id: Long = 0,
    val userId: String,          // FirebaseAuth uid of the logged-in user
    val resourceId: String,      // same type as Resource.id (String if Firestore)
    val title: String,
    val subject: String,
    val educationLevel: String,
    val fileUrl: String,
    val localPath: String,       // absolute path of the saved PDF
    val fileSizeBytes: Long,
    val downloadedAt: Long       // System.currentTimeMillis()
)

/** What Member 6's repository must provide (all suspend functions, run on a background thread by Room). */
interface DownloadStore {
    suspend fun getDownloadsForUser(userId: String): List<DownloadRecord>
    suspend fun find(userId: String, resourceId: String): DownloadRecord?
    suspend fun save(record: DownloadRecord)
    suspend fun delete(userId: String, resourceId: String)
}

/** TEMPORARY store (lost when the app closes) so you can test before Member 6 is ready. */
class InMemoryDownloadStore : DownloadStore {
    private val items = mutableListOf<DownloadRecord>()
    override suspend fun getDownloadsForUser(userId: String) =
        items.filter { it.userId == userId }.sortedByDescending { it.downloadedAt }
    override suspend fun find(userId: String, resourceId: String) =
        items.firstOrNull { it.userId == userId && it.resourceId == resourceId }
    override suspend fun save(record: DownloadRecord) {
        items.removeAll { it.userId == record.userId && it.resourceId == record.resourceId }
        items.add(record)
    }
    override suspend fun delete(userId: String, resourceId: String) {
        items.removeAll { it.userId == userId && it.resourceId == resourceId }
    }
}

/** One place to swap the store. When Member 6 is done: LibraryProvider.store = RoomDownloadStore(db.downloadDao()) */
object LibraryProvider {
    var store: DownloadStore = InMemoryDownloadStore()
}

/** What the Catalogue (Member 4) passes in when the user taps Download. */
data class ResourceInfo(
    val resourceId: String,
    val title: String,
    val subject: String,
    val educationLevel: String,
    val fileUrl: String
)
