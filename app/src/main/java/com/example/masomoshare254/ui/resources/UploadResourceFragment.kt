package com.example.masomoshare254.ui.resources

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.example.masomoshare254.databinding.FragmentUploadResourceBinding
import com.example.masomoshare254.model.Resource

class UploadResourceFragment : Fragment() {

    private var _binding: FragmentUploadResourceBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var selectedUri: Uri? = null
    private var selectedFileName: String? = null
    private var selectedMimeType: String? = null
    private var currentUpload: UploadTask? = null

    // Agreed file formats and size limit (confirm with the group)
    private val allowedMimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
    private val maxFileSizeBytes = 10L * 1024 * 1024 // 10 MB

    // Dropdown values. KEEP IDENTICAL to Member 4's filters so search/filter works.
    private val subjects = listOf(
        "Mathematics", "English", "Kiswahili", "Biology", "Chemistry",
        "Physics", "History", "Geography", "Computer Studies", "Business Studies", "Other"
    )
    private val educationLevels = listOf(
        "Primary", "Junior Secondary", "Senior Secondary", "College", "University"
    )
    private val resourceTypes = listOf(
        "Notes", "Past Paper", "Book", "Revision Guide", "Worksheet", "Other"
    )

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) handleFileSelected(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadResourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdown(binding.actvSubject, subjects)
        setupDropdown(binding.actvEducationLevel, educationLevels)
        setupDropdown(binding.actvResourceType, resourceTypes)

        binding.btnChooseFile.setOnClickListener { filePicker.launch(allowedMimeTypes) }
        binding.btnUpload.setOnClickListener { onUploadClicked() }
        binding.btnCancelUpload.setOnClickListener {
            currentUpload?.cancel()
        }
    }

    private fun setupDropdown(
        view: com.google.android.material.textfield.MaterialAutoCompleteTextView,
        items: List<String>
    ) {
        view.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        )
    }

    // ---------------------------------------------------------------- File selection

    private fun handleFileSelected(uri: Uri) {
        val mime = requireContext().contentResolver.getType(uri)
        var name: String? = null
        var size: Long = -1

        requireContext().contentResolver
            .query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }

        // Validation
        if (mime == null || mime !in allowedMimeTypes) {
            clearSelection()
            binding.tvFileName.text = "Unsupported file. Choose a PDF, JPG or PNG."
            return
        }
        if (size <= 0) {
            clearSelection()
            binding.tvFileName.text = "The selected file is empty or unreadable."
            return
        }
        if (size > maxFileSizeBytes) {
            clearSelection()
            binding.tvFileName.text = "File is too large (max 10 MB)."
            return
        }

        selectedUri = uri
        selectedFileName = name ?: "resource_${System.currentTimeMillis()}"
        selectedMimeType = mime
        binding.tvFileName.text = selectedFileName
    }

    private fun clearSelection() {
        selectedUri = null
        selectedFileName = null
        selectedMimeType = null
    }

    // ---------------------------------------------------------------- Validation

    private fun validateForm(): Boolean {
        var valid = true

        fun check(
            layout: com.google.android.material.textfield.TextInputLayout,
            value: String,
            message: String
        ) {
            if (value.isBlank()) {
                layout.error = message
                valid = false
            } else {
                layout.error = null
            }
        }

        check(binding.tilTitle, binding.etTitle.text.toString(), "Title is required")
        check(binding.tilDescription, binding.etDescription.text.toString(), "Description is required")
        check(binding.tilSubject, binding.actvSubject.text.toString(), "Select a subject")
        check(binding.tilEducationLevel, binding.actvEducationLevel.text.toString(), "Select an education level")
        check(binding.tilTopic, binding.etTopic.text.toString(), "Topic is required")
        check(binding.tilResourceType, binding.actvResourceType.text.toString(), "Select a resource type")

        if (selectedUri == null) {
            binding.tvFileName.text = "Please choose a file to upload."
            valid = false
        }
        return valid
    }

    // ---------------------------------------------------------------- Upload

    private fun onUploadClicked() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showMessage("You need to be logged in to upload.")
            return
        }
        if (!validateForm()) return

        val uri = selectedUri ?: return
        val fileName = selectedFileName ?: return
        val mime = selectedMimeType ?: return

        // Pre-generate the Firestore document ID so the Storage path and doc match
        val docRef = firestore.collection("resources").document()
        val storagePath = "resources/$uid/${docRef.id}_$fileName"
        val storageRef = storage.reference.child(storagePath)

        setUploading(true)

        val task = storageRef.putFile(uri)
        currentUpload = task

        task.addOnProgressListener { snapshot ->
            if (_binding == null) return@addOnProgressListener
            val percent = if (snapshot.totalByteCount > 0)
                (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt() else 0
            binding.progressUpload.setProgressCompat(percent, true)
            binding.tvProgress.text = "Uploading… $percent%"
        }.continueWithTask { t ->
            if (!t.isSuccessful) throw t.exception ?: Exception("Upload failed")
            storageRef.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            saveMetadata(docRef.id, uid, fileName, mime, storagePath, downloadUrl.toString())
        }.addOnFailureListener { e ->
            if (_binding == null) return@addOnFailureListener
            setUploading(false)
            showMessage(
                if (task.isCanceled) "Upload cancelled."
                else "Upload failed: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    private fun saveMetadata(
        docId: String,
        uid: String,
        fileName: String,
        mime: String,
        storagePath: String,
        fileUrl: String
    ) {
        // Field names must match the existing Resource.kt. Adjust if yours differ.
        val resource = Resource(
            resourceId = docId,
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            subject = binding.actvSubject.text.toString(),
            educationLevel = binding.actvEducationLevel.text.toString(),
            topic = binding.etTopic.text.toString().trim(),
            resourceType = binding.actvResourceType.text.toString(),
            fileUrl = fileUrl,
            fileName = fileName,
            fileType = mime,
            storagePath = storagePath,
            contributorId = uid,
            uploaderId = uid,
            uploadedAt = System.currentTimeMillis()
        )

        firestore.collection("resources").document(docId)
            .set(resource)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                setUploading(false)
                resetForm()
                showMessage("Resource uploaded successfully!")
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                // Roll back the orphaned file so Storage and Firestore stay consistent
                storage.reference.child(storagePath).delete()
                setUploading(false)
                showMessage("Could not save resource details: ${e.localizedMessage}")
            }
    }

    // ---------------------------------------------------------------- UI helpers

    private fun setUploading(uploading: Boolean) {
        binding.progressGroup.visibility = if (uploading) View.VISIBLE else View.GONE
        binding.progressUpload.progress = 0
        binding.tvProgress.text = ""
        binding.btnUpload.isEnabled = !uploading
        binding.btnChooseFile.isEnabled = !uploading
        if (!uploading) currentUpload = null
    }

    private fun resetForm() {
        binding.etTitle.text?.clear()
        binding.etDescription.text?.clear()
        binding.actvSubject.setText("", false)
        binding.actvEducationLevel.setText("", false)
        binding.etTopic.text?.clear()
        binding.actvResourceType.setText("", false)
        binding.tvFileName.text = "No file selected"
        clearSelection()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
