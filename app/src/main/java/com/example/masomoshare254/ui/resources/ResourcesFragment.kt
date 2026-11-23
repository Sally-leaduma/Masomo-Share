package com.example.masomoshare254.ui.resources

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masomoshare254.R

class ResourcesFragment : Fragment(R.layout.fragment_resources) {

    private val viewModel: ResourcesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progress = view.findViewById<ProgressBar>(R.id.resourcesProgress)
        val statusText = view.findViewById<TextView>(R.id.emptyResourcesText)
        val retryButton = view.findViewById<Button>(R.id.btnRetry)
        val countText = view.findViewById<TextView>(R.id.resultsCountText)
        val clearButton = view.findViewById<Button>(R.id.btnClearFilters)
        val recyclerView = view.findViewById<RecyclerView>(R.id.resourcesRecyclerView)
        val searchInput = view.findViewById<EditText>(R.id.searchResourcesInput)

        view.findViewById<Button>(R.id.btnBackResources).setOnClickListener {
            findNavController().navigateUp()
        }

        // RecyclerView
        val adapter = ResourceAdapter { resource ->
            findNavController().navigate(
                R.id.action_resources_to_details,
                bundleOf("resourceId" to resource.resourceId)
            )
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Search (restore text first so the listener is not triggered by it)
        searchInput.setText(viewModel.query)
        searchInput.doOnTextChanged { text, _, _, _ -> viewModel.setQuery(text?.toString().orEmpty()) }

        // Filter dropdowns are rebuilt from whatever values exist in the uploaded data
        viewModel.options.observe(viewLifecycleOwner) { o ->
            bindDropdown(view.findViewById(R.id.subjectDropdown), "All subjects", o.subjects, viewModel.subject) { viewModel.setSubject(it) }
            bindDropdown(view.findViewById(R.id.levelDropdown), "All levels", o.levels, viewModel.level) { viewModel.setLevel(it) }
            bindDropdown(view.findViewById(R.id.typeDropdown), "All types", o.types, viewModel.type) { viewModel.setType(it) }
        }

        retryButton.setOnClickListener { viewModel.startListening() }
        clearButton.setOnClickListener {
            viewModel.clearFilters()
            searchInput.setText("")
            // re-bind dropdowns so they show "All ..." again
            viewModel.options.value?.let { o ->
                bindDropdown(view.findViewById(R.id.subjectDropdown), "All subjects", o.subjects, null) { viewModel.setSubject(it) }
                bindDropdown(view.findViewById(R.id.levelDropdown), "All levels", o.levels, null) { viewModel.setLevel(it) }
                bindDropdown(view.findViewById(R.id.typeDropdown), "All types", o.types, null) { viewModel.setType(it) }
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CatalogueState.Loading -> {
                    progress.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    statusText.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    countText.visibility = View.GONE
                }
                is CatalogueState.Error -> {
                    progress.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    countText.visibility = View.GONE
                    statusText.text = state.message
                    statusText.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                }
                is CatalogueState.Success -> {
                    progress.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    adapter.submitList(state.items)
                    recyclerView.visibility = if (state.items.isEmpty()) View.GONE else View.VISIBLE

                    statusText.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
                    statusText.text = if (state.totalCount == 0)
                        "No learning materials have been shared yet."
                    else
                        "No resources match your search or filters."

                    countText.visibility = if (state.totalCount == 0) View.GONE else View.VISIBLE
                    countText.text = "Showing ${state.items.size} of ${state.totalCount} resources"
                    clearButton.visibility = if (viewModel.hasActiveFilters) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun bindDropdown(
        dropdown: AutoCompleteTextView,
        allLabel: String,
        values: List<String>,
        selected: String?,
        onSelected: (String?) -> Unit
    ) {
        val labels = listOf(allLabel) + values
        dropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        dropdown.setText(selected ?: allLabel, false) // false = don't filter the list by this text
        dropdown.setOnItemClickListener { _, _, position, _ ->
            onSelected(if (position == 0) null else labels[position])
        }
    }
}
