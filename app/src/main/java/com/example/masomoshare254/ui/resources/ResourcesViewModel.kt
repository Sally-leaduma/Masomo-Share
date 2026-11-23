package com.example.masomoshare254.ui.resources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.masomoshare254.data.ResourceCatalogueRepository
import com.example.masomoshare254.model.Resource
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

sealed class CatalogueState {
    object Loading : CatalogueState()
    data class Success(val items: List<Resource>, val totalCount: Int) : CatalogueState()
    data class Error(val message: String) : CatalogueState()
}

data class FilterOptions(
    val subjects: List<String>,
    val levels: List<String>,
    val types: List<String>
)

class ResourcesViewModel(
    private val repository: ResourceCatalogueRepository = ResourceCatalogueRepository()
) : ViewModel() {

    private var allResources: List<Resource> = emptyList()
    private var listener: ListenerRegistration? = null

    private val _state = MutableLiveData<CatalogueState>(CatalogueState.Loading)
    val state: LiveData<CatalogueState> = _state

    private val _options = MutableLiveData(FilterOptions(emptyList(), emptyList(), emptyList()))
    val options: LiveData<FilterOptions> = _options

    // current search/filter selections (kept here so they survive screen rotation)
    var query: String = ""
        private set
    var subject: String? = null
        private set
    var level: String? = null
        private set
    var type: String? = null
        private set

    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || subject != null || level != null || type != null

    init {
        startListening()
    }

    fun startListening() {
        listener?.remove()
        _state.value = CatalogueState.Loading
        listener = repository.listenToResources(
            onSuccess = { list ->
                allResources = list.sortedWith(
                    compareByDescending<Resource> { it.uploadedAt }.thenBy { it.title.lowercase() }
                )
                _options.value = FilterOptions(
                    subjects = ResourceFilter.distinctValues(allResources.map { it.subject }),
                    levels = ResourceFilter.distinctValues(allResources.map { it.educationLevel }),
                    types = ResourceFilter.distinctValues(allResources.map { it.resourceType })
                )
                publish()
            },
            onError = { e -> _state.value = CatalogueState.Error(friendlyMessage(e)) }
        )
    }

    fun setQuery(value: String) { query = value; publish() }
    fun setSubject(value: String?) { subject = value; publish() }
    fun setLevel(value: String?) { level = value; publish() }
    fun setType(value: String?) { type = value; publish() }

    fun clearFilters() {
        query = ""; subject = null; level = null; type = null
        publish()
    }

    private fun publish() {
        _state.value = CatalogueState.Success(
            items = ResourceFilter.apply(allResources, query, subject, level, type),
            totalCount = allResources.size
        )
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }

    companion object {
        fun friendlyMessage(e: Exception): String =
            if (e is FirebaseFirestoreException) {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "You don't have permission to view resources. Please log in and try again."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "No internet connection. Check your network and try again."
                    else -> "Could not load resources. Please try again."
                }
            } else "Something went wrong. Please try again."
    }
}
