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

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.android.gnd.BaseHiltTest
import org.junit.Before
import com.google.android.gnd.MainActivity
import org.robolectric.Robolectric
import org.robolectric.Shadows
import android.os.Looper
import android.view.View
import com.google.android.gnd.FakeData
import com.google.common.truth.Truth.assertThat
import com.google.android.gnd.R
import com.google.android.gnd.model.feature.FeatureType
import com.google.common.collect.ImmutableList
import org.junit.Test

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AddFeatureDialogFragmentTest : BaseHiltTest() {
    var addFeatureDialogFragment: AddFeatureDialogFragment? = null

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

        val layer1 = FakeData.newLayer()
            .setId("Layer 1")
            .setContributorsCanAdd(ImmutableList.of(FeatureType.POINT))
            .build()
        val layer2 = FakeData.newLayer()
            .setId("Layer 2")
            .setContributorsCanAdd(ImmutableList.of(FeatureType.POLYGON))
            .build()
        addFeatureDialogFragment!!.show(listOf(layer1, layer2), activity.supportFragmentManager
        ) { }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun show_dialogIsShown() {
        val listView = addFeatureDialogFragment!!.dialog!!.currentFocus

        assertThat(listView).isNotNull()
        assertThat(listView!!.visibility).isEqualTo(View.VISIBLE)
    }
}