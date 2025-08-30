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
package org.groundplatform.android.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.navigation.fragment.NavHostFragment
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.repository.TermsOfServiceRepository
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.auth.SignInState
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowProgressDialog

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainActivityTest : BaseHiltTest() {

  private lateinit var activity: MainActivity
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var viewModel: MainViewModel
  @Inject lateinit var tosRepository: TermsOfServiceRepository

  @Inject lateinit var remoteConfig: FirebaseRemoteConfig

  override fun setUp() {
    super.setUp()
    ShadowProgressDialog.reset()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  override fun closeDb() {
    super.closeDb()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun `Sign in progress dialog is displayed when signing in`() = runWithTestDispatcher {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      Mockito.`when`(remoteConfig.getBoolean("force_update")).thenReturn(false)
      Mockito.`when`(remoteConfig.getString("min_app_version")).thenReturn("0.0.0")

      controller.setup() // Moves Activity to RESUMED state
      activity = controller.get()

      advanceUntilIdle()

      fakeAuthenticationManager.setState(SignInState.SigningIn)
      advanceUntilIdle()

      assertThat(ShadowProgressDialog.getLatestDialog().isShowing).isTrue()
    }
  }

  @Test
  fun `Sign in progress dialog is not displayed when signed out after sign in state`() =
    runWithTestDispatcher {
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
  fun `Sign in error dialog is displayed when sign in fails`() = runWithTestDispatcher {
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

  @Test
  fun `Launch app with survey ID navigates to survey selector when user is logged in`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = true
      val uri = Uri.parse("https://groundplatform.org/android/survey/surveyId")
      val intent = Intent(Intent.ACTION_VIEW, uri)

      Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
        controller.setup()
        activity = controller.get()

        viewModel.setDeepLinkUri(uri)
        advanceUntilIdle()
        val navHost =
          activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            as NavHostFragment
        val navController = navHost.navController

        fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))
        advanceUntilIdle()

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)

        assertThat(navController.currentBackStackEntry?.arguments?.getString("surveyId"))
          .isEqualTo("surveyId")
      }
    }

  @Test
  fun `Launch app with survey ID shows login when user needs to login`() = runWithTestDispatcher {
    val uri = Uri.parse("https://groundplatform.org/android/survey/surveyId")
    val intent = Intent(Intent.ACTION_VIEW, uri)

    Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
      controller.setup()
      activity = controller.get()

      val navHost =
        activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
      val navController = navHost.navController

      fakeAuthenticationManager.setState(SignInState.SignedOut)
      advanceUntilIdle()

      assertThat(navController.currentDestination?.id).isEqualTo(R.id.sign_in_fragment)
    }
  }
}
