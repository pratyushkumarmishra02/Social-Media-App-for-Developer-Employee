package com.app.folionet.Activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Adapters.UserAdapter
import com.app.folionet.Adapters.ProjectAdapter
import com.app.folionet.Domains.User
import com.app.folionet.Domains.Project
import com.app.folionet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var projectRecyclerView: RecyclerView
    private lateinit var noResultsText: TextView
    private lateinit var loadingBar: ProgressBar
    private lateinit var backArrow: ConstraintLayout
    private lateinit var userTv: TextView
    private lateinit var projectTv: TextView

    private lateinit var userAdapter: UserAdapter
    private lateinit var projectAdapter: ProjectAdapter

    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var listener: ListenerRegistration? = null

    private val userList = ArrayList<User>()
    private val projectList = ArrayList<Project>()
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize views
        searchInput = findViewById(R.id.searchInput)
        userRecyclerView = findViewById(R.id.searchResultsRecyclerUser)
        projectRecyclerView = findViewById(R.id.searchResultsRecyclerProject)
        noResultsText = findViewById(R.id.noResultsText)
        loadingBar = findViewById(R.id.loadingBar)
        backArrow = findViewById(R.id.backArrowConstraint)
        userTv = findViewById(R.id.usersTv)
        projectTv = findViewById(R.id.projectsTv)

        // Back press
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Firebase setup
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        // Setup adapters
        userAdapter = UserAdapter(this,userList)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = userAdapter

        projectAdapter = ProjectAdapter(this,projectList)
        projectRecyclerView.layoutManager = LinearLayoutManager(this)
        projectRecyclerView.adapter = projectAdapter

        // Search functionality
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                currentQuery = query
                if (query.isNotEmpty()) {
                    performLiveSearch(query)
                } else {
                    clearResults()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun performLiveSearch(query: String) {
        loadingBar.visibility = View.VISIBLE
        listener?.remove()

        val searchText = query.lowercase()
        val thisQuery = query

        userList.clear()
        projectList.clear()

        // USERS Search
        db.collection("USERS")
            .orderBy("userName")
            .get()
            .addOnSuccessListener { userDocs ->
                if (thisQuery != currentQuery) return@addOnSuccessListener
                for (doc in userDocs) {
                    val name = doc.getString("name") ?: ""
                    val userName = doc.getString("userName") ?: ""
                    val profilePic = doc.getString("profilePicUrl") ?: ""

                    if (userName.lowercase().contains(searchText)) {
                        userList.add(User(name = name, username = userName, profilePicUrl = profilePic))
                    }
                }
                userAdapter.notifyDataSetChanged()

                // PROJECTS Search
                db.collection("SliderItems")
                    .orderBy("lowerTitle")
                    .startAt(searchText)
                    .endAt(searchText + "\uf8ff")
                    .get()
                    .addOnSuccessListener { sliderDocs ->
                        if (thisQuery != currentQuery) return@addOnSuccessListener
                        for (doc in sliderDocs) {
                            val title = doc.getString("title") ?: ""
                            val tech = doc.getString("technologiesUsed") ?: ""
                            val imgUrl = doc.getString("imgUrl") ?: ""

                            if (title.lowercase().contains(searchText) || tech.lowercase().contains(searchText)) {
                                projectList.add(Project(title = title, description = tech, profilePicUrl = imgUrl))
                            }
                        }
                        projectAdapter.notifyDataSetChanged()

                        loadingBar.visibility = View.GONE
                        noResultsText.visibility = if (userList.isEmpty() && projectList.isEmpty()) View.VISIBLE else View.GONE
                        userTv.visibility = if (userList.isEmpty()) View.GONE else View.VISIBLE
                        projectTv.visibility = if (projectList.isEmpty()) View.GONE else View.VISIBLE
                    }
                    .addOnFailureListener {
                        if (thisQuery != currentQuery) return@addOnFailureListener
                        Log.e("SearchActivity", "Error fetching SliderItems", it)
                        loadingBar.visibility = View.GONE
                    }

            }.addOnFailureListener {
                if (thisQuery != currentQuery) return@addOnFailureListener
                Log.e("SearchActivity", "Error fetching USERS", it)
                loadingBar.visibility = View.GONE
            }
    }

    private fun clearResults() {
        userList.clear()
        projectList.clear()
        userAdapter.notifyDataSetChanged()
        projectAdapter.notifyDataSetChanged()
        noResultsText.visibility = View.GONE
        loadingBar.visibility = View.GONE
        userTv.visibility = View.GONE
        projectTv.visibility = View.GONE
        listener?.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
