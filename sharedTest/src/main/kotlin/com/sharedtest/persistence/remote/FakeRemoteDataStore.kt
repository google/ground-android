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
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRemoteDataStore @Inject internal constructor() : RemoteDataStore {
  var lois = emptyList<LocationOfInterest>()
  var surveys = emptyList<Survey>()
  var onLoadSurvey = { surveyId: String -> surveys.firstOrNull { it.id == surveyId } }
  var userProfileRefreshCount = 0
    private set

  // TODO(#1373): Delete once new LOI sync is implemented.
  var termsOfService: Maybe<TermsOfService> = Maybe.empty()

  private val subscribedSurveyIds = mutableSetOf<String>()

  override suspend fun loadSurveySummaries(user: User): List<Survey> = surveys

  override suspend fun loadSurvey(surveyId: String): Survey? = onLoadSurvey.invoke(surveyId)

  override fun loadTermsOfService(): @Cold Maybe<TermsOfService> = termsOfService

  // TODO(#1373): Delete once new LOI sync is implemented.
  override fun loadLocationsOfInterestOnceAndStreamChanges(
    survey: Survey
  ): Flowable<RemoteDataEvent<LocationOfInterest>> =
    Flowable.fromIterable(lois).map { RemoteDataEvent.loaded(it.id, it) }

  override suspend fun loadLocationsOfInterest(survey: Survey) = lois

  override fun loadSubmissions(
    locationOfInterest: LocationOfInterest
  ): Single<List<Result<Submission>>> {
    TODO("Missing implementation")
  }

  override suspend fun applyMutations(mutations: List<Mutation>, user: User) {
    TODO("Missing implementation")
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
