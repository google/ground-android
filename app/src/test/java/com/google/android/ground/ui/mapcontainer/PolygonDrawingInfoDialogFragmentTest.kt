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
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.ui.home.mapcontainer.PolygonDrawingInfoDialogFragment
import com.google.android.ground.ui.surveyselector.SurveySelectorDialogFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PolygonDrawingInfoDialogFragmentTest : BaseHiltTest() {

  private var clicked = false
  private lateinit var polygonDrawingInfoDialogFragment: PolygonDrawingInfoDialogFragment
  private lateinit var onClickRunnable: Runnable

  @Before
  override fun setUp() {
    super.setUp()
    clicked = false
    onClickRunnable = Runnable { clicked = true }
    setUpFragment()
  }

  private fun setUpFragment() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    val activity = activityController.setup().get()

    polygonDrawingInfoDialogFragment = PolygonDrawingInfoDialogFragment(onClickRunnable)
    polygonDrawingInfoDialogFragment.showNow(
      activity.supportFragmentManager,
      SurveySelectorDialogFragment::class.java.simpleName
    )
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun show_dialogIsShown() {
    val dialogView = polygonDrawingInfoDialogFragment.dialog?.currentFocus

    assertThat(dialogView).isNotNull()
    assertThat(dialogView?.visibility).isEqualTo(View.VISIBLE)
    assertThat(
        (dialogView?.parent as ConstraintLayout)
          .findViewById<View>(R.id.polygon_info_image)
          .visibility
      )
      .isEqualTo(View.VISIBLE)
  }

  @Test
  fun startClicked_clickIsRegisteredAndDialogIsDismissed() {
    val dialogView =
      polygonDrawingInfoDialogFragment.dialog?.currentFocus?.parent as ConstraintLayout
    shadowOf(dialogView.findViewById(R.id.get_started_button) as View).checkedPerformClick()
    shadowOf(Looper.getMainLooper()).idle()

    // Verify Dialog is dismissed
    assertThat(polygonDrawingInfoDialogFragment.dialog).isNull()
    assertThat(clicked).isTrue()
  }

  @Test
  fun cancelClicked_dialogIsDismissed() {
    val dialogView =
      polygonDrawingInfoDialogFragment.dialog?.currentFocus?.parent as ConstraintLayout
    shadowOf(dialogView.findViewById(R.id.cancel_text_view) as View).checkedPerformClick()
    shadowOf(Looper.getMainLooper()).idle()

    // Verify Dialog is dismissed
    assertThat(polygonDrawingInfoDialogFragment.dialog).isNull()
  }
}
