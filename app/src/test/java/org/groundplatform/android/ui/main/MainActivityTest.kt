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
import android.os.Bundle
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.common.truth.Truth.assertThat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.deeplink.PlayInstallReferrerService
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.repository.TermsOfServiceRepositoryInterface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainActivityTest : BaseHiltTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var activity: MainActivity
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var viewModel: MainViewModel
  @Inject lateinit var tosRepository: TermsOfServiceRepositoryInterface

  @Inject lateinit var remoteConfig: FirebaseRemoteConfig

  @BindValue @JvmField val playInstallReferrerService: PlayInstallReferrerService = mock()

  @Test
  fun `Launch app with survey ID navigates to survey selector when user is logged in`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = true
      val uri =
        Uri.parse(
          "https://${getString(R.string.deeplink_host)}${getString(R.string.survey_deeplink_path)}/surveyId"
        )
      val intent = Intent(Intent.ACTION_VIEW, uri)

      Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
        controller.setup()
        activity = controller.get()

        viewModel.setDeepLinkUri(uri)
        advanceUntilIdle()
        val navController = activity.navController()

        fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))
        advanceUntilIdle()

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)

        assertThat(navController.currentBackStackEntry?.arguments?.getString("surveyId"))
          .isEqualTo("surveyId")
      }
    }

  @Test
  fun `Launch app with survey ID shows login when user needs to login`() = runWithTestDispatcher {
    val uri =
      Uri.parse(
        "https://${getString(R.string.deeplink_host)}${getString(R.string.survey_deeplink_path)}/surveyId"
      )
    val intent = Intent(Intent.ACTION_VIEW, uri)

    Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
      controller.setup()
      activity = controller.get()
      val navController = activity.navController()

      fakeAuthenticationManager.setState(SignInState.SignedOut)
      advanceUntilIdle()

      assertThat(navController.currentDestination?.id).isEqualTo(R.id.sign_in_fragment)
    }
  }

  @Test
  fun `Deferred deep link routes to the survey after signing in`() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = true
    whenever(playInstallReferrerService.getDeferredSurveyId()).thenReturn("surveyId")

    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup()
      val navController = controller.get().navController()

      fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))
      advanceUntilIdle()

      assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)
      assertThat(navController.currentBackStackEntry?.arguments?.getString("surveyId"))
        .isEqualTo("surveyId")
    }
  }

  @Test
  fun `Signing in from the sign in screen navigates to the start destination`() =
    runWithTestDispatcher {
      Robolectric.buildActivity(MainActivity::class.java).use { controller ->
        controller.setup()
        val navController = controller.get().navController()

        fakeAuthenticationManager.setState(SignInState.SignedOut)
        advanceUntilIdle()
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.sign_in_fragment)

        tosRepository.isTermsOfServiceAccepted = true
        fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))
        advanceUntilIdle()

        assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)
      }
    }

  @Test
  fun `Restoring after process death keeps the user on the screen they were on`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = true
      val savedState = Bundle()

      Robolectric.buildActivity(MainActivity::class.java).use { controller ->
        controller.setup()
        fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))
        advanceUntilIdle()
        val navController = controller.get().navController()
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)

        navController.navigate(R.id.aboutFragment)
        advanceUntilIdle()
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.aboutFragment)

        controller.saveInstanceState(savedState)
      }

      // A new activity with a cleared view model store, as after the process is killed.
      Robolectric.buildActivity(MainActivity::class.java).use { controller ->
        controller.setup(savedState)
        advanceUntilIdle()

        assertThat(controller.get().navController().currentDestination?.id)
          .isEqualTo(R.id.aboutFragment)
      }
    }

  @Test
  fun `Signing out from another screen returns to the sign in screen`() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = true

    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      controller.setup()
      fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))
      advanceUntilIdle()
      val navController = controller.get().navController()
      navController.navigate(R.id.aboutFragment)
      advanceUntilIdle()

      fakeAuthenticationManager.setState(SignInState.SignedOut)
      advanceUntilIdle()

      assertThat(navController.currentDestination?.id).isEqualTo(R.id.sign_in_fragment)
    }
  }

  private fun MainActivity.navController(): NavController =
    (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
      .navController
}
