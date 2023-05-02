package com.example.sikligtas.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.sikligtas.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
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

        val displayNameTextView = view.findViewById<TextView>(R.id.userName)
        displayNameTextView.text = displayName

        // Split the display name into first name and last name
        val names = displayName?.split(" ")
        val firstName = names?.dropLast(1)?.joinToString(" ")
        val lastName = names?.lastOrNull()

        // Update the UI with the user information
        val emailTextView = view.findViewById<TextView>(R.id.emailEt)
        emailTextView.text = email

        val firstNameEditText = view.findViewById<TextInputEditText>(R.id.firstNameEt)
        firstNameEditText.setText(firstName)

        val lastNameEditText = view.findViewById<TextInputEditText>(R.id.lastNameEt)
        lastNameEditText.setText(lastName)

        // Load the user's photo into an ImageView using Glide or Picasso
        val photoUrl = user?.photoUrl
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

        // Add onClickListener to edit button to navigate to edit profile screen
        val editButton = view.findViewById<MaterialButton>(R.id.editBtn)
        editButton.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
            findNavController().navigate(action)
        }

        // Add onClickListener to history button to navigate to history screen
        val historyButton = view.findViewById<MaterialButton>(R.id.historyBtn)
        historyButton.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToHistoryFragment()
            findNavController().navigate(action)
        }

        // Add a fragment result listener to receive the updated profile information
        parentFragmentManager.setFragmentResultListener("editProfile", viewLifecycleOwner) { _, bundle ->
            val names = displayName?.split(" ")
            val firstName = names?.dropLast(1)?.joinToString(" ")
            val lastName = names?.lastOrNull()
            val email = bundle.getString("email")

            // Update the UI with the updated profile information
            updateProfileUI(firstName, lastName, email)

            // Reload the user's profile information to ensure it's up to date
            loadProfileData()
        }
        return view
    }

    private fun updateProfileUI(firstName: String?, lastName: String?, email: String?) {
        view?.findViewById<TextInputEditText>(R.id.firstNameEt)?.setText(firstName)
        view?.findViewById<TextInputEditText>(R.id.lastNameEt)?.setText(lastName)
        view?.findViewById<TextView>(R.id.emailEt)?.text = email
    }
    private fun loadProfileData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Retrieve user's display name and email from Firebase
            val displayName = user.displayName
            val email = user.email

            // Split the display name into first name and last name
            val names = displayName?.split(" ")
            val firstName = names?.dropLast(1)?.joinToString(" ")
            val lastName = names?.lastOrNull()

            // Update the UI with the user information
            updateProfileUI(firstName, lastName, email)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

}
