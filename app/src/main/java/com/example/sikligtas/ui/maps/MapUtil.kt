package com.example.sikligtas.ui.maps

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapUtil {

    fun setCameraPosition(location: LatLng): CameraPosition {
        return CameraPosition.Builder()
            .target(location)
            .zoom(20f)
            .build()
    }

    fun calculateElapsedTime(startTime: Long, stopTime: Long): String {
        val elapsedTime = stopTime - startTime

        val seconds = (elapsedTime / 1000).toInt() % 60
        val minutes = (elapsedTime / (1000 * 60) % 60)
        val hours = (elapsedTime / (1000 * 60 * 60) % 24)

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun calculateTotalDistance(locationList: MutableList<LatLng>): String {
        if (locationList.size > 1) {
            var totalMeters = 0.0

            for (i in 0 until locationList.size - 1) {
                val currentLocation = locationList[i]
                val nextLocation = locationList[i + 1]
                totalMeters += SphericalUtil.computeDistanceBetween(currentLocation, nextLocation)
            }

            val totalKilometers = totalMeters / 1000
            return DecimalFormat("#.##").format(totalKilometers)
        }
        return "0"
    }

}
