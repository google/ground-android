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
import kotlinx.coroutines.flow.emptyFlow
import org.groundplatform.data.stores.RemoteDataStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.mutation.Mutation

class FakeRemoteDataStore : RemoteDataStore {
  var termsOfServiceResult: Result<TermsOfService?> = Result.success(null)
  var surveys: List<Survey> = emptyList()
  val subscribedSurveyUpdates = mutableListOf<String>()
  val unsubscribedSurveyUpdates = mutableListOf<String>()
  val appliedMutations = mutableListOf<Mutation>()

  override suspend fun loadTermsOfService(): TermsOfService? = termsOfServiceResult.getOrThrow()

  override fun getRestrictedSurveyList(user: User): Flow<List<SurveyListItem>> = emptyFlow()

  override fun getPublicSurveyList(): Flow<List<SurveyListItem>> = emptyFlow()

  override suspend fun loadSurvey(surveyId: String): Survey? = surveys.firstOrNull {
    it.id == surveyId
  }

  override fun loadPredefinedLois(survey: Survey): Flow<List<LocationOfInterest>> = emptyFlow()

  override fun loadUserLois(survey: Survey, ownerUserId: String): Flow<List<LocationOfInterest>> =
    emptyFlow()

  override fun loadSharedLois(survey: Survey): Flow<List<LocationOfInterest>> = emptyFlow()

  override suspend fun applyMutations(mutations: List<Mutation>, user: User) {
    appliedMutations.addAll(mutations)
  }

  override suspend fun subscribeToSurveyUpdates(surveyId: String) {
    subscribedSurveyUpdates.add(surveyId)
  }

  override suspend fun unsubscribeFromSurveyUpdates(surveyId: String) {
    unsubscribedSurveyUpdates.add(surveyId)
  }

  override suspend fun refreshUserProfile() = Unit
}
