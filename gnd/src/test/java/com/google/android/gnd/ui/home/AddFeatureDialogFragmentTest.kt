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
package com.google.android.gnd.ui.home

import android.os.Looper
import android.view.View
import com.google.android.gnd.BaseHiltTest
import com.google.android.gnd.FakeData
import com.google.android.gnd.MainActivity
import com.google.android.gnd.model.feature.FeatureType
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AddFeatureDialogFragmentTest : BaseHiltTest() {
    lateinit var addFeatureDialogFragment: AddFeatureDialogFragment

    @Override
    @Before
    override fun setUp() {
        super.setUp()
        setUpFragment()
    }

    private fun setUpFragment() {
        val activityController = Robolectric.buildActivity(
            MainActivity::class.java
        )

        addFeatureDialogFragment = AddFeatureDialogFragment()

        val activity = activityController.setup().get()

        val layer1 = FakeData.newJob()
            .setId("Layer 1")
            .setUserCanAdd(ImmutableList.of(FeatureType.POINT))
            .build()
        val layer2 = FakeData.newJob()
            .setId("Layer 2")
            .setUserCanAdd(ImmutableList.of(FeatureType.POLYGON))
            .build()
        addFeatureDialogFragment.show(listOf(layer1, layer2), activity.supportFragmentManager
        ) { }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun show_dialogIsShown() {
        val listView = addFeatureDialogFragment.dialog!!.currentFocus

        assertThat(listView).isNotNull()
        assertThat(listView!!.visibility).isEqualTo(View.VISIBLE)
    }
}
