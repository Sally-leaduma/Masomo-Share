package com.example.masomoshare254

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Arguments (Bundle keys):
 *  "title"     : String
 *  "localPath" : String? - downloaded file (used first, works offline)
 *  "fileUrl"   : String? - used only if there is no local file (needs internet)
 */
class PdfViewerFragment : Fragment() {

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()   // PdfRenderer allows only one open page at a time

    private lateinit var rvPages: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvLoading: TextView
    private lateinit var errorBox: View
    private lateinit var tvError: TextView
    private lateinit var btnRetry: Button

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View? =
        i.inflate(R.layout.fragment_pdf_viewer, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tvPdfTitle).text = arguments?.getString("title") ?: "Resource"
        rvPages = view.findViewById(R.id.rvPdfPages)
        progress = view.findViewById(R.id.pdfProgress)
        tvLoading = view.findViewById(R.id.tvPdfLoading)
        errorBox = view.findViewById(R.id.pdfErrorBox)
        tvError = view.findViewById(R.id.tvPdfError)
        btnRetry = view.findViewById(R.id.btnPdfRetry)
        rvPages.layoutManager = LinearLayoutManager(requireContext())
        btnRetry.setOnClickListener { loadPdf() }
        loadPdf()
    }

    private fun loadPdf() {
        showLoading("Opening...")
        viewLifecycleOwner.lifecycleScope.launch {
            val localPath = arguments?.getString("localPath")
            val fileUrl = arguments?.getString("fileUrl")

            // 1. Prefer the downloaded copy (works offline)
            var file: File? = localPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { FileStorage.isValid(it) }

            // 2. Otherwise stream from the URL into the cache folder
            if (file == null) {
                if (fileUrl.isNullOrBlank()) return@launch showError("File not found. Please download it again.")
                if (!NetworkUtils.isOnline(requireContext())) {
                    return@launch showError("No internet connection and this file is not downloaded.")
                }
                val temp = File(requireContext().cacheDir, "preview_${fileUrl.hashCode()}.pdf")
                when (val r = FileDownloader().download(fileUrl, temp) { p -> showLoading("Loading... $p%") }) {
                    is FileResult.Success -> file = r.file
                    is FileResult.Failure -> return@launch showError(r.message)
                }
            }

            try {
                val (pfd, pdf) = withContext(Dispatchers.IO) {
                    val d = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    d to PdfRenderer(d)
                }
                descriptor = pfd
                renderer = pdf
                if (pdf.pageCount == 0) return@launch showError("This PDF has no pages.")
                rvPages.adapter = PdfPageAdapter(pdf.pageCount, viewLifecycleOwner.lifecycleScope, ::renderPage)
                showContent()
            } catch (e: Exception) {
                showError("This file could not be opened. It may be corrupted.")
            }
        }
    }

    private suspend fun renderPage(index: Int, targetWidth: Int): Bitmap =
        renderMutex.withLock {
            withContext(Dispatchers.IO) {
                val page = renderer!!.openPage(index)
                val scale = targetWidth.toFloat() / page.width
                val bmp = Bitmap.createBitmap(targetWidth, (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bmp
            }
        }

    private fun showLoading(msg: String) {
        progress.visibility = View.VISIBLE; tvLoading.visibility = View.VISIBLE; tvLoading.text = msg
        errorBox.visibility = View.GONE; rvPages.visibility = View.GONE
    }

    private fun showError(msg: String) {
        progress.visibility = View.GONE; tvLoading.visibility = View.GONE
        rvPages.visibility = View.GONE; errorBox.visibility = View.VISIBLE; tvError.text = msg
    }

    private fun showContent() {
        progress.visibility = View.GONE; tvLoading.visibility = View.GONE
        errorBox.visibility = View.GONE; rvPages.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        rvPages.adapter = null
        renderer?.close(); renderer = null
        descriptor?.close(); descriptor = null
        super.onDestroyView()
    }
}
