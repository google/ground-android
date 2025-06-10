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
package org.groundplatform.android.persistence.local.room.stores

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.persistence.local.room.LocalDataStoreException
import org.groundplatform.android.persistence.local.room.converter.toLocalDataStoreObject
import org.groundplatform.android.persistence.local.room.converter.toModelObject
import org.groundplatform.android.persistence.local.room.dao.LocationOfInterestDao
import org.groundplatform.android.persistence.local.room.dao.LocationOfInterestMutationDao
import org.groundplatform.android.persistence.local.room.dao.insertOrUpdate
import org.groundplatform.android.persistence.local.room.entity.LocationOfInterestEntity
import org.groundplatform.android.persistence.local.room.entity.LocationOfInterestMutationEntity
import org.groundplatform.android.persistence.local.room.fields.EntityDeletionState
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.util.Debug.logOnFailure
import timber.log.Timber

/** Manages access to [LocationOfInterest] objects persisted in local storage. */
@Singleton
class RoomLocationOfInterestStore @Inject internal constructor() : LocalLocationOfInterestStore {
  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao
  @Inject lateinit var locationOfInterestMutationDao: LocationOfInterestMutationDao
  @Inject lateinit var userStore: RoomUserStore

  override suspend fun getLoiCount(surveyId: String): Int =
    locationOfInterestDao.countByDeletionState(surveyId, EntityDeletionState.DEFAULT)

  override fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>> =
    locationOfInterestDao.getByDeletionState(survey.id, EntityDeletionState.DEFAULT).map {
      toLocationsOfInterest(survey, it)
    }

  override suspend fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String,
  ): LocationOfInterest? =
    locationOfInterestDao.findById(locationOfInterestId)?.toModelObject(survey)

  // TODO: Apply pending local mutations before saving.
  // Issue URL: https://github.com/google/ground-android/issues/706
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
        val entity = checkNotNull(locationOfInterestDao.findById(loiId))
        locationOfInterestDao.update(entity.copy(deletionState = EntityDeletionState.DELETED))
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
    locationOfInterestEntities: List<LocationOfInterestEntity>,
  ): Set<LocationOfInterest> =
    locationOfInterestEntities.mapNotNull { logOnFailure { it.toModelObject(survey) } }.toSet()

  private fun toLocationOfInterestMutationEntities(
    mutations: List<LocationOfInterestMutation>
  ): List<LocationOfInterestMutationEntity> = mutations.map { it.toLocalDataStoreObject() }

  override suspend fun deleteLocationOfInterest(locationOfInterestId: String) {
    Timber.d("Deleting local location of interest : $locationOfInterestId")
    locationOfInterestDao.findById(locationOfInterestId)?.let { locationOfInterestDao.delete(it) }
  }

  override fun getAllSurveyMutations(survey: Survey): Flow<List<LocationOfInterestMutation>> =
    locationOfInterestMutationDao.getAllMutationsFlow().map { mutations ->
      mutations.filter { it.surveyId == survey.id }.map { it.toModelObject() }
    }

  override fun getAllMutationsFlow(): Flow<List<LocationOfInterestMutation>> =
    locationOfInterestMutationDao.getAllMutationsFlow().map { mutations ->
      mutations.map { it.toModelObject() }
    }

  override suspend fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus,
  ): List<LocationOfInterestMutationEntity> =
    locationOfInterestMutationDao.getMutations(id, *states) ?: listOf()

  override suspend fun insertOrUpdate(loi: LocationOfInterest) =
    locationOfInterestDao.insertOrUpdate(loi.toLocalDataStoreObject())

  override suspend fun deleteNotIn(surveyId: String, ids: List<String>) =
    locationOfInterestDao.deleteNotIn(surveyId, ids)
}
