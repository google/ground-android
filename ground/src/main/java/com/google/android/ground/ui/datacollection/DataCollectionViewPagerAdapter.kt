package com.google.android.ground.ui.datacollection

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.ground.model.task.Step
import com.google.common.collect.ImmutableList

/**
 * A simple pager adapter that presents the [Step]s associated with a given Submission, in
 * sequence.
 */
class DataCollectionViewPagerAdapter(
    fragment: Fragment,
    private val steps: ImmutableList<Step>
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = steps.size

    override fun createFragment(position: Int): Fragment = DataCollectionTaskFragment()
}