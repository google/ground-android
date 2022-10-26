/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.persistence.local.room

import androidx.room.Transaction
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.Companion.byDescendingClientTimestamp
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.Mutation.Type.*
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.submission.TaskDataMap
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.room.converter.*
import com.google.android.ground.persistence.local.room.converter.ResponseMapConverter.toString
import com.google.android.ground.persistence.local.room.dao.*
import com.google.android.ground.persistence.local.room.entity.*
import com.google.android.ground.persistence.local.room.models.EntityState
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.models.TileSetEntityState
import com.google.android.ground.persistence.local.room.models.UserDetails
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.ui.util.FileUtil
import com.google.android.ground.util.StreamUtil.logErrorsAndSkipKt
import com.google.android.ground.util.toImmutableList
import com.google.android.ground.util.toImmutableSet
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.*
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Implementation of local data store using Room ORM. Room abstracts persistence between a local db
 * and Java objects using a mix of inferred mappings based on Java field names and types, and custom
 * annotations. Mappings are defined through the various Entity objects in the package and related
 * embedded classes.
 */
@Singleton
class RoomLocalDataStore @Inject internal constructor() : LocalDataStore {
  @Inject lateinit var optionDao: OptionDao

  @Inject lateinit var multipleChoiceDao: MultipleChoiceDao

  @Inject lateinit var taskDao: TaskDao

  @Inject lateinit var jobDao: JobDao

  @Inject lateinit var surveyDao: SurveyDao

  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao

  @Inject lateinit var locationOfInterestMutationDao: LocationOfInterestMutationDao

  @Inject lateinit var submissionDao: SubmissionDao

  @Inject lateinit var submissionMutationDao: SubmissionMutationDao

  @Inject lateinit var tileSetDao: TileSetDao

  @Inject lateinit var userDao: UserDao

  @Inject lateinit var offlineAreaDao: OfflineAreaDao

  @Inject lateinit var baseMapDao: BaseMapDao

  @Inject lateinit var schedulers: Schedulers

  @Inject lateinit var fileUtil: FileUtil

  private fun insertOrUpdateOption(taskId: String, option: Option): Completable =
    optionDao.insertOrUpdate(option.toLocalDataStoreObject(taskId)).subscribeOn(schedulers.io())

  private fun insertOrUpdateOptions(
    taskId: String,
    options: kotlinx.collections.immutable.ImmutableList<Option>
  ): Completable =
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

  private fun insertOrUpdateTasks(jobId: String, tasks: ImmutableCollection<Task>): Completable =
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

  @Transaction
  override fun insertOrUpdateSurvey(survey: Survey): Completable =
    surveyDao
      .insertOrUpdate(survey.toLocalDataStoreObject())
      .andThen(jobDao.deleteBySurveyId(survey.id))
      .andThen(insertOrUpdateJobs(survey.id, survey.jobs))
      .andThen(baseMapDao.deleteBySurveyId(survey.id))
      .andThen(insertOfflineBaseMapSources(survey))
      .subscribeOn(schedulers.io())

  override fun insertOrUpdateUser(user: User): Completable =
    userDao.insertOrUpdate(user.toLocalDataStoreObject()).subscribeOn(schedulers.io())

  override fun getUser(id: String): Single<User> =
    userDao
      .findById(id)
      .doOnError {
        Timber.e(it, "Error loading user from local db: $id")
      } // Fail with NoSuchElementException if not found.
      .toSingle()
      .map { it.toModelObject() }
      .subscribeOn(schedulers.io())

  override val surveys: Single<ImmutableList<Survey>>
    get() =
      surveyDao
        .getAllSurveys()
        .map { list: List<SurveyEntityAndRelations> ->
          list.map { it.toModelObject() }.toImmutableList()
        }
        .subscribeOn(schedulers.io())

  override fun getSurveyById(id: String): Maybe<Survey> =
    surveyDao.getSurveyById(id).map { it.toModelObject() }.subscribeOn(schedulers.io())

  override fun deleteSurvey(survey: Survey): Completable =
    surveyDao.delete(survey.toLocalDataStoreObject()).subscribeOn(schedulers.io())

  @Transaction
  override fun applyAndEnqueue(mutation: LocationOfInterestMutation): Completable {
    return try {
      apply(mutation).andThen(enqueue(mutation))
    } catch (e: LocalDataStoreException) {
      FirebaseCrashlytics.getInstance()
        .log(
          "Error enqueueing ${mutation.type} mutation for location of interest ${mutation.locationOfInterestId}"
        )
      FirebaseCrashlytics.getInstance().recordException(e)
      Completable.error(e)
    }
  }

