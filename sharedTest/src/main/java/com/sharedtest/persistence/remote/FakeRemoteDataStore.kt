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
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.sharedtest.FakeData
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRemoteDataStore @Inject internal constructor() : RemoteDataStore {
  private var loiEvent: RemoteDataEvent<LocationOfInterest>? = null

  // TODO(#1045): Allow default survey to be initialized by tests.
  private var testSurveys = listOf(FakeData.SURVEY)

  // TODO(#1045): Allow default ToS to be initialized by tests.
  private var termsOfService = Optional.of(FakeData.TERMS_OF_SERVICE)

  /**
   * Set this before the test scenario is loaded.
   *
   * In that case, launch scenario manually using ActivityScenario.launch instead of using
   * ActivityScenarioRule.
   */
  fun setTestSurvey(survey: Survey) {
    testSurveys = listOf(survey)
  }

  /**
   * Set this before the test scenario is loaded.
   *
   * In that case, launch scenario manually using ActivityScenario.launch instead of using
   * ActivityScenarioRule.
   */
  fun setTestSurveys(surveys: List<Survey>) {
    testSurveys = surveys
  }

  override fun loadSurveySummaries(user: User): Single<List<Survey>> {
    return Single.just(testSurveys)
  }

  override fun loadSurvey(surveyId: String): Single<Survey> {
    return Single.just(testSurveys[0])
  }

  fun setTermsOfService(termsOfService: Optional<TermsOfService>) {
    this.termsOfService = termsOfService
  }

  override fun loadTermsOfService(): @Cold Maybe<TermsOfService> {
    return if (termsOfService.isEmpty) Maybe.empty() else Maybe.just(termsOfService.get())
  }

  override fun loadLocationsOfInterest(survey: Survey): Single<List<LocationOfInterest>> =
    Single.just(emptyList())

  override fun loadSubmissions(
    locationOfInterest: LocationOfInterest
  ): Single<ImmutableList<Result<Submission>>> {
    TODO("Missing implementation")
  }

  override fun applyMutations(mutations: ImmutableCollection<Mutation>, user: User): Completable {
    TODO("Missing implementation")
  }

  override fun subscribeToSurveyUpdates(surveyId: String): Completable = Completable.complete()

  fun streamLoiOnce(loiEvent: RemoteDataEvent<LocationOfInterest>) {
    this.loiEvent = loiEvent
  }
}
