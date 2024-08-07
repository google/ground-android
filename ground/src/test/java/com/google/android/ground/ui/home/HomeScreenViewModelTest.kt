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
package com.google.android.ground.ui.home

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.android.ground.testNavigateTo
import com.google.android.ground.ui.common.Navigator
import com.sharedtest.FakeData.OFFLINE_AREA
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenViewModelTest : BaseHiltTest() {

  @Inject lateinit var navigator: Navigator
  @Inject lateinit var homeScreenViewModel: HomeScreenViewModel
  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore

  @Test
  fun `showAbout is navigating to correct screen`() = runWithTestDispatcher {
    testNavigateTo(navigator.getNavigateRequests(), HomeScreenFragmentDirections.showAbout()) {
      homeScreenViewModel.showAbout()
    }
  }

  @Test
  fun `showTermsOfService is navigating to correct screen`() = runWithTestDispatcher {
    testNavigateTo(
      navigator.getNavigateRequests(),
      HomeScreenFragmentDirections.showTermsOfService(true),
    ) {
      homeScreenViewModel.showTermsOfService()
    }
  }

  @Test
  fun `showSyncStatus is navigating to correct screen`() = runWithTestDispatcher {
    testNavigateTo(navigator.getNavigateRequests(), HomeScreenFragmentDirections.showSyncStatus()) {
      homeScreenViewModel.showSyncStatus()
    }
  }

  @Test
  fun `showSettings is navigating to correct screen`() = runWithTestDispatcher {
    testNavigateTo(
      navigator.getNavigateRequests(),
      HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity(),
    ) {
      homeScreenViewModel.showSettings()
    }
  }

  @Test
  fun `showSurveySelector is navigating to correct screen`() = runWithTestDispatcher {
    testNavigateTo(
      navigator.getNavigateRequests(),
      HomeScreenFragmentDirections.actionHomeScreenFragmentToSurveySelectorFragment(false),
    ) {
      homeScreenViewModel.showSurveySelector()
    }
  }

  @Test
  fun `showOfflineAreas is navigating to correct screen when offlineAreas are not empty`() =
    runWithTestDispatcher {
      localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
      advanceUntilIdle()

      testNavigateTo(
        navigator.getNavigateRequests(),
        HomeScreenFragmentDirections.showOfflineAreas(),
      ) {
        homeScreenViewModel.showOfflineAreas()
      }
    }

  @Test
  fun `showOfflineAreas is navigating to correct screen when offlineAreas are empty`() =
    runWithTestDispatcher {
      testNavigateTo(
        navigator.getNavigateRequests(),
        HomeScreenFragmentDirections.showOfflineAreaSelector(),
      ) {
        homeScreenViewModel.showOfflineAreas()
      }
    }
}
