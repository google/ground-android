/*
 * Copyright 2025 Google LLC
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
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.R
import org.groundplatform.android.model.AuditInfo
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.task.Task
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LoiJobSheetTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Add data button is shown for predefined LOIs`() {
    setContent(TASK_AND_PREDEFINED_LOI)

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.add_data))
      .assertIsDisplayed()
  }

  @Test
  fun `Add data button is not shown for LOIs without Job`() {
    setContent(NO_TASK_LOI)

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.add_data))
      .assertIsNotDisplayed()
  }

  @Test
  fun `Add data button is not shown for LOIs with no submissions`() {
    setContent(NO_SUBMISSION_LOI)

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.add_data))
      .assertIsNotDisplayed()
  }

  private fun setContent(loi: LocationOfInterest) {
    composeTestRule.setContent {
      LoiJobSheet(
        loi = loi,
        canUserSubmitData = true,
        submissionCount = 0,
        onCollectClicked = {},
      ) {}
    }
  }

  companion object {
    private val NO_TASK_LOI =
      LocationOfInterest(
        id = FakeData.LOI_ID,
        surveyId = FakeData.SURVEY_ID,
        job = Job(id = FakeData.JOB_ID),
        created = AuditInfo(USER),
        lastModified = AuditInfo(USER),
        geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
      )

    private val TASK_AND_PREDEFINED_LOI =
      LocationOfInterest(
        id = FakeData.LOI_ID,
        surveyId = FakeData.SURVEY_ID,
        job =
          Job(
            id = FakeData.JOB_ID,
            style = Style(color = "#4169E1"),
            name = "Job name",
            tasks =
              mapOf(
                Pair(
                  FakeData.TASK_ID,
                  Task(
                    id = FakeData.TASK_ID,
                    index = 1,
                    type = Task.Type.TEXT,
                    label = "task",
                    isRequired = false,
                  ),
                )
              ),
          ),
        created = AuditInfo(USER),
        lastModified = AuditInfo(USER),
        geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
        isPredefined = true,
      )

    private val NO_SUBMISSION_LOI =
      LocationOfInterest(
        id = FakeData.LOI_ID,
        surveyId = FakeData.SURVEY_ID,
        job =
          Job(
            id = "job",
            style = Style(color = "#FFEE8C00"),
            tasks =
              mapOf(
                Pair(
                  FakeData.TASK_ID,
                  Task(
                    id = FakeData.TASK_ID,
                    index = 1,
                    type = Task.Type.TEXT,
                    label = "task",
                    isRequired = false,
                  ),
                )
              ),
          ),
        created = AuditInfo(USER),
        lastModified = AuditInfo(USER),
        geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
      )
  }
}
