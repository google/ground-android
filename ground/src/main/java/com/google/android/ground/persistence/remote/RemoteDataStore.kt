/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.persistence.remote

import com.google.android.ground.model.Survey
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.rx.annotations.Cold
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * Defines API for accessing data in a remote data store. Implementations must ensure all
 * subscriptions are run in a background thread (i.e., not the Android main thread).
 */
interface RemoteDataStore {
  fun loadSurveySummaries(user: User): @Cold Single<List<Survey>>

  /**
   * Loads the survey with the specified id from the remote data store. The return Single fails with
   * if the survey is not found, or if the remote data store is not available.
   */
  fun loadSurvey(surveyId: String): @Cold Single<Survey>

  /**
   * Loads the survey terms from the remote data store. The returned Maybe is empty if not found,
   * otherwise it completes with the loaded terms of service.
   */
  fun loadTermsOfService(): @Cold Maybe<TermsOfService>

  /**
   * Returns all LOIs in the specified survey, then continues to emit any remote updates to the set
   * of LOIs in the survey until all subscribers have been disposed.
   */
  fun loadLocationsOfInterestOnceAndStreamChanges(
    survey: Survey
  ): @Cold(stateful = true, terminates = false) Flowable<RemoteDataEvent<LocationOfInterest>>

  /**
   * Returns a list of all submissions associated with the specified LOI, or an empty list if none
   * are found.
   */
  fun loadSubmissions(
    locationOfInterest: LocationOfInterest
  ): @Cold Single<ImmutableList<Result<Submission>>>

  /**
   * Applies the provided mutations to the remote data store in a single batched transaction. If one
   * update fails, none of the mutations will be applied.
   */
  fun applyMutations(mutations: ImmutableCollection<Mutation>, user: User): @Cold Completable
}
