package com.example.sikligtas.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import androidx.navigation.ui.AppBarConfiguration
import com.example.sikligtas.R
import com.example.sikligtas.databinding.ActivityMainBinding
import com.example.sikligtas.ui.home.HomeFragmentDirections
import com.example.sikligtas.ui.maps.MapsFragment
import com.example.sikligtas.util.Permissions.hasLocationPermission
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var firebaseAuth: FirebaseAuth

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
        binding.navView.menu.findItem(R.id.logout).setOnMenuItemClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "You are logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            true
        }

        // fabButton
        binding.playBtn.setOnClickListener {
            findNavController(R.id.navHostFragment).navigate(R.id.action_global_mapsFragment)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
}