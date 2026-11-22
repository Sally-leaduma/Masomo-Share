
package com.example.masomoshare254.ui.resources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.masomoshare254.R
import com.example.masomoshare254.model.Resource

class ResourceAdapter(
    private val resources: List<Resource>
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    class ResourceViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.resourceTitle)
        val subject: TextView = itemView.findViewById(R.id.resourceSubject)
        val description: TextView =
            itemView.findViewById(R.id.resourceDescription)
        val type: TextView = itemView.findViewById(R.id.resourceType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)

        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ResourceViewHolder,
        position: Int
    ) {
        val resource = resources[position]

        holder.title.text = resource.title
        holder.subject.text = "Subject: ${resource.subject}"
        holder.description.text = resource.description
        holder.type.text = resource.resourceType
    }

    override fun getItemCount(): Int = resources.size
}
