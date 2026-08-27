/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.data.stores.RemoteDataStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.toListItem
import org.groundplatform.testing.FakeCall

@Singleton
class FakeRemoteDataStore @Inject internal constructor() : RemoteDataStore {
  var predefinedLois = emptyList<LocationOfInterest>()
  /**
   * Pages of predefined LOIs, taking precedence over [predefinedLois] when set.
   *
   * Lets a test drive a sync over several pages, or fail one part way through, which
   * [predefinedLois] cannot express since it always stands for exactly one page.
   */
  var predefinedLoiPages: Flow<List<LocationOfInterest>>? = null
  var userLois = emptyList<LocationOfInterest>()
  var sharedLois = emptyList<LocationOfInterest>()
  var surveys = emptyList<Survey>()
  var publicSurveys = emptyList<Survey>()
  var onLoadSurvey: suspend (String) -> Survey? = { surveyId ->
    surveys.firstOrNull { it.id == surveyId }
  }
  var userProfileRefreshCount = 0
    private set

  var termsOfService: Result<TermsOfService?>? = null
  var applyMutationError: Error? = null

  val subscribedSurveyIds = mutableSetOf<String>()
  val loadUserLoisCall = FakeCall<Survey, List<LocationOfInterest>> { userLois }

  val loadSharedLoisCall = FakeCall<Survey, List<LocationOfInterest>> { sharedLois }

  override fun getRestrictedSurveyList(user: User): Flow<List<SurveyListItem>> =
    flowOf(surveys.map { it.toListItem(false) })

  override fun getPublicSurveyList(): Flow<List<SurveyListItem>> =
    flowOf(publicSurveys.map { it.toListItem(false) })

  override suspend fun loadSurvey(surveyId: String): Survey? = onLoadSurvey.invoke(surveyId)

  override suspend fun loadTermsOfService(): TermsOfService? = termsOfService?.getOrThrow()

  override fun loadPredefinedLois(survey: Survey): Flow<List<LocationOfInterest>> =
    predefinedLoiPages ?: flowOf(predefinedLois)

  override suspend fun applyMutations(mutations: List<Mutation>, user: User) {
    if (applyMutationError != null) {
      throw applyMutationError as Error
    }
  }

  override suspend fun subscribeToSurveyUpdates(surveyId: String) {
    subscribedSurveyIds.add(surveyId)
  }

  override suspend fun unsubscribeFromSurveyUpdates(surveyId: String) {
    subscribedSurveyIds.remove(surveyId)
  }

  override suspend fun refreshUserProfile() {
    userProfileRefreshCount++
  }

  override fun loadUserLois(survey: Survey, ownerUserId: String): Flow<List<LocationOfInterest>> =
    flow {
      emit(loadUserLoisCall(survey))
    }

  override fun loadSharedLois(survey: Survey): Flow<List<LocationOfInterest>> = flow {
    emit(loadSharedLoisCall(survey))
  }
}
