
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
                openFeature("My Library", "Access your downloaded learning materials.")
            }

        view.findViewById<Button>(R.id.btnUploadResource)
            .setOnClickListener {
                openFeature("Upload Learning Material", "Share learning materials with other students.")
            }

        view.findViewById<Button>(R.id.btnProfile)
            .setOnClickListener {
                openFeature("My Profile", "View and manage your account information.")
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
