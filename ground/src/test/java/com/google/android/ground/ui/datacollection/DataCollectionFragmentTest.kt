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

import android.widget.RadioButton
import androidx.lifecycle.ViewModelStore
import androidx.navigation.Navigation
import androidx.navigation.set
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.*
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.JOB
import com.sharedtest.FakeData.LOCATION_OF_INTEREST
import com.sharedtest.FakeData.SUBMISSION
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.FakeData.TASK_1_NAME
import com.sharedtest.FakeData.TASK_2_NAME
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Single
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

  @Inject lateinit var testDispatcher: TestDispatcher
  @BindValue @Mock lateinit var submissionRepository: SubmissionRepository
  @Captor lateinit var taskDataDeltaCaptor: ArgumentCaptor<List<TaskDataDelta>>
  lateinit var fragment: DataCollectionFragment

  @Before
  override fun setUp() {
    super.setUp()

    setupSubmission()
  }

  @Test
  fun created_submissionIsLoaded_loiNameIsShown() {
    setupFragment()

    onView(withText(LOCATION_OF_INTEREST.caption)).check(matches(isDisplayed()))
  }

  @Test
  fun created_submissionIsLoaded_jobNameIsShown() {
    setupFragment()

    onView(withText(JOB.name)).check(matches(isDisplayed()))
  }

  @Test
  fun created_submissionIsLoaded_viewPagerAdapterIsSet() {
    setupFragment()

    onView(withId(R.id.pager)).check(matches(isDisplayed()))
  }

  @Test
  fun created_submissionIsLoaded_firstTaskIsShown() {
    setupSubmission(mapOf(Pair("field id", Task("field id", 0, Task.Type.TEXT, TASK_1_NAME, true))))
    setupFragment()

    onView(allOf(withText(TASK_1_NAME))).check(matches(isDisplayed()))
    onView(withId(R.id.text_input_layout)).check(matches(isDisplayed()))
  }

  @Test
  fun created_multipleChoice_selectMultiple_submissionIsLoaded_properTaskIsShown() {
    val label = "multiple_choice_task"
    val option1Label = "Option 1"
    setupSubmission(
      mapOf(
        Pair(
          "field id",
          Task(
            "1",
            0,
            Task.Type.MULTIPLE_CHOICE,
            label,
            isRequired = false,
            multipleChoice =
              MultipleChoice(
                persistentListOf(
                  Option("1", "code1", option1Label),
                  Option("2", "code2", "Option 2"),
                ),
                MultipleChoice.Cardinality.SELECT_MULTIPLE
              )
          )
        )
      )
    )
    setupFragment()

    onView(allOf(withText(label))).check(matches(isDisplayed()))
    onView(withId(R.id.select_option_list)).check(matches(allOf(isDisplayed(), hasChildCount(2))))
    onView(withText(option1Label))
      .check(matches(allOf(isDisplayed(), instanceOf(MaterialCheckBox::class.java))))
  }

  @Test
  fun created_multipleChoice_selectOne_submissionIsLoaded_properTaskIsShown() {
    val label = "multiple_choice_task"
    val option1Label = "Option 1"
    setupSubmission(
      mapOf(
        Pair(
          "field id",
          Task(
            "1",
            0,
            Task.Type.MULTIPLE_CHOICE,
            label,
            isRequired = false,
            multipleChoice =
              MultipleChoice(
                persistentListOf(
                  Option("1", "code1", option1Label),
                  Option("2", "code2", "Option 2"),
                ),
                MultipleChoice.Cardinality.SELECT_ONE
              )
          )
        )
      )
    )
    setupFragment()

    onView(allOf(withText(label))).check(matches(isDisplayed()))
    onView(withId(R.id.select_option_list)).check(matches(allOf(isDisplayed(), hasChildCount(2))))
    onView(withText(option1Label))
      .check(matches(allOf(isDisplayed(), instanceOf(RadioButton::class.java))))
  }

  @Test
  fun onContinueClicked_noUserInput_toastIsShown() {
    setupFragment()

    onView(withId(R.id.data_collection_continue_button)).perform(click())

    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("This field is required")
    onView(withText(TASK_1_NAME)).check(matches(isDisplayed()))
    onView(withText(TASK_2_NAME)).check(matches(not(isDisplayed())))
  }

  @Test
  fun onContinueClicked_newTaskIsShown() {
    setupFragment()
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText("user input"))

    onView(withId(R.id.data_collection_continue_button)).perform(click())

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    onView(withText(TASK_1_NAME)).check(matches(not(isDisplayed())))
    onView(withText(TASK_2_NAME)).check(matches(isDisplayed()))
  }

  @Test
  fun onContinueClicked_thenOnBack_initialTaskIsShown() {
    setupFragment()
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText("user input"))
    onView(withId(R.id.data_collection_continue_button)).perform(click())
    onView(withText(TASK_1_NAME)).check(matches(not(isDisplayed())))
    onView(withText(TASK_2_NAME)).check(matches(isDisplayed()))

    assertThat(fragment.onBack()).isTrue()

    assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    onView(withText(TASK_1_NAME)).check(matches(isDisplayed()))
    onView(withText(TASK_2_NAME)).check(matches(not(isDisplayed())))
  }

  @Test
  fun onContinueClicked_onFinalTask_resultIsSaved() =
    runTest(testDispatcher) {
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
      onView(withId(R.id.data_collection_continue_button)).perform(click())
      onView(withText(TASK_1_NAME)).check(matches(not(isDisplayed())))
      onView(withText(TASK_2_NAME)).check(matches(isDisplayed()))

      // Click continue on final task
      onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText(task2Response))
      onView(withId(R.id.data_collection_continue_button)).perform(click())
      advanceUntilIdle()

      verify(submissionRepository)
        .createOrUpdateSubmission(eq(SUBMISSION), capture(taskDataDeltaCaptor), eq(true))
      expectedTaskDataDeltas.forEach { taskData ->
        assertThat(taskDataDeltaCaptor.value).contains(taskData)
      }
    }

  @Test
  fun onContinueClicked_onFinalTask_withMultipleChoiceTask_resultIsSaved() =
    runTest(testDispatcher) {
      val label = "multiple_choice_task"
      val option2Label = "Option 2"
      val option2Id = "2"
      val multipleChoice =
        MultipleChoice(
          persistentListOf(
            Option("1", "code1", "Option 1"),
            Option(option2Id, "code2", option2Label),
          ),
          MultipleChoice.Cardinality.SELECT_ONE
        )
      val taskId = "task id"
      setupSubmission(
        mapOf(
          Pair(
            "field id",
            Task(
              taskId,
              0,
              Task.Type.MULTIPLE_CHOICE,
              label,
              isRequired = false,
              multipleChoice = multipleChoice
            )
          )
        )
      )
      setupFragment()
      val expectedTaskDataDeltas =
        TaskDataDelta(
          taskId,
          Task.Type.MULTIPLE_CHOICE,
          MultipleChoiceTaskData.fromList(multipleChoice, listOf(option2Id))
        )

      onView(allOf(withText(option2Label), isDisplayed())).perform(click())
      onView(withId(R.id.data_collection_continue_button)).perform(click())
      advanceUntilIdle()

      verify(submissionRepository)
        .createOrUpdateSubmission(any(), capture(taskDataDeltaCaptor), eq(true))
      assertThat(taskDataDeltaCaptor.value[0]).isEqualTo(expectedTaskDataDeltas)
    }

  @Test
  fun onBack_firstViewPagerItem_returnsFalse() {
    setupFragment()

    assertThat(fragment.onBack()).isFalse()
  }

  private fun setupSubmission(tasks: Map<String, Task>? = null) {
    var submission = SUBMISSION
    if (tasks != null) {
      submission = submission.copy(job = SUBMISSION.job.copy(tasks = tasks))
    }

    whenever(submissionRepository.createSubmission(SURVEY.id, LOCATION_OF_INTEREST.id))
      .thenReturn(Single.just(submission))
  }

  private fun setupFragment() {
    val argsBundle =
      DataCollectionFragmentArgs.Builder(SURVEY.id, LOCATION_OF_INTEREST.id).build().toBundle()

    val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    navController.setViewModelStore(ViewModelStore()) // required for graph scoped view models.
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.data_collection_fragment, argsBundle)

    hiltActivityScenario()
      .launchFragment<DataCollectionFragment>(
        argsBundle,
        preTransactionAction = {
          fragment = this as DataCollectionFragment
          this.also {
            it.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
              if (viewLifecycleOwner != null) {
                // Bind the controller after the view is created but before onViewCreated is called.
                Navigation.setViewNavController(fragment.requireView(), navController)
              }
            }
          }
        }
      )
  }
}
