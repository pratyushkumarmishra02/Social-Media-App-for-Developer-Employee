package com.app.folionet.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.folionet.Fragments.PostsFragment
import com.app.folionet.Fragments.ProjectsFragment
import com.app.folionet.Fragments.ReelsFragment

class ProfilePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        PostsFragment(),
        ReelsFragment(),
        ProjectsFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
