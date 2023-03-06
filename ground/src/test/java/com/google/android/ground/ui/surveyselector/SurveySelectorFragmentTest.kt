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
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.*
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowPopupMenu

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySelectorFragmentTest : BaseHiltTest() {

  @BindValue @Mock lateinit var navigator: Navigator
  @BindValue @Mock lateinit var surveyRepository: SurveyRepository
  @BindValue @Mock lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var testDispatcher: TestDispatcher

  private lateinit var fragment: SurveySelectorFragment

  @Before
  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(TEST_USER)
  }

  @Test
  fun created_surveysAvailable_whenNoSurveySynced() {
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(listOf())
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
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(listOf(TEST_SURVEY_2))
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
    assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.VISIBLE)
    assertThat(viewHolder.binding.overflowMenu.visibility).isEqualTo(View.VISIBLE)
  }

  @Test
  fun click_activatesSurvey() =
    runTest(testDispatcher) {
      setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
      setOfflineSurveys(listOf())
      setUpFragment()

      // Click second item
      onView(withId(R.id.recycler_view))
        .perform(
          RecyclerViewActions.actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(1, click())
        )
      advanceUntilIdle()

      // Assert survey is activated.
      verify(activateSurvey).invoke(TEST_SURVEY_2.id)
      // Assert that navigation to home screen was requested
      verify(navigator).navigate(HomeScreenFragmentDirections.showHomeScreen())
    }

  @Test
  fun shouldExitAppOnBackPress_defaultFalse() {
    setAllSurveys(listOf())
    setOfflineSurveys(listOf())
    setUpFragment()

    assertThat(fragment.onBack()).isFalse()
    assertThat(fragment.requireActivity().isFinishing).isFalse()
  }

  @Test
  fun shouldExitAppOnBackPress_whenArgIsPresent() {
    setAllSurveys(listOf())
    setOfflineSurveys(listOf())
    setUpFragment(bundleOf(Pair("shouldExitApp", true)))

    assertThat(fragment.onBack()).isTrue()
    assertThat(fragment.requireActivity().isFinishing).isTrue()
  }

  @Test
  fun deleteSurveyOnOverflowMenuClick() =
    runTest(testDispatcher) {
      setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
      setOfflineSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
      setUpFragment()

      // Click second item's overflow menu
      onView(withId(R.id.recycler_view))
        .perform(
          RecyclerViewActions.actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(
            1,
            recyclerChildAction<TextView>(R.id.overflowMenu) { performClick() }
          )
        )
      advanceUntilIdle()

      // Verify that popup menu is visible and it contains items
      val latestPopupMenu: PopupMenu = ShadowPopupMenu.getLatestPopupMenu()
      val menu: Menu = latestPopupMenu.menu
      assertThat(menu.hasVisibleItems()).isTrue()

      // Click delete item
      onView(withText("Delete")).inRoot(isPlatformPopup()).perform(click())
      advanceUntilIdle()

      // Assert survey is deleted
      verify(surveyRepository).removeOfflineSurvey(TEST_SURVEY_2.id)
    }

  private fun setUpFragment(optBundle: Bundle = bundleOf(Pair("shouldExitApp", false))) {
    launchFragmentInHiltContainer<SurveySelectorFragment>(optBundle) {
      fragment = this as SurveySelectorFragment
    }
  }

  private fun setAllSurveys(surveys: List<Survey>) {
    whenever(surveyRepository.getSurveySummaries(FakeData.USER)).thenReturn(Single.just(surveys))
  }

  private fun setOfflineSurveys(surveys: List<Survey>) {
    whenever(surveyRepository.offlineSurveys).thenReturn(Flowable.just(surveys))
  }

  private fun getViewHolder(index: Int): SurveyListAdapter.ViewHolder {
    val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.recycler_view)
    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(index)
    return viewHolder as SurveyListAdapter.ViewHolder
  }

  companion object {
    private val TEST_USER = FakeData.USER
    private val TEST_SURVEY_1 =
      FakeData.SURVEY.copy(id = "1", title = "survey 1", description = "description 1")
    private val TEST_SURVEY_2 =
      FakeData.SURVEY.copy(id = "2", title = "survey 2", description = "description 2")
  }
}
