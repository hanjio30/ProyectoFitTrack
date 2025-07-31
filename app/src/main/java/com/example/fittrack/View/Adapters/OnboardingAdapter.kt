package com.example.fittrack.View.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fittrack.View.ui.fragments.OnboardingFragment

class OnboardingAdapter(
    fragmentActivity: FragmentActivity,
    private val itemCount: Int = 5
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = itemCount

    override fun createFragment(position: Int): Fragment {
        return OnboardingFragment.newInstance(position)
    }
}