package com.example.sikligtas.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sikligtas.JetsonNanoClient
import com.example.sikligtas.OnConnectionErrorListener
import com.example.sikligtas.OnDataReceivedListener
import com.example.sikligtas.R
import com.example.sikligtas.data.HistoryItem
import com.example.sikligtas.databinding.FragmentMapsBinding
import com.example.sikligtas.service.TrackerService
import com.example.sikligtas.ui.history.HistoryViewModel
import com.example.sikligtas.ui.maps.MapUtil.calculateElapsedTime
import com.example.sikligtas.ui.maps.MapUtil.calculateTotalDistance
import com.example.sikligtas.ui.maps.MapUtil.setCameraPosition
import com.example.sikligtas.util.Constants.ACTION_SERVICE_START
import com.example.sikligtas.util.Constants.ACTION_SERVICE_STOP
import com.example.sikligtas.util.Constants.LOCATION_PERMISSION_REQUEST_CODE
import com.example.sikligtas.util.Constants.REQUEST_CHECK_SETTINGS
import com.example.sikligtas.util.ExtensionFunctions.disable
import com.example.sikligtas.util.ExtensionFunctions.enable
import com.example.sikligtas.util.ExtensionFunctions.hide
import com.example.sikligtas.util.ExtensionFunctions.show
import com.example.sikligtas.util.Permissions.hasBackgroundLocationPermission
import com.example.sikligtas.util.Permissions.requestBackgroundLocationPermission
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.tapadoo.alerter.Alerter
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    OnMarkerClickListener,
    EasyPermissions.PermissionCallbacks, OnDataReceivedListener {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    private val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private lateinit var tts: TextToSpeech
    private var jnc: JetsonNanoClient? = null

    private val hostIP: String = "192.168.43.189"

    private val dataBuffer = LinkedList<String>()
    private var previousDistance: String? = null
    private var previousType: String? = null
    private var lastAlertTime: Long = 0
    private val minAlertInterval: Long = 5000

    enum class Direction {
        LEFT, RIGHT, BACK
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        binding.bottomSheetMaps.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.bottomSheetMaps.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.bottomSheetMaps.resetButton.setOnClickListener {
            onResetButtonClicked()
        }
        binding.bottomSheetMaps.homeButton.setOnClickListener {
            findNavController().navigate(R.id.action_mapsFragment_to_homeFragment)
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        auth = FirebaseAuth.getInstance()
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        bottomNavigationView = requireActivity().findViewById(R.id.bottomNav)
        toolbar = requireActivity().findViewById(R.id.navToolbar)

        bottomNavigationView.visibility = View.GONE
        (activity as AppCompatActivity).supportActionBar?.hide()

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest.create())
                .build()

            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener {
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try {
                            exception.startResolutionForResult(
                                requireActivity(),
                                REQUEST_CHECK_SETTINGS
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
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location settings are not enabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("MissingPermission", "PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.uiSettings.isCompassEnabled = true

        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        lifecycleScope.launch {
            binding.bottomSheetMaps.startButton.show()
        }
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = true
            isScrollGesturesEnabled = false
        }

        setMapStyle(map)
        observeTrackerService()
    }

    private fun setMapStyle(googleMap: GoogleMap) {
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.d("MapStyle", "Failed to add Style.")
            }
        } catch (e: Exception) {
            Log.d("MapStyle", e.toString())
        }
    }

    private fun observeTrackerService() {
        TrackerService.locationList.observe(viewLifecycleOwner) {
            if (it != null) {
                locationList = it
                if (locationList.size > 1) {
                    binding.bottomSheetMaps.stopButton.enable()
                }
                drawPolyline()
                followPolyline()
            }
        }
        TrackerService.started.observe(viewLifecycleOwner) {
            started.value = it
        }
        TrackerService.startTime.observe(viewLifecycleOwner) {
            startTime = it
        }
        TrackerService.stopTime.observe(viewLifecycleOwner) {
            stopTime = it

            if (stopTime != 0L) {
                showPolylineFinalRoute()
                displayResults()
            }
        }
    }

    private fun drawPolyline() {
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(15f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polylineList.add(polyline)
    }

    private fun followPolyline() {
        if (locationList.isNotEmpty()) {
            map.animateCamera(
                (
                        CameraUpdateFactory.newCameraPosition(
                            setCameraPosition(
                                locationList.last()
                            )
                        )
                        ), 1000, null
            )
        }
    }

    private fun onStartButtonClicked() {
        if (hasBackgroundLocationPermission(requireContext())) {
            jnc = JetsonNanoClient(hostIP, 8080)
            jnc!!.setOnDataReceivedListener(this)
            jnc!!.setOnConnectionErrorListener(object : OnConnectionErrorListener {
                override fun onConnectionError(exception: Exception) {
                    Alerter.create(requireActivity())
                        .setTitle("Connection Error")
                        .setText("Failed to connect to Jetson Nano, check the device if Turned ON.")
                        .setBackgroundColorRes(R.color.orange)
                        .setDuration(10000)
                        .setIcon(R.drawable.ic_error)
                        .show()
                }
            })
            startCountDown()

            binding.bottomSheetMaps.startButton.disable()
            binding.bottomSheetMaps.startButton.hide()
            binding.bottomSheetMaps.stopButton.show()
            binding.bottomSheetMaps.homeButton.isEnabled = false
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    private fun onStopButtonClicked() {
        stopForegroundService()

        stopAlert()
        disconnectJetsonNanoClient()

        binding.bottomSheetMaps.stopButton.hide()
        binding.bottomSheetMaps.startButton.show()
        binding.bottomSheetMaps.homeButton.isEnabled = true
    }

    private fun onResetButtonClicked() {
        mapReset()
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.bottomSheetMaps.stopButton.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = getString(R.string.Go)
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.md_theme_light_primary
                        )
                    )
                } else {
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.md_theme_light_error
                        )
                    )
                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }

    @SuppressLint("SetTextI18n")
    private fun updateInfo() {
        val currentTime = Calendar.getInstance().time
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(currentTime)
        binding.bottomSheetMaps.timeTv.text = formattedTime

        if (startTime != 0L && stopTime == 0L) {
            binding.bottomSheetMaps.elapsedTimeTv.text =
                calculateElapsedTime(startTime, System.currentTimeMillis())
        }

        binding.bottomSheetMaps.distanceTv.text = calculateTotalDistance(locationList) + " KM"
    }

    private val updateInfoHandler = Handler(Looper.getMainLooper())
    private val updateInfoRunnable = object : Runnable {
        override fun run() {
            updateInfo()
            updateInfoHandler.postDelayed(this, 1000)
        }
    }

    override fun onDataReceived(data: String) {
        dataBuffer.add(data)
        processBufferedData()
    }

    private fun processBufferedData() {
        while (dataBuffer.isNotEmpty()) {
            val data = dataBuffer.removeFirst()
            val outputList: List<String> = data.split(",")

            val type = outputList[0]
            val direction = when (outputList[1]) {
                "Left" -> Direction.LEFT
                "Right" -> Direction.RIGHT
                "Back" -> Direction.BACK
                else -> throw IllegalArgumentException("Invalid direction")
            }

            val meters = outputList[2].toInt() * 0.01
            val distance = String.format("%.2f", meters).toDouble().toString()

            val hazard = outputList[3]

            if (hazard == "true") {
                alertHazard(distance, type, direction)
            } else {
                Log.d("Alert", "Not Hazard")
            }
        }
    }

    private fun alertHazard(distance: String, type: String, direction: Direction) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAlertTime >= minAlertInterval) {

            if (distance != previousDistance || type != previousType) {
                previousDistance = distance
                previousType = type

                lastAlertTime = currentTime

                tts = TextToSpeech(requireContext()) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts.apply {
                            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String) {
                                    showAlert(distance, type, direction)
                                }

                                override fun onDone(utteranceId: String) {
                                    tts.shutdown()
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String) {
                                    tts.shutdown()
                                }
                            })
                            val params = HashMap<String, String>()
                            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "stringId"
                            speak(getString(R.string.hazard_info, distance, type, direction.name), TextToSpeech.QUEUE_FLUSH, params)
                            setSpeechRate(1.5f)
                        }
                    } else {
                        Log.e("TTS", "TextToSpeech initialization failed")
                    }
                }

                Log.d("Alert", "Received Alert: $distance, $type, ${direction.name}")
            } else {
                Log.d("Alert", "No Alert")
            }
        } else {
            Log.d("Alert", "Min interval not passed")
        }
    }

    private fun showAlert(distance: String, type: String, direction: Direction) {
        val vAlert = Alerter.create(requireActivity())
            .setTitle("Hazard Alert")
            .setText(getString(R.string.hazard_info, distance, type, direction.name))
            .setBackgroundColorRes(R.color.md_theme_light_error)
            .setDuration(5000)

        when (direction) {
            Direction.LEFT -> {
                vAlert.setIcon(R.drawable.ic_left_arrow)
            }
            Direction.RIGHT -> {
                vAlert.setIcon(R.drawable.ic_right_arrow)
            }
            Direction.BACK -> {
                vAlert.setIcon(R.drawable.ic_behind_arrow)
            }
        }

        vAlert.show()
    }

    private fun stopForegroundService() {
        binding.bottomSheetMaps.startButton.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    private fun sendActionCommandToService(action: String) {
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun showPolylineFinalRoute() {
        val bounds = LatLngBounds.Builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(), 100
            ), 2000, null
        )
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng) {
        val marker = map.addMarker(MarkerOptions().position(position))
        if (marker != null) {
            markerList.add(marker)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayResults() {
        val totalDistance = calculateTotalDistance(locationList)
        val elapsedTime = calculateElapsedTime(startTime, stopTime)

        // Save history data
        saveHistoryData(totalDistance, elapsedTime)

        lifecycleScope.launch {
            delay(2500)
            // Create a dialog
            val builder = AlertDialog.Builder(requireContext())
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_maps, null)

            val (startLoc, endLoc) = getStartAndEndLocations()

            dialogView.findViewById<TextView>(R.id.startLocation).text = " $startLoc"
            dialogView.findViewById<TextView>(R.id.endLocation).text = " $endLoc"
            dialogView.findViewById<TextView>(R.id.distance).text = totalDistance
            dialogView.findViewById<TextView>(R.id.elapsed_time).text = elapsedTime

            val alertDialog = builder.setView(dialogView)
                .setCancelable(false)
                .create()

            // Set the transparent background
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Set up the done button click listener
            val doneBtn = dialogView.findViewById<Button>(R.id.doneBtn)
            doneBtn.setOnClickListener {
                alertDialog.dismiss()
                binding.bottomSheetMaps.startButton.apply {
                    hide()
                    enable()
                }
                binding.bottomSheetMaps.stopButton.hide()
                binding.bottomSheetMaps.resetButton.show()
            }

            alertDialog.show()
        }
    }

    private fun saveHistoryData(totalDistance: String, elapsedTime: String) {
        // Check if the user is logged in
        val user = auth.currentUser
        if (user != null && locationList.isNotEmpty()) {
            val (startLoc, endLoc) = getStartAndEndLocations()

            // Create a HistoryItem object
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val historyItem = HistoryItem(date, startLoc, endLoc, elapsedTime, totalDistance)

            // Save the history item using HistoryViewModel
            historyViewModel.saveHistoryItem(historyItem)
        } else {
            // Handle the case when the user is not logged in or locationList is empty
            Toast.makeText(
                requireContext(),
                "You must be logged in and have a valid location to save history data.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getStartAndEndLocations(): Pair<String, String> {
        val startLocation = locationList.first()
        val endLocation = locationList.last()

        // Get the name of the place
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val startAddress =
            geocoder.getFromLocation(startLocation.latitude, startLocation.longitude, 1)
        val endAddress =
            geocoder.getFromLocation(endLocation.latitude, endLocation.longitude, 1)

        val startLoc = startAddress?.firstOrNull()?.let { address ->
            "${address.thoroughfare ?: ""}, ${address.locality ?: ""}"
        } ?: "Unknown"
        val endLoc = endAddress?.firstOrNull()?.let { address ->
            "${address.thoroughfare ?: ""}, ${address.locality ?: ""}"
        } ?: "Unknown"

        return Pair(startLoc, endLoc)
    }


    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(
                it.result.latitude,
                it.result.longitude
            )
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(lastKnownLocation)
                )
            )
            for (polyline in polylineList) {
                polyline.remove()
            }
            for (marker in markerList) {
                marker.remove()
            }
            locationList.clear()
            markerList.clear()

            TrackerService.stopTime.value = 0L
            TrackerService.startTime.value = 0L

            binding.bottomSheetMaps.distanceTv.text = "0.0"
            binding.bottomSheetMaps.elapsedTimeTv.text = "00:00"

            binding.bottomSheetMaps.resetButton.hide()
            binding.bottomSheetMaps.startButton.show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    private fun disconnectJetsonNanoClient() {
        jnc?.disconnect()
        jnc = null
    }

    private fun stopAlert() {
        if (::tts.isInitialized && tts.isSpeaking) {
            tts.stop()
            tts.shutdown()
        }

        if (Alerter.isShowing) {
            Alerter.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavigationView.visibility = View.VISIBLE
        (activity as AppCompatActivity).supportActionBar?.show()
        _binding = null

        disconnectJetsonNanoClient()
        stopAlert()
    }

    override fun onResume() {
        super.onResume()
        updateInfoHandler.post(updateInfoRunnable)

        if (jnc == null) {
            jnc = JetsonNanoClient(hostIP, 8000) // Replace with your host and port
        }
    }

    override fun onPause() {
        super.onPause()
        updateInfoHandler.removeCallbacks(updateInfoRunnable)

        disconnectJetsonNanoClient()
        stopAlert()
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }
}

