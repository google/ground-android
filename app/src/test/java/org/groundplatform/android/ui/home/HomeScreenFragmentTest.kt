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
package org.groundplatform.android.ui.home

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavController
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.Survey
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner

abstract class AbstractHomeScreenFragmentTest : BaseHiltTest() {

  @Inject @ApplicationContext lateinit var context: Context
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  lateinit var fragment: HomeScreenFragment
  protected lateinit var navController: NavController

  @Before
  override fun setUp() {
    super.setUp()
    val config =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.INFO)
        .setExecutor(SynchronousExecutor())
        .build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

    launchFragmentWithNavController<HomeScreenFragment>(
      destId = R.id.home_screen_fragment,
      navControllerCallback = { navController = it },
    ) {
      fragment = this as HomeScreenFragment
    }
  }

  // ... (inside class)
  protected fun openDrawer(
    composeTestRule:
      AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
  ) {
    composeTestRule.onNodeWithTag(MapFloatingActionButtonType.OpenNavDrawer.testTag).performClick()
    composeTestRule.waitForIdle()
    verifyDrawerOpen(composeTestRule)
  }

  @Suppress("SwallowedException")
  protected fun verifyDrawerOpen(
    composeTestRule:
      AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
  ) {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }
    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
  }

  protected fun verifyDrawerClosed(
    composeTestRule:
      AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
  ) {
    // Drawer content should not be displayed
    composeTestRule.onNodeWithText("Offline map imagery").assertIsNotDisplayed()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenFragmentTest : AbstractHomeScreenFragmentTest() {

  @Inject lateinit var surveyRepository: SurveyRepository

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `all menu item is always enabled`() = runWithTestDispatcher {
    openDrawer(composeTestRule)
    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed().assertIsEnabled()
    composeTestRule.onNodeWithText("Data sync status").assertIsDisplayed().assertIsEnabled()
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.settings))
      .performScrollTo()
      .assertIsDisplayed()
      .assertIsEnabled()
    composeTestRule.onNodeWithText("About").performScrollTo().assertIsDisplayed().assertIsEnabled()
    composeTestRule
      .onNodeWithText("Terms of service")
      .performScrollTo()
      .assertIsDisplayed()
      .assertIsEnabled()
    composeTestRule
      .onNodeWithText("Build ${org.groundplatform.android.BuildConfig.VERSION_NAME}")
      .performScrollTo()
      .assertIsDisplayed()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner::class)
class NavigationDrawerItemClickTest(
  private val menuItemLabel: String,
  private val expectedNavDirection: Int?,
  private val survey: Survey,
) : AbstractHomeScreenFragmentTest() {

  @Inject lateinit var surveyRepository: SurveyRepository

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `clicking drawer menu item navigates correctly`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.activateSurvey(survey.id)
    advanceUntilIdle()

    // openDrawer via helper in Abstract?
    // We should implement openDrawer in Abstract using composeTestRule.
    // AbstractHomeScreenFragmentTest needs composeTestRule reference?
    // It takes it as arg.

    // Using MapFloatingActionButtonType.OpenNavDrawer.testTag
    composeTestRule.onNodeWithTag(MapFloatingActionButtonType.OpenNavDrawer.testTag).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(menuItemLabel).performScrollTo().performClick()

    if (expectedNavDirection != null) {
      assertThat(navController.currentDestination?.id).isEqualTo(expectedNavDirection)
    }

    verifyDrawerClosed(composeTestRule)
  }

  companion object {
    private val TEST_SURVEY = FakeData.SURVEY.copy()

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{3}")
    fun data() =
      listOf(
        // TODO: Restore tests deleted in #2382.
        // Issue URL: https://github.com/google/ground-android/issues/2385
        arrayOf("Data sync status", R.id.sync_status_fragment, TEST_SURVEY),
        arrayOf("Terms of service", R.id.terms_of_service_fragment, TEST_SURVEY),
        arrayOf("About", R.id.aboutFragment, TEST_SURVEY),
        arrayOf("Offline map imagery", R.id.offline_area_selector_fragment, TEST_SURVEY),
        arrayOf("Settings", R.id.settings_activity, TEST_SURVEY),
      )
  }
}
