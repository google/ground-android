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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.Test
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.groundplatform.android.FakeData
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoiJobSheetTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `add data button is shown for predefined LOIs`() {
    setContent(TASK_AND_PREDEFINED_LOI)

    composeTestRule.onNodeWithText(getString(R.string.add_data)).assertIsDisplayed()
  }

  @Test
  fun `add data button is not shown for LOIs without Job`() {
    setContent(NO_TASK_LOI)

    composeTestRule.onNodeWithText(getString(R.string.add_data)).assertIsNotDisplayed()
  }

  @Test
  fun `add data button is not shown for LOIs with no submissions`() {
    setContent(NO_SUBMISSION_LOI)

    composeTestRule.onNodeWithText(getString(R.string.add_data)).assertIsNotDisplayed()
  }

  @Test
  fun `delete button is shown for free-form LOIs when user can delete`() {
    setContent(FREE_FORM_LOI, showDeleteLoiButton = true)

    composeTestRule.onNodeWithText(getString(R.string.delete_site)).assertIsDisplayed()
  }

  @Test
  fun `delete button is not shown for predefined LOIs`() {
    setContent(TASK_AND_PREDEFINED_LOI, showDeleteLoiButton = true)

    composeTestRule.onNodeWithText(getString(R.string.delete_site)).assertIsNotDisplayed()
  }

  @Test
  fun `delete button is not shown when user cannot delete`() {
    setContent(FREE_FORM_LOI, showDeleteLoiButton = false)

    composeTestRule.onNodeWithText(getString(R.string.delete_site)).assertIsNotDisplayed()
  }

  @Test
  fun `share button is shown when there is a LoiReport`() {
    setContent(FREE_FORM_LOI)

    composeTestRule.onNodeWithText(getString(R.string.share)).assertIsDisplayed()
  }

  @Test
  fun `share button is not shown when there is no LoiReport`() {
    setContent(FREE_FORM_LOI, loiReport = null)

    composeTestRule.onNodeWithText(getString(R.string.share)).assertIsNotDisplayed()
  }

  private fun setContent(
    loi: LocationOfInterest,
    showDeleteLoiButton: Boolean = false,
    loiReport: LoiReport? = getLoiReport(loi.id),
  ) {
    composeTestRule.setContent {
      LoiJobSheet(
        state =
          SelectedLoiSheetData(
            canCollectData = true,
            submissionCount = 0,
            loi = loi,
            showDeleteLoiButton = showDeleteLoiButton,
            loiReport = loiReport,
          ),
        onCollectClicked = {},
        onDismiss = {},
        onShareClicked = {},
      )
    }
  }

  private fun getLoiReport(name: String): LoiReport =
    LoiReport(
      loiName = name,
      geoJson =
        JsonObject(
          mapOf(
            "type" to JsonPrimitive("Feature"),
            "properties" to JsonObject(mapOf("name" to JsonPrimitive(name))),
            "geometry" to
              JsonObject(
                mapOf(
                  "type" to JsonPrimitive("Point"),
                  "coordinates" to JsonArray(listOf(JsonPrimitive(20.0), JsonPrimitive(20.0))),
                )
              ),
          )
        ),
    )

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

    private val FREE_FORM_LOI =
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
        isPredefined = false,
      )
  }
}
