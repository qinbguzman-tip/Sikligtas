package com.example.sikligtas.ui.history

import androidx.lifecycle.ViewModel
import com.example.sikligtas.data.HistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HistoryViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://sikligtas-default-rtdb.asia-southeast1.firebasedatabase.app/")

    fun saveHistoryItem(historyItem: HistoryItem) {
        val userId = auth.currentUser?.uid ?: return
        val historyRef = database.getReference("users").child(userId).child("history")
        historyRef.child(historyItem.date).setValue(historyItem)
    }
}
