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
import android.view.Gravity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavDirections
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.model.Survey
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.Navigator
import com.sharedtest.FakeData
import com.squareup.picasso.Picasso
import dagger.hilt.android.testing.HiltAndroidTest
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner

abstract class AbstractHomeScreenFragmentTest : BaseHiltTest() {

  private lateinit var fragment: HomeScreenFragment
  private var initializedPicasso = false

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<HomeScreenFragment> {
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
    onView(withId(R.id.drawer_layout)).check(matches(DrawerMatchers.isClosed(Gravity.START)))
    onView(withId(R.id.hamburger_btn)).check(matches(ViewMatchers.isDisplayed())).perform(click())
    computeScrollForDrawerLayout()
    onView(withId(R.id.drawer_layout)).check(matches(DrawerMatchers.isOpen(Gravity.START)))
    onView(withId(R.id.nav_view)).check(matches(ViewMatchers.isDisplayed()))
  }

  protected fun verifyDrawerClosed() {
    computeScrollForDrawerLayout()
    onView(withId(R.id.drawer_layout)).check(matches(DrawerMatchers.isClosed(Gravity.START)))
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

  private val surveyWithoutBasemap: Survey =
    Survey(
      "SURVEY",
      "Survey title",
      "Test survey description",
      mapOf(FakeData.JOB.id to FakeData.JOB),
      listOf(),
      mapOf(Pair(FakeData.USER.email, "data-collector"))
    )

  private val surveyWithBasemap: Survey =
    surveyWithoutBasemap.copy(
      baseMaps =
        listOf(
          BaseMap(URL("http://google.com"), BaseMap.BaseMapType.MBTILES_FOOTPRINTS),
        ),
      id = "BASEMAPS"
    )

  @Test
  fun offlineBasemapMenuIsDisabledWhenActiveSurveyHasNoBasemap() = runWithTestDispatcher {
    surveyRepository.activeSurvey = surveyWithoutBasemap
    advanceUntilIdle()

    openDrawer()
    onView(withId(R.id.nav_offline_areas)).check(matches(not(isEnabled())))
  }

  @Test
  fun offlineBasemapMenuIsEnabledWhenActiveSurveyHasBasemap() = runWithTestDispatcher {
    surveyRepository.activeSurvey = surveyWithBasemap
    advanceUntilIdle()

    openDrawer()
    onView(withId(R.id.nav_offline_areas)).check(matches(isEnabled()))
  }
}

@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner::class)
class NavigationDrawerItemClickTest(
  private val menuItemLabel: String,
  private val expectedNavDirection: NavDirections?
) : AbstractHomeScreenFragmentTest() {

  @Inject lateinit var navigator: Navigator

  @Test
  fun clickDrawerMenuItem() {
    val navDirectionsTestObserver = navigator.getNavigateRequests().test()

    openDrawer()
    onView(withText(menuItemLabel)).check(matches(isEnabled())).perform(click())

    navDirectionsTestObserver.assertValue(expectedNavDirection)
    verifyDrawerClosed()
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun data() =
      listOf(
        arrayOf(
          "Change survey",
          HomeScreenFragmentDirections.actionHomeScreenFragmentToSurveySelectorFragment(false)
        ),
        arrayOf("Sync status", HomeScreenFragmentDirections.showSyncStatus()),
        arrayOf("Offline base maps", HomeScreenFragmentDirections.showOfflineAreas()),
        arrayOf(
          "Settings",
          HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity()
        )
      )
  }
}
