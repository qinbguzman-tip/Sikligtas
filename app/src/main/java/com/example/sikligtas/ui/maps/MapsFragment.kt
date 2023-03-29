package com.example.sikligtas.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sikligtas.R
import com.example.sikligtas.databinding.FragmentMapsBinding
import com.example.sikligtas.service.TrackerService
import com.example.sikligtas.ui.home.HomeFragment
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
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tapadoo.alerter.Alerter
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    OnMarkerClickListener,
    EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
//    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        bottomNavigationView = requireActivity().findViewById(R.id.bottomNav)
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        bottomNavigationView.visibility = View.GONE

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.resetButton.setOnClickListener {
            onResetButtonClicked()
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

//        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.alert_sound)
////
//////        binding.alertButton.setOnClickListener {
//////            mediaPlayer.start()
//////
//////            Alerter.create(requireActivity())
//////                .setTitle("Hazard Alert")
//////                .setText("In 300m, there is an approaching vehicle on your right")
//////                .setBackgroundColorRes(R.color.md_theme_light_error)
//////                .setIcon(R.drawable.ic_left_arrow)
//////                .show()
//////        }

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
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
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

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
//                        val markerOptions = MarkerOptions()
//                            .position(currentLatLng)
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
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        lifecycleScope.launch {
            binding.startButton.show()
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
                    binding.stopButton.enable()
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
            startCountDown()
            binding.startButton.disable()
            binding.startButton.hide()
            binding.stopButton.show()
//            binding.alertButton.show()
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    private fun onStopButtonClicked() {
        stopForegroundService()
        binding.stopButton.hide()
        binding.startButton.show()
//        binding.alertButton.hide()
    }

    private fun onResetButtonClicked() {
        mapReset()
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopButton.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = "GO"
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

    private fun stopForegroundService() {
        binding.startButton.disable()
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

    private fun displayResults() {
        val result = com.example.sikligtas.model.Result(
            calculateTotalDistance(locationList),
            calculateElapsedTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.startButton.apply {
                hide()
                enable()
            }
            binding.stopButton.hide()
            binding.resetButton.show()
        }
    }

    @SuppressLint("MissingPermission")
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
            binding.resetButton.hide()
            binding.startButton.show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavigationView.visibility = View.VISIBLE
        mapReset()
        _binding = null
    }

//    override fun onDestroy() {
////        mediaPlayer.release()
//        super.onDestroy()
//    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }

}