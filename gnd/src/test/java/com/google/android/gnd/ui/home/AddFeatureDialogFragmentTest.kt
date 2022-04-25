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
import javax.inject.Inject
import com.google.android.gnd.ui.home.AddFeatureDialogFragment
import org.junit.Before
import org.robolectric.android.controller.ActivityController
import com.google.android.gnd.MainActivity
import org.robolectric.Robolectric
import org.robolectric.Shadows
import android.os.Looper
import android.view.View
import com.google.common.truth.Truth
import com.google.android.gnd.R
import org.junit.Test

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AddFeatureDialogFragmentTest : BaseHiltTest() {
    @Inject
    var addFeatureDialogFragment: AddFeatureDialogFragment? = null
    @Before
    override fun setUp() {
        super.setUp()
        setUpFragment()
    }

    private fun setUpFragment() {
        val activityController = Robolectric.buildActivity(
            MainActivity::class.java
        )
        val activity = activityController.setup().get()
        addFeatureDialogFragment!!.showNow(
            activity.supportFragmentManager, AddFeatureDialogFragment::class.java.simpleName
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun show_dialogIsShown() {
        val listView = addFeatureDialogFragment!!.dialog!!.currentFocus
        Truth.assertThat(listView).isNotNull()
        Truth.assertThat(listView!!.visibility).isEqualTo(View.VISIBLE)
        Truth.assertThat(listView.findViewById<View>(R.id.project_name).visibility)
            .isEqualTo(View.VISIBLE)
    } //  @Test
    //  public void show_dataTypeSelected_correctDataTypeIsPassed() {
    //    ListView listView =
    //        (ListView) addFeatureDialogFragment.getDialog().getCurrentFocus();
    //
    //    int positionToSelect = 1;
    //    shadowOf(listView).performItemClick(positionToSelect);
    //    shadowOf(getMainLooper()).idle();
    //
    //    // Verify Dialog is dismissed
    //    assertThat(addFeatureDialogFragment.getDialog()).isNull();
    //
    //    assertThat(selectedPosition).isEqualTo(1);
    //  }
}