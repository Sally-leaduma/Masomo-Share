package com.example.masomoshare254.ui.resources

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.masomoshare254.R
import com.example.masomoshare254.model.Resource
import com.example.masomoshare254.DownloadService

class ResourceDetailsFragment : Fragment(R.layout.fragment_resource_details) {


    private val viewModel: ResourceDetailsViewModel by viewModels()

    private val downloadService by lazy {
        DownloadService(requireContext().applicationContext)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resourceId = arguments?.getString("resourceId").orEmpty()
        val progress = view.findViewById<ProgressBar>(R.id.detailsProgress)
        val content = view.findViewById<View>(R.id.detailsContent)
        val errorText = view.findViewById<TextView>(R.id.detailsError)
        val retry = view.findViewById<Button>(R.id.btnDetailsRetry)
        val openButton = view.findViewById<Button>(R.id.btnOpenResource)
        val deleteButton = view.findViewById<Button>(R.id.btnDeleteResource)

        view.findViewById<Button>(R.id.btnBackDetails).setOnClickListener {
            findNavController().navigateUp()
        }
        retry.setOnClickListener { viewModel.load(resourceId, force = true) }

        viewModel.load(resourceId)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            progress.visibility = if (state is DetailsState.Loading) View.VISIBLE else View.GONE
            content.visibility = if (state is DetailsState.Loaded) View.VISIBLE else View.GONE
            errorText.visibility = if (state is DetailsState.Error) View.VISIBLE else View.GONE
            retry.visibility =
                if (state is DetailsState.Error && state.canRetry) View.VISIBLE else View.GONE

            when (state) {
                is DetailsState.Loaded -> {
                    val r = state.resource
                    view.findViewById<TextView>(R.id.detailTitle).text = r.title
                    view.findViewById<TextView>(R.id.detailType).text = r.resourceType
                    view.findViewById<TextView>(R.id.detailSubject).text = "Subject: ${r.subject}"
                    view.findViewById<TextView>(R.id.detailLevel).text = "Education level: ${r.educationLevel}"
                    view.findViewById<TextView>(R.id.detailTopic).text = "Topic: ${r.topic}"
                    view.findViewById<TextView>(R.id.detailDescription).text = r.description

                    openButton.isEnabled = !state.isDeleting
                    openButton.setOnClickListener { openResource(r) }

                    deleteButton.visibility = if (state.canDelete) View.VISIBLE else View.GONE
                    deleteButton.isEnabled = !state.isDeleting
                    deleteButton.text = if (state.isDeleting) "Deleting..." else "Delete my upload"
                    deleteButton.setOnClickListener { confirmDelete(r) }
                }
                is DetailsState.Error -> errorText.text = state.message
                is DetailsState.Deleted -> {
                    Toast.makeText(requireContext(), "Resource deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is DetailsState.Loading -> Unit
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.messageShown()
            }
        }
    }

    /**
     * TEMPORARY: hands the file URL to whatever app can view it.
     * To be replaced with Member 5's agreed PDF viewer / download workflow.
     */
    private fun openResource(resource: Resource) {
        if (resource.fileUrl.isBlank()) {
            Toast.makeText(
                requireContext(),
                "This resource has no downloadable file.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Toast.makeText(
                    requireContext(),
                    "Downloading resource...",
                    Toast.LENGTH_SHORT
                ).show()

                downloadService.download(resource)

                Toast.makeText(
                    requireContext(),
                    "Downloaded successfully! Find it in My Library.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Download failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmDelete(resource: Resource) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete resource?")
            .setMessage("\"${resource.title}\" will be removed for everyone. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.delete() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
