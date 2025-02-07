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
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import junit.framework.Assert.assertFalse
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreaSelectorFragment
  @Inject lateinit var viewModel: OfflineAreaSelectorViewModel

  private val offlineAreaRepository: OfflineAreaRepository = mock()

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

  // TODO: Complete below test
  // Issue URL: https://github.com/google/ground-android/issues/3032
  @Test
  fun `stopDownloading cancels active download and updates UI state`() = runWithTestDispatcher {
    composeTestRule.setContent { DownloadProgressDialog(viewModel.downloadProgress.value!!, {}) }

    val progressFlow = MutableSharedFlow<Pair<Int, Int>>()
    whenever(offlineAreaRepository.downloadTiles(any())).thenReturn(progressFlow)

    val downloadProgressValues = mutableListOf<Float>()
    val observer = Observer<Float> { downloadProgressValues.add(it) }

    viewModel.downloadProgress.observeForever(observer)

    viewModel.onDownloadClick()
    advanceUntilIdle()

    progressFlow.emit(Pair(50, 100))
    advanceUntilIdle()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .isDisplayed()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()
    progressFlow.emit(Pair(75, 100))

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .isNotDisplayed()

    assertFalse(viewModel.isDownloadProgressVisible.value!!)
    assertNull(viewModel.downloadJob)

    viewModel.downloadProgress.removeObserver(observer)
  }

  // TODO: Mock complete download flow
  // Issue URL: https://github.com/google/ground-android/issues/3038
  @Test
  fun `test failure case displays toast`() = runWithTestDispatcher {
    val isFailureObserver = mock(Observer::class.java) as Observer<Boolean>
    fragment.viewLifecycleOwner.lifecycleScope.launch {
      viewModel.isFailure.observeForever(isFailureObserver)
      viewModel.isFailure.postValue(true)
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
      viewModel.isFailure.removeObserver(isFailureObserver)
    }
  }
}
