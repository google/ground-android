/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.persistence.local.room.stores

import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.ConditionDao
import com.google.android.ground.persistence.local.room.dao.ExpressionDao
import com.google.android.ground.persistence.local.room.dao.JobDao
import com.google.android.ground.persistence.local.room.dao.MultipleChoiceDao
import com.google.android.ground.persistence.local.room.dao.OptionDao
import com.google.android.ground.persistence.local.room.dao.SurveyDao
import com.google.android.ground.persistence.local.room.dao.TaskDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Manages access to [Survey] objects persisted in local storage. */
@Singleton
class RoomSurveyStore @Inject internal constructor() : LocalSurveyStore {
  @Inject lateinit var optionDao: OptionDao

  @Inject lateinit var multipleChoiceDao: MultipleChoiceDao

  @Inject lateinit var taskDao: TaskDao

  @Inject lateinit var jobDao: JobDao

  @Inject lateinit var surveyDao: SurveyDao

  @Inject lateinit var conditionDao: ConditionDao

  @Inject lateinit var expressionDao: ExpressionDao

  override val surveys: Flow<List<Survey>>
    get() = surveyDao.getAll().map { surveyEntities -> surveyEntities.map { it.toModelObject() } }

  override fun survey(id: String): Flow<Survey?> = surveyDao.survey(id).map { it?.toModelObject() }

  /**
   * Attempts to update persisted data associated with a [Survey] in the local database. If the
   * provided survey does not exist, inserts the given survey into the database.
   */
  override suspend fun insertOrUpdateSurvey(survey: Survey) {
    surveyDao.insertOrUpdate(survey.toLocalDataStoreObject())
    jobDao.deleteBySurveyId(survey.id)
    insertOrUpdateJobs(survey.id, survey.jobs)
  }

  /**
   * Returns the [Survey] with the given ID from the local database. Returns `null` if retrieval
   * fails.
   */
  override suspend fun getSurveyById(id: String): Survey? =
    surveyDao.findSurveyById(id)?.toModelObject()

  /** Deletes the provided [Survey] from the local database, if it exists in the database. */
  override suspend fun deleteSurvey(survey: Survey) =
    surveyDao.delete(survey.toLocalDataStoreObject())

  private suspend fun insertOrUpdateOption(taskId: String, option: Option) =
    optionDao.insertOrUpdate(option.toLocalDataStoreObject(taskId))

  private suspend fun insertOrUpdateOptions(taskId: String, options: List<Option>) {
    options.forEach { insertOrUpdateOption(taskId, it) }
  }

  private suspend fun insertOrUpdateCondition(taskId: String, condition: Condition) {
    conditionDao.insertOrUpdate(condition.toLocalDataStoreObject(parentTaskId = taskId))
    condition.expressions.forEach {
      expressionDao.insertOrUpdate(it.toLocalDataStoreObject(parentTaskId = taskId))
    }
  }

  private suspend fun insertOrUpdateMultipleChoice(taskId: String, multipleChoice: MultipleChoice) {
    multipleChoiceDao.insertOrUpdate(multipleChoice.toLocalDataStoreObject(taskId))
    insertOrUpdateOptions(taskId, multipleChoice.options)
  }

  private suspend fun insertOrUpdateTask(jobId: String, task: Task) {
    taskDao.insertOrUpdate(task.toLocalDataStoreObject(jobId))
    if (task.multipleChoice != null) {
      insertOrUpdateMultipleChoice(task.id, task.multipleChoice)
    }
    if (task.condition != null) {
      insertOrUpdateCondition(task.id, task.condition)
    }
  }

  private suspend fun insertOrUpdateTasks(jobId: String, tasks: Collection<Task>) =
    tasks.forEach { insertOrUpdateTask(jobId, it) }

  private suspend fun insertOrUpdateJob(surveyId: String, job: Job) {
    jobDao.insertOrUpdate(job.toLocalDataStoreObject(surveyId))
    insertOrUpdateTasks(job.id, job.tasks.values)
  }

  private suspend fun insertOrUpdateJobs(surveyId: String, jobs: Collection<Job>) =
    jobs.forEach { insertOrUpdateJob(surveyId, it) }
}
