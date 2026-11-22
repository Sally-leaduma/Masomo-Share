
package com.example.masomoshare254.ui.feature

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.masomoshare254.R
import android.widget.Button

class FeatureFragment : Fragment(R.layout.fragment_feature) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val title = arguments?.getString("screenTitle") ?: "Feature"
        val description = arguments?.getString("screenDescription") ?: ""

        view.findViewById<TextView>(R.id.featureTitle).text = title
        view.findViewById<TextView>(R.id.featureDescription).text = description
    }
}
