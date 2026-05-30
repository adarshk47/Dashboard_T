package com.messageorganizer.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.messageorganizer.ui.groups.GroupsFragment
import com.messageorganizer.ui.messages.AllMessagesFragment
import com.messageorganizer.ui.senders.SendersFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> GroupsFragment()
        1 -> SendersFragment()
        2 -> AllMessagesFragment()
        else -> GroupsFragment()
    }
}
