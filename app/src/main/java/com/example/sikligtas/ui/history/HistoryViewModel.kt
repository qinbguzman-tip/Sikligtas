package com.example.sikligtas.ui.history

import androidx.lifecycle.ViewModel
import com.example.sikligtas.data.HistoryItem
import com.google.firebase.database.FirebaseDatabase

class HistoryViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance("https://sikligtas-default-rtdb.asia-southeast1.firebasedatabase.app/")

    fun saveHistoryItem(historyItem: HistoryItem) {
        val historyRef = database.getReference("history")
        historyRef.child(historyItem.date).setValue(historyItem)
    }
}
