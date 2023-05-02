package com.example.sikligtas.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileFragment : Fragment() {

    // Declare variables for views and Firebase references
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
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

        // Initialize views
        firstNameEditText = view.findViewById(R.id.firstNameEt)
        lastNameEditText = view.findViewById(R.id.lastNameEt)
        emailEditText = view.findViewById(R.id.emailEt)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        // Initialize Firebase authentication reference
        auth = FirebaseAuth.getInstance()

        // Load user's existing information into the EditText fields
        loadUserData(user)

        // Set click listener for save button
        saveButton.setOnClickListener {
            saveUserData(user)
        }

        // Set click listener for cancel button
        cancelButton.setOnClickListener {
            showCancelConfirmationDialog()
        }

        return view
    }

    // Load user's existing information into the EditText fields
    private fun loadUserData(user: FirebaseUser?) {
        if (user != null) {
            emailEditText.setText(user.email)
            val displayName = user.displayName

            val nameParts = displayName?.split(" ")

            if (nameParts != null && nameParts.isNotEmpty()) {
                firstNameEditText.setText(nameParts.dropLast(1).joinToString(" "))
                lastNameEditText.setText(nameParts.last())
            } else {
                firstNameEditText.setText(displayName)
                lastNameEditText.setText("")
            }

            // Hide the password field for users signed in with Google
            if (isGoogleSignInUser(user)) {
                passwordEditText.visibility = View.GONE
            }
        }
    }


    private fun saveUserData(user: FirebaseUser?) {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isGoogleSignInUser(user!!)) {
            // User signed in with Google, show email confirmation dialog
            showEmailConfirmationDialog(user?.email ?: "", firstName, lastName, email)
        } else {
            // User signed in with email and password, show password confirmation dialog
            showPasswordConfirmationDialog(user?.email ?: "", firstName, lastName, email, password)
        }
    }

    private fun isGoogleSignInUser(user: FirebaseUser): Boolean {
        val providerData = user.providerData
        return providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    private fun showEmailConfirmationDialog(currentEmail: String, firstName: String, lastName: String, email: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_email_confirmation, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEt)
//        emailEditText.setText(currentEmail)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Email Address")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val enteredEmail = emailEditText.text.toString().trim()

                if (validateEmail(enteredEmail)) {
                    // Update user's display name and email in Firebase authentication
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$firstName $lastName")
                        .build()
                    auth.currentUser?.updateProfile(profileUpdates)
                    auth.currentUser?.updateEmail(enteredEmail)

                    // Create a bundle containing the updated profile information
                    val bundle = bundleOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to enteredEmail
                    )

                    // Set the fragment result to pass the updated profile information back to the ProfileFragment
                    parentFragmentManager.setFragmentResult("editProfile", bundle)

                    // Navigate back to the ProfileFragment after saving the user's profile data
                    findNavController().popBackStack(R.id.profileFragment, false)

                    Toast.makeText(requireContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Email confirmation failed", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    private fun showPasswordConfirmationDialog(currentEmail: String, firstName: String, lastName: String, email: String, password: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_password_confirmation, null)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.passwordEditText)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle("Enter Current Password")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val currentPassword = passwordEditText.text.toString().trim()
                val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)

                auth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {
                        // Password is correct, proceed with saving user data

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

                        Toast.makeText(
                            requireContext(),
                            "Profile updated successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Password is incorrect, show an error message
                        Toast.makeText(
                            requireContext(),
                            "Incorrect password. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    private fun validateEmail(enteredEmail: String): Boolean {
        val user = auth.currentUser
        val currentEmail = user?.email

        return enteredEmail.equals(currentEmail, ignoreCase = true)
    }

    private fun showCancelConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cancel Confirmation")
        builder.setMessage("Are you sure you want to cancel editing your profile?")
        builder.setPositiveButton("Yes") { _, _ ->
            findNavController().popBackStack()
        }
        builder.setNegativeButton("No", null)
        val dialog = builder.create()
        dialog.show()
    }
    companion object {
        private const val TAG = "EditProfileFragment"
    }
}