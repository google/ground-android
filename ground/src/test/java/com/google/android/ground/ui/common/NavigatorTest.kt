/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.common

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.testNavigate
import com.google.android.ground.testNavigateTo
import com.google.android.ground.ui.signin.SignInFragmentDirections
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class NavigatorTest : BaseHiltTest() {

  @Inject lateinit var navigator: Navigator

  @Test
  fun `navigate up`() = runWithTestDispatcher {
    testNavigate(navigator.getNavigateRequests(), { assertThat(it).isEqualTo(NavigateUp) }) {
      navigator.navigateUp()
    }
  }

  @Test
  fun `finish app`() = runWithTestDispatcher {
    testNavigate(navigator.getNavigateRequests(), { assertThat(it).isEqualTo(FinishApp) }) {
      navigator.finishApp()
    }
  }

  @Test
  fun `navigate to`() = runWithTestDispatcher {
    testNavigateTo(navigator.getNavigateRequests(), SignInFragmentDirections.showSignInScreen()) {
      navigator.navigate(SignInFragmentDirections.showSignInScreen())
    }
  }
}
