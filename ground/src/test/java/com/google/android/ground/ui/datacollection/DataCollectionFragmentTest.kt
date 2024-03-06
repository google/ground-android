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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.*
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.submission.TextResponse
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.repository.SubmissionRepository
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.JOB
import com.sharedtest.FakeData.LOCATION_OF_INTEREST
import com.sharedtest.FakeData.SUBMISSION
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.FakeData.TASK_1_NAME
import com.sharedtest.FakeData.TASK_2_NAME
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @BindValue @Mock lateinit var submissionRepository: SubmissionRepository
  @Captor lateinit var deltaCaptor: ArgumentCaptor<List<ValueDelta>>
  lateinit var fragment: DataCollectionFragment

  @Test
  fun created_submissionIsLoaded_toolbarIsShown() {
    setupSubmission()
    setupFragment()

    runner()
      .validateTextIsDisplayed("Unnamed point")
      .validateTextIsDisplayed(requireNotNull(JOB.name))
  }

  @Test
  fun created_submissionIsLoaded_firstTaskIsShown() {
    setupSubmission(mapOf("field id" to Task("field id", 0, Task.Type.TEXT, TASK_1_NAME, true)))
    setupFragment()

    onView(allOf(withText(TASK_1_NAME))).check(matches(isDisplayed()))
    onView(withId(R.id.text_input_layout)).check(matches(isDisplayed()))
  }

  @Test
  fun onNextClicked_noUserInput_buttonDisabled() {
    setupSubmission()
    setupFragment()

    runner()
      .clickNextButton()
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)
  }

  @Test
  fun onNextClicked_newTaskIsShown() {
    setupSubmission()
    setupFragment()

    runner()
      .inputText("user input")
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun onNextClicked_thenOnBack_initialTaskIsShown() {
    setupSubmission()
    setupFragment()

    runner()
      .inputText("user input")
      .clickNextButton()
      .pressBackButton(true)
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Click previous button shows initial task`() {
    setupSubmission()
    setupFragment()

    runner()
      .inputText("user input")
      .clickNextButton()
      .inputText("user input")
      .clickPreviousButton()
      .validateTextIsDisplayed(TASK_1_NAME)
      .validateTextIsNotDisplayed(TASK_2_NAME)

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
  }

  @Test
  fun `Next click saves draft`() = runWithTestDispatcher {
    setupSubmission()
    setupFragment()

    runner().inputText("user input").clickNextButton()

    verify(submissionRepository).deleteDraftSubmission()
    verify(submissionRepository)
      .saveDraftSubmission(
        eq(JOB.id),
        eq(LOCATION_OF_INTEREST.id),
        eq(SURVEY.id),
        capture(deltaCaptor),
      )

    val expectedDeltas =
      listOf(
        ValueDelta(
          SUBMISSION.job.tasksSorted[0].id,
          Task.Type.TEXT,
          TextResponse.fromString("user input"),
        )
      )

    expectedDeltas.forEach { value -> assertThat(deltaCaptor.value).contains(value) }
  }

  @Test
  fun `Clicking previous button saves draft`() = runWithTestDispatcher {
    setupSubmission()
    setupFragment()

    runner()
      .inputText("user input")
      .clickNextButton()
      .inputText("user input 2")
      .clickPreviousButton()

    verify(submissionRepository, times(2)).deleteDraftSubmission()
    verify(submissionRepository, times(2))
      .saveDraftSubmission(
        eq(JOB.id),
        eq(LOCATION_OF_INTEREST.id),
        eq(SURVEY.id),
        capture(deltaCaptor),
      )

    val expectedDeltas =
      listOf(
        ValueDelta(
          SUBMISSION.job.tasksSorted[0].id,
          Task.Type.TEXT,
          TextResponse.fromString("user input"),
        ),
        ValueDelta(
          SUBMISSION.job.tasksSorted[1].id,
          Task.Type.TEXT,
          TextResponse.fromString("user input 2"),
        ),
      )

    expectedDeltas.forEach { value -> assertThat(deltaCaptor.value).contains(value) }
  }

  @Test
  fun `Click previous button does not show initial task if validation failed`() {
    setupSubmission()
    setupFragment()

    runner()
      .inputText("user input")
      .clickNextButton()
      .clickPreviousButton()
      .validateTextIsDisplayed(TASK_2_NAME)
      .validateTextIsNotDisplayed(TASK_1_NAME)

    assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
  }

  @Test
  fun `Load tasks from draft`() = runWithTestDispatcher {
    // TODO(#708): add coverage for loading from draft for all types of tasks
    val expectedDeltas =
      listOf(
        ValueDelta(
          SUBMISSION.job.tasksSorted[0].id,
          Task.Type.TEXT,
          TextResponse.fromString("user input"),
        ),
        ValueDelta(
          SUBMISSION.job.tasksSorted[1].id,
          Task.Type.TEXT,
          TextResponse.fromString("user input 2"),
        ),
      )

    setupSubmission()
    setupFragment(
      DataCollectionFragmentArgs.Builder(
          LOCATION_OF_INTEREST.id,
          JOB.id,
          true,
          SubmissionDeltasConverter.toString(expectedDeltas),
        )
        .build()
        .toBundle()
    )

    runner()
      .validateTextIsDisplayed("user input")
      .clickNextButton()
      .validateTextIsDisplayed("user input 2")
  }

  @Test
  fun onNextClicked_onFinalTask_resultIsSaved() = runWithTestDispatcher {
    setupSubmission()
    setupFragment()
    val task1Response = "response 1"
    val task2Response = "response 2"
    val expectedDeltas =
      listOf(
        ValueDelta(
          SUBMISSION.job.tasksSorted[0].id,
          Task.Type.TEXT,
          TextResponse.fromString(task1Response),
        ),
        ValueDelta(
          SUBMISSION.job.tasksSorted[1].id,
          Task.Type.TEXT,
          TextResponse.fromString(task2Response),
        ),
      )

    runner()
      .inputText(task1Response)
      .clickNextButton()
      .validateTextIsNotDisplayed(TASK_1_NAME)
      .validateTextIsDisplayed(TASK_2_NAME)
      .inputText(task2Response)
      .clickDoneButton() // Click "done" on final task

    verify(submissionRepository)
      .saveSubmission(eq(SURVEY.id), eq(LOCATION_OF_INTEREST.id), capture(deltaCaptor))
    expectedDeltas.forEach { value -> assertThat(deltaCaptor.value).contains(value) }
  }

  @Test
  fun onBack_firstViewPagerItem_returnsFalse() {
    setupSubmission()
    setupFragment()

    runner().pressBackButton(false)
  }

  private fun setupSubmission(tasks: Map<String, Task>? = null) = runWithTestDispatcher {
    var submission = SUBMISSION
    var job = SUBMISSION.job
    if (tasks != null) {
      job = job.copy(tasks = tasks)
      submission = submission.copy(job = job)
    }

    whenever(submissionRepository.createSubmission(SURVEY.id, LOCATION_OF_INTEREST.id))
      .thenReturn(submission)

    runWithTestDispatcher {
      // Setup survey and LOIs
      val jobMap = SURVEY.jobMap.entries.associate { it.key to job }
      val survey = SURVEY.copy(jobMap = jobMap)

      fakeRemoteDataStore.surveys = listOf(survey)
      fakeRemoteDataStore.lois = listOf(LOCATION_OF_INTEREST)
      activateSurvey(SURVEY.id)
      advanceUntilIdle()
    }
  }

  private fun setupFragment(fragmentArgs: Bundle? = null) {
    val argsBundle =
      fragmentArgs
        ?: DataCollectionFragmentArgs.Builder(LOCATION_OF_INTEREST.id, JOB.id, false, null)
          .build()
          .toBundle()

    launchFragmentWithNavController<DataCollectionFragment>(
      argsBundle,
      destId = R.id.data_collection_fragment,
    ) {
      fragment = this as DataCollectionFragment
    }
  }

  private fun runner() = Runner(this, fragment)

  /** Helper class for interacting with the data collection tasks and verifying the ui state. */
  class Runner(
    private val baseHiltTest: BaseHiltTest,
    private val fragment: DataCollectionFragment,
  ) {

    internal fun clickNextButton(): Runner {
      clickButton("Next")
      return this
    }

    internal fun clickPreviousButton(): Runner {
      clickButton("Previous")
      return this
    }

    internal fun clickDoneButton(): Runner {
      clickButton("Done")
      return this
    }

    private fun clickButton(text: String) =
      baseHiltTest.runWithTestDispatcher {
        onView(allOf(withText(text), isDisplayed())).perform(click())
        advanceUntilIdle()
      }

    internal fun inputText(text: String): Runner {
      onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText(text))
      return this
    }

    internal fun validateTextIsDisplayed(text: String): Runner {
      onView(withText(text)).check(matches(isDisplayed()))
      return this
    }

    internal fun validateTextIsNotDisplayed(text: String): Runner {
      onView(withText(text)).check(matches(not(isDisplayed())))
      return this
    }

    internal fun pressBackButton(result: Boolean): Runner {
      assertThat(fragment.onBack()).isEqualTo(result)
      return this
    }
  }
}
