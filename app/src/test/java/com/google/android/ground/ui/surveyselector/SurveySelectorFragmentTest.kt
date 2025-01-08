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
package com.google.android.ground.ui.surveyselector

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.*
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowPopupMenu
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySelectorFragmentTest : BaseHiltTest() {

  @BindValue @Mock lateinit var surveyRepository: SurveyRepository
  @BindValue @Mock lateinit var userRepository: UserRepository
  @BindValue @Mock lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  private lateinit var fragment: SurveySelectorFragment
  private lateinit var navController: NavController

  @Before
  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(TEST_USER)
  }

  @Test
  fun created_surveysAvailable_whenNoSurveySynced() {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setLocalSurveys(listOf())
    setUpFragment()

    // Assert that 2 surveys are displayed
    onView(withId(R.id.recycler_view)).check(matches(allOf(isDisplayed(), hasChildCount(2))))

    var viewHolder = getViewHolder(0)
    assertThat(viewHolder.binding.title.text).isEqualTo(TEST_SURVEY_1.title)
    assertThat(viewHolder.binding.description.text).isEqualTo(TEST_SURVEY_1.description)
    assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.GONE)
    assertThat(viewHolder.binding.overflowMenu.visibility).isEqualTo(View.GONE)

    viewHolder = getViewHolder(1)
    assertThat(viewHolder.binding.title.text).isEqualTo(TEST_SURVEY_2.title)
    assertThat(viewHolder.binding.description.text).isEqualTo(TEST_SURVEY_2.description)
    assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.GONE)
    assertThat(viewHolder.binding.overflowMenu.visibility).isEqualTo(View.GONE)
  }

  @Test
  fun created_surveysAvailable_whenOneSurveySynced() {
    val syncedSurvey = TEST_SURVEY_2.copy(availableOffline = true)
    val unsyncedSurvey = TEST_SURVEY_1
    setSurveyList(listOf(syncedSurvey, unsyncedSurvey))
    setLocalSurveys(listOf(syncedSurvey))
    setUpFragment()

    // Assert that 2 surveys are displayed.
    onView(withId(R.id.recycler_view)).check(matches(allOf(isDisplayed(), hasChildCount(2))))

    // The survey which is available offline should appear first.

    var viewHolder = getViewHolder(0)
    assertThat(viewHolder.binding.title.text).isEqualTo(syncedSurvey.title)
    assertThat(viewHolder.binding.description.text).isEqualTo(syncedSurvey.description)
    assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.VISIBLE)
    assertThat(viewHolder.binding.overflowMenu.visibility).isEqualTo(View.VISIBLE)

    viewHolder = getViewHolder(1)
    assertThat(viewHolder.binding.title.text).isEqualTo(unsyncedSurvey.title)
    assertThat(viewHolder.binding.description.text).isEqualTo(unsyncedSurvey.description)
    assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.GONE)
    assertThat(viewHolder.binding.overflowMenu.visibility).isEqualTo(View.GONE)
  }

  @Test
  fun click_activatesSurvey() = runWithTestDispatcher {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setLocalSurveys(listOf())

    launchFragmentWithNavController<SurveySelectorFragment>(
      fragmentArgs = bundleOf(Pair("shouldExitApp", false)),
      destId = R.id.surveySelectorFragment,
      navControllerCallback = { navController = it },
    )
    advanceUntilIdle()

    // Click second item
    onView(withId(R.id.recycler_view))
      .perform(actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(1, click()))
    advanceUntilIdle()

    // Assert survey is activated.
    verify(activateSurvey).invoke(TEST_SURVEY_2.id)
    // Assert that navigation to home screen was requested
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.home_screen_fragment)
    // No error toast should be displayed
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun click_activatesSurvey_whenActiveSurveyFails() = runWithTestDispatcher {
    whenever(activateSurvey(any())).thenThrow(Error("Some exception"))

    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setLocalSurveys(listOf())

    launchFragmentWithNavController<SurveySelectorFragment>(
      fragmentArgs = bundleOf(Pair("shouldExitApp", false)),
      destId = R.id.surveySelectorFragment,
      navControllerCallback = { navController = it },
    )
    advanceUntilIdle()

    // Click second item
    onView(withId(R.id.recycler_view))
      .perform(actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(1, click()))
    advanceUntilIdle()

    // Assert survey is activated.
    verify(activateSurvey).invoke(TEST_SURVEY_2.id)
    // Assert that navigation to home screen was not requested
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)
    // Error toast message
    assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
  }

  @Test
  fun shouldExitAppOnBackPress_defaultFalse() {
    setSurveyList(listOf())
    setLocalSurveys(listOf())
    setUpFragment()

    assertThat(fragment.onBack()).isFalse()
    assertThat(fragment.requireActivity().isFinishing).isFalse()
  }

  @Test
  fun shouldExitAppOnBackPress_whenArgIsPresent() {
    setSurveyList(listOf())
    setLocalSurveys(listOf())
    setUpFragment(bundleOf(Pair("shouldExitApp", true)))

    assertThat(fragment.onBack()).isTrue()
    assertThat(fragment.requireActivity().isFinishing).isTrue()
  }

  @Test
  fun `hide sign out button when survey list is not empty`() {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setLocalSurveys(listOf())
    setUpFragment()

    onView(withText("Sign out")).check(matches(not(isDisplayed())))
  }

  @Test
  fun `show sign out button when survey list is empty`() {
    setSurveyList(listOf())
    setLocalSurveys(listOf())
    setUpFragment()

    onView(withText("Sign out")).check(matches(isDisplayed())).perform(click())
    verify(userRepository, times(1)).signOut()
  }

  @Test
  fun `remove offline survey on menu item click`() = runWithTestDispatcher {
    setSurveyList(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setLocalSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setUpFragment()

    // Click second item's overflow menu
    onView(withId(R.id.recycler_view))
      .perform(
        actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(
          1,
          recyclerChildAction<Button>(R.id.overflowMenu) { performClick() },
        )
      )
    advanceUntilIdle()

    // Verify that popup menu is visible and it contains items
    val latestPopupMenu: PopupMenu = ShadowPopupMenu.getLatestPopupMenu()
    val menu: Menu = latestPopupMenu.menu
    assertThat(menu.hasVisibleItems()).isTrue()

    // Click "remove" menu item.
    onView(withText("Remove offline access")).inRoot(isPlatformPopup()).perform(click())
    advanceUntilIdle()

    // Assert survey is deleted
    verify(surveyRepository).removeOfflineSurvey(TEST_SURVEY_2.id)
  }

  private fun setUpFragment(optBundle: Bundle = bundleOf(Pair("shouldExitApp", false))) =
    runWithTestDispatcher {
      launchFragmentInHiltContainer<SurveySelectorFragment>(optBundle) {
        fragment = this as SurveySelectorFragment
      }

      // Wait for survey cards to be populated
      advanceUntilIdle()
    }

  private fun setSurveyList(surveys: List<SurveyListItem>) = runWithTestDispatcher {
    whenever(surveyRepository.getSurveyList(TEST_USER)).thenReturn(listOf(surveys).asFlow())
  }

  private fun setLocalSurveys(surveys: List<SurveyListItem>) {
    whenever(surveyRepository.localSurveyListFlow).thenReturn(listOf(surveys).asFlow())
  }

  private fun getViewHolder(index: Int): SurveyListAdapter.ViewHolder {
    val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.recycler_view)
    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(index)
    return viewHolder as SurveyListAdapter.ViewHolder
  }

  companion object {
    private val TEST_USER = FakeData.USER
    private val TEST_SURVEY_1 =
      SurveyListItem(id = "1", title = "survey 1", description = "description 1", false)
    private val TEST_SURVEY_2 =
      SurveyListItem(id = "2", title = "survey 2", description = "description 2", false)
  }
}
