package com.example.masomoshare254

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadedResourceAdapter(
    private val onOpen: (DownloadRecord) -> Unit,
    private val onLongPress: (DownloadRecord) -> Unit
) : ListAdapter<DownloadRecord, DownloadedResourceAdapter.ViewHolder>(Diff) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val date: TextView = view.findViewById(R.id.tvDate)
        val open: Button = view.findViewById(R.id.btnOpen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_downloaded_resource, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title
        holder.meta.text = "${item.subject} • ${item.educationLevel}"
        holder.date.text = "Downloaded ${dateFormat.format(Date(item.downloadedAt))} • ${item.fileSizeBytes / 1024} KB"
        holder.open.setOnClickListener { onOpen(item) }
        holder.itemView.setOnClickListener { onOpen(item) }
        holder.itemView.setOnLongClickListener { onLongPress(item); true }
    }

    private object Diff : DiffUtil.ItemCallback<DownloadRecord>() {
        override fun areItemsTheSame(a: DownloadRecord, b: DownloadRecord) =
            a.userId == b.userId && a.resourceId == b.resourceId
        override fun areContentsTheSame(a: DownloadRecord, b: DownloadRecord) = a == b
    }
}
