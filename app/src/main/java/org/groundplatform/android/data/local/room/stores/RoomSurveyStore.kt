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
package org.groundplatform.android.data.local.room.stores

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.groundplatform.android.data.local.room.LocalDatabase
import org.groundplatform.android.data.local.room.converter.toLocalDataStoreObject
import org.groundplatform.android.data.local.room.converter.toModelObject
import org.groundplatform.android.data.local.room.dao.ConditionDao
import org.groundplatform.android.data.local.room.dao.ExpressionDao
import org.groundplatform.android.data.local.room.dao.JobDao
import org.groundplatform.android.data.local.room.dao.MultipleChoiceDao
import org.groundplatform.android.data.local.room.dao.OptionDao
import org.groundplatform.android.data.local.room.dao.SurveyDao
import org.groundplatform.android.data.local.room.dao.TaskDao
import org.groundplatform.android.data.local.room.dao.insertOrUpdate
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task

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

  @Inject lateinit var localDatabase: LocalDatabase

  override val surveys: Flow<List<Survey>>
    get() = surveyDao.getAll().map { surveyEntities -> surveyEntities.map { it.toModelObject() } }

  override fun survey(id: String): Flow<Survey?> = surveyDao.survey(id).map { it?.toModelObject() }

  /**
   * Attempts to update persisted data associated with a [Survey] in the local database. If the
   * provided survey does not exist, inserts the given survey into the database.
   */
  override suspend fun insertOrUpdateSurvey(survey: Survey) =
    localDatabase.withTransaction {
      // Update survey.
      surveyDao.insertOrUpdate(survey.toLocalDataStoreObject())
      // Add or update jobs and tasks.
      insertOrUpdateJobs(survey.id, survey.jobs)
      // Delete removed jobs.
      jobDao.deleteNotIn(survey.id, survey.jobs.map { it.id })
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
    optionDao.deleteNotIn(taskId, options.map { it.id })
  }

  private suspend fun insertOrUpdateCondition(taskId: String, condition: Condition) {
    // Condition and expression are leaf entities with no cascading deletes, so delete and re-insert
    // are acceptable here.
    deleteCondition(taskId)
    conditionDao.insert(condition.toLocalDataStoreObject(parentTaskId = taskId))
    condition.expressions.forEach {
      expressionDao.insert(it.toLocalDataStoreObject(parentTaskId = taskId))
    }
  }

  private suspend fun deleteCondition(taskId: String) {
    conditionDao.deleteByTaskId(taskId)
    expressionDao.deleteByTaskId(taskId)
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
    } else {
      deleteCondition(task.id)
    }
  }

  private suspend fun insertOrUpdateTasks(jobId: String, tasks: Collection<Task>) {
    tasks.sortedBy { it.index }.forEach { insertOrUpdateTask(jobId, it) }
    taskDao.deleteNotIn(jobId, tasks.map { it.id })
  }

  private suspend fun insertOrUpdateJob(surveyId: String, job: Job) {
    jobDao.insertOrUpdate(job.toLocalDataStoreObject(surveyId))
    insertOrUpdateTasks(job.id, job.tasks.values)
  }

  private suspend fun insertOrUpdateJobs(surveyId: String, jobs: Collection<Job>) =
    jobs.forEach { insertOrUpdateJob(surveyId, it) }
}
