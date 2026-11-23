package com.example.masomoshare254.ui.resources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masomoshare254.R
import com.example.masomoshare254.model.Resource

class ResourceAdapter(
    private val onItemClick: (Resource) -> Unit
) : ListAdapter<Resource, ResourceAdapter.ResourceViewHolder>(DiffCallback) {

    class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.resourceTitle)
        val subject: TextView = itemView.findViewById(R.id.resourceSubject)
        val level: TextView = itemView.findViewById(R.id.resourceLevel)
        val description: TextView = itemView.findViewById(R.id.resourceDescription)
        val type: TextView = itemView.findViewById(R.id.resourceType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = getItem(position)
        holder.title.text = resource.title
        holder.subject.text = "Subject: ${resource.subject}"
        holder.level.text = "Level: ${resource.educationLevel}"
        holder.description.text = resource.description
        holder.type.text = resource.resourceType
        holder.itemView.setOnClickListener { onItemClick(resource) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Resource>() {
        override fun areItemsTheSame(old: Resource, new: Resource) = old.resourceId == new.resourceId
        override fun areContentsTheSame(old: Resource, new: Resource) = old == new
    }
}
