package com.app.folionet.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Adapters.ReelsAdapter
import com.app.folionet.Domains.Reels
import com.app.folionet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ReelsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reelsAdapter: ReelsAdapter
    private val reelsList = ArrayList<Reels>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_reels, container, false)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        recyclerView = view.findViewById(R.id.recyclerViewReels)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        reelsAdapter = ReelsAdapter(reelsList)
        recyclerView.adapter = reelsAdapter

        fetchReels()

        return view
    }

    private fun fetchReels() {
        db.collection("Reels")
            .whereEqualTo("userId", getCurrentUserId())
            .get()
            .addOnSuccessListener { documents ->
                reelsList.clear()
                for (doc in documents) {
                    val post = doc.toObject(Reels::class.java)
                    reelsList.add(post)
                }
                reelsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.e("ReelsFragment", "Error fetching reels: ${it.message}")
            }
    }

    private fun getCurrentUserId(): String {
        val userId = firebaseUser.uid
        return userId
    }
}
