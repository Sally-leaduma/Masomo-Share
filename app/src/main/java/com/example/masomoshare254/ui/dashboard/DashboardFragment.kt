package com.example.masomoshare254.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.masomoshare254.R

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnBrowseResources)
            .setOnClickListener {
                findNavController().navigate(R.id.action_dashboard_to_resources)
            }

        view.findViewById<Button>(R.id.btnMyLibrary)
            .setOnClickListener {
                findNavController().navigate(R.id.action_dashboard_to_library)
            }

        view.findViewById<Button>(R.id.btnUploadResource)
            .setOnClickListener {
                findNavController().navigate(R.id.action_dashboard_to_upload)
            }

        view.findViewById<Button>(R.id.btnProfile)
            .setOnClickListener {
                findNavController().navigate(R.id.action_dashboard_to_profile)
            }
    }

    private fun openFeature(title: String, description: String) {
        val bundle = Bundle().apply {
            putString("screenTitle", title)
            putString("screenDescription", description)
        }

        findNavController().navigate(
            R.id.action_dashboard_to_feature,
            bundle
        )
    }
}