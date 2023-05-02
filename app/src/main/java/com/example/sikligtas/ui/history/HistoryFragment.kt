package com.example.sikligtas.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sikligtas.R
import com.example.sikligtas.data.HistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryFragmentAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        auth = FirebaseAuth.getInstance()
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        database = FirebaseDatabase.getInstance("https://sikligtas-default-rtdb.asia-southeast1.firebasedatabase.app/")

        adapter = HistoryFragmentAdapter()
        recyclerView.adapter = adapter

        // Initialize HistoryViewModel
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        val user = auth.currentUser
        if (user != null) {
            val historyRef = database.getReference("users").child(user.uid).child("history")

            historyRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyItems = mutableListOf<HistoryItem>()
                    for (child in snapshot.children) {
                        val date = child.key ?: ""
                        val startLoc = child.child("startLoc").getValue(String::class.java) ?: ""
                        val endLoc = child.child("endLoc").getValue(String::class.java) ?: ""
                        val elapsedTime = child.child("elapsedTime").getValue(String::class.java) ?: ""
                        val distance = child.child("distance").getValue(String::class.java) ?: ""
                        historyItems.add(0,HistoryItem(date, startLoc, endLoc, elapsedTime, distance))
                    }
                    adapter.setHistoryItems(historyItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "loadHistory:onCancelled", error.toException())
                }
            })
        } else {
            // Handle the case when the user is not logged in
            Toast.makeText(
                requireContext(),
                "You must be logged in to view history data.",
                Toast.LENGTH_LONG
            ).show()
        }

        return view
    }

    companion object {
        private const val TAG = "HistoryFragment"
    }
}
