/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.ui.datacollection

import android.os.Looper.getMainLooper
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.rx.Loadable
import com.google.android.ground.ui.common.Navigator
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

    private lateinit var dataCollectionFragment: DataCollectionFragment
    private lateinit var activity: MainActivity

    @BindValue
    @Mock
    lateinit var dataCollectionViewModel: DataCollectionViewModel

    @Inject
    lateinit var navigator: Navigator

    @Before
    override fun setUp() {
        super.setUp()

        val activityController = Robolectric.buildActivity(
            MainActivity::class.java
        ).setup()
        activity = activityController.get()
    }

    @Test
    fun created_submissionIsLoaded() {
        setupFragment()
        verify(dataCollectionViewModel).loadSubmissionDetails(
            eq(DataCollectionTestData.args)
        )
    }

    @Test
    fun created_submissionIsLoaded_viewPagerAdapterIsSet() {
        whenever(dataCollectionViewModel.submission).doReturn(
            MutableLiveData(Loadable.loaded(DataCollectionTestData.submission))
        )

        setupFragment()

        val viewPager = dataCollectionFragment.view!!.findViewById<ViewPager2>(R.id.pager)
        assertThat(viewPager).isNotNull()
        assertThat(viewPager.adapter).isInstanceOf(DataCollectionViewPagerAdapter::class.java)
    }

    @Test
    fun onNextClicked_viewPagerItemIsUpdated() {
        whenever(dataCollectionViewModel.submission).doReturn(
            MutableLiveData(Loadable.loaded(DataCollectionTestData.submission))
        )

        setupFragment()

        val viewPager = dataCollectionFragment.view!!.findViewById<ViewPager2>(R.id.pager)
        assertThat(viewPager.currentItem).isEqualTo(0)
        dataCollectionFragment.view!!.findViewById<Button>(R.id.data_collection_next_button)
            .performClick()
        assertThat(viewPager.currentItem).isEqualTo(1)
    }

    @Test
    fun onNextClicked_thenOnBack_viewPagerItemIsUpdated() {
        whenever(dataCollectionViewModel.submission).doReturn(
            MutableLiveData(Loadable.loaded(DataCollectionTestData.submission))
        )

        setupFragment()

        val viewPager = dataCollectionFragment.view!!.findViewById<ViewPager2>(R.id.pager)
        assertThat(viewPager.currentItem).isEqualTo(0)
        dataCollectionFragment.view!!.findViewById<Button>(R.id.data_collection_next_button)
            .performClick()
        assertThat(viewPager.currentItem).isEqualTo(1)
        assertThat(dataCollectionFragment.onBack()).isTrue()
        assertThat(viewPager.currentItem).isEqualTo(0)
    }

    @Test
    fun onBack_firstViewPagerItem_returnsFalse() {
        whenever(dataCollectionViewModel.submission).doReturn(
            MutableLiveData(Loadable.loaded(DataCollectionTestData.submission))
        )

        setupFragment()

        shadowOf(getMainLooper()).idle()
        assertThat(dataCollectionFragment.onBack()).isFalse()
    }

    private fun setupFragment() {
        dataCollectionFragment = DataCollectionFragment()
        dataCollectionFragment.arguments = DataCollectionTestData.args.toBundle()
        activity.supportFragmentManager.beginTransaction().add(dataCollectionFragment, null)
            .commitNow()
    }
}