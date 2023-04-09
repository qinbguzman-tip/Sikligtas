package com.example.sikligtas.ui.history

import HistoryFragmentAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sikligtas.R
import com.example.sikligtas.data.HistoryItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryFragmentAdapter
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        database = FirebaseDatabase.getInstance("https://sikligtas-default-rtdb.asia-southeast1.firebasedatabase.app/")


        adapter = HistoryFragmentAdapter()
        recyclerView.adapter = adapter

        val historyRef = database.getReference("history")

        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyItems = mutableListOf<HistoryItem>()
                for (child in snapshot.children) {
                    val startLoc = child.child("startLoc").getValue(String::class.java) ?: ""
                    val endLoc = child.child("endLoc").getValue(String::class.java) ?: ""
                    val elapsedTime = child.child("elapsedTime").getValue(String::class.java) ?: ""
                    val distance = child.child("distance").getValue(String::class.java) ?: ""
                    historyItems.add(HistoryItem(startLoc, endLoc, elapsedTime, distance))
                }
                adapter.setHistoryItems(historyItems)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "loadHistory:onCancelled", error.toException())
            }
        })

        return view
    }

    companion object {
        private const val TAG = "HistoryFragment"
    }
}
