/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.ui.offlineareas.viewer

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.OFFLINE_AREA
import org.groundplatform.android.R
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.util.view.isGone
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaViewerFragmentTest : BaseHiltTest() {

  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  private lateinit var fragment: OfflineAreaViewerFragment

  @Test
  fun `RemoveButton is displayed and enable`() = runWithTestDispatcher {
    setupFragment()
    onView(withId(R.id.remove_button)).check(matches(isDisplayed()))
    onView(withId(R.id.remove_button)).check(matches(isEnabled()))
  }

  @Test
  fun `All values are correctly displayed`() = runWithTestDispatcher {
    setupFragment()
    onView(withId(R.id.offline_area_name_text)).check(matches(withText(OFFLINE_AREA.name)))
    onView(withId(R.id.offline_area_size_on_device)).check(matches(withText("<1\u00A0MB on disk")))
    onView(withId(R.id.remove_button))
      .check(matches(withText(fragment.getString(R.string.offline_area_viewer_remove_button))))
    onView(withId(R.id.offline_area_viewer_toolbar))
      .check(
        matches(hasDescendant(withText(fragment.getString(R.string.offline_area_viewer_title))))
      )
  }

  @Test
  fun `When no offline areas available`() = runWithTestDispatcher {
    setupFragmentWithoutDb()
    advanceUntilIdle()
    onView(withId(R.id.offline_area_viewer_toolbar))
      .check(
        matches(hasDescendant(withText(fragment.getString(R.string.offline_area_viewer_title))))
      )
    onView(withId(R.id.offline_area_name_text)).check(matches(withText("")))
    onView(withId(R.id.offline_area_size_on_device)).check(matches(isGone()))
    onView(withId(R.id.remove_button)).check(matches(isNotEnabled()))
    onView(withId(R.id.remove_button))
      .check(matches(withText(fragment.getString(R.string.offline_area_viewer_remove_button))))
  }

  @Test
  fun `default mapConfig value should be correct`() {
    setupFragment()

    assertThat(fragment.getMapConfig())
      .isEqualTo(
        MapConfig(
          allowGestures = false,
          overrideMapType = MapType.TERRAIN,
          showOfflineImagery = true,
        )
      )
  }

  private fun setupFragment() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
    setupFragmentWithoutDb()
  }

  private fun setupFragmentWithoutDb(fragmentArgs: Bundle? = null) = runWithTestDispatcher {
    val argsBundle =
      fragmentArgs ?: OfflineAreaViewerFragmentArgs.Builder("id_1").build().toBundle()

    launchFragmentWithNavController<OfflineAreaViewerFragment>(
      argsBundle,
      destId = R.id.offline_area_viewer_fragment,
    ) {
      fragment = this as OfflineAreaViewerFragment
    }
  }
}
