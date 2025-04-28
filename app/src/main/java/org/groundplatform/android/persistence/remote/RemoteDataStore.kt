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
package org.groundplatform.android.persistence.remote

import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.model.TermsOfService
import org.groundplatform.android.model.User
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.Mutation

/**
 * Defines API for accessing data in a remote data store. Implementations must ensure all
 * subscriptions are run in a background thread (i.e., not the Android main thread).
 */
interface RemoteDataStore {
  fun getRestrictedSurveyList(user: User): Flow<List<SurveyListItem>>

  /**
   * Fetches and continuously observes all surveys that have been marked public. Whenever public
   * surveys are added, updated, or removed in the remote store, this function will emit the latest
   * list of [SurveyListItem].
   */
  fun getPublicSurveyList(): Flow<List<SurveyListItem>>

  /**
   * Loads the survey with the specified id from the remote data store. Returns `null` if the survey
   * is not found. Throws an error if the remote data store is not available.
   */
  suspend fun loadSurvey(surveyId: String): Survey?

  /**
   * Loads the app terms from the remote data store. Returns `null` if is not found. Throws an error
   * if the remote data store is not available.
   */
  suspend fun loadTermsOfService(): TermsOfService?

  /** Returns predefined LOIs in the specified survey. Main-safe. */
  suspend fun loadPredefinedLois(survey: Survey): List<LocationOfInterest>

  /** Returns LOIs owned by the specified user in the specified survey. Main-safe. */
  suspend fun loadUserLois(survey: Survey, ownerUserId: String): List<LocationOfInterest>

  /**
   * Applies the provided mutations to the remote data store in a single batched transaction. If one
   * update fails, none of the mutations will be applied.
   */
  suspend fun applyMutations(mutations: List<Mutation>, user: User)

  /**
   * Listens for remote changes to the survey with the specified id. Implementations should handle
   * synchronization of new, changed, and deleted surveys and LOIs to the local db in the
   * background.
   */
  suspend fun subscribeToSurveyUpdates(surveyId: String)

  /** Refreshes the current user's profile info in the remote database. */
  suspend fun refreshUserProfile()
}
