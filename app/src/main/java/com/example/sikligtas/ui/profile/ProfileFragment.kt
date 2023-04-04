package com.example.sikligtas.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.sikligtas.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Get the current user
        val user = FirebaseAuth.getInstance().currentUser

        // Access the user's email address
        val email = user?.email

        // Access the user's display name
        val displayName = user?.displayName

        // Access the user's phone number
        val photoURL = user?.photoUrl

        // Update the UI with the user information
        val emailTextView = view.findViewById<TextView>(R.id.emailEt)
        emailTextView.text = email

        val displayNameTextView = view.findViewById<TextView>(R.id.userName)
        displayNameTextView.text = displayName

        // Access the user's photo URL
        val photoUrl = user?.photoUrl

        // Load the user's photo into an ImageView using Glide or Picasso
        val photoImageView = view.findViewById<ImageView>(R.id.profileIC)
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .into(photoImageView)
        } else {
            // Display a placeholder image if the user has no photo
            photoImageView.setImageResource(R.drawable.ic_profile)
        }

        return view
    }
}