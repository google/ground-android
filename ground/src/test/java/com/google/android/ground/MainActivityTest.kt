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
package com.google.android.ground

import com.google.android.ground.system.auth.SignInState
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowProgressDialog

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainActivityTest : BaseHiltTest() {

  private lateinit var activity: MainActivity
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  override fun setUp() {
    super.setUp()
    ShadowProgressDialog.reset()
  }

  @Test
  fun signInProgressDialog_whenSigningIn_isDisplayed() = runWithTestDispatcher {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      fakeAuthenticationManager.setState(SignInState.SigningIn)
      advanceUntilIdle()

      assertThat(ShadowProgressDialog.getLatestDialog().isShowing).isTrue()
    }
  }

  @Test
  fun signInProgressDialog_whenSignedOutAfterSignInState_isNotDisplayed() = runWithTestDispatcher {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      fakeAuthenticationManager.setState(SignInState.SigningIn)
      fakeAuthenticationManager.setState(SignInState.SignedOut)
      advanceUntilIdle()

      assertThat(ShadowProgressDialog.getLatestDialog().isShowing).isFalse()
    }
  }

  @Test
  fun signInErrorDialog_isDisplayed() = runWithTestDispatcher {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      fakeAuthenticationManager.setState(
        SignInState.Error(
          error =
            FirebaseFirestoreException("error", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        )
      )
      advanceUntilIdle()

      assertThat(ShadowProgressDialog.getLatestDialog().isShowing).isTrue()
    }
  }
}