  override fun getLocationsOfInterestOnceAndStream(
    survey: Survey
  ): Flowable<ImmutableSet<LocationOfInterest>> =
    locationOfInterestDao
      .findOnceAndStream(survey.id, EntityState.DEFAULT)
      .map { toLocationsOfInterest(survey, it) }
      .subscribeOn(schedulers.io())

  private fun toLocationsOfInterest(
    survey: Survey,
    locationOfInterestEntities: List<LocationOfInterestEntity>
  ): ImmutableSet<LocationOfInterest> =
    locationOfInterestEntities
      .flatMap { logErrorsAndSkipKt { it.toModelObject(survey) } }
      .toImmutableSet()

  override fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String
  ): Maybe<LocationOfInterest> =
    locationOfInterestDao
      .findById(locationOfInterestId)
      .map { it.toModelObject(survey) }
      .doOnError { Timber.e(it) }
      .onErrorComplete()
      .subscribeOn(schedulers.io())

  override fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String
  ): Maybe<Submission> =
    submissionDao
      .findById(submissionId)
      .map { it.toModelObject(locationOfInterest) }
      .doOnError { Timber.d(it) }
      .onErrorComplete()
      .subscribeOn(schedulers.io())

  override fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String
  ): Single<ImmutableList<Submission>> =
    submissionDao
      .findByLocationOfInterestId(locationOfInterest.id, jobId, EntityState.DEFAULT)
      .map { toSubmissions(locationOfInterest, it) }
      .subscribeOn(schedulers.io())

  private fun toSubmissions(
    locationOfInterest: LocationOfInterest,
    submissionEntities: List<SubmissionEntity>
  ): ImmutableList<Submission> =
    submissionEntities
      .flatMap { logErrorsAndSkipKt { it.toModelObject(locationOfInterest) } }
      .toImmutableList()

  override val tileSetsOnceAndStream: Flowable<ImmutableSet<TileSet>>
    get() =
      tileSetDao
        .findAllOnceAndStream()
        .map { list: List<TileSetEntity> -> list.map { it.toModelObject() }.toImmutableSet() }
        .subscribeOn(schedulers.io())

  override fun getMutationsOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableList<Mutation>> {
    // TODO: Show mutations for all surveys, not just current one.
    val locationOfInterestMutations =
      locationOfInterestMutationDao
        .loadAllOnceAndStream()
        .map { list: List<LocationOfInterestMutationEntity> ->
          list.filter { it.surveyId == survey.id }.map { it.toModelObject() }.toImmutableList()
        }
        .subscribeOn(schedulers.io())
    val submissionMutations =
      submissionMutationDao
        .loadAllOnceAndStream()
        .map { list: List<SubmissionMutationEntity> ->
          list
            .filter { it.surveyId == survey.id }
            .map { it.toModelObject(survey) }
            .toImmutableList()
        }
        .subscribeOn(schedulers.io())
    return Flowable.combineLatest(
      locationOfInterestMutations,
      submissionMutations,
      this::combineAndSortMutations
    )
  }

  private fun combineAndSortMutations(
    locationOfInterestMutations: ImmutableList<LocationOfInterestMutation>,
    submissionMutations: ImmutableList<SubmissionMutation>
  ): ImmutableList<Mutation> =
    ImmutableList.sortedCopyOf(
      byDescendingClientTimestamp(),
      ImmutableList.builder<Mutation>()
        .addAll(locationOfInterestMutations)
        .addAll(submissionMutations)
        .build()
    )

  override fun getPendingMutations(locationOfInterestId: String): Single<ImmutableList<Mutation>> =
    locationOfInterestMutationDao
      .findByLocationOfInterestId(locationOfInterestId, MutationEntitySyncStatus.PENDING)
      .flattenAsObservable { it }
      .map { it.toModelObject() }
      .cast(Mutation::class.java)
      .mergeWith(
        submissionMutationDao
          .findByLocationOfInterestId(locationOfInterestId, MutationEntitySyncStatus.PENDING)
          .flattenAsObservable { it }
          .flatMap { ome ->
            getSurveyById(ome.surveyId)
              .toSingle()
              .map { ome.toModelObject(it) }
              .toObservable()
              .doOnError { Timber.e(it, "Submission mutation skipped") }
              .onErrorResumeNext(Observable.empty())
          }
          .cast(Mutation::class.java)
      )
      .toList()
      .map { it.toImmutableList() }
      .subscribeOn(schedulers.io())

  @Transaction
  override fun updateMutations(mutations: ImmutableList<Mutation>): Completable =
    locationOfInterestMutationDao
      .updateAll(toLocationOfInterestMutationEntities(mutations))
      .andThen(
        submissionMutationDao
          .updateAll(toSubmissionMutationEntities(mutations))
          .subscribeOn(schedulers.io())
      )
      .subscribeOn(schedulers.io())

  private fun toSubmissionMutationEntities(
    mutations: ImmutableList<Mutation>
  ): ImmutableList<SubmissionMutationEntity> =
    SubmissionMutation.filter(mutations).map { it.toLocalDataStoreObject() }.toImmutableList()

  private fun toLocationOfInterestMutationEntities(
    mutations: ImmutableList<Mutation>
  ): ImmutableList<LocationOfInterestMutationEntity> =
    LocationOfInterestMutation.filter(mutations)
      .map { it.toLocalDataStoreObject() }
      .toImmutableList()

  override fun finalizePendingMutations(mutations: ImmutableList<Mutation>): Completable =
    finalizeDeletions(mutations).andThen(markComplete(mutations))

  private fun finalizeDeletions(mutations: ImmutableList<Mutation>): Completable =
    Observable.fromIterable(mutations)
      .filter { it.type === DELETE }
      .flatMapCompletable { mutation ->
        when (mutation) {
          is SubmissionMutation -> {
            deleteSubmission(mutation.submissionId)
          }
          is LocationOfInterestMutation -> {
            deleteLocationOfInterest(mutation.locationOfInterestId)
          }
        }
      }

  private fun markComplete(mutations: ImmutableList<Mutation>): Completable {
    val locationOfInterestMutations =
      LocationOfInterestMutation.filter(mutations)
        .map { it.copy(syncStatus = SyncStatus.COMPLETED) }
        .map { it.toLocalDataStoreObject() }
    val submissionMutations =
      SubmissionMutation.filter(mutations)
        .map { it.copy(syncStatus = SyncStatus.COMPLETED) }
        .map { it.toLocalDataStoreObject() }
        .toImmutableList()
    return locationOfInterestMutationDao
      .updateAll(locationOfInterestMutations)
      .andThen(submissionMutationDao.updateAll(submissionMutations).subscribeOn(schedulers.io()))
      .subscribeOn(schedulers.io())
  }

  @Transaction
  override fun mergeLocationOfInterest(locationOfInterest: LocationOfInterest): Completable =
    // TODO(#706): Apply pending local mutations before saving.
    locationOfInterestDao
      .insertOrUpdate(locationOfInterest.toLocalDataStoreObject())
      .subscribeOn(schedulers.io())

  @Transaction
  override fun mergeSubmission(submission: Submission): Completable {
    val submissionEntity = submission.toLocalDataStoreObject()
    return submissionMutationDao
      .findBySubmissionId(
        submission.id,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS
      )
      .flatMapCompletable { mergeSubmission(submission.job, submissionEntity, it) }
      .subscribeOn(schedulers.io())
  }

  private fun mergeSubmission(
    job: Job,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>
  ): Completable {
    if (mutations.isEmpty()) {
      return submissionDao.insertOrUpdate(submission)
    }
    val lastMutation = mutations[mutations.size - 1]
    Preconditions.checkNotNull(lastMutation, "Could not get last mutation")
    return getUser(lastMutation.userId)
      .map { user -> applyMutations(job, submission, mutations, user) }
      .flatMapCompletable { submissionDao.insertOrUpdate(it) }
  }

  private fun applyMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>,
    user: User
  ): SubmissionEntity {
    val lastMutation = mutations[mutations.size - 1]
    val clientTimestamp = lastMutation.clientTimestamp
    Timber.v("Merging submission $this with mutations $mutations")

    return submission.apply {
      responses = toString(applyMutations(job, this, mutations))
      lastModified = AuditInfoEntity(UserDetails.fromUser(user), clientTimestamp)
      Timber.v("Merged submission $this")
    }
  }

  private fun applyMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>
  ): TaskDataMap {
    val responseMap = ResponseMapConverter.fromString(job!!, submission.responses)
    val deltas = ImmutableList.builder<TaskDataDelta>()
    for (mutation in mutations) {
      // Merge changes to responses.
      deltas.addAll(ResponseDeltasConverter.fromString(job, mutation.responseDeltas))
    }
    return responseMap.copyWithDeltas(deltas.build())
  }

  @Throws(LocalDataStoreException::class)
  private fun apply(mutation: LocationOfInterestMutation): Completable {
    return when (mutation.type) {
      CREATE,
      UPDATE ->
        getUser(mutation.userId).flatMapCompletable { user ->
          insertOrUpdateLocationOfInterest(mutation, user)
        }
      DELETE ->
        locationOfInterestDao
          .findById(mutation.locationOfInterestId)
          .flatMapCompletable { entity -> markLocationOfInterestForDeletion(entity, mutation) }
          .subscribeOn(schedulers.io())
      UNKNOWN -> throw LocalDataStoreException("Unknown Mutation.Type")
    }
  }

  private fun markLocationOfInterestForDeletion(
    entity: LocationOfInterestEntity,
    mutation: LocationOfInterestMutation
  ): Completable =
    locationOfInterestDao
      .update(entity.apply { state = EntityState.DELETED })
      .doOnSubscribe { Timber.d("Marking location of interest as deleted : $mutation") }
      .ignoreElement()

  private fun insertOrUpdateLocationOfInterest(
    mutation: LocationOfInterestMutation,
    user: User
  ): Completable =
    locationOfInterestDao
      .insertOrUpdate(mutation.toLocalDataStoreObject(AuditInfo(user)))
      .subscribeOn(schedulers.io())

  override fun deleteLocationOfInterest(locationOfInterestId: String): Completable =
    locationOfInterestDao
      .findById(locationOfInterestId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting local location of interest : $locationOfInterestId") }
      .flatMapCompletable { locationOfInterestDao.delete(it) }
      .subscribeOn(schedulers.io())

  private fun enqueue(mutation: LocationOfInterestMutation): Completable =
    locationOfInterestMutationDao
      .insert(mutation.toLocalDataStoreObject())
      .subscribeOn(schedulers.io())

  @Transaction
  override fun applyAndEnqueue(mutation: SubmissionMutation): Completable =
    try {
      apply(mutation).andThen(enqueue(mutation))
    } catch (e: LocalDataStoreException) {
      FirebaseCrashlytics.getInstance()
        .log("Error enqueueing ${mutation.type} mutation for submission ${mutation.submissionId}")
      FirebaseCrashlytics.getInstance().recordException(e)
      Completable.error(e)
    }

  /**
   * Applies mutation to submission in database or creates a new one.
   *
   * @return A Completable that emits an error if mutation type is "UPDATE" but entity does not
   * exist, or if type is "CREATE" and entity already exists.
   */
  @Throws(LocalDataStoreException::class)
  override fun apply(mutation: SubmissionMutation): Completable {
    return when (mutation.type) {
      CREATE ->
        getUser(mutation.userId).flatMapCompletable { user -> createSubmission(mutation, user) }
      UPDATE ->
        getUser(mutation.userId).flatMapCompletable { user -> updateSubmission(mutation, user) }
      DELETE ->
        submissionDao.findById(mutation.submissionId).flatMapCompletable { entity ->
          markSubmissionForDeletion(entity, mutation)
        }
      UNKNOWN -> throw LocalDataStoreException("Unknown Mutation.Type")
    }
  }

  private fun createSubmission(mutation: SubmissionMutation, user: User): Completable =
    submissionDao
      .insert(mutation.toLocalDataStoreObject(AuditInfo(user)))
      .doOnSubscribe { Timber.v("Inserting submission: $mutation") }
      .subscribeOn(schedulers.io())

  private fun updateSubmission(mutation: SubmissionMutation, user: User): Completable {
    val mutationEntity = mutation.toLocalDataStoreObject()
    return submissionDao
      .findById(mutation.submissionId)
      .doOnSubscribe { Timber.v("Applying mutation: $mutation") }
      .switchIfEmpty(fallbackSubmission(mutation))
      .map { applyMutations(mutation.job, it, ImmutableList.of(mutationEntity), user) }
      .flatMapCompletable { submissionDao.insertOrUpdate(it).subscribeOn(schedulers.io()) }
      .subscribeOn(schedulers.io())
  }

  /**
   * Returns a source which creates an submission based on the provided mutation. Used in rare cases
   * when the submission is no longer in the local db, but the user is updating rather than creating
   * a new submission. In these cases creation metadata is unknown, so empty audit info is used.
   */
  private fun fallbackSubmission(mutation: SubmissionMutation): SingleSource<SubmissionEntity> =
    SingleSource {
      it.onSuccess(mutation.toLocalDataStoreObject(AuditInfo(User("", "", ""))))
    }

  private fun markSubmissionForDeletion(
    entity: SubmissionEntity,
    mutation: SubmissionMutation
  ): Completable =
    submissionDao
      .update(entity.apply { state = EntityState.DELETED })
      .doOnSubscribe { Timber.d("Marking submission as deleted : $mutation") }
      .ignoreElement()
      .subscribeOn(schedulers.io())

  override fun deleteSubmission(submissionId: String): Completable =
    submissionDao
      .findById(submissionId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting local submission : $submissionId") }
      .flatMapCompletable { submissionDao.delete(it) }
      .subscribeOn(schedulers.io())

  private fun enqueue(mutation: SubmissionMutation): Completable =
    submissionMutationDao
      .insert(mutation.toLocalDataStoreObject())
      .doOnSubscribe { Timber.v("Enqueuing mutation: $mutation") }
      .subscribeOn(schedulers.io())

  override fun insertOrUpdateTileSet(tileSet: TileSet): Completable =
    tileSetDao.insertOrUpdate(tileSet.toLocalDataStoreObject()).subscribeOn(schedulers.io())

  override fun getTileSet(tileUrl: String): Maybe<TileSet> =
    tileSetDao.findByUrl(tileUrl).map { it.toModelObject() }.subscribeOn(schedulers.io())

  override val pendingTileSets: Single<ImmutableList<TileSet>>
    get() =
      tileSetDao
        .findByState(TileSetEntityState.PENDING.intValue())
        .map { list: List<TileSetEntity> -> list.map { it.toModelObject() }.toImmutableList() }
        .subscribeOn(schedulers.io())

  override fun insertOrUpdateOfflineArea(area: OfflineArea): Completable =
    offlineAreaDao.insertOrUpdate(area.toOfflineAreaEntity()).subscribeOn(schedulers.io())

  override val offlineAreasOnceAndStream: Flowable<ImmutableList<OfflineArea>>
    get() =
      offlineAreaDao
        .findAllOnceAndStream()
        .map { areas: List<OfflineAreaEntity> ->
          areas.map { it.toModelObject() }.toImmutableList()
        }
        .subscribeOn(schedulers.io())

  override fun getOfflineAreaById(id: String): Single<OfflineArea> =
    offlineAreaDao.findById(id).map { it.toModelObject() }.toSingle().subscribeOn(schedulers.io())

  override fun deleteOfflineArea(offlineAreaId: String): Completable =
    offlineAreaDao
      .findById(offlineAreaId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting offline area: $offlineAreaId") }
      .flatMapCompletable { offlineAreaDao.delete(it) }
      .subscribeOn(schedulers.io())

  override fun updateTileSetOfflineAreaReferenceCountByUrl(
    newCount: Int,
    url: String
  ): Completable = Completable.fromSingle(tileSetDao.updateBasemapReferenceCount(newCount, url))

  override fun deleteTileSetByUrl(tileSet: TileSet): Completable =
    if (tileSet.offlineAreaReferenceCount < 1) {
      Completable.fromAction { fileUtil.deleteFile(tileSet.path) }
        .andThen(Completable.fromMaybe(tileSetDao.deleteByUrl(tileSet.url)))
        .subscribeOn(schedulers.io())
    } else {
      Completable.complete().subscribeOn(schedulers.io())
    }

  override fun getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<ImmutableList<LocationOfInterestMutation>> =
    locationOfInterestMutationDao
      .findByLocationOfInterestIdOnceAndStream(locationOfInterestId, *allowedStates)
      .map { list: List<LocationOfInterestMutationEntity> ->
        list.map { it.toModelObject() }.toImmutableList()
      }

  override fun getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<ImmutableList<SubmissionMutation>> =
    submissionMutationDao
      .findByLocationOfInterestIdOnceAndStream(locationOfInterestId, *allowedStates)
      .map { list: List<SubmissionMutationEntity> ->
        list.map { it.toModelObject(survey) }.toImmutableList()
      }
}
