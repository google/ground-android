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

package com.google.android.ground.ui.datacollection

import android.os.Bundle
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.launchFragmentWithNavController
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.DraftSubmission
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Expression
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.FakeData.LOCATION_OF_INTEREST
import com.sharedtest.FakeData.LOCATION_OF_INTEREST_NAME
import com.sharedtest.FakeData.USER
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var mutationRepository: MutationRepository
  @Inject lateinit var submissionRepository: SubmissionRepository
  @Inject lateinit var userRepository: UserRepository

  lateinit var fragment: DataCollectionFragment

  override fun setUp() = runBlocking {
    super.setUp()
    setupSubmission()
    setupFragment()
  }

  @Test
  fun `Job and LOI names are displayed correctly`() {
    runner()
      .validateTextIsDisplayed("Unnamed point")
      .validateTextIsDisplayed(requireNotNull(JOB.name))
  }

  @Test
  fun `First task is loaded and is visible`() {
    runner().validateTextIsDisplayed(TASK_1_NAME).validateTextIsNotDisplayed(TASK_2_NAME)
  }

  @Test
  fun `Next button is disabled when task doesn't have any value`() {
    runner().assertButtonIsDisabled("Next")
  }

  @Test
  fun `Next button proceeds to the second task when task has value`() {
    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)

    // Ensure that no validation error toasts were displayed
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Previous button navigates back to first task`() {
    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .pressBackButton(true)
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)

    // Ensure that no validation error toasts were displayed
    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Next click saves draft`() = runWithTestDispatcher {
    runner().inputText(TASK_1_RESPONSE).clickNextButton()

    assertDraftSaved(listOf(TASK_1_VALUE_DELTA), currentTaskId = TASK_ID_2)
  }

  @Test
  fun `Clicking previous button saves draft`() = runWithTestDispatcher {
    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .selectMultipleChoiceOption(TASK_2_OPTION_LABEL)
      .clickPreviousButton()

    assertDraftSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA), currentTaskId = TASK_ID_1)
  }

  @Test
  fun `Click previous button moves to previous task if task is empty`() {
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
    // TODO(#708): add coverage for loading from draft for all types of tasks
    val expectedDeltas = listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA)

    // Start the fragment with draft values
    setupFragment(
      DataCollectionFragmentArgs.Builder(
          LOCATION_OF_INTEREST.id,
          LOCATION_OF_INTEREST_NAME,
          JOB.id,
          true,
          SubmissionDeltasConverter.toString(expectedDeltas),
          "",
        )
        .build()
        .toBundle()
    )

    runner()
      .validateTextIsDisplayed(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_OPTION_LABEL)
  }

  @Test
  fun `Clicking done on final task saves the submission`() = runWithTestDispatcher {
    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)
      .selectMultipleChoiceOption(TASK_2_OPTION_LABEL)
      .clickDoneButton() // Click "done" on final task

    assertSubmissionSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA))
  }

  @Test
  fun `Clicking back button on first task clears the draft and returns false`() =
    runWithTestDispatcher {
      runner()
        .inputText(TASK_1_RESPONSE)
        .clickNextButton()
        .pressBackButton(true)
        .pressBackButton(false)

      assertNoDraftSaved()
    }

  @Test
  fun `Clicking done after triggering conditional task saves task data`() = runWithTestDispatcher {
    runner()
      .inputText(TASK_1_RESPONSE)
      .clickNextButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      // Select the option to unhide the conditional task.
      .selectMultipleChoiceOption(TASK_2_OPTION_CONDITIONAL_LABEL)
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
      runner()
        .inputText(TASK_1_RESPONSE)
        .clickNextButton()
        .validateTextIsDisplayed(TASK_2_NAME)
        // Select the option to unhide the conditional task.
        .selectMultipleChoiceOption(TASK_2_OPTION_CONDITIONAL_LABEL)
        .clickNextButton()
        .validateTextIsDisplayed(TASK_CONDITIONAL_NAME)
        // Input a value, then go back to hide the task again.
        .inputText(TASK_CONDITIONAL_RESPONSE)
        .clickPreviousButton()
        .validateTextIsDisplayed(TASK_2_NAME)
        // Unselect the option to hide the conditional task.
        .selectMultipleChoiceOption(TASK_2_OPTION_CONDITIONAL_LABEL)
        .selectMultipleChoiceOption(TASK_2_OPTION_LABEL)
        .clickDoneButton()
        .validateTextIsNotDisplayed(TASK_CONDITIONAL_NAME)

      // Conditional task data is not submitted.
      assertSubmissionSaved(listOf(TASK_1_VALUE_DELTA, TASK_2_VALUE_DELTA))
    }

  private suspend fun assertSubmissionSaved(valueDeltas: List<ValueDelta>) {
    assertNoDraftSaved()

    val loiId = LOCATION_OF_INTEREST.id

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

  private fun setupFragment(fragmentArgs: Bundle? = null) {
    val argsBundle =
      fragmentArgs
        ?: DataCollectionFragmentArgs.Builder(
            LOCATION_OF_INTEREST.id,
            LOCATION_OF_INTEREST_NAME,
            JOB.id,
            false,
            null,
            "",
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

  companion object {
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
        Task(TASK_ID_1, 0, Task.Type.TEXT, TASK_1_NAME, true),
        Task(
          TASK_ID_2,
          1,
          Task.Type.MULTIPLE_CHOICE,
          TASK_2_NAME,
          true,
          multipleChoice = TASK_2_MULTIPLE_CHOICE,
        ),
        Task(
          TASK_ID_CONDITIONAL,
          2,
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
