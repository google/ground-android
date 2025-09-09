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
package org.groundplatform.android.ui.surveyselector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentInHiltContainer
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySelectorFragmentTest : BaseHiltTest() {

  @BindValue @Mock lateinit var surveyRepository: SurveyRepository
  @BindValue @Mock lateinit var userRepository: UserRepository
  @BindValue @Mock lateinit var activateSurvey: ActivateSurveyUseCase
  @BindValue @Mock lateinit var listAvailableSurveysUseCase: ListAvailableSurveysUseCase
  @BindValue @Mock lateinit var removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  private lateinit var fragment: SurveySelectorFragment
  private lateinit var navController: NavController

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(TEST_USER)
  }

  @Test
  fun `Surveys are available when no survey is synced`() {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setUpFragment()

    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_on_device), 0)
      )
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_shared_with_me), 2)
      )
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_public), 0)
      )
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_shared_with_me), 2)
      )
      .performClick()

    composeTestRule.onNodeWithText(TEST_SURVEY_1.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY_2.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY_1.description).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY_2.description).assertIsDisplayed()

    composeTestRule
      .onAllNodesWithText(composeTestRule.activity.getString(R.string.access_restricted))
      .assertCountEquals(2)
  }

  @Test
  fun `Surveys are available when one survey is synced`() {
    val syncedSurvey = TEST_SURVEY_2.copy(availableOffline = true)
    val unsyncedSurvey = TEST_SURVEY_1
    setSurveyList(listOf(syncedSurvey, unsyncedSurvey))
    setUpFragment()

    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_on_device), 1)
      )
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY_2.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY_2.description).assertIsDisplayed()
  }

  @Test
  fun `Click activates survey`() = runWithTestDispatcher {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    whenever(activateSurvey(TEST_SURVEY_2.id)).thenReturn(true)

    launchFragmentWithNavController<SurveySelectorFragment>(
      fragmentArgs = bundleOf(Pair("shouldExitApp", false), Pair("surveyId", "")),
      destId = R.id.surveySelectorFragment,
      navControllerCallback = { navController = it },
    )
    advanceUntilIdle()

    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_shared_with_me), 2)
      )
      .performClick()
    advanceUntilIdle()
    composeTestRule.onNodeWithText(TEST_SURVEY_2.title).performClick()
    advanceUntilIdle()

    // Assert that navigation to home screen was requested
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.home_screen_fragment)
    // No error toast should be displayed
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Click activates survey when active survey fails`() = runWithTestDispatcher {
    whenever(activateSurvey(any())).thenThrow(Error("Some exception"))

    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))

    launchFragmentWithNavController<SurveySelectorFragment>(
      fragmentArgs = bundleOf(Pair("shouldExitApp", false), Pair("surveyId", "")),
      destId = R.id.surveySelectorFragment,
      navControllerCallback = { navController = it },
    )
    advanceUntilIdle()

    // Click second item
    composeTestRule
      .onNodeWithText(
        formatSectionTitle(composeTestRule.activity.getString(R.string.section_shared_with_me), 2)
      )
      .performClick()
    composeTestRule.onNodeWithText(TEST_SURVEY_2.title).performClick()
    advanceUntilIdle()

    // Assert survey is activated.
    verify(activateSurvey).invoke(TEST_SURVEY_2.id)
    // Assert that navigation to home screen was not requested
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)
    // Error toast message
    assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
  }

  @Test
  fun `Should exit app on back press is false by default`() {
    setSurveyList(listOf())
    setUpFragment()

    assertThat(fragment.onBack()).isFalse()
    assertThat(fragment.requireActivity().isFinishing).isFalse()
  }

  @Test
  fun `Should exit app on back press when arg is present`() {
    setSurveyList(listOf())
    setUpFragment(bundleOf(Pair("shouldExitApp", true), Pair("surveyId", "")))

    assertThat(fragment.onBack()).isTrue()
    assertThat(fragment.requireActivity().isFinishing).isTrue()
  }

  @Test
  fun `hide sign out button when survey list is not empty`() {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setUpFragment()

    onView(withText("Sign out")).check(matches(not(isDisplayed())))
  }

  @Test
  fun `show sign out button when survey list is empty`() {
    setSurveyList(listOf())
    setUpFragment()

    onView(withText("Sign out")).check(matches(isDisplayed())).perform(click())
    verify(userRepository, times(1)).signOut()
  }

  @Test
  fun `activateSurvey is called when surveyId arg is non-blank`() = runWithTestDispatcher {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setUpFragment(bundleOf(Pair("shouldExitApp", false), Pair("surveyId", TEST_SURVEY_1.id)))
    verify(activateSurvey).invoke(TEST_SURVEY_1.id)
  }

  private fun setUpFragment(
    optBundle: Bundle = bundleOf(Pair("shouldExitApp", false), Pair("surveyId", ""))
  ) = runWithTestDispatcher {
    launchFragmentInHiltContainer<SurveySelectorFragment>(optBundle) {
      fragment = this as SurveySelectorFragment
    }

    // Wait for survey cards to be populated
    advanceUntilIdle()
  }

  private fun setSurveyList(surveys: List<SurveyListItem>) = runWithTestDispatcher {
    whenever(listAvailableSurveysUseCase()).thenReturn(listOf(surveys).asFlow())
  }

  companion object {
    private val TEST_USER = FakeData.USER
    private val TEST_SURVEY_1 =
      SurveyListItem(
        id = "1",
        title = "survey 1",
        description = "description 1",
        false,
        generalAccess = Survey.GeneralAccess.RESTRICTED,
      )
    private val TEST_SURVEY_2 =
      SurveyListItem(
        id = "2",
        title = "survey 2",
        description = "description 2",
        false,
        generalAccess = Survey.GeneralAccess.RESTRICTED,
      )
  }
}
