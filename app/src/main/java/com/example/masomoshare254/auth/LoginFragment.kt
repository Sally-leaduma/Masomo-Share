package com.example.masomoshare254.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.masomoshare254.R
import com.example.masomoshare254.databinding.FragmentLoginBinding

/**
 * LoginFragment handles User Authentication via Firebase Auth and local session persistence.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())

        // Auto-navigate to Dashboard if user is already logged in
        if (authRepository.isUserLoggedIn()) {
            navigateToDashboard()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvRegisterPrompt.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            try {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            } catch (_: Exception) {
                // Safe navigation fallback if NavHost is initialized differently
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        // Reset previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tvError.visibility = View.GONE

        // Input validation
        var isValid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email address is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        }

        if (!isValid) return

        showLoading(true)

        authRepository.loginUser(email, password) { result ->
            if (_binding == null || !isAdded) return@loginUser
            showLoading(false)
            result.onSuccess { userSession ->
                Toast.makeText(
                    requireContext(),
                    "Welcome back, ${userSession.displayName}!",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToDashboard()
            }.onFailure { exception ->
                binding.tvError.text = exception.localizedMessage ?: "Login failed. Please check your credentials."
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun handleForgotPassword() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address to receive password reset link"
            return
        }

        authRepository.sendPasswordReset(email) { _, message ->
            if (_binding == null || !isAdded) return@sendPasswordReset
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToDashboard() {
        if (!isAdded) return
        try {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Authenticated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
