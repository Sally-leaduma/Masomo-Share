package com.example.masomoshare254

import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Shows each PDF page as an image; pages are rendered lazily as the user scrolls. */
class PdfPageAdapter(
    private val pageCount: Int,
    private val scope: CoroutineScope,
    private val render: suspend (pageIndex: Int, widthPx: Int) -> Bitmap
) : RecyclerView.Adapter<PdfPageAdapter.VH>() {

    class VH(val image: ImageView) : RecyclerView.ViewHolder(image) {
        var job: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = ImageView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            minimumHeight = 600
            setPadding(0, 0, 0, 8)
        }
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.job?.cancel()
        holder.image.setImageBitmap(null)
        val width = holder.image.resources.displayMetrics.widthPixels
        holder.job = scope.launch {
            holder.image.setImageBitmap(render(position, width))
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.job?.cancel()
        super.onViewRecycled(holder)
    }

    override fun getItemCount() = pageCount
}
