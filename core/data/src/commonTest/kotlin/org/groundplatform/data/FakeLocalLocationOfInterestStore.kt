/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.groundplatform.data.stores.LocalLocationOfInterestStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus

class FakeLocalLocationOfInterestStore : LocalLocationOfInterestStore {
  val appliedMutations = mutableListOf<LocationOfInterestMutation>()
  val loisFlow = MutableStateFlow<Map<String, LocationOfInterest>>(emptyMap())

  override suspend fun getLoiCount(surveyId: String): Int =
    loisFlow.value.values.count { it.surveyId == surveyId }

  override fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>> = loisFlow.map {
    it.values.filter { loi -> loi.surveyId == survey.id }.toSet()
  }

  override suspend fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String,
  ): LocationOfInterest? = loisFlow.value[locationOfInterestId]?.takeIf { it.surveyId == survey.id }

  override suspend fun deleteLocationOfInterest(locationOfInterestId: String) {
    loisFlow.value = loisFlow.value - locationOfInterestId
  }

  override fun getAllSurveyMutations(survey: Survey): Flow<List<LocationOfInterestMutation>> =
    emptyFlow()

  override fun getAllMutationsFlow(): Flow<List<LocationOfInterestMutation>> = emptyFlow()

  override suspend fun findByLocationOfInterestId(
    id: String,
    vararg states: SyncStatus,
  ): List<LocationOfInterestMutation> = emptyList()

  override suspend fun insertOrUpdate(loi: LocationOfInterest) {
    loisFlow.value = loisFlow.value + (loi.id to loi)
  }

  override suspend fun insertOrUpdateAll(lois: List<LocationOfInterest>) {
    loisFlow.value = loisFlow.value + lois.associateBy { it.id }
  }

  override suspend fun deleteNotIn(surveyId: String, ids: List<String>) {
    val idsSet = ids.toSet()
    loisFlow.value =
      loisFlow.value.filterNot { (id, loi) -> loi.surveyId == surveyId && id !in idsSet }
  }

  override suspend fun merge(model: LocationOfInterest) = insertOrUpdate(model)

  override suspend fun enqueue(mutation: LocationOfInterestMutation) {
    appliedMutations.add(mutation)
  }

  override suspend fun apply(mutation: LocationOfInterestMutation) {
    appliedMutations.add(mutation)
  }

  override suspend fun updateAll(mutations: List<LocationOfInterestMutation>) {
    appliedMutations.addAll(mutations)
  }

  override suspend fun applyAndEnqueue(mutation: LocationOfInterestMutation) {
    appliedMutations.add(mutation)
  }
}
