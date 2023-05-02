package com.example.sikligtas.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sikligtas.R
import com.example.sikligtas.databinding.FragmentHomeBinding
import com.example.sikligtas.util.Constants
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchLatestHistoryData()

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationSettingsRequest =
                LocationSettingsRequest.Builder().addLocationRequest(LocationRequest.create())
                    .build()

            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        fetchWeatherData(location.latitude, location.longitude)
                    }
                }
            }.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(
                            requireActivity(), Constants.REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Get the current user
        val user = FirebaseAuth.getInstance().currentUser

        // Access First Name
        val displayName = user?.displayName
        val firstNameParts = displayName?.split(" ")
        val firstName = if (firstNameParts?.size ?: 0 >= 2) {
            firstNameParts?.take(2)?.joinToString(" ")
        } else {
            firstNameParts?.getOrNull(0)
        }

        val displayNameTextView = view.findViewById<TextView>(R.id.userName)
        displayNameTextView.text = "$firstName"

        val savedIpAddress = getSavedIpAddress()
        updateWifiStatus(savedIpAddress)
    }

    private fun showIpInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_connect_device, null)
        val ipAddressEditText = dialogView.findViewById<EditText>(R.id.ip_address_edittext)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelBtn)
        val saveButton = dialogView.findViewById<Button>(R.id.saveBtn)

        val dialog = builder.setView(dialogView).create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString()
            if (isValidIpAddress(ipAddress)) {
                saveIpAddress(ipAddress)
                updateWifiStatus(ipAddress)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Invalid IP address", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }


    private fun isValidIpAddress(ipAddress: String): Boolean {
        val ipAddressPattern =
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$".toRegex()
        return ipAddressPattern.matches(ipAddress)
    }

    private fun saveIpAddress(ipAddress: String) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("ip_address", ipAddress)
            apply()
        }
    }

    private fun getSavedIpAddress(): String? {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getString("ip_address", null)
    }

    private fun removeIpAddress() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            remove("ip_address")
            apply()
        }
    }

    private fun updateWifiStatus(ipAddress: String?) {
        if (ipAddress != null) {
            binding.wifiIcon.setImageResource(R.drawable.ic_wifi_on)
            binding.wifiIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary), PorterDuff.Mode.SRC_IN)
            binding.wifiStatus.text = "Listening to $ipAddress"
            binding.wifiConnect.text = "Change the Device IP"

            binding.wifiConnect.setOnClickListener {
                removeIpAddress()
                updateWifiStatus(null)
            }
        } else {
            binding.wifiIcon.setImageResource(R.drawable.ic_wifi_off)
            binding.wifiIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_light_error), PorterDuff.Mode.SRC_IN)
            binding.wifiStatus.text = "No Device Connected"
            binding.wifiConnect.text = "Setup the Device IP"

            binding.wifiConnect.setOnClickListener {
                showIpInputDialog()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchLatestHistoryData() {
        // Access the Firebase instances
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance("https://sikligtas-default-rtdb.asia-southeast1.firebasedatabase.app/")

        // Get the current user
        val user = auth.currentUser
        if (user != null) {
            // Get the reference to the user's history
            val historyRef = database.getReference("users").child(user.uid).child("history")

            // Fetch the latest history data
            historyRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val latestEntry = snapshot.children.first()
                        val startLocation = latestEntry.child("startLoc").getValue(String::class.java) ?: "None"
                        val endLocation = latestEntry.child("endLoc").getValue(String::class.java) ?: "None"
                        val distance = latestEntry.child("distance").getValue(String::class.java) ?: "None"
                        val duration = latestEntry.child("elapsedTime").getValue(String::class.java) ?: "None"

                        // Update the UI
                        _binding?.let { binding ->
                            binding.startLocation.text = startLocation
                            binding.endLocation.text = endLocation
                            binding.distance.text = distance
                            binding.duration.text = duration
                        }
                    } else {
                        _binding?.let { binding ->
                            binding.startLocation.text = "None"
                            binding.endLocation.text = "None"
                            binding.distance.text = "None"
                            binding.duration.text = "None"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("HomeFragment", "fetchLatestHistoryData:onCancelled", error.toException())
                }
            })
        } else {
            // Handle the case when the user is not logged in
            binding.startLocation.text = "None"
            binding.endLocation.text = "None"
            binding.distance.text = "None"
            binding.duration.text = "None"
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) = lifecycleScope.launch {
        val weatherAPI =
            resources.getString(R.string.weather_api_key) // Replace with your OpenWeatherMap API key

        /* Showing the ProgressBar, Making the main design GONE */
        binding.weatherLayout.visibility = View.GONE

        try {
            val response = withContext(Dispatchers.IO) {
                URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$weatherAPI").readText(
                    Charsets.UTF_8
                )
            }

            /* Extracting JSON returns from the API */
            val jsonObj = JSONObject(response)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

            val temp = main.getString("temp") + "°C"
            val weatherDescription = weather.getString("description")
            val currentLocation = jsonObj.getString("name") + ", " + sys.getString("country")
            val weatherIcon = weather.getString("icon")
            val iconUrl = "https://openweathermap.org/img/w/$weatherIcon.png"

            /* Populating extracted data into our views */
            binding.location.text = currentLocation
            binding.status.text =
                weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            binding.temp.text = temp
            Picasso.get().load(iconUrl).fit().into(binding.weatherIC)

            /* Views populated, Hiding the loader, Showing the main design */
            binding.weatherLayout.visibility = View.VISIBLE

        } catch (e: Exception) {
            // Handle the error
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}