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
package org.groundplatform.android.ui.signin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Ignore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentInHiltContainer
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.auth.SignInState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SignInFragmentTest : BaseHiltTest() {

  @BindValue @Mock lateinit var networkManager: NetworkManager

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  @Before
  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setState(SignInState.SignedOut)
  }

  @Test
  fun `Clicking sign-in button when network is available should attempt login`() =
    runWithTestDispatcher {
      whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))
      launchFragmentInHiltContainer<SignInFragment>()
      fakeAuthenticationManager.setUser(TEST_USER)

      // TODO: Replace with a single click action.
      getSignInButton().performClick().performClick()
      advanceUntilIdle()

      fakeAuthenticationManager.signInState.test {
        assertThat(expectMostRecentItem()).isEqualTo(SignInState.SignedIn(TEST_USER))
      }
    }

  @Test
  fun `Sign-in button should be enabled when network is not available`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.UNAVAILABLE))
    launchFragmentInHiltContainer<SignInFragment>()
    fakeAuthenticationManager.setUser(TEST_USER)

    getSignInButton().assertIsEnabled().assertIsDisplayed().performClick()

    // Assert that the sign-in state is still signed out
    fakeAuthenticationManager.signInState.test {
      assertThat(expectMostRecentItem()).isEqualTo(SignInState.SignedOut)
    }

    onView(withId(com.google.android.material.R.id.snackbar_text))
      .check(matches(withText(R.string.network_error_when_signing_in)))
  }

  @Test
  fun `Sign-in should only execute once when clicked multiple times`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))
    launchFragmentInHiltContainer<SignInFragment>()
    fakeAuthenticationManager.setUser(TEST_USER)

    getSignInButton().performClick().performClick()
    advanceUntilIdle()

    fakeAuthenticationManager.signInState.test {
      assertThat(awaitItem()).isEqualTo(SignInState.SignedIn(TEST_USER))
      // Fails if there are further emitted sign-in events.
    }
  }

  @Test
  @Ignore("Fix flakiness on remote builds")
  fun `Back press should finish activity`() {
    launchFragmentInHiltContainer<SignInFragment> {
      val fragment = this as SignInFragment
      assertThat(fragment.onBack()).isFalse()
      assertThat(activity?.isFinishing).isTrue()
    }
  }

  private fun getSignInButton() = composeTestRule.onNodeWithTag(BUTTON_TEST_TAG)

  companion object {
    private val TEST_USER = FakeData.USER
  }
}
