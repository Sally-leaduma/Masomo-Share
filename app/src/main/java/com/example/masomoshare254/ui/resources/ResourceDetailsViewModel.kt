package com.example.masomoshare254.ui.resources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.masomoshare254.data.ResourceCatalogueRepository
import com.example.masomoshare254.model.Resource
import com.google.firebase.auth.FirebaseAuth

sealed class DetailsState {
    object Loading : DetailsState()
    data class Loaded(
        val resource: Resource,
        val canDelete: Boolean,
        val isDeleting: Boolean = false
    ) : DetailsState()
    data class Error(val message: String, val canRetry: Boolean = true) : DetailsState()
    object Deleted : DetailsState()
}

class ResourceDetailsViewModel(
    private val repository: ResourceCatalogueRepository = ResourceCatalogueRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableLiveData<DetailsState>(DetailsState.Loading)
    val state: LiveData<DetailsState> = _state

    /** One-off message (e.g. a failed delete) shown without replacing the loaded resource. */
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    fun messageShown() { _message.value = null }

    private var loadedId: String? = null

    fun load(resourceId: String, force: Boolean = false) {
        if (!ResourceCatalogueRepository.isValidId(resourceId)) {
            loadedId = null
            _state.value = DetailsState.Error("This resource link is invalid.", canRetry = false)
            return
        }
        if (!force && loadedId == resourceId) return
        loadedId = resourceId
        _state.value = DetailsState.Loading
        repository.getResource(
            resourceId,
            onSuccess = { resource ->
                _state.value = if (resource == null) {
                    DetailsState.Error("This resource no longer exists.", canRetry = false)
                } else {
                    DetailsState.Loaded(resource, canDelete = isOwner(resource))
                }
            },
            onError = { e -> _state.value = DetailsState.Error(ResourcesViewModel.friendlyMessage(e)) }
        )
    }

    fun delete() {
        val current = _state.value as? DetailsState.Loaded ?: return
        if (current.isDeleting) return // ignore repeated taps while a delete is running
        if (!isOwner(current.resource)) {
            _message.value = "Only the person who uploaded this resource can delete it."
            return
        }

        _state.value = current.copy(isDeleting = true)
        repository.deleteResource(
            current.resource,
            onSuccess = { _state.value = DetailsState.Deleted },
            onError = { e ->
                // keep showing the resource; just tell the user it failed
                _state.value = current.copy(isDeleting = false)
                _message.value = "Could not delete the resource. " + ResourcesViewModel.friendlyMessage(e)
            }
        )
    }

    // UX check only. The real enforcement is in the Firebase security rules.
    private fun isOwner(resource: Resource): Boolean {
        val uid = auth.currentUser?.uid
        return uid != null && (uid == resource.uploaderId || uid == resource.contributorId)
    }
}
