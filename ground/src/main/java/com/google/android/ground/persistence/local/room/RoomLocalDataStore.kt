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

import com.google.android.ground.model.Survey
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.Companion.byDescendingClientTimestamp
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.Mutation.Type.DELETE
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.stores.*
import com.google.android.ground.persistence.local.stores.*
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of local data store using Room ORM. Room abstracts persistence between a local db
 * and Java objects using a mix of inferred mappings based on Java field names and types, and custom
 * annotations. Mappings are defined through the various Entity objects in the package and related
 * embedded classes.
 */
@Singleton
class RoomLocalDataStore @Inject internal constructor() : LocalDataStore {
  @Inject lateinit var schedulers: Schedulers
  @Inject override lateinit var locationOfInterestStore: LocationOfInterestStore
  @Inject override lateinit var offlineAreaStore: OfflineAreaStore
  @Inject override lateinit var submissionStore: SubmissionStore
  @Inject override lateinit var surveyStore: SurveyStore
  @Inject override lateinit var tileSetStore: TileSetStore
  @Inject override lateinit var userStore: UserStore

  override fun getMutationsOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableList<Mutation>> {
    // TODO: Show mutations for all surveys, not just current one.
    val locationOfInterestMutations =
      locationOfInterestStore
        .getAllMutationsAndStream()
        .map { it.filter { it.surveyId == survey.id }.map { it.toModelObject() }.toImmutableList() }
        .subscribeOn(schedulers.io())
    val submissionMutations =
      submissionStore
        .getAllMutationsAndStream()
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

  override fun getPendingMutations(locationOfInterestId: String): Single<ImmutableList<Mutation>> =
    locationOfInterestStore
      .findByLocationOfInterestId(locationOfInterestId, MutationEntitySyncStatus.PENDING)
      .flattenAsObservable { it }
      .map { it.toModelObject() }
      .cast(Mutation::class.java)
      .mergeWith(
        submissionStore
          .findByLocationOfInterestId(locationOfInterestId, MutationEntitySyncStatus.PENDING)
          .flattenAsObservable { it }
          .flatMap { ome ->
            surveyStore
              .getSurveyById(ome.surveyId)
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

  override fun updateMutations(mutations: ImmutableList<Mutation>): Completable {
    val loiMutations = mutations.filterIsInstance<LocationOfInterestMutation>().toImmutableList()
    val submissionMutations = mutations.filterIsInstance<SubmissionMutation>().toImmutableList()

    return locationOfInterestStore
      .updateAll(loiMutations)
      .andThen(submissionStore.updateAll(submissionMutations))
      .subscribeOn(schedulers.io())
  }

  override fun finalizePendingMutations(mutations: ImmutableList<Mutation>): Completable =
    finalizeDeletions(mutations).andThen(markComplete(mutations))

  private fun finalizeDeletions(mutations: ImmutableList<Mutation>): Completable =
    Observable.fromIterable(mutations)
      .filter { it.type === DELETE }
      .flatMapCompletable { mutation ->
        when (mutation) {
          is SubmissionMutation -> {
            submissionStore.deleteSubmission(mutation.submissionId)
          }
          is LocationOfInterestMutation -> {
            locationOfInterestStore.deleteLocationOfInterest(mutation.locationOfInterestId)
          }
        }
      }

  private fun markComplete(mutations: ImmutableList<Mutation>): Completable {
    val locationOfInterestMutations =
      LocationOfInterestMutation.filter(mutations)
        .map { it.copy(syncStatus = SyncStatus.COMPLETED) }
        .toImmutableList()
    val submissionMutations =
      SubmissionMutation.filter(mutations)
        .map { it.copy(syncStatus = SyncStatus.COMPLETED) }
        .toImmutableList()

    return locationOfInterestStore
      .updateAll(locationOfInterestMutations)
      .andThen(submissionStore.updateAll(submissionMutations).subscribeOn(schedulers.io()))
      .subscribeOn(schedulers.io())
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
}
