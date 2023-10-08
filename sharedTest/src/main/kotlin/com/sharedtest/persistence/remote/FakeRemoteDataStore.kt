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
package com.sharedtest.persistence.remote

import com.google.android.ground.model.Survey
import com.google.android.ground.model.RemoteAppConfig
import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.remote.RemoteDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRemoteDataStore @Inject internal constructor() : RemoteDataStore {
  var lois = emptyList<LocationOfInterest>()
  var surveys = emptyList<Survey>()
  var onLoadSurvey = { surveyId: String -> surveys.firstOrNull { it.id == surveyId } }
  var userProfileRefreshCount = 0
    private set

  var remoteAppConfig: Result<RemoteAppConfig?>? = null
  var applyMutationError: Error? = null

  private val subscribedSurveyIds = mutableSetOf<String>()

  override suspend fun loadSurveySummaries(user: User): List<Survey> = surveys

  override suspend fun loadSurvey(surveyId: String): Survey? = onLoadSurvey.invoke(surveyId)

  override suspend fun loadRemoteAppConfig(): RemoteAppConfig? = remoteAppConfig?.getOrThrow()

  override suspend fun loadLocationsOfInterest(survey: Survey) = lois

  override suspend fun loadSubmissions(locationOfInterest: LocationOfInterest): List<Submission> {
    TODO("Missing implementation")
  }

  override suspend fun applyMutations(mutations: List<Mutation>, user: User) {
    if (applyMutationError != null) {
      throw applyMutationError as Error
    }
  }

  override suspend fun subscribeToSurveyUpdates(surveyId: String) {
    subscribedSurveyIds.add(surveyId)
  }

  override suspend fun refreshUserProfile() {
    userProfileRefreshCount++
  }

  /** Returns true iff [subscribeToSurveyUpdates] has been called with the specified id. */
  fun isSubscribedToSurveyUpdates(surveyId: String): Boolean =
    subscribedSurveyIds.contains(surveyId)
}
