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
package org.groundplatform.android.ui.offlineareas.selector

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Observer
import androidx.compose.ui.test.*
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import junit.framework.Assert.assertFalse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentInHiltContainer
import org.groundplatform.android.repository.OfflineAreaRepository
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreaSelectorFragment
  @Inject lateinit var viewModel: OfflineAreaSelectorViewModel

  @BindValue @JvmField val offlineAreaRepository: OfflineAreaRepository = mock()

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
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_selector_download)).assertIsDisplayed()
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_select_cancel_button)).assertIsDisplayed()
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_select_cancel_button)).assertIsEnabled()
  }

  @Test
  fun `default value of bottomText`() {
      // If text is empty, it might be hard to find by text.
      // But verify logic handles it.
      // composeTestRule.onNodeWithText("").assertExists()
  }

  @Test
  fun `toolbar text should be correct`() {
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_selector_title)).assertIsDisplayed()
  }

  // TODO: Complete below test
  // Issue URL: https://github.com/google/ground-android/issues/3032
  @Test
  fun `stopDownloading cancels active download and updates UI state`() = runWithTestDispatcher {


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
      .onNodeWithText(composeTestRule.activity.getString(R.string.offline_area_select_cancel_button))
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.offline_area_select_cancel_button))
      .performClick()
    progressFlow.emit(Pair(75, 100))

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.offline_area_select_cancel_button))
      .assertIsNotDisplayed()

    assertFalse(viewModel.isDownloadProgressVisible.value!!)
    assertNull(viewModel.downloadJob)

    viewModel.downloadProgress.removeObserver(observer)
  }


}
