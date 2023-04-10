package com.example.sikligtas.ui.home

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        displayNameTextView.text = firstName

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