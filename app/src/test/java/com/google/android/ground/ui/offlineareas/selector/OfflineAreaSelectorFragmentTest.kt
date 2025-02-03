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
package com.google.android.ground.ui.offlineareas.selector

import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreaSelectorFragment

  @Inject lateinit var offlineAreaSelectorViewModel: OfflineAreaSelectorViewModel

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<OfflineAreaSelectorFragment> {
      fragment = this as OfflineAreaSelectorFragment
    }
  }

  @Test
  fun `all the buttons are visible`() {
    onView(withId(R.id.download_button)).check(matches(isDisplayed()))
    onView(withId(R.id.cancel_button)).check(matches(isDisplayed()))
    onView(withId(R.id.cancel_button)).check(matches(isEnabled()))
  }

  @Test
  fun `default value of bottomText`() {
    onView(withId(R.id.bottom_text)).check(matches(withText("")))
  }

  @Test
  fun `toolbar text should be correct`() {
    onView(withId(R.id.offline_area_selector_toolbar))
      .check(
        matches(hasDescendant(withText(fragment.getString(R.string.offline_area_selector_title))))
      )
  }

  @Test
  fun `test failure case displays toast`() = runWithTestDispatcher {
    val isFailureObserver = mock(Observer::class.java) as Observer<Boolean>
    fragment.viewLifecycleOwner.lifecycleScope.launch {
      offlineAreaSelectorViewModel.isFailure.observeForever(isFailureObserver)
      offlineAreaSelectorViewModel.isFailure.postValue(true)
    }

    verify(isFailureObserver).onChanged(true)

    ShadowToast.reset()

    advanceUntilIdle()

    val toast = ShadowToast.getLatestToast()
    assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
    assertEquals(toast.duration, Toast.LENGTH_LONG)
    assertEquals(
      ShadowToast.getTextOfLatestToast(),
      fragment.getString(R.string.offline_area_download_error),
    )

    fragment.viewLifecycleOwner.lifecycleScope.launch {
      offlineAreaSelectorViewModel.isFailure.removeObserver(isFailureObserver)
    }
  }
}
