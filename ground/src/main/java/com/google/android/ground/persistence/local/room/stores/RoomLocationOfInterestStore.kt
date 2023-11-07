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
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.util.Debug.logOnFailure
import io.reactivex.Flowable
import io.reactivex.Maybe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** Manages access to [LocationOfInterest] objects persisted in local storage. */
@Singleton
class RoomLocationOfInterestStore @Inject internal constructor() : LocalLocationOfInterestStore {
  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao
  @Inject lateinit var locationOfInterestMutationDao: LocationOfInterestMutationDao
  @Inject lateinit var userStore: RoomUserStore
  @Inject lateinit var schedulers: Schedulers

  /**
   * Retrieves the complete set of [LocationOfInterest] associated with the given [Survey] from the
   * local database and returns a [Flowable] that continually emits the complete set anew any time
   * the underlying table changes (insertions, deletions, updates).
   */
  override fun findLocationsOfInterest(survey: Survey) =
    locationOfInterestDao.findByState(survey.id, EntityState.DEFAULT).map {
      toLocationsOfInterest(survey, it)
    }

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

  // TODO(#706): Apply pending local mutations before saving.
  override suspend fun merge(model: LocationOfInterest) {
    locationOfInterestDao.insertOrUpdate(model.toLocalDataStoreObject())
  }

  override suspend fun enqueue(mutation: LocationOfInterestMutation) =
    locationOfInterestMutationDao.insert(mutation.toLocalDataStoreObject())

  override suspend fun apply(mutation: LocationOfInterestMutation) {
    when (mutation.type) {
      Mutation.Type.CREATE,
      Mutation.Type.UPDATE -> {
        val user = userStore.getUser(mutation.userId)
        val entity = mutation.toLocalDataStoreObject(user)
        locationOfInterestDao.insertOrUpdate(entity)
      }
      Mutation.Type.DELETE -> {
        val loiId = mutation.locationOfInterestId
        val entity = checkNotNull(locationOfInterestDao.findByIdSuspend(loiId))
        locationOfInterestDao.update(entity.copy(state = EntityState.DELETED))
      }
      Mutation.Type.UNKNOWN -> {
        throw LocalDataStoreException("Unknown Mutation.Type")
      }
    }
  }

  override suspend fun applyAndEnqueue(mutation: LocationOfInterestMutation) {
    apply(mutation)
    enqueue(mutation)
  }

  override suspend fun updateAll(mutations: List<LocationOfInterestMutation>) {
    locationOfInterestMutationDao.updateAll(toLocationOfInterestMutationEntities(mutations))
  }

  private fun toLocationsOfInterest(
    survey: Survey,
    locationOfInterestEntities: List<LocationOfInterestEntity>
  ): Set<LocationOfInterest> =
    locationOfInterestEntities.mapNotNull { logOnFailure { it.toModelObject(survey) } }.toSet()

  private fun toLocationOfInterestMutationEntities(
    mutations: List<LocationOfInterestMutation>
  ): List<LocationOfInterestMutationEntity> = mutations.map { it.toLocalDataStoreObject() }

  override suspend fun deleteLocationOfInterest(locationOfInterestId: String) {
    Timber.d("Deleting local location of interest : $locationOfInterestId")
    locationOfInterestDao.findByIdSuspend(locationOfInterestId)?.let {
      locationOfInterestDao.delete(it)
    }
  }

  override fun getMutationsFlow(
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flow<List<LocationOfInterestMutation>> =
    locationOfInterestMutationDao.getMutationsFlow(locationOfInterestId, *allowedStates).map {
      mutations ->
      mutations.map { it.toModelObject() }
    }

  override fun getAllSurveyMutations(survey: Survey): Flow<List<LocationOfInterestMutation>> =
    locationOfInterestMutationDao.getAllMutationsFlow().map { mutations ->
      mutations.filter { it.surveyId == survey.id }.map { it.toModelObject() }
    }

  override suspend fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus
  ): List<LocationOfInterestMutationEntity> =
    locationOfInterestMutationDao.getMutations(id, *states) ?: listOf()

  override suspend fun insertOrUpdate(loi: LocationOfInterest) =
    locationOfInterestDao.insertOrUpdate(loi.toLocalDataStoreObject())

  override suspend fun deleteNotIn(surveyId: String, ids: List<String>) =
    locationOfInterestDao.deleteNotIn(surveyId, ids)
}
