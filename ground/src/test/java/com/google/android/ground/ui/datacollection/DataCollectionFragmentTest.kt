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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.*
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.Task
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
import io.reactivex.Single
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
  @Captor lateinit var taskDataDeltaCaptor: ArgumentCaptor<List<TaskDataDelta>>
  lateinit var fragment: DataCollectionFragment

  @Test
  fun created_submissionIsLoaded_toolbarIsShown() {
    setupSubmission()
    setupFragment()

    onView(withText(LOCATION_OF_INTEREST.caption)).check(matches(isDisplayed()))
    onView(withText(JOB.name)).check(matches(isDisplayed()))
  }

  @Test
  fun created_submissionIsLoaded_firstTaskIsShown() {
    setupSubmission(mapOf("field id" to Task("field id", 0, Task.Type.TEXT, TASK_1_NAME, true)))
    setupFragment()

    onView(allOf(withText(TASK_1_NAME))).check(matches(isDisplayed()))
    onView(withId(R.id.text_input_layout)).check(matches(isDisplayed()))
  }

  @Test
  fun onContinueClicked_noUserInput_buttonDisabled() {
    setupSubmission()
    setupFragment()

    onView(allOf(withText("Continue"), isDisplayed(), isNotEnabled())).perform(click())

    onView(withText(TASK_1_NAME)).check(matches(isDisplayed()))
    onView(withText(TASK_2_NAME)).check(matches(not(isDisplayed())))
  }

  @Test
  fun onContinueClicked_newTaskIsShown() {
    setupSubmission()
    setupFragment()
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText("user input"))

    onView(allOf(withText("Continue"), isDisplayed())).perform(click())

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    onView(withText(TASK_1_NAME)).check(matches(not(isDisplayed())))
    onView(withText(TASK_2_NAME)).check(matches(isDisplayed()))
  }

  @Test
  fun onContinueClicked_thenOnBack_initialTaskIsShown() {
    setupSubmission()
    setupFragment()
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText("user input"))
    onView(allOf(withText("Continue"), isDisplayed())).perform(click())
    onView(withText(TASK_1_NAME)).check(matches(not(isDisplayed())))
    onView(withText(TASK_2_NAME)).check(matches(isDisplayed()))

    assertThat(fragment.onBack()).isTrue()

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    onView(withText(TASK_1_NAME)).check(matches(isDisplayed()))
    onView(withText(TASK_2_NAME)).check(matches(not(isDisplayed())))
  }

  @Test
  fun onContinueClicked_onFinalTask_resultIsSaved() = runWithTestDispatcher {
    setupSubmission()
    setupFragment()
    val task1Response = "response 1"
    val task2Response = "response 2"
    val expectedTaskDataDeltas =
      listOf(
        TaskDataDelta(
          SUBMISSION.job.tasksSorted[0].id,
          Task.Type.TEXT,
          TextTaskData.fromString(task1Response)
        ),
        TaskDataDelta(
          SUBMISSION.job.tasksSorted[1].id,
          Task.Type.TEXT,
          TextTaskData.fromString(task2Response)
        ),
      )
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText(task1Response))
    onView(allOf(withText("Continue"), isDisplayed())).perform(click())
    onView(withText(TASK_1_NAME)).check(matches(not(isDisplayed())))
    onView(withText(TASK_2_NAME)).check(matches(isDisplayed()))

    // Click continue on final task
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText(task2Response))
    onView(allOf(withText("Continue"), isDisplayed())).perform(click())
    advanceUntilIdle()

    verify(submissionRepository)
      .saveSubmission(eq(SURVEY.id), eq(LOCATION_OF_INTEREST.id), capture(taskDataDeltaCaptor))
    expectedTaskDataDeltas.forEach { taskData ->
      assertThat(taskDataDeltaCaptor.value).contains(taskData)
    }
  }

  @Test
  fun onBack_firstViewPagerItem_returnsFalse() {
    setupSubmission()
    setupFragment()

    assertThat(fragment.onBack()).isFalse()
  }

  private fun setupSubmission(tasks: Map<String, Task>? = null) {
    var submission = SUBMISSION
    var job = SUBMISSION.job
    if (tasks != null) {
      job = job.copy(tasks = tasks)
      submission = submission.copy(job = job)
    }

    whenever(submissionRepository.createSubmission(SURVEY.id, LOCATION_OF_INTEREST.id))
      .thenReturn(Single.just(submission))

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

  private fun setupFragment() {
    val argsBundle =
      DataCollectionFragmentArgs.Builder(LOCATION_OF_INTEREST.id, JOB.id).build().toBundle()

    launchFragmentWithNavController<DataCollectionFragment>(
      argsBundle,
      destId = R.id.data_collection_fragment
    ) {
      fragment = this as DataCollectionFragment
    }
  }
}
