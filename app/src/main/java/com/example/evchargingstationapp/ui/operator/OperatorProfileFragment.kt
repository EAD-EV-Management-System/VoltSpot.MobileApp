package com.example.evchargingstationapp.ui.operator

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
import com.example.evchargingstationapp.ui.auth.LoginActivity

class OperatorProfileFragment : Fragment() {

    private lateinit var prefs: PrefsHelper
    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_operator_profile, container, false)

        prefs = PrefsHelper(requireContext())

        tvUsername = view.findViewById(R.id.tvUsername)
        tvRole = view.findViewById(R.id.tvRole)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Display user info
        val userId = prefs.getUserId() ?: "operator23"
        val role = prefs.getUserRole() ?: "Station Operator"

        tvUsername.text = "User ID: $userId"
        tvRole.text = "Role: $role"

        btnLogout.setOnClickListener {
            logout()
        }

        return view
    }

    private fun logout() {
        prefs.clear()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}