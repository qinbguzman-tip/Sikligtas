package com.example.sikligtas.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.example.sikligtas.R
import com.example.sikligtas.databinding.ActivityMainBinding
import com.example.sikligtas.util.Permissions.hasLocationPermission
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.navHostFragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
            ), drawerLayout
        )
        setSupportActionBar(findViewById(R.id.navToolbar))
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        // Checks if granted location permission
        if (!hasLocationPermission(this)) {
            navController.navigate(R.id.action_homeFragment_to_permissionFragment)
        }

        // Calling intent for Logout
        firebaseAuth = FirebaseAuth.getInstance()
        // Initialize Google sign-in options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Create Google sign-in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.navView.menu.findItem(R.id.logout).setOnMenuItemClickListener {
            firebaseAuth.signOut()
            googleSignInClient.signOut().addOnCompleteListener {
                Toast.makeText(this, "You are logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            }
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

}