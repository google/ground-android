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
package org.groundplatform.android.ui.tos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentInHiltContainer
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.TermsOfService
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.repository.TermsOfServiceRepository
import org.groundplatform.android.system.NetworkManager
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Suppress("StringShouldBeRawString")
class TermsOfServiceFragmentTest : BaseHiltTest() {

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var termsOfServiceRepository: TermsOfServiceRepository
  private lateinit var navController: NavController

  @BindValue @Mock lateinit var networkManager: NetworkManager

  /**
   * composeTestRule has to be created in the specific test file in order to access the required
   * activity. [composeTestRule.activity]
   */
  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.termsOfService = Result.success(TEST_TOS)
  }

  @Test
  fun `Toolbar is displayed`() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.tos_title))
      .assertIsDisplayed()
  }

  @Test
  fun `Toolbar Back Arrow is displayed`() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", true)))

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun termsOfServiceText_shouldBeDisplayed() {
    whenever(networkManager.isNetworkConnected()).thenReturn(true)
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    composeTestRule.onNodeWithText("This is a heading\n\nSample terms of service\n\n").isDisplayed()

    composeTestRule
      .onNodeWithText(
        "<p dir=\"ltr\"><span style=\"font-size:1.50em;\"><b>This is a heading</b></span></p>\n" +
          "<p dir=\"ltr\">Sample terms of service</p>\n"
      )
      .isDisplayed()
  }

  @Test
  fun agreeButton_default_shouldNotBeEnabled() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    getCheckbox().assertIsDisplayed()
    getButton().assertIsNotEnabled()
  }

  @Test
  fun agreeButton_whenCheckBoxClicked_shouldBeEnabled() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    getCheckbox().performClick()

    getButton().assertIsEnabled()
  }

  @Test
  fun agreeButton_whenPressed_shouldUpdatePrefAndNavigate() = runWithTestDispatcher {
    launchFragmentWithNavController<TermsOfServiceFragment>(
      fragmentArgs = bundleOf(Pair("isViewOnly", false)),
      destId = R.id.terms_of_service_fragment,
      navControllerCallback = { navController = it },
    )

    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()

    getCheckbox().performClick()
    getButton().performClick()

    assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun viewOnlyMode_controlsHidden() = runWithTestDispatcher {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", true)))

    getCheckbox().assertIsNotDisplayed()
    getButton().assertIsNotDisplayed()
  }

  private fun getCheckbox() =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.agree_checkbox))

  private fun getButton() =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.agree_terms))

  companion object {
    const val TEST_TOS_TEXT = "# This is a heading\n\nSample terms of service"
    val TEST_TOS = TermsOfService("TERMS_OF_SERVICE", TEST_TOS_TEXT)
  }
}
