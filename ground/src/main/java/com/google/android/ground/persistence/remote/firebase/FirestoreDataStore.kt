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
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.toListItem
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.remote.firebase.schema.GroundFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

const val PROFILE_REFRESH_CLOUD_FUNCTION_NAME = "profile-refresh"

@Singleton
class FirestoreDataStore
@Inject
internal constructor(
  private val firebaseFunctions: FirebaseFunctions,
  private val firestoreProvider: FirebaseFirestoreProvider,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RemoteDataStore {

  private suspend fun db() = GroundFirestore(firestoreProvider.get())

  override suspend fun loadSurvey(surveyId: String): Survey? =
    withContext(ioDispatcher) { db().surveys().survey(surveyId).get() }

  override suspend fun loadTermsOfService(): TermsOfService? =
    withContext(ioDispatcher) { db().termsOfService().terms().get() }

  override fun getSurveyList(user: User): Flow<List<SurveyListItem>> = flow {
    emitAll(
      db().surveys().getReadable(user).map { list ->
        // TODO(#2031): Return SurveyListItem from getReadable(), only fetch required fields.
        list.map { it.toListItem(false) }
      }
    )
  }

  override suspend fun loadPredefinedLois(survey: Survey) =
    withContext(ioDispatcher) { db().surveys().survey(survey.id).lois().fetchPredefined(survey) }

  override suspend fun loadUserLois(survey: Survey, ownerUserId: String) =
    withContext(ioDispatcher) {
      db().surveys().survey(survey.id).lois().fetchUserDefined(survey, ownerUserId)
    }

  override suspend fun subscribeToSurveyUpdates(surveyId: String) {
    Timber.d("Subscribing to FCM topic $surveyId")
    try {
      Firebase.messaging.subscribeToTopic(surveyId).await()
    } catch (e: CancellationException) {
      Timber.i(e, "Subscribing to FCM topic was cancelled")
    }
  }

  /**
   * Calls Cloud Function to refresh the current user's profile info in the remote database if
   * network is available.
   */
  override suspend fun refreshUserProfile() {
    try {
      firebaseFunctions.getHttpsCallable(PROFILE_REFRESH_CLOUD_FUNCTION_NAME).call().await()
    } catch (e: CancellationException) {
      Timber.i(e, "Calling profile refresh function was cancelled")
    }
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
    batch: WriteBatch,
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
    batch: WriteBatch,
  ) {
    db()
      .surveys()
      .survey(mutation.surveyId)
      .submissions()
      .submission(mutation.submissionId)
      .addMutationToBatch(mutation, user, batch)
  }
}
