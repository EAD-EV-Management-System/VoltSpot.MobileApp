package com.example.evchargingstationapp.ui.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.local.UserDbHelper
import com.example.evchargingstationapp.data.repository.UserRepository
import com.example.evchargingstationapp.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    private lateinit var profileContainer: View
    private lateinit var loadingContainer: View
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvNic: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnDeactivate: Button
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        userRepository = UserRepository(requireContext())

        loadingContainer = view.findViewById(R.id.loadingContainer)
        profileContainer = view.findViewById(R.id.profileContainer)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvNic = view.findViewById(R.id.tvNic)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnDeactivate = view.findViewById(R.id.btnDeactivate)

        // Show loading
        loadingContainer.visibility = View.VISIBLE
        profileContainer.visibility = View.GONE

        // Load data
        view.postDelayed({
            loadUserData()
        }, 500)

        // Logout button
        btnLogout.setOnClickListener {
            logout()
        }

        // Deactivate button
        btnDeactivate.setOnClickListener {
            showDeactivateConfirmationDialog()
        }

        return view
    }

    private fun loadUserData() {
        val context = requireContext()
        val prefs = PrefsHelper(context)
        val db = UserDbHelper(context)

        // Get NIC from prefs
        val userNic = prefs.getNic()
        val user = userNic?.let { db.getUser(it) }

        if (user != null) {
            tvName.text = "${user.firstName} ${user.lastName}"
            tvEmail.text = user.email
            tvNic.text = user.nic
        }

        loadingContainer.visibility = View.GONE
        profileContainer.visibility = View.VISIBLE
    }

    private fun showDeactivateConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account? You will be logged out and your account will be deactivated.")
            .setPositiveButton("Yes, Deactivate") { _, _ ->
                deactivateAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateAccount() {
        // Show loading
        loadingContainer.visibility = View.VISIBLE
        profileContainer.visibility = View.GONE
        btnDeactivate.isEnabled = false

        userRepository.deactivateAccount { success, message ->
            requireActivity().runOnUiThread {
                if (success) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    // Clear session and navigate to login
                    userRepository.logout()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    // Re-enable button and show profile again
                    btnDeactivate.isEnabled = true
                    loadingContainer.visibility = View.GONE
                    profileContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun logout() {
        userRepository.logout()

        // Navigate to login screen
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
