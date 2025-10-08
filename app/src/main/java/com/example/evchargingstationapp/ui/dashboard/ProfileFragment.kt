package com.example.evchargingstationapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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

    private fun logout() {
        userRepository.logout()

        // Navigate to login screen
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
