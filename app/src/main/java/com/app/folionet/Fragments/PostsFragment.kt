package com.app.folionet.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Adapters.PostsAdapter
import com.app.folionet.Domains.Post
import com.app.folionet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class PostsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostsAdapter
    private val postList = ArrayList<Post>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_posts, container, false)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        recyclerView = view.findViewById(R.id.recyclerViewPosts)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        postAdapter = PostsAdapter(postList)
        recyclerView.adapter = postAdapter

        fetchPosts()

        return view
    }

    private fun fetchPosts() {
        db.collection("Posts")
            .whereEqualTo("userId", getCurrentUserId())
            .get()
            .addOnSuccessListener { documents ->
                postList.clear()
                for (doc in documents) {
                    val post = doc.toObject(Post::class.java)
                    postList.add(post)
                }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.e("PostsFragment", "Error fetching posts: ${it.message}")
            }
    }

    private fun getCurrentUserId(): String {
        val userId = firebaseUser.uid
        return userId
    }
}
