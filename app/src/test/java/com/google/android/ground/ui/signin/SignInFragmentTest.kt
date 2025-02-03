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
package com.google.android.ground.ui.signin

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.NetworkStatus
import com.google.android.ground.system.auth.FakeAuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Ignore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.Matchers.allOf
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

      onView(withId(R.id.sign_in_button)).perform(click())
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

    onView(allOf(withId(R.id.sign_in_button), isDisplayed(), isEnabled())).perform(click())

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

    onView(withId(R.id.sign_in_button)).perform(click())
    onView(withId(R.id.sign_in_button)).perform(click())
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

  companion object {
    private val TEST_USER = FakeData.USER
  }
}
