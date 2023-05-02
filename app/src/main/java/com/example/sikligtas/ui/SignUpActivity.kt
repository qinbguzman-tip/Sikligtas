package com.example.sikligtas.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.sikligtas.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.txtLogin.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        binding.regButton.setOnClickListener{
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassET.text.toString()
            val firstName = binding.firstNameEt.text.toString()
            val lastName = binding.lastNameEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()) {
                if (pass == confirmPass) {
                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Set the display name for the user
                            val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                                "$firstName"
                            } else if (firstName.isNotEmpty()) {
                                firstName
                            } else if (lastName.isNotEmpty()) {
                                lastName
                            } else {
                                "User"
                            }
                            firebaseAuth.currentUser!!.updateProfile(
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build()
                            ).addOnSuccessListener {
                                val intent = Intent(this, SignInActivity::class.java)
                                startActivity(intent)
                                finish()
                            }.addOnFailureListener { e ->
                                Log.e("SignUpActivity", "Error setting display name for user", e)
                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty fields not allowed!", Toast.LENGTH_SHORT).show()
            }

        }

    }
}