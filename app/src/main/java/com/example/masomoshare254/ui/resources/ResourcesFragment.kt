
package com.example.masomoshare254.ui.resources

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.masomoshare254.R
import com.example.masomoshare254.model.Resource

class ResourcesFragment : Fragment(R.layout.fragment_resources) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnBackResources)
            .setOnClickListener {
                findNavController().navigateUp()
            }


        val sampleResources = listOf(
            Resource(
                resourceId = "1",
                title = "Introduction to Programming",
                description = "Basic programming concepts and fundamentals.",
                subject = "Computer Science",
                educationLevel = "University",
                topic = "Programming Basics",
                resourceType = "PDF",
                fileUrl = "",
                contributorId = "sample_user"
            ),
            Resource(
                resourceId = "2",
                title = "Database Management Notes",
                description = "Introduction to databases and SQL.",
                subject = "Database Systems",
                educationLevel = "University",
                topic = "Database Management",
                resourceType = "PDF",
                fileUrl = "",
                contributorId = "sample_user"
            ),
            Resource(
                resourceId = "3",
                title = "Networking Fundamentals",
                description = "Learn the basics of computer networks.",
                subject = "Computer Networks",
                educationLevel = "University",
                topic = "Networking Basics",
                resourceType = "PDF",
                fileUrl = "",
                contributorId = "sample_user"
            )
        )

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.resourcesRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ResourceAdapter(sampleResources)

        view.findViewById<TextView>(R.id.emptyResourcesText).visibility =
            if (sampleResources.isEmpty()) View.VISIBLE else View.GONE
    }
}
