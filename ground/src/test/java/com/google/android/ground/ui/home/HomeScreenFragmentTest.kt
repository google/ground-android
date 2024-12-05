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
package com.google.android.ground.ui.home

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentWithNavController
import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.repository.SurveyRepository
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.squareup.picasso.Picasso
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner

abstract class AbstractHomeScreenFragmentTest : BaseHiltTest() {

  @Inject lateinit var localSurveyStore: LocalSurveyStore
  lateinit var fragment: HomeScreenFragment
  private var initializedPicasso = false
  protected lateinit var navController: NavController

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentWithNavController<HomeScreenFragment>(
      destId = R.id.home_screen_fragment,
      navControllerCallback = { navController = it },
    ) {
      fragment = this as HomeScreenFragment
      initPicasso(fragment.requireContext())
    }
  }

  private fun initPicasso(context: Context) {
    if (initializedPicasso) {
      return
    }
    try {
      Picasso.setSingletonInstance(Picasso.Builder(context).build())
    } catch (_: Exception) {
      // ignore failures if context is already set
      // Tracking bug : https://github.com/square/picasso/issues/1929
    }
    initializedPicasso = true
  }

  protected fun openDrawer() {
    onView(withId(R.id.drawer_layout)).check(matches(isClosed()))
    onView(withId(R.id.hamburger_btn)).check(matches(ViewMatchers.isDisplayed())).perform(click())
    verifyDrawerOpen()
    onView(withId(R.id.nav_view)).check(matches(ViewMatchers.isDisplayed()))
  }

  protected fun swipeUpDrawer() {
    onView(withId(R.id.drawer_layout)).perform(swipeUp())
  }

  protected fun verifyDrawerOpen() {
    computeScrollForDrawerLayout()
    onView(withId(R.id.drawer_layout)).check(matches(isOpen()))
  }

  protected fun verifyDrawerClosed() {
    computeScrollForDrawerLayout()
    onView(withId(R.id.drawer_layout)).check(matches(isClosed()))
  }

  /**
   * Invoke this method before doing any verifications on navigation drawer after performing an
   * action on it.
   */
  private fun computeScrollForDrawerLayout() {
    val drawerLayout = fragment.requireView().findViewById<DrawerLayout>(R.id.drawer_layout)
    // Note that this only initiates a single computeScroll() in Robolectric. Normally, Android
    // will compute several of these across multiple draw calls, but one seems sufficient for
    // Robolectric. Note that Robolectric is also *supposed* to handle the animation loop one call
    // to this method initiates in the view choreographer class, but it seems to not actually
    // flush the choreographer per observation. In Espresso, this method is automatically called
    // during draw (and a few other situations), but it's fine to call it directly once to kick it
    // off (to avoid disparity between Espresso/Robolectric runs of the tests).
    // NOTE TO DEVELOPERS: if this ever flakes, we can probably put this in a loop with fake time
    // adjustments to simulate the render loop.
    // Tracking bug: https://github.com/robolectric/robolectric/issues/5954
    drawerLayout.computeScroll()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenFragmentTest : AbstractHomeScreenFragmentTest() {

  @Inject lateinit var surveyRepository: SurveyRepository

  /**
   * composeTestRule has to be created in the specific test file in order to access the required
   * activity. [composeTestRule.activity]
   */
  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val surveyWithoutBasemap: Survey =
    Survey(
      "SURVEY",
      "Survey title",
      "Test survey description",
      mapOf(FakeData.JOB.id to FakeData.JOB),
      mapOf(Pair(FakeData.USER.email, "data-collector")),
    )

  @Test
  fun `all menu item is always enabled`() = runWithTestDispatcher {
    surveyRepository.selectedSurveyId = surveyWithoutBasemap.id
    advanceUntilIdle()

    openDrawer()
    onView(withId(R.id.nav_offline_areas)).check(matches(isEnabled()))
    onView(withId(R.id.sync_status)).check(matches(isEnabled()))
    onView(withId(R.id.nav_settings)).check(matches(isEnabled()))
    onView(withId(R.id.about)).check(matches(isEnabled()))

    swipeUpDrawer()

    onView(withId(R.id.terms_of_service)).check(matches(isEnabled()))
    onView(withId(R.id.nav_log_version)).check(matches(isEnabled()))
  }

  @Test
  fun `signOut dialog is Displayed`() = runWithTestDispatcher {
    openDrawer()

    onView(withId(R.id.user_image)).check(matches(isDisplayed()))
    openSignOutDialog()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close))
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close))
      .performClick()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close))
      .assertIsNotDisplayed()

    openSignOutWarningDialog()

    advanceUntilIdle()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out_dialog_title))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out_dialog_body))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .assertIsNotDisplayed()

    openSignOutWarningDialog()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .performClick()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .assertIsNotDisplayed()
  }

  @Test
  fun `onBack() should return false and do nothing`() {
    assertFalse(fragment.onBack())
  }

  private fun openSignOutDialog() {
    onView(withId(R.id.user_image)).perform(click())
  }

  private fun openSignOutWarningDialog() {
    openSignOutDialog()
    composeTestRule.onNodeWithText("Sign out").performClick()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner::class)
class NavigationDrawerItemClickTest(
  private val menuItemLabel: String,
  private val expectedNavDirection: Int?,
  private val survey: Survey,
  private val shouldDrawerCloseAfterClick: Boolean,
) : AbstractHomeScreenFragmentTest() {

  @Inject lateinit var surveyRepository: SurveyRepository

  @Test
  fun clickDrawerMenuItem() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.selectedSurveyId = survey.id
    advanceUntilIdle()

    openDrawer()

    onView(withId(R.id.drawer_layout)).perform(swipeUp())

    onView(withText(menuItemLabel)).check(matches(isEnabled())).perform(click())

    if (expectedNavDirection != null) {
      assertThat(navController.currentDestination?.id).isEqualTo(expectedNavDirection)
    }

    if (shouldDrawerCloseAfterClick) {
      verifyDrawerClosed()
    } else {
      verifyDrawerOpen()
    }
  }

  companion object {
    private val TEST_SURVEY = FakeData.SURVEY.copy()

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{3}")
    fun data() =
      listOf(
        // TODO(#2385): Restore tests deleted in #2382.
        arrayOf("Data sync status", R.id.sync_status_fragment, TEST_SURVEY, true),
        arrayOf("Terms of service", R.id.terms_of_service_fragment, TEST_SURVEY, true),
        arrayOf("About", R.id.aboutFragment, TEST_SURVEY, true),
        arrayOf("Offline map imagery", R.id.offline_area_selector_fragment, TEST_SURVEY, true),
        arrayOf("Settings", R.id.settings_activity, TEST_SURVEY, true),
      )
  }
}
