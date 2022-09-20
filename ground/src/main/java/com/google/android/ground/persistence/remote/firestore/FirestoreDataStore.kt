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
package com.google.android.ground.persistence.remote.firestore

import com.google.android.gms.tasks.Task
import com.google.android.ground.model.Survey
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.remote.firestore.schema.GroundFirestore
import com.google.android.ground.rx.RxTask
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.ApplicationErrorManager
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.WriteBatch
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FirestoreDataStore
@Inject
internal constructor(
  val errorManager: ApplicationErrorManager,
  val db: GroundFirestore,
  val schedulers: Schedulers
) : RemoteDataStore {

  /**
   * Prevents known `FirebaseFirestoreException` from propagating downstream. Also, notifies the
   * event to a processor that should be handled commonly.
   */
  private fun shouldInterceptException(throwable: Throwable): Boolean {
    return errorManager.handleException(throwable)
  }

  private fun recordException(t: Throwable, message: String) {
    FirebaseCrashlytics.getInstance().log(message)
    FirebaseCrashlytics.getInstance().recordException(t)
  }

  override fun loadSurvey(surveyId: String): @Cold Single<Survey> {
    return db
      .surveys()
      .survey(surveyId)
      .get()
      .onErrorResumeNext { e: Throwable ->
        if (shouldInterceptException(e)) Maybe.never() else Maybe.error(e)
      }
      .switchIfEmpty(Single.error { NotFoundException("Survey $surveyId") })
      .subscribeOn(schedulers.io())
  }

  override fun loadSubmissions(
    locationOfInterest: LocationOfInterest
  ): @Cold Single<ImmutableList<Result<Submission>>> {
    return db
      .surveys()
      .survey(locationOfInterest.surveyId)
      .submissions()
      .submissionsByLocationOfInterestId(locationOfInterest)
      .onErrorResumeNext { e: Throwable ->
        if (shouldInterceptException(e)) Single.never() else Single.error(e)
      }
      .subscribeOn(schedulers.io())
  }

  override fun loadTermsOfService(): @Cold Maybe<TermsOfService> {
    return db
      .termsOfService()
      .terms()
      .get()
      .onErrorResumeNext { e: Throwable ->
        if (shouldInterceptException(e)) Maybe.never() else Maybe.error(e)
      }
      .subscribeOn(schedulers.io())
  }

  override fun loadSurveySummaries(user: User): @Cold Single<List<Survey>> {
    return db
      .surveys()
      .getReadable(user)
      .onErrorResumeNext { e: Throwable ->
        if (shouldInterceptException(e)) Single.never() else Single.error(e)
      }
      .subscribeOn(schedulers.io())
  }

  override fun loadLocationsOfInterestOnceAndStreamChanges(
    survey: Survey
  ): @Cold(stateful = true, terminates = false) Flowable<RemoteDataEvent<LocationOfInterest>> {
    return db
      .surveys()
      .survey(survey.id)
      .lois()
      .loadOnceAndStreamChanges(survey)
      .onErrorResumeNext { e: Throwable ->
        if (shouldInterceptException(e)) Flowable.never() else Flowable.error(e)
      }
      .subscribeOn(schedulers.io())
  }

  override fun applyMutations(
    mutations: ImmutableCollection<Mutation>,
    user: User
  ): @Cold Completable {
    return RxTask.toCompletable { applyMutationsInternal(mutations, user) }
      .doOnError { e: Throwable -> recordException(e, "Error applying mutation") }
      .onErrorResumeNext { e: Throwable ->
        if (shouldInterceptException(e)) Completable.never() else Completable.error(e)
      }
      .subscribeOn(schedulers.io())
  }

  private fun applyMutationsInternal(
    mutations: ImmutableCollection<Mutation>,
    user: User
  ): Task<*> {
    val batch = db.batch()
    for (mutation in mutations) {
      try {
        addMutationToBatch(mutation, user, batch)
      } catch (e: DataStoreException) {
        val mutationId =
          if (mutation is SubmissionMutation) mutation.submissionId
          else mutation.locationOfInterestId
        recordException(
          e,
          "Error adding ${mutation.type} ${mutation.javaClass.simpleName} for $mutationId  to batch"
        )
        Timber.e(e, "Skipping invalid mutation")
      }
    }
    return batch.commit()
  }

  @Throws(DataStoreException::class)
  private fun addMutationToBatch(mutation: Mutation, user: User, batch: WriteBatch) {
    when (mutation) {
      is LocationOfInterestMutation -> addLocationOfInterestMutationToBatch(mutation, user, batch)
      is SubmissionMutation -> addSubmissionMutationToBatch(mutation, user, batch)
    }
  }

  @Throws(DataStoreException::class)
  private fun addLocationOfInterestMutationToBatch(
    mutation: LocationOfInterestMutation,
    user: User,
    batch: WriteBatch
  ) {
    db
      .surveys()
      .survey(mutation.surveyId)
      .lois()
      .loi(mutation.locationOfInterestId)
      .addMutationToBatch(mutation, user, batch)
  }

  @Throws(DataStoreException::class)
  private fun addSubmissionMutationToBatch(
    mutation: SubmissionMutation,
    user: User,
    batch: WriteBatch
  ) {
    db
      .surveys()
      .survey(mutation.surveyId)
      .submissions()
      .submission(mutation.submissionId)
      .addMutationToBatch(mutation, user, batch)
  }
}
