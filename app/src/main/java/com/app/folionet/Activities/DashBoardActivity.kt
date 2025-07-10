package com.app.folionet.Activities

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.app.folionet.Adapters.SliderAdapter
import com.app.folionet.Domains.SliderItems
import com.app.folionet.R
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.abs

class DashBoardActivity : AppCompatActivity() {

    private lateinit var viewPager2: ViewPager2
    private lateinit var sliderAdapter: SliderAdapter
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var currentPage = 1
    private lateinit var progressBar: ProgressBar
    private lateinit var customDots: LinearLayout
    private var realItemCount = 0
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileImageView: CircleImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var imgProfile: CircleImageView
    private lateinit var fuser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dash_board)

        // Full screen window status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val spinner: Spinner = findViewById(R.id.filterSpinner)
        progressBar = findViewById(R.id.progressBarProjects)
        customDots = findViewById(R.id.customDots)
        viewPager2 = findViewById(R.id.viewPager2)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        imgProfile = findViewById(R.id.imgProfile)
        etSearch = findViewById(R.id.etSearch)

        //searching handle
        etSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        //firebase initialization
        firebaseAuth = FirebaseAuth.getInstance()
        fuser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        // Access the header view of the navigation drawer to display profile info
        val headerView = navigationView.getHeaderView(0)
        profileImageView = headerView.findViewById(R.id.nav_profile_image)
        userNameTextView = headerView.findViewById(R.id.textViewName)
        userEmailTextView = headerView.findViewById(R.id.textViewEmailEmail)


        // Open Drawer when profile icon is clicked
        imgProfile.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START)
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
        })


        // Handle menu item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_logout -> {
                    logOut()
                    true
                }
