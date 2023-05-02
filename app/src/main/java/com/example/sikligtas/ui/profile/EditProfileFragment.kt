package com.example.sikligtas.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.sikligtas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileFragment : Fragment() {

    // Declare variables for views and Firebase references
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Get the current user
        val user = FirebaseAuth.getInstance().currentUser

        // Access the user's display name
        val displayName = user?.displayName

        val displayNameTextView = view.findViewById<TextView>(R.id.userName)
        displayNameTextView.text = displayName

        // Access the user's photo URL
        val photoUrl = user?.photoUrl

        // Load the user's photo into an ImageView using Glide or Picasso
        val photoImageView = view.findViewById<ImageView>(R.id.profileIC)
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(photoImageView)
        } else {
            // Display a placeholder image if the user has no photo
            photoImageView.setImageResource(R.drawable.ic_profile)
        }
        // Ad

        // Initialize views
        firstNameEditText = view.findViewById(R.id.firstNameEt)
        lastNameEditText = view.findViewById(R.id.lastNameEt)
        emailEditText = view.findViewById(R.id.emailEt)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        saveButton = view.findViewById(R.id.saveButton)

        // Initialize Firebase authentication reference
        auth = FirebaseAuth.getInstance()

        // Load user's existing information into the EditText fields
        loadUserData()

        // Set click listener for save button
        saveButton.setOnClickListener {
            saveUserData()
        }

        return view
    }

    // Load user's existing information into the EditText fields
    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            emailEditText.setText(user.email)
            firstNameEditText.setText(user.displayName?.substringBefore(' ') ?: "")
            lastNameEditText.setText(user.displayName?.substringAfter(' ') ?: "")
        }
    }

    // Save user's edited information to Firebase
    private fun saveUserData() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Update user's display name and email in Firebase authentication
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName("$firstName $lastName")
            .build()
        auth.currentUser?.updateProfile(profileUpdates)
        auth.currentUser?.updateEmail(email)

        // If the user has entered a new password, update it in Firebase authentication as well
        if (password.isNotEmpty()) {
            auth.currentUser?.updatePassword(password)
        }
        // Create a bundle containing the updated profile information
        val bundle = bundleOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email
        )

        // Set the fragment result to pass the updated profile information back to the ProfileFragment
        parentFragmentManager.setFragmentResult("editProfile", bundle)

        // Navigate back to the ProfileFragment after saving the user's profile data
        findNavController().popBackStack(R.id.profileFragment, false)

        Toast.makeText(requireContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "EditProfileFragment"
    }
}