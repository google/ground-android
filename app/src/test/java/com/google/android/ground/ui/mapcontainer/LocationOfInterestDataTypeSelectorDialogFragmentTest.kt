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
package com.google.android.ground.ui.mapcontainer

import android.os.Looper
import android.view.View
import android.widget.ListView
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.ui.home.mapcontainer.LocationOfInterestDataTypeSelectorDialogFragment
import com.google.android.ground.ui.surveyselector.SurveySelectorDialogFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java8.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationOfInterestDataTypeSelectorDialogFragmentTest : BaseHiltTest() {
  private lateinit var dialogFragment: LocationOfInterestDataTypeSelectorDialogFragment
  private lateinit var onSelectLoiDataType: Consumer<Int>
  private var selectedPosition = -1

  @Before
  override fun setUp() {
    super.setUp()
    selectedPosition = -1
    onSelectLoiDataType = Consumer { integer: Int -> selectedPosition = integer }
    setUpFragment()
  }

  private fun setUpFragment() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    val activity = activityController.setup().get()

    dialogFragment = LocationOfInterestDataTypeSelectorDialogFragment(onSelectLoiDataType)

    dialogFragment.showNow(
      activity.supportFragmentManager,
      SurveySelectorDialogFragment::class.java.simpleName
    )
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun show_dialogIsShown() {
    val listView = dialogFragment.dialog!!.currentFocus as ListView

    assertThat(listView.visibility).isEqualTo(View.VISIBLE)
    assertThat(listView.findViewById<View>(R.id.survey_name)?.visibility).isEqualTo(View.VISIBLE)
  }

  @Test
  fun show_dataTypeSelected_correctDataTypeIsPassed() {
    val listView = dialogFragment.dialog!!.currentFocus as ListView

    val positionToSelect = 1
    shadowOf(listView).performItemClick(positionToSelect)
    shadowOf(Looper.getMainLooper()).idle()

    // Verify Dialog is dismissed
    assertThat(dialogFragment.dialog).isNull()
    assertThat(selectedPosition).isEqualTo(1)
  }
}
