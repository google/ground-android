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
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.system.auth.SignInState
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SignInFragmentTest : BaseHiltTest() {

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setState(SignInState.signedOut())
  }

  @Test
  fun buttonClick_shouldSignIn() {
    launchFragmentInHiltContainer<SignInFragment>()
    fakeAuthenticationManager.setUser(TEST_USER)

    onView(withId(R.id.sign_in_button)).perform(click())

    fakeAuthenticationManager.signInState
      .test()
      .assertValue(SignInState(SignInState.State.SIGNED_IN, Result.success(TEST_USER)))
  }

  @Test
  fun pressBack_shouldFinishActivity() {
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
