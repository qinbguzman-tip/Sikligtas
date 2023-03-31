package com.example.sikligtas.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sikligtas.R
import com.example.sikligtas.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchWeatherData()
    }

    private fun fetchWeatherData() = lifecycleScope.launch {
        val city = "Manila, phl" // Set the desired city here
        val weatherAPI = R.string.weather_api_key // Replace with your OpenWeatherMap API key

        /* Showing the ProgressBar, Making the main design GONE */
        binding.weatherLayout.visibility = View.GONE

        try {
            val response = withContext(Dispatchers.IO) {
                URL("https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$weatherAPI")
                    .readText(Charsets.UTF_8)
            }

            /* Extracting JSON returns from the API */
            val jsonObj = JSONObject(response)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

            val temp = main.getString("temp") + "°C"
            val weatherDescription = weather.getString("description")
            val currloc = jsonObj.getString("name") + ", " + sys.getString("country")

            /* Populating extracted data into our views */
            binding.location.text = currloc
            binding.status.text =
                weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            binding.temp.text = temp

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