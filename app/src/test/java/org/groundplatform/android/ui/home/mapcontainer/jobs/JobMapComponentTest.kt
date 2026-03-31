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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertTrue
import org.groundplatform.android.FakeData.ADHOC_JOB
import org.groundplatform.android.FakeData.JOB
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST_LOI_REPORT
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JobMapComponentTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `ActionButton to add new LOI is shown when there is data`() {
    setContent(
      JobMapComponentState.AddLoiButton(
        jobs = listOf(AdHocDataCollectionButtonData(canCollectData = true, job = ADHOC_JOB))
      )
    )

    composeTestRule.onNodeWithContentDescription(getString(R.string.add_site)).assertIsDisplayed()
  }

  @Test
  fun `ActionButton to add new LOI is not shown when the state is Hidden`() {
    setContent(JobMapComponentState.Hidden)

    composeTestRule
      .onNodeWithContentDescription(getString(R.string.add_site))
      .assertIsNotDisplayed()
  }

  @Test
  fun `Should list all available jobs when JobSelectionModal state is triggered`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    setContent(
      JobMapComponentState.JobSelectionModal(
        jobs =
          listOf(
            AdHocDataCollectionButtonData(
              canCollectData = true,
              job = ADHOC_JOB.copy(name = "Job 1"),
            ),
            AdHocDataCollectionButtonData(
              canCollectData = true,
              job = ADHOC_JOB.copy(name = "Job 2"),
            ),
          )
      ),
      onAction = { performedActions += it },
    )

    composeTestRule.onNodeWithText("Job 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Job 2").assertIsDisplayed()
    composeTestRule
      .onNodeWithContentDescription(getString(R.string.add_site))
      .assertIsNotDisplayed()
  }

  @Test
  fun `Clicking a job in the job selection modal triggers a JobSelected action`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    setContent(
      JobMapComponentState.JobSelectionModal(
        jobs =
          listOf(
            AdHocDataCollectionButtonData(
              canCollectData = true,
              job = ADHOC_JOB.copy(name = "Job 1"),
            ),
            AdHocDataCollectionButtonData(
              canCollectData = true,
              job = ADHOC_JOB.copy(name = "Job 2"),
            ),
          )
      ),
      onAction = { performedActions += it },
    )

    composeTestRule.onNodeWithText("Job 1").performClick()

    composeTestRule.waitForIdle()

    assertTrue(
      performedActions.any { it is JobMapComponentAction.OnJobSelected && it.job.name == "Job 1" }
    )
  }

  @Test
  fun `Dismissing job selection modal triggers OnJobSelectionModalDismissed action`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    setContent(
      JobMapComponentState.JobSelectionModal(
        jobs = listOf(AdHocDataCollectionButtonData(canCollectData = true, job = ADHOC_JOB))
      ),
      onAction = { performedActions += it },
    )

    composeTestRule.onNodeWithContentDescription(getString(R.string.close)).performClick()

    assertTrue(performedActions.any { it is JobMapComponentAction.OnJobSelectionModalDismissed })
  }

  @Test
  fun `Clicking button to add new LOI triggers the OnAddLoiButtonClicked action`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    setContent(
      JobMapComponentState.AddLoiButton(
        jobs = listOf(AdHocDataCollectionButtonData(canCollectData = true, job = ADHOC_JOB))
      ),
      onAction = { performedActions += it },
    )

    composeTestRule.onNodeWithContentDescription(getString(R.string.add_site)).performClick()

    composeTestRule.onNodeWithText(ADHOC_JOB.name!!).assertDoesNotExist()
    assertTrue(performedActions.any { it is JobMapComponentAction.OnAddLoiButtonClicked })
  }

  @Test
  fun `LoiJobSheet should be shown when there is a selected LOI`() {
    val selectedLoiSheetData =
      SelectedLoiSheetData(
        canCollectData = true,
        LOCATION_OF_INTEREST,
        0,
        true,
        LOCATION_OF_INTEREST_LOI_REPORT,
      )
    setContent(JobMapComponentState.LoiSelected(selectedLoiSheetData))

    composeTestRule
      .onNodeWithContentDescription(getString(R.string.add_site))
      .assertIsNotDisplayed()
    composeTestRule.onNodeWithText(selectedLoiSheetData.loi.job.name!!).assertIsDisplayed()
  }

  @Test
  fun `Clicking to delete site in the LoiJobSheet should dispatch the OnDeleteSiteClicked action`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    val selectedLoiSheetData =
      SelectedLoiSheetData(
        canCollectData = true,
        LOCATION_OF_INTEREST,
        0,
        true,
        LOCATION_OF_INTEREST_LOI_REPORT,
      )
    setContent(
      state = JobMapComponentState.LoiSelected(selectedLoiSheetData),
      onAction = { performedActions += it },
    )

    composeTestRule.onNodeWithText(getString(R.string.delete_site)).performClick()
    composeTestRule.onNodeWithText(getString(R.string.delete)).performClick()

    assertTrue(
      performedActions.any {
        it is JobMapComponentAction.OnDeleteSiteClicked && it.selectedLoi == selectedLoiSheetData
      }
    )
  }

  @Test
  fun `Clicking to add data in the LoiJobSheet should dispatch the OnAddDataClicked action`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    val predefinedTask = newTask()
    val selectedLoiSheetData =
      SelectedLoiSheetData(
        canCollectData = true,
        loi =
          LOCATION_OF_INTEREST.copy(
            isPredefined = true,
            job = JOB.copy(tasks = mapOf(predefinedTask.id to predefinedTask)),
          ),
        submissionCount = 20,
        showDeleteLoiButton = false,
        loiReport = LOCATION_OF_INTEREST_LOI_REPORT,
      )
    setContent(
      state = JobMapComponentState.LoiSelected(selectedLoiSheetData),
      onAction = { performedActions += it },
    )

    composeTestRule.onNodeWithText(getString(R.string.add_data)).performClick()

    composeTestRule.waitForIdle()

    assertTrue(
      performedActions.any {
        it is JobMapComponentAction.OnAddDataClicked && it.selectedLoi == selectedLoiSheetData
      }
    )
  }

  private fun setContent(
    state: JobMapComponentState,
    onAction: (JobMapComponentAction) -> Unit = {},
  ) {
    composeTestRule.setContent { JobMapComponent(state = state, onAction = { onAction(it) }) }
  }
}
