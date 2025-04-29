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
package org.groundplatform.android.persistence.local

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.Condition.MatchType.MATCH_ALL
import org.groundplatform.android.model.task.Expression
import org.groundplatform.android.model.task.Expression.ExpressionType.ALL_OF_SELECTED
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.MultipleChoice.Cardinality.SELECT_ONE
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task.Type.MULTIPLE_CHOICE
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalSurveyStoreTest : BaseHiltTest() {
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  private val job1 = Job("job 1", Style(""), "job 1 name")
  private val job2 = Job("job 2", Style(""), "job 2 name")

  @Test
  fun `insertOrUpdateSurvey() inserts new survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    assertThat(localSurveyStore.surveys.first()).containsExactly(SURVEY)
  }

  @Test
  fun `getSurveyById() retrieves survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    assertThat(localSurveyStore.getSurveyById(SURVEY.id)).isEqualTo(SURVEY)
  }

  @Test
  fun `deleteSurvey() removes survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    localSurveyStore.deleteSurvey(SURVEY)

    // Verify survey is no longer found in store.
    assertThat(localSurveyStore.surveys.first()).isEmpty()
  }

  @Test
  fun `insertOrUpdateSurvey() removes deleted jobs`() = runWithTestDispatcher {
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(
      SURVEY.copy(jobMap = mapOf(job1.id to job1, job2.id to job2))
    )

    // Update survey, removing one job.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job2.id to job2)))

    // Verify updated survey is returned.
    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs).containsExactly(job2)
  }

  @Test
  fun `insertOrUpdateSurvey() updates existing jobs`() = runWithTestDispatcher {
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job1.id to job1)))

    // Update survey, renaming both jobs.
    val modifiedJob1 = job1.copy(name = "New name 1")
    val modifiedJob2 = job2.copy(name = "New name 2")
    localSurveyStore.insertOrUpdateSurvey(
      SURVEY.copy(jobMap = mapOf(job1.id to modifiedJob1, job2.id to modifiedJob2))
    )

    // Verify updated survey is returned.
    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs).containsExactly(modifiedJob1, modifiedJob2)
  }

  @Test
  fun `insertOrUpdateSurvey() inserts new jobs`() = runWithTestDispatcher {
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job1.id to job1)))

    // Update data survey, removing one job.
    localSurveyStore.insertOrUpdateSurvey(
      SURVEY.copy(jobMap = mapOf(job1.id to job1, job2.id to job2))
    )

    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs).containsExactly(job1, job2)
  }

  @Test
  fun `insertOrUpdateSurvey() removes deleted tasks`() = runWithTestDispatcher {
    val task1 = newTask(id = "task1")
    val task2 = newTask(id = "task2")
    val job = job1.copy(tasks = mapOf(task1.id to task1, task2.id to task2))
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job.id to job)))

    val updatedJob = job.copy(tasks = mapOf(task2.id to task2))
    // Update survey, removing one job.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(updatedJob.id to updatedJob)))

    // Verify updated survey is returned.
    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs).containsExactly(updatedJob)
  }

  @Test
  fun `insertOrUpdateSurvey() removes deleted conditional expressions`() = runWithTestDispatcher {
    val task =
      newTask(
        condition =
          Condition(
            matchType = MATCH_ALL,
            expressions = listOf(Expression(expressionType = ALL_OF_SELECTED, taskId = "123")),
          )
      )
    val job = job1.copy(tasks = mapOf(task.id to task))
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job.id to job)))

    val updatedTask = task.copy(condition = null)
    val updatedJob = job.copy(tasks = mapOf(updatedTask.id to updatedTask))
    // Update survey, removing one job.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(updatedJob.id to updatedJob)))

    // Verify updated survey is returned.
    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs.first().tasks[task.id]!!.condition).isNull()
  }

  @Test
  fun `insertOrUpdateSurvey() removes deleted multiple choice options`() = runWithTestDispatcher {
    val multipleChoice =
      MultipleChoice(
        cardinality = SELECT_ONE,
        options =
          persistentListOf(
            Option(id = "option 1", code = "", label = "option 1"),
            Option(id = "option 2", code = "", label = "option 2"),
            Option(id = "option 3", code = "", label = "option 3"),
          ),
      )
    val task = newTask(type = MULTIPLE_CHOICE, multipleChoice = multipleChoice)
    val job = job2.copy(tasks = mapOf(task.id to task))
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job.id to job)))

    val updatedMultipleChoice =
      multipleChoice.copy(options = persistentListOf(multipleChoice.options[1]))
    val updatedTask = task.copy(multipleChoice = updatedMultipleChoice)
    val updatedJob = job.copy(tasks = mapOf(updatedTask.id to updatedTask))
    // Update survey, removing one job.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(updatedJob.id to updatedJob)))

    // Verify updated survey is returned.
    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs).containsExactly(updatedJob)
  }
}
