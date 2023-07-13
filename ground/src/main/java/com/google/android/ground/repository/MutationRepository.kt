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

package com.google.android.ground.repository

import com.google.android.ground.model.Survey
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.rx2.rxCompletable

/**
 * Coordinates persistence of mutations across [LocationOfInterestMutation] and [SubmissionMutation]
 * local data stores.
 */
@Singleton
class MutationRepository
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val localLocationOfInterestStore: LocalLocationOfInterestStore,
  private val localSubmissionStore: LocalSubmissionStore,
  private val schedulers: Schedulers
) {
  /**
   * Returns a long-lived stream that emits the full list of mutations for specified survey on
   * subscribe and a new list on each subsequent change.
   */
  fun getMutationsOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<List<Mutation>> {
    // TODO: Show mutations for all surveys, not just current one.
    val locationOfInterestMutations =
      localLocationOfInterestStore
        .getAllMutationsAndStream()
        .map { it.filter { it.surveyId == survey.id }.map { it.toModelObject() } }
        .subscribeOn(schedulers.io())
    val submissionMutations =
      localSubmissionStore
        .getAllMutationsAndStream()
        .map { list: List<SubmissionMutationEntity> ->
          list.filter { it.surveyId == survey.id }.map { it.toModelObject(survey) }
        }
        .subscribeOn(schedulers.io())
    return Flowable.combineLatest(
      locationOfInterestMutations,
      submissionMutations,
      this::combineAndSortMutations
    )
  }

  /**
   * Returns all LOI and submission mutations in the local mutation queue relating to LOI with the
   * specified id.
   */
  suspend fun getPendingMutations(locationOfInterestId: String): List<Mutation> {
    val pendingLoiMutations =
      localLocationOfInterestStore
        .findByLocationOfInterestId(locationOfInterestId, MutationEntitySyncStatus.PENDING)
        .map { it.toModelObject() }
    val pendingSubmissionMutations =
      localSubmissionStore
        .findByLocationOfInterestId(locationOfInterestId, MutationEntitySyncStatus.PENDING)
        .mapNotNull { entity ->
          localSurveyStore.getSurveyByIdSuspend(entity.surveyId)?.let { entity.toModelObject(it) }
        }
    return pendingLoiMutations + pendingSubmissionMutations
  }

  /** Updates the provided list of mutations. */
  suspend fun updateMutations(mutations: List<Mutation>) {
    val loiMutations = mutations.filterIsInstance<LocationOfInterestMutation>()
    localLocationOfInterestStore.updateAllSuspend(loiMutations)

    val submissionMutations = mutations.filterIsInstance<SubmissionMutation>()
    localSubmissionStore.updateAllSuspend(submissionMutations)
  }

  /**
   * Mark pending mutations as complete. If the mutation is of type DELETE, also removes the
   * corresponding submission or LOI.
   */
  fun finalizePendingMutations(mutations: List<Mutation>): Completable =
    finalizeDeletions(mutations).andThen(markComplete(mutations))

  private fun finalizeDeletions(mutations: List<Mutation>): Completable =
    Observable.fromIterable(mutations)
      .filter { it.type === Mutation.Type.DELETE }
      .flatMapCompletable { mutation ->
        when (mutation) {
          is SubmissionMutation -> {
            rxCompletable { localSubmissionStore.deleteSubmission(mutation.submissionId) }
          }
          is LocationOfInterestMutation -> {
            rxCompletable {
              localLocationOfInterestStore.deleteLocationOfInterest(mutation.locationOfInterestId)
            }
          }
        }
      }

  private fun markComplete(mutations: List<Mutation>): Completable {
    val locationOfInterestMutations =
      LocationOfInterestMutation.filter(mutations).map {
        it.copy(syncStatus = Mutation.SyncStatus.COMPLETED)
      }
    val submissionMutations =
      SubmissionMutation.filter(mutations).map {
        it.copy(syncStatus = Mutation.SyncStatus.COMPLETED)
      }

    return rxCompletable {
      localLocationOfInterestStore.updateAllSuspend(locationOfInterestMutations)
      localSubmissionStore.updateAllSuspend(submissionMutations)
    }
  }

  private fun combineAndSortMutations(
    locationOfInterestMutations: List<LocationOfInterestMutation>,
    submissionMutations: List<SubmissionMutation>
  ): List<Mutation> =
    (locationOfInterestMutations + submissionMutations).sortedWith(
      Mutation.byDescendingClientTimestamp()
    )
}
