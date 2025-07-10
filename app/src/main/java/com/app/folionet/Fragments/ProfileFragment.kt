package com.app.folionet.Fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.app.folionet.Activities.ProfileActivity
import com.app.folionet.Adapters.ProfilePagerAdapter
import com.app.folionet.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private var profileImage: CircleImageView? = null
    private var nameText: TextView? = null
    private var usernameText: TextView? = null
    private var bioText: TextView? = null
    private var postsText: TextView? = null
    private var followersText: TextView? = null
    private var followingText: TextView? = null
    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var toggleDetails: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        profileImage = view.findViewById(R.id.profile_image)
        nameText = view.findViewById(R.id.name)
        usernameText = view.findViewById(R.id.username)
        bioText = view.findViewById(R.id.bio)
        postsText = view.findViewById(R.id.posts)
        followersText = view.findViewById(R.id.followers)
        followingText = view.findViewById(R.id.following)
        tabLayout = view.findViewById(R.id.profileTabLayout)
        viewPager = view.findViewById(R.id.profileViewPager)
        toggleDetails = view.findViewById(R.id.toggleDetails)

        toggleDetails?.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        // Set up ViewPager2 and TabLayout
        val adapter = ProfilePagerAdapter(requireActivity())
        viewPager?.adapter = adapter

        val tabTitles = listOf("Posts", "Reels", "Projects")
        val tabIcons = listOf(R.drawable.doted_post, R.drawable.ic_reel, R.drawable.ic_file)

        tabLayout?.let { tabLayout ->
            viewPager?.let { viewPager ->
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    when (position) {
                        0 -> {
                            tab.text = tabTitles[position]
                            tab.setIcon(tabIcons[position])
                        }
                        1 -> {
                            tab.text = tabTitles[position]
                            tab.setIcon(tabIcons[position])
                        }
                        2 -> {
                            tab.text = tabTitles[position]
                            tab.setIcon(tabIcons[position])
                        }

                    }
                }.attach()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileImage = null
        nameText = null
        usernameText = null
        bioText = null
        postsText = null
        followersText = null
        followingText = null
        tabLayout = null
        viewPager = null
    }
}
