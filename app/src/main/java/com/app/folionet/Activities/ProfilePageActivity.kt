package com.app.folionet.Activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.folionet.Adapters.ProfilePagerAdapter
import com.app.folionet.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ProfilePageActivity : AppCompatActivity() {

    private lateinit var profilePagerAdapter: ProfilePagerAdapter
    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout

    private val tabIcons = listOf(
        R.drawable.ic_file,
        R.drawable.ic_reel,
        R.drawable.ic_project
    )

    private val tabTitles = listOf("Posts", "Reels", "Projects")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)

        viewPager = findViewById(R.id.profileViewPager)
        tabLayout = findViewById(R.id.profileTabLayout)

        profilePagerAdapter = ProfilePagerAdapter(this)
        viewPager.adapter = profilePagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
            tab.setIcon(tabIcons[position])
        }.attach()
    }
}