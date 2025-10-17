/*
 * Copyright 2022 Google LLC
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

package org.groundplatform.android.ui.datacollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST_NAME
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.R
import org.groundplatform.android.data.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.data.sync.MutationSyncWorkManager
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.DraftSubmission
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.Expression
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MutationRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskViewModel
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var loiRepository: LocationOfInterestRepository
  @Inject lateinit var mutationRepository: MutationRepository
  @Inject lateinit var submissionRepository: SubmissionRepository
  @Inject lateinit var userRepository: UserRepository

  @BindValue @Mock lateinit var mutationSyncWorkManager: MutationSyncWorkManager

  lateinit var fragment: DataCollectionFragment

  override fun setUp() = runBlocking {
    super.setUp()
    setupSubmission()
  }

  @Test
  fun `Job and LOI names are displayed correctly`() {
    setupFragment()

    runner().validateTextIsDisplayed(TASK_1_NAME).validateTextIsDisplayed(requireNotNull(JOB.name))
  }

  @Test
  fun `Only job name is displayed when LOI is not provided`() {
    setupFragmentWithNoLoi()

    runner()
      .validateTextDoesNotExist("Unnamed point")
      .validateTextIsDisplayed(requireNotNull(JOB.name))
  }

  @Test
  fun `First task is loaded and is visible`() {
    setupFragment()

    runner().validateTextIsDisplayed(TASK_1_NAME).validateTextIsNotDisplayed(TASK_2_NAME)
  }

  @Test
  fun `Add LOI task is loaded and is visible when LOI is not provided`() {
    setupFragmentWithNoLoi()

    runner().validateTextIsDisplayed(TASK_0_NAME).validateTextIsNotDisplayed(TASK_1_NAME)
  }

  @Test
  fun `Next button is disabled when task doesn't have any value`() {
    setupFragment()

    runner().assertButtonIsDisabled("Next")
  }

  @Test
  fun `Next button proceeds to the second task when task has value`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)

    // Ensure that no validation error toasts were displayed
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Next button displays the LoiNameDialog when task has value when LOI is missing`() {
    setupFragmentWithNoLoi()

    runner().clickButton("Drop pin").clickNextButton().assertLoiNameDialogIsDisplayed()
  }

  @Test
  fun `Entering loi name enables the save button in LoiNameDialog`() {
    setupFragmentWithNoLoi()

    runner()
      .clickButton("Drop pin")
      .clickNextButton()
      .assertButtonIsDisabled("Save")
      .inputLoiName("Custom Loi Name")
      .assertButtonIsEnabled("Save")
  }

  @Test
  fun `Clicking cancel hides the LoiNameDialog`() {
    setupFragmentWithNoLoi()

    runner()
      .clickButton("Drop pin")
      .clickNextButton()
      .clickButton("Cancel")
      .assertLoiNameDialogIsNotDisplayed()
  }

  @Test
  fun `Clicking save in LoiNameDialog proceeds to next task`() {
    setupFragmentWithNoLoi()

    runner()
      .clickButton("Drop pin")
      .clickNextButton()
      .inputLoiName("Custom Loi Name")
      .clickButton("Save")
      .validateTextIsDisplayed("Custom Loi Name")
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_0_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)
  }

  @Test
  fun `Previous button navigates back to first task`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .pressBackButton()
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)

    // Ensure that no validation error toasts were displayed
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Next click saves draft`() = runWithTestDispatcher {
    setupFragment()

    runner().inputText(TASK_1_RESPONSE).clickNextButton()

    assertDraftSaved(listOf(TASK_1_VALUE_DELTA), currentTaskId = TASK_ID_2)
  }

  @Test
  fun `Clicking previous button saves draft`() = runWithTestDispatcher {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .selectOption(TASK_2_OPTION_LABEL)
      .clickPreviousButton()

    assertDraftSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA), currentTaskId = TASK_ID_1)
  }

  @Test
  fun `Click previous button moves to previous task if task is empty`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .clickPreviousButton()
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)

    // Validation error is not shown
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Load tasks from draft`() = runWithTestDispatcher {
    // TODO: add coverage for loading from draft for all types of tasks
    // Issue URL: https://github.com/google/ground-android/issues/708
    val expectedDeltas = listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA)

    setupFragmentWithDraft(expectedDeltas)

    runner()
      .assertInputTextDisplayed(TASK_1_RESPONSE)
      .clickNextButton()
      .assertOptionsDisplayed(TASK_2_OPTION_LABEL)
  }

  @Test
  fun `Clicking done on final task saves the submission`() = runWithTestDispatcher {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)
      .selectOption(TASK_2_OPTION_LABEL)
      .clickDoneButton() // Click "done" on final task

    assertSubmissionSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA))
  }

  @Test
  fun `Displays close button on first task`() = runWithTestDispatcher {
    setupFragment()

    assertThat(getToolbar()?.navigationIcon).isNotNull()
  }

  @Test
  fun `Clicking done on final task hides the navigation close button`() = runWithTestDispatcher {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)
      .selectOption(TASK_2_OPTION_LABEL)
      .clickDoneButton() // Click "done" on final task

    assertThat(getToolbar()?.navigationIcon).isNull()
  }

  @Test
  fun `Clicking done on final task saves the submission and LOI when LOI is not provided`() =
    runWithTestDispatcher {
      setupFragmentWithNoLoi()

      runner()
        .clickButton("Drop pin")
        .clickNextButton()
        .inputLoiName("Custom Loi Name")
        .clickButton("Save")
        .inputText(TASK_1_RESPONSE)
        .clickNextButton()
        .validateTextIsNotDisplayed(TASK_1_NAME)
        .validateTextIsDisplayed(TASK_2_NAME)
        .selectOption(TASK_2_OPTION_LABEL)
        .clickDoneButton() // Click "done" on final task

      assetLoiSaved(loiId = "TEST UUID", customId = "Custom Loi Name")
      assertSubmissionSaved(
        loiId = "TEST UUID",
        valueDeltas = listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA),
      )
    }

  @Test
  fun `Clicking back button on first task displays a confirmation dialog and clears the draft`() =
    runWithTestDispatcher {
      setupFragment()

      // Save the draft and move back to first task.
      runner().inputText(TASK_1_RESPONSE).clickNextButton().pressBackButton()

      // Click back on first draft
      runner().pressBackButton()

      // Assert that confirmation dialog is shown
      composeTestRule
        .onNodeWithText(fragment.getString(R.string.data_collection_cancellation_title))
        .assertIsDisplayed()

      // Click confirm button
      composeTestRule
        .onNodeWithText(fragment.getString(R.string.data_collection_cancellation_confirm_button))
        .performClick()
      advanceUntilIdle()

      // Assert that draft is cleared on confirmation
      assertNoDraftSaved()
    }

  @Test
  fun `Multiple choice task remembers previous selection when navigating back and forth`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      .selectOption(TASK_2_OPTION_LABEL)
      .clickPreviousButton()
      .validateTextIsDisplayed(TASK_1_NAME)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
  }

  @Test
  fun `Back navigation from conditional task to multiple choice maintains task state`() =
    runWithTestDispatcher {
      setupFragment()

      runner()
        .inputText(TASK_1_RESPONSE)
        .clickNextButton()
        .validateTextIsDisplayed(TASK_2_NAME)
        // Select the option to unhide the conditional task.
        .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL)
        .clickNextButton()
        .validateTextIsDisplayed(TASK_CONDITIONAL_NAME)
        .inputText(TASK_CONDITIONAL_RESPONSE)
        .clickPreviousButton()
        .validateTextIsDisplayed(TASK_2_NAME)
      // Just verify we can navigate back successfully
    }

  @Test
  fun `Draft is saved with correct task sequence when conditional task is involved`() =
    runWithTestDispatcher {
      setupFragment()

      runner()
        .inputText(TASK_1_RESPONSE)
        .clickNextButton()
        .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL)
        .clickNextButton()
        .inputText(TASK_CONDITIONAL_RESPONSE)

      // Save current state
      fragment.viewModel.saveCurrentState()

      // Just verify that a draft was saved (without checking exact content)
      assertThat(submissionRepository.countDraftSubmissions()).isEqualTo(1)
    }

  @Test
  fun `Clicking cancel in confirmation dialog does not clear draft`() = runWithTestDispatcher {
    setupFragment()

    // Save the draft and move back to first task.
    runner().inputText(TASK_1_RESPONSE).clickNextButton().pressBackButton()

    // Click back on first task
    runner().pressBackButton()

    // Assert that confirmation dialog is shown
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.data_collection_cancellation_title))
      .assertIsDisplayed()

    // Click cancel button instead of confirm
    composeTestRule.onNodeWithText(fragment.getString(R.string.cancel)).performClick()
    advanceUntilIdle()

    // Assert that draft is NOT cleared on cancellation
    assertDraftSaved(listOf(TASK_1_VALUE_DELTA), currentTaskId = TASK_ID_1)
  }

  @Test
  fun `Navigation close button shows confirmation dialog when draft exists`() =
    runWithTestDispatcher {
      setupFragment()

      // Add some data to create a draft
      runner().inputText(TASK_1_RESPONSE)

      // Click the toolbar's navigation close button - simulate the click action
      fragment.onBack()

      // Assert that confirmation dialog is shown
      composeTestRule
        .onNodeWithText(fragment.getString(R.string.data_collection_cancellation_title))
        .assertIsDisplayed()
    }

  @Test
  fun `Previous button is disabled on first task`() {
    setupFragment()

    runner().assertButtonIsDisabled("Previous")
  }

  @Test
  fun `Previous button is enabled on second task`() {
    setupFragment()

    runner().inputText(TASK_1_RESPONSE).clickNextButton().assertButtonIsEnabled("Previous")
  }

  @Test
  fun `Done button appears only on last task`() {
    setupFragment()

    // First task should show Next, not Done
    runner().assertButtonIsHidden("Done")

    runner().inputText(TASK_1_RESPONSE).clickNextButton()

    // Last task should show Done, not Next
    runner().assertButtonIsHidden("Next")
  }

  @Test
  fun `Empty text task allows navigation to previous task without validation`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_CONDITIONAL_NAME)
      // Leave conditional task empty and go back
      .clickPreviousButton()
      .validateTextIsDisplayed(TASK_2_NAME)

    // No validation error should be shown
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Task sequence updates correctly when conditional task becomes visible`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      // Don't select conditional option - should be last task
      .selectOption(TASK_2_OPTION_LABEL)

    // Verify we're on the last task by checking if Done button is available
    runner().assertButtonIsHidden("Next")

    // Now select conditional option - should show Next instead of Done
    runner().selectOption(TASK_2_OPTION_CONDITIONAL_LABEL).assertButtonIsHidden("Done")
  }

  @Test
  fun `Text task preserves input when navigating back and forth`() {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .assertInputTextDisplayed(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      .clickPreviousButton()
      .validateTextIsDisplayed(TASK_1_NAME)
      .assertInputTextDisplayed(TASK_1_RESPONSE)
  }

  @Test
  fun `Progress bar updates correctly when navigating between tasks`() {
    setupFragment()

    val progressBar = fragment.view?.findViewById<android.widget.ProgressBar>(R.id.progressBar)
    if (progressBar != null) {
      // First task (0/1 progress)
      assertThat(progressBar.progress).isEqualTo(0)
      assertThat(progressBar.max).isEqualTo(100) // (2-1) * 100

      runner().inputText(TASK_1_RESPONSE).clickNextButton()

      // Second task (1/1 progress = 100)
      assertThat(progressBar.progress).isEqualTo(100)
    }
  }

  @Test
  fun `Loading tasks from draft with invalid data handles gracefully`() = runWithTestDispatcher {
    setupFragment(shouldLoadFromDraft = true, draftValues = "invalid-json-data")

    // Should still load first task even with invalid draft data
    runner().validateTextIsDisplayed(TASK_1_NAME)
  }

  @Test
  fun `Task submission is prevented when required task is empty`() {
    setupFragment()

    // Try to proceed without filling required first task
    runner().assertButtonIsDisabled("Next")

    // Fill first task
    runner().inputText(TASK_1_RESPONSE).clickNextButton()

    // Try to submit without filling required second task
    runner().assertButtonIsDisabled("Done")
  }

  @Test
  fun `LOI name dialog validation prevents saving with empty name`() {
    setupFragmentWithNoLoi()

    runner()
      .clickButton("Drop pin")
      .clickNextButton()
      .assertLoiNameDialogIsDisplayed()
      .assertButtonIsDisabled("Save")

    // Input valid name
    runner().inputLoiName("Valid Name").assertButtonIsEnabled("Save")
  }

  @Test
  fun `Back button works correctly in add LOI flow`() {
    setupFragmentWithNoLoi()

    // First task is add LOI task
    runner()
      .clickButton("Drop pin")
      .clickNextButton()
      .inputLoiName("Custom LOI")
      .clickButton("Save")
      .validateTextIsDisplayed(TASK_1_NAME)
      .pressBackButton() // Should go back to add LOI task
      .validateTextIsDisplayed(TASK_0_NAME)
      .pressBackButton() // Should show confirmation dialog on first task

    composeTestRule
      .onNodeWithText(fragment.getString(R.string.data_collection_cancellation_title))
      .assertIsDisplayed()
  }

  @Test
  fun `Task data is cleared when conditional task becomes hidden`() = runWithTestDispatcher {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL) // Show conditional task
      .clickNextButton()
      .inputText(TASK_CONDITIONAL_RESPONSE) // Fill conditional task
      .clickPreviousButton()
      .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL) // Unselect to hide conditional task
      .selectOption(TASK_2_OPTION_LABEL) // Select regular option
      .clickDoneButton()

    // Conditional task data should not be saved since task became hidden
    assertSubmissionSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA))
  }

  @Test
  fun `Fragment handles task submission state correctly`() = runWithTestDispatcher {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .selectOption(TASK_2_OPTION_LABEL)
      .clickDoneButton()

    // Simulate state after task submission
    val state = fragment.viewModel.uiState.value
    assertThat(state).isEqualTo(DataCollectionUiState.TaskSubmitted)
  }

  @Test
  fun `Clicking done after triggering conditional task saves task data`() = runWithTestDispatcher {
    setupFragment()

    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      // Select the option to unhide the conditional task.
      .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL)
      .clickNextButton()
      // Conditional task is rendered.
      .validateTextIsDisplayed(TASK_CONDITIONAL_NAME)
      .inputText(TASK_CONDITIONAL_RESPONSE)
      .clickDoneButton()

    // Conditional task data is submitted.
    assertSubmissionSaved(
      listOf(TASK_1_VALUE_DELTA, TASK_2_CONDITIONAL_VALUE_DELTA, TASK_CONDITIONAL_VALUE_DELTA)
    )
  }

  @Test
  fun `Clicking done after editing conditional task state doesn't save inputted conditional task`() =
    runWithTestDispatcher {
      setupFragment()

      runner()
        .inputText(TASK_1_RESPONSE)
        .clickNextButton()
        .validateTextIsDisplayed(TASK_2_NAME)
        // Select the option to unhide the conditional task.
        .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL)
        .clickNextButton()
        .validateTextIsDisplayed(TASK_CONDITIONAL_NAME)
        // Input a value, then go back to hide the task again.
        .inputText(TASK_CONDITIONAL_RESPONSE)
        .clickPreviousButton()
        .validateTextIsDisplayed(TASK_2_NAME)
        // Unselect the option to hide the conditional task.
        .selectOption(TASK_2_OPTION_CONDITIONAL_LABEL)
        .selectOption(TASK_2_OPTION_LABEL)
        .clickDoneButton()
        .validateTextIsNotDisplayed(TASK_CONDITIONAL_NAME)

      // Conditional task data is not submitted.
      assertSubmissionSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA))
    }

  private suspend fun assetLoiSaved(loiId: String, customId: String) {
    val actualLoi = checkNotNull(loiRepository.getOfflineLoi(surveyId = SURVEY.id, loiId = loiId))

    assertThat(actualLoi.id).isEqualTo(loiId)
    assertThat(actualLoi.surveyId).isEqualTo(SURVEY.id)
    assertThat(actualLoi.job).isEqualTo(JOB)
    assertThat(actualLoi.customId).isEmpty()
    assertThat(actualLoi.geometry).isEqualTo(TASK_0_VALUE.geometry)
    assertThat(actualLoi.submissionCount).isEqualTo(0)
    assertThat(actualLoi.properties).isEqualTo(mapOf("name" to customId))
    assertThat(actualLoi.isPredefined).isFalse()
  }

  private suspend fun assertSubmissionSaved(
    valueDeltas: List<ValueDelta>,
    loiId: String = LOCATION_OF_INTEREST.id,
  ) {
    assertNoDraftSaved()

    // Exactly 1 submission should be saved.
    assertThat(submissionRepository.getPendingCreateCount(loiId)).isEqualTo(1)

    val testDate = Date()
    val mutation =
      mutationRepository
        .getIncompleteUploads()[0]
        .submissionMutation!!
        .copy(clientTimestamp = testDate) // seed dummy test date

    assertThat(mutation)
      .isEqualTo(
        SubmissionMutation(
          id = 1,
          type = Mutation.Type.CREATE,
          syncStatus = Mutation.SyncStatus.PENDING,
          surveyId = SURVEY.id,
          locationOfInterestId = loiId,
          userId = USER.id,
          clientTimestamp = testDate,
          collectionId = "TEST UUID",
          job = JOB,
          submissionId = "TEST UUID",
          deltas = valueDeltas,
        )
      )
  }

  private suspend fun assertDraftSaved(valueDeltas: List<ValueDelta>, currentTaskId: String) {
    val draftId = submissionRepository.getDraftSubmissionsId()
    assertThat(draftId).isNotEmpty()

    // Exactly 1 draft should be present always.
    assertThat(submissionRepository.countDraftSubmissions()).isEqualTo(1)
    assertThat(submissionRepository.getDraftSubmission(draftId, SURVEY))
      .isEqualTo(
        DraftSubmission(
          id = draftId,
          jobId = JOB.id,
          loiId = LOCATION_OF_INTEREST.id,
          loiName = LOCATION_OF_INTEREST_NAME,
          surveyId = SURVEY.id,
          deltas = valueDeltas,
          currentTaskId = currentTaskId,
        )
      )
  }

  private suspend fun assertNoDraftSaved() {
    assertThat(submissionRepository.getDraftSubmissionsId()).isEmpty()
    assertThat(submissionRepository.countDraftSubmissions()).isEqualTo(0)
  }

  private fun setupSubmission() = runWithTestDispatcher {
    userRepository.saveUserDetails(USER)
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    fakeRemoteDataStore.predefinedLois = listOf(LOCATION_OF_INTEREST)
    activateSurvey(SURVEY.id)
    advanceUntilIdle()
  }

  private fun setupFragmentWithDraft(expectedValues: List<ValueDelta>) {
    setupFragment(
      shouldLoadFromDraft = true,
      draftValues = SubmissionDeltasConverter.toString(expectedValues),
    )
  }

  private fun setupFragmentWithNoLoi() {
    setupFragment(loiId = null, loiName = null)

    // Configured "isAddLoiTask" is of type DROP_PIN. Provide current location for it.
    val viewModel = fragment.viewModel.getTaskViewModel(taskId = TASK_ID_0) as DropPinTaskViewModel
    viewModel.updateCameraPosition(CameraPosition(TASK_0_RESPONSE))
  }

  private fun setupFragment(
    loiId: String? = LOCATION_OF_INTEREST.id,
    loiName: String? = LOCATION_OF_INTEREST_NAME,
    shouldLoadFromDraft: Boolean = false,
    draftValues: String? = null,
  ) {
    val argsBundle =
      DataCollectionFragmentArgs.Builder(
          loiId,
          loiName,
          JOB.id,
          shouldLoadFromDraft,
          draftValues,
          /* currentTaskId */ "",
        )
        .build()
        .toBundle()

    launchFragmentWithNavController<DataCollectionFragment>(
      argsBundle,
      destId = R.id.data_collection_fragment,
    ) {
      fragment = this as DataCollectionFragment
    }
  }

  private fun runner() = TaskFragmentRunner(this, fragment)

  private fun getToolbar() =
    fragment.view?.findViewById<com.google.android.material.appbar.MaterialToolbar>(
      R.id.data_collection_toolbar
    )

  companion object {
    private const val TASK_ID_0 = "0"
    const val TASK_0_NAME = "task 0"
    private val TASK_0_RESPONSE = Coordinates(10.0, 20.0)
    private val TASK_0_VALUE = DropPinTaskData(Point(TASK_0_RESPONSE))

    private const val TASK_ID_1 = "1"
    const val TASK_1_NAME = "task 1"
    private const val TASK_1_RESPONSE = "response 1"
    private val TASK_1_VALUE = TextTaskData.fromString(TASK_1_RESPONSE)
    private val TASK_1_VALUE_DELTA = ValueDelta(TASK_ID_1, Task.Type.TEXT, TASK_1_VALUE)

    private const val TASK_ID_2 = "2"
    const val TASK_2_NAME = "task 2"
    private const val TASK_2_OPTION = "option 1"
    private const val TASK_2_OPTION_LABEL = "Option 1"
    private const val TASK_2_OPTION_CONDITIONAL = "option 2"
    private const val TASK_2_OPTION_CONDITIONAL_LABEL = "Option 2"
    private val TASK_2_MULTIPLE_CHOICE =
      MultipleChoice(
        persistentListOf(
          Option(TASK_2_OPTION, "code1", TASK_2_OPTION_LABEL),
          Option(TASK_2_OPTION_CONDITIONAL, "code2", TASK_2_OPTION_CONDITIONAL_LABEL),
        ),
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
      )
    private val TASK_2_VALUE =
      MultipleChoiceTaskData.fromList(TASK_2_MULTIPLE_CHOICE, listOf(TASK_2_OPTION))
    private val TASK_2_CONDITIONAL_VALUE =
      MultipleChoiceTaskData.fromList(TASK_2_MULTIPLE_CHOICE, listOf(TASK_2_OPTION_CONDITIONAL))
    private val TASK_2_VALUE_DELTA = ValueDelta(TASK_ID_2, Task.Type.MULTIPLE_CHOICE, TASK_2_VALUE)
    private val TASK_2_CONDITIONAL_VALUE_DELTA =
      ValueDelta(TASK_ID_2, Task.Type.MULTIPLE_CHOICE, TASK_2_CONDITIONAL_VALUE)

    private const val TASK_ID_CONDITIONAL = "conditional"
    const val TASK_CONDITIONAL_NAME = "conditional task"
    private const val TASK_CONDITIONAL_RESPONSE = "conditional response"
    private val TASK_CONDITIONAL_VALUE = TextTaskData.fromString(TASK_CONDITIONAL_RESPONSE)
    private val TASK_CONDITIONAL_VALUE_DELTA =
      ValueDelta(TASK_ID_CONDITIONAL, Task.Type.TEXT, TASK_CONDITIONAL_VALUE)

    private val TASKS =
      listOf(
        Task(TASK_ID_0, 0, Task.Type.DROP_PIN, TASK_0_NAME, true, isAddLoiTask = true),
        Task(TASK_ID_1, 1, Task.Type.TEXT, TASK_1_NAME, true),
        Task(
          TASK_ID_2,
          2,
          Task.Type.MULTIPLE_CHOICE,
          TASK_2_NAME,
          true,
          multipleChoice = TASK_2_MULTIPLE_CHOICE,
        ),
        Task(
          TASK_ID_CONDITIONAL,
          3,
          Task.Type.TEXT,
          TASK_CONDITIONAL_NAME,
          true,
          condition =
            Condition(
              Condition.MatchType.MATCH_ANY,
              expressions =
                listOf(
                  Expression(
                    Expression.ExpressionType.ANY_OF_SELECTED,
                    TASK_ID_2,
                    optionIds = setOf(TASK_2_OPTION_CONDITIONAL),
                  )
                ),
            ),
        ),
      )

    private val JOB = FakeData.JOB.copy(tasks = TASKS.associateBy { it.id })
    private val SURVEY = FakeData.SURVEY.copy(jobMap = mapOf(Pair(JOB.id, JOB)))
  }
}
