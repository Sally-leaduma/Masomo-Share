package com.example.masomoshare254

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class MyLibraryFragment : Fragment() {

    private lateinit var rvDownloads: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DownloadedResourceAdapter
    private val downloadService by lazy { DownloadService(requireContext().applicationContext) }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View? =
        i.inflate(R.layout.fragment_my_library, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvDownloads = view.findViewById(R.id.rvDownloads)
        tvEmpty = view.findViewById(R.id.tvEmptyLibrary)
        adapter = DownloadedResourceAdapter(onOpen = ::openResource, onLongPress = ::confirmDelete)
        rvDownloads.layoutManager = LinearLayoutManager(requireContext())
        rvDownloads.adapter = adapter
    }

    // Reload every time the screen is shown so new downloads appear
    override fun onResume() {
        super.onResume()
        loadLibrary()
    }

    private fun currentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun loadLibrary() {
        val uid = currentUserId() ?: return showEmpty("Please log in to see your library.")
        viewLifecycleOwner.lifecycleScope.launch {
            val records = try {
                LibraryProvider.store.getDownloadsForUser(uid)   // local DB: works offline
            } catch (e: Exception) {
                return@launch showEmpty("Could not load your library.")
            }
            if (records.isEmpty()) {
                showEmpty("No downloaded resources yet.\nDownload from the Catalogue to read offline.")
            } else {
                tvEmpty.visibility = View.GONE
                rvDownloads.visibility = View.VISIBLE
                adapter.submitList(records)
            }
        }
    }

    private fun showEmpty(message: String) {
        adapter.submitList(emptyList())
        rvDownloads.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = message
    }

    private fun openResource(record: DownloadRecord) {
        if (!FileStorage.isValid(File(record.localPath))) {
            // Record exists but file is gone: clean up so they stay in sync
            Toast.makeText(requireContext(), "File missing. Please download it again.", Toast.LENGTH_LONG).show()
            viewLifecycleOwner.lifecycleScope.launch {
                LibraryProvider.store.delete(record.userId, record.resourceId)
                loadLibrary()
            }
            return
        }
        val args = Bundle().apply {
            putString("title", record.title)
            putString("localPath", record.localPath)
        }
        // Needs a destination with this id in nav_graph.xml (ask Member 1)
        findNavController().navigate(R.id.pdfViewerFragment, args)
    }

    private fun confirmDelete(record: DownloadRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove download?")
            .setMessage("\"${record.title}\" will be deleted from this device.")
            .setPositiveButton("Remove") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    downloadService.deleteDownload(record)
                    loadLibrary()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
