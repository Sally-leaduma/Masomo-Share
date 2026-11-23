package com.example.masomoshare254.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.masomoshare254.R
import com.example.masomoshare254.data.model.UserSession
import com.example.masomoshare254.databinding.FragmentRegisterBinding

/**
 * RegisterFragment handles User Registration and account creation.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())

        setupRoleDropdown()
        setupListeners()
    }

    private fun setupRoleDropdown() {
        val roles = arrayOf(UserSession.ROLE_STUDENT, UserSession.ROLE_TEACHER)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.actRole.setAdapter(adapter)
        binding.actRole.setText(roles[0], false)
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.tvLoginPrompt.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            try {
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            } catch (_: Exception) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun performRegistration() {
        val fullName = binding.etFullName.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val role = binding.actRole.text?.toString()?.ifBlank { UserSession.ROLE_STUDENT } ?: UserSession.ROLE_STUDENT
        val password = binding.etPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        // Reset errors
        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tvError.visibility = View.GONE

        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full Name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email address is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (!isValid) return

        showLoading(true)

        authRepository.registerUser(email, password, fullName, role) { result ->
            if (_binding == null || !isAdded) return@registerUser
            showLoading(false)
            result.onSuccess { userSession ->
                Toast.makeText(
                    requireContext(),
                    "Account created successfully as ${userSession.role}! Welcome, ${userSession.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToDashboard()
            }.onFailure { exception ->
                binding.tvError.text = exception.localizedMessage ?: "Registration failed. Please try again."
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToDashboard() {
        if (!isAdded) return
        try {
            findNavController().navigate(R.id.action_registerFragment_to_dashboardFragment)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressBarRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
