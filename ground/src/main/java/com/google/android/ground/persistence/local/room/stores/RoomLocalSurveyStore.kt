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
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.*
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.rx.Schedulers
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/** Manages access to [Survey] objects persisted in local storage. */
@Singleton
class RoomLocalSurveyStore @Inject internal constructor() : LocalSurveyStore {
  @Inject lateinit var optionDao: OptionDao
  @Inject lateinit var multipleChoiceDao: MultipleChoiceDao
  @Inject lateinit var taskDao: TaskDao
  @Inject lateinit var jobDao: JobDao
  @Inject lateinit var surveyDao: SurveyDao
  @Inject lateinit var baseMapDao: BaseMapDao
  @Inject lateinit var schedulers: Schedulers

  override val surveys: Single<List<Survey>>
    get() =
      surveyDao
        .getAllSurveys()
        .map { list: List<SurveyEntityAndRelations> -> list.map { it.toModelObject() } }
        .subscribeOn(schedulers.io())

  /**
   * Attempts to update persisted data associated with a [Survey] in the local database. If the
   * provided survey does not exist, inserts the given survey into the database.
   */
  override fun insertOrUpdateSurvey(survey: Survey): Completable =
    surveyDao
      .insertOrUpdate(survey.toLocalDataStoreObject())
      .andThen(jobDao.deleteBySurveyId(survey.id))
      .andThen(insertOrUpdateJobs(survey.id, survey.jobs))
      .andThen(baseMapDao.deleteBySurveyId(survey.id))
      .andThen(insertOfflineBaseMapSources(survey))
      .subscribeOn(schedulers.io())

  /**
   * Attempts to retrieve the [Survey] with the given ID from the local database. If retrieval
   * fails, returns a [NoSuchElementException].
   */
  override fun getSurveyById(id: String): Maybe<Survey> =
    surveyDao.getSurveyById(id).map { it.toModelObject() }.subscribeOn(schedulers.io())

  /** Deletes the provided [Survey] from the local database, if it exists in the database. */
  override fun deleteSurvey(survey: Survey): Completable =
    surveyDao.delete(survey.toLocalDataStoreObject()).subscribeOn(schedulers.io())

  private fun insertOrUpdateOption(taskId: String, option: Option): Completable =
    optionDao.insertOrUpdate(option.toLocalDataStoreObject(taskId)).subscribeOn(schedulers.io())

  private fun insertOrUpdateOptions(taskId: String, options: List<Option>): Completable =
    Observable.fromIterable(options)
      .flatMapCompletable { insertOrUpdateOption(taskId, it) }
      .subscribeOn(schedulers.io())

  private fun insertOrUpdateMultipleChoice(
    taskId: String,
    multipleChoice: MultipleChoice
  ): Completable =
    multipleChoiceDao
      .insertOrUpdate(multipleChoice.toLocalDataStoreObject(taskId))
      .andThen(insertOrUpdateOptions(taskId, multipleChoice.options))
      .subscribeOn(schedulers.io())

  private fun insertOrUpdateTask(jobId: String, task: Task): Completable =
    taskDao
      .insertOrUpdate(task.toLocalDataStoreObject(jobId))
      .andThen(
        Observable.just(task)
          .filter { task.multipleChoice != null }
          .flatMapCompletable { insertOrUpdateMultipleChoice(task.id, task.multipleChoice!!) }
      )
      .subscribeOn(schedulers.io())

  private fun insertOrUpdateTasks(jobId: String, tasks: Collection<Task>): Completable =
    Observable.fromIterable(tasks).flatMapCompletable { insertOrUpdateTask(jobId, it) }

  private fun insertOrUpdateJob(surveyId: String, job: Job): Completable =
    jobDao
      .insertOrUpdate(job.toLocalDataStoreObject(surveyId))
      .andThen(insertOrUpdateTasks(job.id, job.tasks.values))
      .subscribeOn(schedulers.io())

  private fun insertOrUpdateJobs(surveyId: String, jobs: List<Job>): Completable =
    Observable.fromIterable(jobs).flatMapCompletable { insertOrUpdateJob(surveyId, it) }

  private fun insertOfflineBaseMapSources(survey: Survey): Completable =
    Observable.fromIterable(survey.baseMaps).flatMapCompletable {
      baseMapDao.insert(it.toLocalDataStoreObject(surveyId = survey.id))
    }
}
