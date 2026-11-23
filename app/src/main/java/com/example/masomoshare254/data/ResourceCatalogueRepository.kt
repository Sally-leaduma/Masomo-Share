package com.example.masomoshare254.data

import com.example.masomoshare254.model.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

/**
 * Member 4: reads (and lets owners delete) resources stored in Firestore.
 * Member 3 writes to the SAME collection, so COLLECTION must match their upload code.
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
        collection.document(resourceId).get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.toObject(Resource::class.java)?.copy(resourceId = doc.id))
            }
            .addOnFailureListener { e -> onError(e) }
    }

    /** Deletes the stored file first (best effort), then the Firestore record. */
    fun deleteResource(
        resource: Resource,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
                else -> null
            }
        } catch (e: IllegalArgumentException) {
            null // fileUrl is not a Firebase Storage URL
        }

        if (fileRef == null) {
            deleteRecord()
        } else {
            fileRef.delete().addOnCompleteListener { deleteRecord() } // continue even if the file is already gone
        }
    }

    companion object {
        const val COLLECTION = "resources"
    }
}
