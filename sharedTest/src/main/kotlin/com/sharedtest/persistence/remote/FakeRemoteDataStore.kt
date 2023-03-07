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
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRemoteDataStore @Inject internal constructor() : RemoteDataStore {
  var lois = emptyList<LocationOfInterest>()
  var surveys = emptyList<Survey>()

  // TODO(#1373): Delete once new LOI sync is implemented.
  var termsOfService: TermsOfService? = null

  private val subscribedSurveyIds = mutableSetOf<String>()

  override fun loadSurveySummaries(user: User): Single<List<Survey>> = Single.just(surveys)

  override fun loadSurvey(surveyId: String): Single<Survey> =
    Single.just(
      surveys.firstOrNull { it.id == surveyId } ?: throw NotFoundException("Invalid survey id")
    )

  override fun loadTermsOfService(): @Cold Maybe<TermsOfService> =
    if (termsOfService == null) Maybe.empty() else Maybe.just(termsOfService)

  // TODO(#1373): Delete once new LOI sync is implemented.
  override fun loadLocationsOfInterestOnceAndStreamChanges(
    survey: Survey
  ): Flowable<RemoteDataEvent<LocationOfInterest>> =
    Flowable.fromIterable(lois).map { RemoteDataEvent.loaded(it.id, it) }

  override suspend fun loadLocationsOfInterest(survey: Survey) = listOf<LocationOfInterest>()

  override fun loadSubmissions(
    locationOfInterest: LocationOfInterest
  ): Single<List<Result<Submission>>> {
    TODO("Missing implementation")
  }

  override fun applyMutations(mutations: List<Mutation>, user: User): Completable {
    TODO("Missing implementation")
  }

  override fun subscribeToSurveyUpdates(surveyId: String): Completable =
    Completable.fromRunnable { subscribedSurveyIds.add(surveyId) }

  /** Returns true iff [subscribeToSurveyUpdates] has been called with the specified id. */
  fun isSubscribedToSurveyUpdates(surveyId: String): Boolean =
    subscribedSurveyIds.contains(surveyId)
}
