package com.example.masomoshare254.ui.resources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.masomoshare254.data.ResourceCatalogueRepository
import com.example.masomoshare254.model.Resource
import com.google.firebase.auth.FirebaseAuth

sealed class DetailsState {
    object Loading : DetailsState()
    data class Loaded(val resource: Resource, val canDelete: Boolean) : DetailsState()
    data class Error(val message: String) : DetailsState()
    object Deleted : DetailsState()
}

class ResourceDetailsViewModel(
    private val repository: ResourceCatalogueRepository = ResourceCatalogueRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableLiveData<DetailsState>(DetailsState.Loading)
    val state: LiveData<DetailsState> = _state
    private var loadedId: String? = null

    fun load(resourceId: String, force: Boolean = false) {
        if (!force && loadedId == resourceId) return
        loadedId = resourceId
        _state.value = DetailsState.Loading
        repository.getResource(
            resourceId,
            onSuccess = { resource ->
                _state.value = if (resource == null) {
                    DetailsState.Error("This resource no longer exists.")
                } else {
                    val uid = auth.currentUser?.uid
                    // only the contributor may delete their own upload
                    DetailsState.Loaded(resource, canDelete = uid != null && (uid == resource.contributorId || uid == resource.uploaderId))
                }
            },
            onError = { e -> _state.value = DetailsState.Error(ResourcesViewModel.friendlyMessage(e)) }
        )
    }

    fun delete(resource: Resource) {
        _state.value = DetailsState.Loading
        repository.deleteResource(
            resource,
            onSuccess = { _state.value = DetailsState.Deleted },
            onError = { e -> _state.value = DetailsState.Error(ResourcesViewModel.friendlyMessage(e)) }
        )
    }
}
