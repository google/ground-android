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

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestDao
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestMutationDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestMutationEntity
import com.google.android.ground.persistence.local.room.fields.EntityState
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestMutationStore
import com.google.android.ground.rx.Schedulers
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Manages access to [LocationOfInterest] objects persisted in local storage. */
@Singleton
class RoomLocationOfInterestMutationStore @Inject internal constructor() :
  LocalLocationOfInterestMutationStore {
  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao
  @Inject lateinit var locationOfInterestMutationDao: LocationOfInterestMutationDao
  @Inject lateinit var userStore: RoomUserStore
  @Inject lateinit var schedulers: Schedulers

  /**
   * Retrieves the complete set of [LocationOfInterest] associated with the given [Survey] from the
   * local database and returns a [Flowable] that continually emits the complete set anew any time
   * the underlying table changes (insertions, deletions, updates).
   */
  override fun getLocationsOfInterestOnceAndStream(
    survey: Survey
  ): Flowable<Set<LocationOfInterest>> =
    locationOfInterestDao
      .findOnceAndStream(survey.id, EntityState.DEFAULT)
      .map { toLocationsOfInterest(survey, it) }
      .subscribeOn(schedulers.io())

  /**
   * Attempts to retrieve the [LocationOfInterest] with the given ID that's associated with the
   * given [Survey]. Returns a [Maybe] that completes immediately (with no data) if the location of
   * interest isn't found and that succeeds with the location of interest otherwise (and then
   * completes). Does not stream subsequent data changes.
   */
  override fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String
  ): Maybe<LocationOfInterest> =
    locationOfInterestDao
      .findById(locationOfInterestId)
      .map { it.toModelObject(survey) }
      .subscribeOn(schedulers.io())

  fun delete(locationOfInterestId: String): Completable =
    locationOfInterestDao
      .findById(locationOfInterestId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting local location of interest : $locationOfInterestId") }
      .flatMapCompletable { locationOfInterestDao.delete(it) }
      .subscribeOn(schedulers.io())

  // TODO(#706): Apply pending local mutations before saving.
  override fun merge(model: LocationOfInterest): Completable =
    locationOfInterestDao
      .insertOrUpdate(model.toLocalDataStoreObject())
      .subscribeOn(schedulers.io())

  override fun enqueue(mutation: LocationOfInterestMutation): Completable =
    locationOfInterestMutationDao
      .insert(mutation.toLocalDataStoreObject())
      .subscribeOn(schedulers.io())

  override fun apply(mutation: LocationOfInterestMutation): Completable =
    when (mutation.type) {
      Mutation.Type.CREATE,
      Mutation.Type.UPDATE ->
        userStore.getUser(mutation.userId).flatMapCompletable { user ->
          insertOrUpdateLocationOfInterestFromMutation(mutation, user)
        }
      Mutation.Type.DELETE ->
        locationOfInterestDao
          .findById(mutation.locationOfInterestId)
          .flatMapCompletable { entity -> markLocationOfInterestForDeletion(entity, mutation) }
          .subscribeOn(schedulers.io())
      Mutation.Type.UNKNOWN -> throw LocalDataStoreException("Unknown Mutation.Type")
    }

  override fun applyAndEnqueue(mutation: LocationOfInterestMutation): Completable =
    try {
      apply(mutation).andThen(enqueue(mutation))
    } catch (e: LocalDataStoreException) {
      FirebaseCrashlytics.getInstance()
        .log(
          "Error enqueueing ${mutation.type} mutation for location of interest ${mutation.locationOfInterestId}"
        )
      FirebaseCrashlytics.getInstance().recordException(e)
      Completable.error(e)
    }

  override fun updateAll(mutations: List<LocationOfInterestMutation>): Completable =
    locationOfInterestMutationDao.updateAll(toLocationOfInterestMutationEntities(mutations))

  private fun toLocationsOfInterest(
    survey: Survey,
    locationOfInterestEntities: List<LocationOfInterestEntity>
  ): Set<LocationOfInterest> =
    locationOfInterestEntities.flatMap { logAndSkip { it.toModelObject(survey) } }.toSet()

  private fun toLocationOfInterestMutationEntities(
    mutations: List<LocationOfInterestMutation>
  ): List<LocationOfInterestMutationEntity> =
    LocationOfInterestMutation.filter(mutations).map { it.toLocalDataStoreObject() }

  private fun insertOrUpdateLocationOfInterestFromMutation(
    mutation: LocationOfInterestMutation,
    user: User
  ): Completable =
    locationOfInterestDao
      .insertOrUpdate(mutation.toLocalDataStoreObject(AuditInfo(user)))
      .subscribeOn(schedulers.io())

  private fun markLocationOfInterestForDeletion(
    entity: LocationOfInterestEntity,
    mutation: LocationOfInterestMutation
  ): Completable =
    locationOfInterestDao
      .update(entity.apply { state = EntityState.DELETED })
      .doOnSubscribe { Timber.d("Marking location of interest as deleted : $mutation") }
      .ignoreElement()

  override fun deleteLocationOfInterest(locationOfInterestId: String): Completable =
    locationOfInterestDao
      .findById(locationOfInterestId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting local location of interest : $locationOfInterestId") }
      .flatMapCompletable { locationOfInterestDao.delete(it) }
      .subscribeOn(schedulers.io())

  override fun getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<List<LocationOfInterestMutation>> =
    locationOfInterestMutationDao
      .findByLocationOfInterestIdOnceAndStream(locationOfInterestId, *allowedStates)
      .map { list: List<LocationOfInterestMutationEntity> -> list.map { it.toModelObject() } }

  override fun getAllMutationsAndStream(): Flowable<List<LocationOfInterestMutationEntity>> =
    locationOfInterestMutationDao.loadAllOnceAndStream()

  override fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus
  ): Single<List<LocationOfInterestMutationEntity>> =
    locationOfInterestMutationDao.findByLocationOfInterestId(id, *states)
}
