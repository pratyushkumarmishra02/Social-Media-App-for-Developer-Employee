package com.app.folionet.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.app.folionet.Activities.LoginActivity
import com.app.folionet.Activities.MainActivity
import com.app.folionet.Activities.ProfileActivity
import com.app.folionet.Activities.SearchActivity
import com.app.folionet.Adapters.SliderAdapter
import com.app.folionet.Domains.SliderItems
import com.app.folionet.R
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.abs

class DashboardFragment : Fragment() {

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
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        fuser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        // Fragment layout views
        val spinner: Spinner = view.findViewById(R.id.filterSpinner)
        progressBar = view.findViewById(R.id.progressBarProjects)
        customDots = view.findViewById(R.id.customDots)
        viewPager2 = view.findViewById(R.id.viewPager2)
        imgProfile = view.findViewById(R.id.imgProfile)
        etSearch = view.findViewById(R.id.etSearch)

        etSearch.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.post_filter_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // If you want to use spinner selection, uncomment and fill in logic
        /*
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (parent.getItemAtPosition(position).toString()) {
                    "All" -> {}
                    "Photos" -> {}
                    "Videos" -> {}
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        */

        fetchSliderDataFromFirestore()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Activity-level views (must be accessed after fragment's view is created)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout1)
        navigationView = requireActivity().findViewById(R.id.navigation_view1)

        val headerView = navigationView.getHeaderView(0)
        profileImageView = headerView.findViewById(R.id.nav_profile_image)
        userNameTextView = headerView.findViewById(R.id.textViewName)
        userEmailTextView = headerView.findViewById(R.id.textViewEmailEmail)

        imgProfile.setOnClickListener {
            (requireActivity() as MainActivity).toggleDrawer()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(requireContext(), ProfileActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    logOut()
                    true
                }
                else -> false
            }
        }

        loadDrawerHeader()
    }

    private fun loadDrawerHeader() {
        val userId: String = fuser.uid
        db.collection("USERS").document(userId).get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val profileImageUrl = documentSnapshot.getString("profilePicUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(profileImageUrl).placeholder(R.drawable.person)
                            .error(R.drawable.no_profile_pic).into(profileImageView)

                        Picasso.get().load(profileImageUrl).placeholder(R.drawable.person)
                            .error(R.drawable.no_profile_pic).into(imgProfile)
                    }

                    userNameTextView.text = documentSnapshot.getString("name") ?: ""
                    userEmailTextView.text = documentSnapshot.getString("email") ?: ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchSliderDataFromFirestore() {
        if (!isAdded || context == null) return

        progressBar.visibility = View.VISIBLE
        db.collection("SliderItems").get()
            .addOnSuccessListener { result ->
                if (!isAdded || context == null) return@addOnSuccessListener

                val sliderItems = mutableListOf<SliderItems>()
                for (doc in result) {
                    val title = doc.getString("title") ?: "No Title"
                    val description = doc.getString("description") ?: ""
                    val techStack = doc.getString("technologiesUsed") ?: ""
                    val imageUrl = doc.getString("imgUrl") ?: ""
                    sliderItems.add(SliderItems(title, description, techStack, imageUrl))
                }
                progressBar.visibility = View.GONE

                val cyclicList = mutableListOf<SliderItems>()
                if (sliderItems.isNotEmpty()) {
                    cyclicList.add(sliderItems.last())
                    cyclicList.addAll(sliderItems)
                    cyclicList.add(sliderItems.first())
                }

                realItemCount = sliderItems.size
                setupViewPager(cyclicList)
                setupCustomDots(realItemCount)
            }
            .addOnFailureListener {
                if (!isAdded || context == null) return@addOnFailureListener

                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load slider data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupViewPager(sliderItems: List<SliderItems>) {
        sliderAdapter = SliderAdapter(sliderItems)
        viewPager2.adapter = sliderAdapter

        viewPager2.clipToPadding = false
        viewPager2.clipChildren = false
        viewPager2.offscreenPageLimit = 3
        (viewPager2.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val transformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(20))
            addTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + r * 0.15f
            }
        }

        viewPager2.setPageTransformer(transformer)
        viewPager2.setCurrentItem(1, false)

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                val realPosition = when (position) {
                    0 -> realItemCount - 1
                    realItemCount + 1 -> 0
                    else -> position - 1
                }
                updateDots(realPosition)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val count = sliderAdapter.itemCount
                    when (viewPager2.currentItem) {
                        0 -> viewPager2.setCurrentItem(count - 2, false)
                        count - 1 -> viewPager2.setCurrentItem(1, false)
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

    private fun setupCustomDots(count: Int) {
        customDots.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(requireContext())
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

    override fun onDestroyView() {
        super.onDestroyView()
        if (::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
        viewPager2.adapter = null
    }

    override fun onPause() {
        super.onPause()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        if (::runnable.isInitialized) handler.postDelayed(runnable, 3000)
        loadDrawerHeader()
    }

    private fun logOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("LOGOUT")
            .setMessage("Do you want to Logout?")
            .setPositiveButton("YES") { _, _ ->
                firebaseAuth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
