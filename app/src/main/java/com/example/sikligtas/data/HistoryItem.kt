package com.example.sikligtas.data

import java.text.SimpleDateFormat
import java.util.*

data class HistoryItem(
    val date: String,
    val startLoc: String,
    val endLoc: String,
    val elapsedTime: String,
    val distance: String
) {
    companion object {
        fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS", Locale.getDefault())
            return dateFormat.format(Date())
        }
    }
}

