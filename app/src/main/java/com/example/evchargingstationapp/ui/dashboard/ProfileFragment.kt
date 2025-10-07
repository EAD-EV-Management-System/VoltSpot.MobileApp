package com.example.evchargingstationapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.UserDbHelper
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.facebook.shimmer.ShimmerFrameLayout
import android.widget.TextView

class ProfileFragment : Fragment() {

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var profileContainer: View
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvNic: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        profileContainer = view.findViewById(R.id.profileContainer)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvNic = view.findViewById(R.id.tvNic)

        // Start shimmer
        shimmerLayout.startShimmer()

        // Simulate data loading
        view.postDelayed({
            loadUserData()
        }, 1500)

        return view
    }

    private fun loadUserData() {
        val context = requireContext()
        val prefs = PrefsHelper(context)
        val db = UserDbHelper(context)

        val nic = prefs.getNic() ?: return
        val user = db.getUser(nic)

        if (user != null) {
            tvName.text = user.name
            tvEmail.text = user.email
            tvNic.text = user.nic
        }

        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        profileContainer.visibility = View.VISIBLE
    }
}