//                R.id.menu_settings -> {
//                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
//                    true
//                }
                else -> false
            }
        }

        loadDrawerHeader()


        // Set up adapter from string array for filter
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.post_filter_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Handle selected item
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                when (selected) {
                    "All" -> {
                        // Show all posts
                    }

                    "Photos" -> {
                        // Filter and show only photo posts
                    }

                    "Videos" -> {
                        // Filter and show only video posts
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        fetchSliderDataFromFirestore()
    }

    private fun loadDrawerHeader() {
        if (fuser != null) {
            val userId: String = fuser.getUid()
            Log.d(ContentValues.TAG, "UserId: " + userId)

            db.collection("USERS").document(userId)
                .get()
                .addOnSuccessListener(OnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                    if (documentSnapshot!!.exists()) {
                        // Get profile image URL
                        val profileImageUrl = documentSnapshot.getString("profilePicUrl")
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Picasso.get()
                                .load(profileImageUrl?:"")
                                .placeholder(R.drawable.person)
                                .error(R.drawable.no_profile_pic)
                                .into(profileImageView)
                        }
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Picasso.get()
                                .load(profileImageUrl?:"")
                                .placeholder(R.drawable.no_profile_pic)
                                .error(R.drawable.no_profile_pic)
                                .into(imgProfile)
                        }

                        // Get and set user name
                        val userName = documentSnapshot.getString("name")
                        if (userNameTextView != null && userName != null) {
                            userNameTextView.text = userName
                        }

                        // Get and set user email
                        val userEmail = documentSnapshot.getString("email")
                        if (userEmailTextView != null && userEmail != null) {
                            userEmailTextView.setText(userEmail)
                        }

                        Log.d(ContentValues.TAG, "Profile Data: " + userName + ", " + userEmail)
                    } else {
                        Log.e(ContentValues.TAG, "User document does not exist")
                    }
                })
                .addOnFailureListener(OnFailureListener { e: Exception? ->
                    Log.e(ContentValues.TAG, "Failed to fetch user details: " + e!!.message)
                    Toast.makeText(
                        this@DashBoardActivity,
                        "Failed to load profile details",
                        Toast.LENGTH_SHORT
                    ).show()
                })
        } else {
            Log.e(ContentValues.TAG, "User not authenticated")
        }
    }

    private fun fetchSliderDataFromFirestore() {
        progressBar.visibility = View.VISIBLE

        db.collection("SliderItems")
            .get()
            .addOnSuccessListener { result ->
                val sliderItems = mutableListOf<SliderItems>()
                for (document in result) {
                    val title = document.getString("title") ?: "No Title"
                    val description = document.getString("description") ?: ""
                    val techStack = document.getString("technologiesUsed") ?: ""
                    val imageUrl = document.getString("imgUrl") ?: ""
                    sliderItems.add(SliderItems(title, description, techStack, imageUrl))
                }
                progressBar.visibility = View.GONE

                // Prepare cyclic list
                val cyclicList = mutableListOf<SliderItems>()
                if (sliderItems.isNotEmpty()) {
                    cyclicList.add(sliderItems.last()) // Add last item at the start
                    cyclicList.addAll(sliderItems)
                    cyclicList.add(sliderItems.first()) // Add first item at the end
                }
                realItemCount = sliderItems.size
                setupViewPager(cyclicList)
                setupCustomDots(realItemCount)
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Failed to load slider data: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setupViewPager(sliderItems: List<SliderItems>) {
        sliderAdapter = SliderAdapter(sliderItems)
        viewPager2.adapter = sliderAdapter

        viewPager2.clipToPadding = false
        viewPager2.clipChildren = false
        viewPager2.offscreenPageLimit = 3
        (viewPager2.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER


        //for zoom in the current position
        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(20))
            addTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + r * 0.15f
            }
        }
        viewPager2.setPageTransformer(compositePageTransformer)

        viewPager2.setCurrentItem(1, false) // Start at first real item

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                // Map cyclic position to real position for dots
                val realPosition = when (position) {
                    0 -> realItemCount - 1
                    realItemCount + 1 -> 0
                    else -> position - 1
                }
                updateDots(realPosition)
            }

            //infinite scroll illusion.
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val itemCount = sliderAdapter.itemCount
                    when (viewPager2.currentItem) {
                        0 -> viewPager2.setCurrentItem(itemCount - 2, false)
                        itemCount - 1 -> viewPager2.setCurrentItem(1, false)
                    }
                }
            }
        })

        runnable = Runnable {
            if (sliderAdapter.itemCount <= 2) return@Runnable
            currentPage = (currentPage + 1) % sliderAdapter.itemCount
            viewPager2.setCurrentItem(currentPage, true)
            handler.postDelayed(runnable, 3000)
        }
        handler.postDelayed(runnable, 3000)
    }

    //TabLayoutMediator(tabLayout, viewPager2) { _, _ -> }.attach()
//    dotsIndicator.setViewPager2(viewPager2)

    private fun setupCustomDots(count: Int) {
        customDots.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            dot.layoutParams = params
            dot.setImageResource(if (i == 0) R.drawable.selected_dot else R.drawable.default_dot)
            customDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until customDots.childCount) {
            val dot = customDots.getChildAt(i) as ImageView
            dot.setImageResource(if (i == selected) R.drawable.selected_dot else R.drawable.default_dot)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::runnable.isInitialized) {
            handler.postDelayed(runnable, 3000)
        }
        loadDrawerHeader()
    }

    //logout
    private fun logOut() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("LOGOUT")
        builder.setMessage("Do you want to Logout ?")
        builder.setPositiveButton("YES") { dialog, which ->
            firebaseAuth.signOut()
            // Redirect to Login and clear back stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("NO") { dialog, which ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("EXIT")
            builder.setMessage("Do you want to really exit ?")
            builder.setPositiveButton("Exit", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    finishAffinity() // Close all activities in the stack (exit app)
                }
            })
            builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.dismiss()
                }
            })
            builder.setCancelable(false)
            val dialog = builder.create()
            dialog.show()
        }
    }

}
