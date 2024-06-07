/*
 * Copyright 2024 Google LLC
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

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Expression
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.SubmissionRepository
import com.sharedtest.FakeData
import com.sharedtest.FakeData.LOCATION_OF_INTEREST
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionViewModelTest : BaseHiltTest() {
  @Inject lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @BindValue @Mock lateinit var submissionRepository: SubmissionRepository
  @Captor lateinit var deltaCaptor: ArgumentCaptor<List<ValueDelta>>

  override fun setUp() {
    super.setUp()
    setupSubmission()
  }

  private fun setupSubmission() = runWithTestDispatcher {
    whenever(submissionRepository.createSubmission(SURVEY.id, LOCATION_OF_INTEREST.id))
      .thenReturn(SUBMISSION)

    fakeRemoteDataStore.surveys = listOf(SURVEY)
    fakeRemoteDataStore.predefinedLois = listOf(LOCATION_OF_INTEREST)
    activateSurvey(SURVEY.id)
    advanceUntilIdle()
  }

  @Test fun `Conditional task data is not saved if skipped`() = runWithTestDispatcher {}

  companion object {
    private const val TASK_ID_1 = "1"
    const val TASK_1_NAME = "task 1"
    private const val TASK_1_RESPONSE = "response 1"
    private val TASK_1_VALUE = TextTaskData.fromString(TASK_1_RESPONSE)
    private val TASK_1_VALUE_DELTA = ValueDelta(TASK_ID_1, Task.Type.TEXT, TASK_1_VALUE)

    private const val TASK_ID_2 = "2"
    const val TASK_2_NAME = "task 2"
    private const val TASK_2_RESPONSE = "response 2"
    private val TASK_2_VALUE = TextTaskData.fromString(TASK_2_RESPONSE)
    private val TASK_2_VALUE_DELTA = ValueDelta(TASK_ID_2, Task.Type.TEXT, TASK_2_VALUE)
    private const val TASK_ID_HIDDEN = "hidden"
    private const val TASK_HIDDEN_NAME = "task hidden"
    private const val TASK_HIDDEN_RESPONSE = "response hidden"
    private val TASK_HIDDEN_VALUE = TextTaskData.fromString(TASK_HIDDEN_RESPONSE)
    private val TASK_HIDDEN_VALUE_DELTA =
      ValueDelta(TASK_ID_HIDDEN, Task.Type.TEXT, TASK_HIDDEN_VALUE)
    private val TASKS =
      listOf(
        Task(
          TASK_ID_1,
          0,
          Task.Type.MULTIPLE_CHOICE,
          TASK_1_NAME,
          true,
          multipleChoice =
            MultipleChoice(
              persistentListOf(
                Option("option id 1", "code1", "Option 1"),
                Option("option id 2", "code2", "Option 2"),
              ),
              MultipleChoice.Cardinality.SELECT_MULTIPLE,
            ),
        ),
        Task(TASK_ID_2, 1, Task.Type.TEXT, TASK_2_NAME, true),
        Task(
          TASK_ID_HIDDEN,
          1,
          Task.Type.TEXT,
          TASK_HIDDEN_NAME,
          true,
          condition =
            Condition(
              Condition.MatchType.MATCH_ANY,
              listOf(Expression(Expression.ExpressionType.ALL_OF_SELECTED, TASK_ID_1)),
            ),
        ),
      )
    private val JOB = FakeData.JOB.copy(tasks = TASKS.associateBy { it.id })
    private val SUBMISSION = FakeData.SUBMISSION.copy(job = JOB)
    private val SURVEY = FakeData.SURVEY.copy(jobMap = mapOf(Pair(JOB.id, JOB)))
  }
}
