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
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.NetworkStatus
import com.google.android.ground.system.auth.SignInState
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SignInFragmentTest : BaseHiltTest() {

  @BindValue @Mock lateinit var networkManager: NetworkManager

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setState(SignInState.signedOut())
  }

  @Test
  fun `Clicking sign-in button when network is available should attempt login`() {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))
    launchFragmentInHiltContainer<SignInFragment>()
    fakeAuthenticationManager.setUser(TEST_USER)

    onView(withId(R.id.sign_in_button)).perform(click())

    fakeAuthenticationManager.signInState
      .test()
      .assertValue(SignInState(SignInState.State.SIGNED_IN, Result.success(TEST_USER)))
  }

  @Test
  fun `Sign-in button should be disabled when network is not available`() {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.UNAVAILABLE))
    launchFragmentInHiltContainer<SignInFragment>()
    fakeAuthenticationManager.setUser(TEST_USER)

    onView(allOf(withId(R.id.sign_in_button), isDisplayed(), isNotEnabled())).perform(click())

    // Assert that the sign-in state is still signed out
    fakeAuthenticationManager.signInState
      .test()
      .assertValue(SignInState(SignInState.State.SIGNED_OUT, Result.success(null)))

    onView(withId(com.google.android.material.R.id.snackbar_text))
      .check(matches(withText(R.string.network_error_when_signing_in)))
  }

  @Test
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
