package com.example.masomoshare254.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.masomoshare254.R
import com.example.masomoshare254.auth.AuthRepository

class MyProfileFragment : Fragment(R.layout.fragment_my_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = view.findViewById<TextView>(R.id.tvProfileName)
        val email = view.findViewById<TextView>(R.id.tvProfileEmail)
        val role = view.findViewById<TextView>(R.id.tvProfileRole)
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        val authRepository = AuthRepository(requireContext())
        val session = authRepository.getCurrentUserSession()

        if (session != null) {
            name.text = session.displayName.ifBlank { "Not provided" }
            email.text = session.email
            role.text = session.role
        } else {
            name.text = "Not available"
            email.text = "Not available"
            role.text = "Not available"
        }

        logoutButton.setOnClickListener {
            authRepository.logout()

            Toast.makeText(
                requireContext(),
                "You have logged out.",
                Toast.LENGTH_SHORT
            ).show()

            val options = NavOptions.Builder()
                .setPopUpTo(R.id.dashboardFragment, true)
                .build()

            findNavController().navigate(
                R.id.loginFragment,
                null,
                options
            )
        }
    }
}