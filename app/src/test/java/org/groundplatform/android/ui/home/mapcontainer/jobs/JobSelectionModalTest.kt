/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.home.mapcontainer.jobs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JobSelectionModalTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Modal shows the job names and the close button`() {
    composeTestRule.setContent {
      AppTheme { JobSelectionModal(jobs = JOBS, onJobClicked = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText(JOB_1_NAME).assertExists()
    composeTestRule.onNodeWithText(JOB_2_NAME).assertExists()
    composeTestRule.onNodeWithContentDescription(getString(R.string.close)).assertIsDisplayed()
  }

  @Test
  fun `Clicking a job triggers onJobClicked with that job`() {
    var clicked: Job? = null

    composeTestRule.setContent {
      AppTheme { JobSelectionModal(jobs = JOBS, onJobClicked = { clicked = it }, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText(JOB_1_NAME).performClick()

    composeTestRule.runOnIdle { assertTrue(clicked == JOBS[0]) }
  }

  @Test
  fun `onDismiss callback is triggered when close button is clicked`() {
    var dismissed = false

    composeTestRule.setContent {
      AppTheme {
        JobSelectionModal(jobs = JOBS, onJobClicked = {}, onDismiss = { dismissed = true })
      }
    }

    composeTestRule.onNodeWithContentDescription(getString(R.string.close)).performClick()

    composeTestRule.runOnIdle { assertTrue(dismissed) }
  }

  @Test
  fun `Back press triggers onDismiss`() {
    var dismissed = false

    composeTestRule.setContent {
      AppTheme {
        JobSelectionModal(jobs = JOBS, onJobClicked = {}, onDismiss = { dismissed = true })
      }
    }

    composeTestRule.runOnUiThread {
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }

    composeTestRule.runOnIdle { assertTrue(dismissed) }
  }

  private companion object {
    const val JOB_1_NAME = "job 1"
    const val JOB_2_NAME = "job 2"
    val JOBS =
      listOf(
        Job(id = "1", style = Style(color = "#4169E1"), name = JOB_1_NAME, tasks = emptyMap()),
        Job(id = "2", style = Style(color = "#FFA500"), name = JOB_2_NAME, tasks = emptyMap()),
      )
  }
}
