package com.example.masomoshare254.data

import com.example.masomoshare254.model.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException

/**
 * Member 4: reads (and lets the uploader delete) resources stored in Firestore.
 * Member 3 writes to the SAME collection, so COLLECTION must match their upload code.
 *
 * NOTE: the real protection for deletes is in Firebase security rules
 * (see firestore.rules / storage.rules). The checks in the app only give a friendly UX.
 */
class ResourceCatalogueRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val collection = db.collection(COLLECTION)

    /** Real-time listener: the list updates automatically when anyone uploads or deletes. */
    fun listenToResources(
        onSuccess: (List<Resource>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration =
        collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                // The Firestore document id becomes resourceId
                doc.toObject(Resource::class.java)?.copy(resourceId = doc.id)
            }
            onSuccess(list)
        }

    fun getResource(
        resourceId: String,
        onSuccess: (Resource?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!isValidId(resourceId)) {
            onError(IllegalArgumentException("Invalid resource id"))
            return
        }
        collection.document(resourceId).get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.toObject(Resource::class.java)?.copy(resourceId = doc.id))
            }
            .addOnFailureListener { e -> onError(e) }
    }

    /**
     * Deletes the stored file first, then the Firestore record.
     * - If the file deletion FAILS (network, permission, ...) the record is kept and onError is called.
     * - If the file is genuinely not there (OBJECT_NOT_FOUND) we continue, so a delete that
     *   half-failed earlier (file removed, record still present) can be completed by retrying.
     */
    fun deleteResource(
        resource: Resource,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!isValidId(resource.resourceId)) {
            onError(IllegalArgumentException("Invalid resource id"))
            return
        }

        fun deleteRecord() {
            collection.document(resource.resourceId).delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e) }
        }

        // Prefer the exact storagePath saved at upload time; fall back to the download URL
        val fileRef = try {
            when {
                resource.storagePath.isNotBlank() -> storage.reference.child(resource.storagePath)
                resource.fileUrl.isNotBlank() -> storage.getReferenceFromUrl(resource.fileUrl)
                else -> null // this resource never had a file
            }
        } catch (e: IllegalArgumentException) {
            onError(e) // we can't tell which file to remove, so don't orphan it by deleting the record
            return
        }

        if (fileRef == null) {
            deleteRecord()
            return
        }

        fileRef.delete()
            .addOnSuccessListener { deleteRecord() }
            .addOnFailureListener { e ->
                if (isObjectNotFound(e)) deleteRecord() else onError(e)
            }
    }

    companion object {
        const val COLLECTION = "resources"

        /** Firestore document ids can't be empty and can't contain '/'. */
        fun isValidId(id: String?): Boolean =
            !id.isNullOrBlank() && !id.contains('/')

        private fun isObjectNotFound(e: Exception): Boolean =
            e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND
    }
}
