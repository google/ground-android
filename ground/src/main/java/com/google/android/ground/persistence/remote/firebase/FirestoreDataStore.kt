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
package com.google.android.ground.persistence.remote.firebase

import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.Survey
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

const val PROFILE_REFRESH_CLOUD_FUNCTION_NAME = "profile-refresh"

@Singleton
class FirestoreDataStore
@Inject
internal constructor(
  private val firebaseFunctions: FirebaseFunctions,
  private val groundFirestoreProvider: GroundFirestoreProvider,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RemoteDataStore {

  private suspend fun db() = groundFirestoreProvider.get()

  override suspend fun loadSurvey(surveyId: String): Survey =
    withContext(ioDispatcher) { db().surveys().survey(surveyId).get() }

  override suspend fun loadSubmissions(locationOfInterest: LocationOfInterest): List<Submission> =
    withContext(ioDispatcher) {
      db()
        .surveys()
        .survey(locationOfInterest.surveyId)
        .submissions()
        .submissionsByLocationOfInterestId(locationOfInterest)
    }

  override suspend fun loadTermsOfService(): TermsOfService? =
    withContext(ioDispatcher) { db().termsOfService().terms().get() }

  override suspend fun loadSurveySummaries(user: User): List<Survey> =
    withContext(ioDispatcher) { db().surveys().getReadable(user) }

  override suspend fun loadLocationsOfInterest(survey: Survey) =
    db().surveys().survey(survey.id).lois().locationsOfInterest(survey)

  override suspend fun subscribeToSurveyUpdates(surveyId: String) {
    Timber.d("Subscribing to FCM topic $surveyId")
    Firebase.messaging.subscribeToTopic(surveyId).await()
  }

  /**
   * Calls Cloud Function to refresh the current user's profile info in the remote database if
   * network is available.
   */
  override suspend fun refreshUserProfile() {
    firebaseFunctions.getHttpsCallable(PROFILE_REFRESH_CLOUD_FUNCTION_NAME).call().await()
  }

  override suspend fun applyMutations(mutations: List<Mutation>, user: User) {
    val batch = db().batch()
    for (mutation in mutations) {
      when (mutation) {
        is LocationOfInterestMutation -> addLocationOfInterestMutationToBatch(mutation, user, batch)
        is SubmissionMutation -> addSubmissionMutationToBatch(mutation, user, batch)
      }
    }
    batch.commit().await()
  }

  private suspend fun addLocationOfInterestMutationToBatch(
    mutation: LocationOfInterestMutation,
    user: User,
    batch: WriteBatch
  ) {
    db()
      .surveys()
      .survey(mutation.surveyId)
      .lois()
      .loi(mutation.locationOfInterestId)
      .addMutationToBatch(mutation, user, batch)
  }

  private suspend fun addSubmissionMutationToBatch(
    mutation: SubmissionMutation,
    user: User,
    batch: WriteBatch
  ) {
    db()
      .surveys()
      .survey(mutation.surveyId)
      .submissions()
      .submission(mutation.submissionId)
      .addMutationToBatch(mutation, user, batch)
  }
}
