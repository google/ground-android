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

package com.google.android.gnd.persistence.remote.firestore;

import com.google.android.gms.tasks.Task;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.mutation.FeatureMutation;
import com.google.android.gnd.model.mutation.Mutation;
import com.google.android.gnd.model.mutation.SubmissionMutation;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.firestore.schema.GroundFirestore;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.system.ApplicationErrorManager;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class FirestoreDataStore implements RemoteDataStore {

  static final String ID_COLLECTION = "/ids";

  @Inject
  ApplicationErrorManager errorManager;
  @Inject
  GroundFirestore db;
  @Inject
  Schedulers schedulers;

  @Inject
  FirestoreDataStore() {
  }

  /**
   * Prevents known {@link FirebaseFirestoreException} from propagating downstream. Also, notifies
   * the event to a processor that should be handled commonly.
   */
  private boolean shouldInterceptException(Throwable throwable) {
    return errorManager.handleException(throwable);
  }

  private void recordException(Throwable t, String message) {
    FirebaseCrashlytics.getInstance().log(message);
    FirebaseCrashlytics.getInstance().recordException(t);
  }

  @Cold
  @Override
  public Single<Survey> loadSurvey(String surveyId) {
    return db.surveys()
        .survey(surveyId)
        .get()
        .onErrorResumeNext(e -> shouldInterceptException(e) ? Maybe.never() : Maybe.error(e))
        .switchIfEmpty(Single.error(() -> new NotFoundException("Survey " + surveyId)))
        .subscribeOn(schedulers.io());
  }

  @Cold
  @Override
  public Single<ImmutableList<ValueOrError<Submission>>> loadSubmissions(Feature feature) {
    return db.surveys()
        .survey(feature.getSurvey().getId())
        .submissions()
        .submissionsByLoiId(feature)
        .onErrorResumeNext(e -> shouldInterceptException(e) ? Single.never() : Single.error(e))
        .subscribeOn(schedulers.io());
  }

  @Cold
  @Override
  public Maybe<TermsOfService> loadTermsOfService() {
    return db.termsOfService()
        .getTerm()
        .get()
        .onErrorResumeNext(e -> shouldInterceptException(e) ? Maybe.never() : Maybe.error(e))
        .subscribeOn(schedulers.io());
  }

  @Cold
  @Override
  public Single<List<Survey>> loadSurveySummaries(User user) {
    return db.surveys()
        .getReadable(user)
        .onErrorResumeNext(e -> shouldInterceptException(e) ? Single.never() : Single.error(e))
        .subscribeOn(schedulers.io());
  }

  @Cold(stateful = true, terminates = false)
  @Override
  public Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Survey survey) {
    return db.surveys()
        .survey(survey.getId())
        .lois()
        .loadOnceAndStreamChanges(survey)
        .onErrorResumeNext(e -> shouldInterceptException(e) ? Flowable.never() : Flowable.error(e))
        .subscribeOn(schedulers.io());
  }

  @Cold
  @Override
  public Completable applyMutations(ImmutableCollection<Mutation> mutations, User user) {
    return RxTask.toCompletable(() -> applyMutationsInternal(mutations, user))
        .doOnError(e -> recordException(e, "Error applying mutation"))
        .onErrorResumeNext(
            e -> shouldInterceptException(e) ? Completable.never() : Completable.error(e))
        .subscribeOn(schedulers.io());
  }

  private Task<?> applyMutationsInternal(ImmutableCollection<Mutation> mutations, User user) {
    WriteBatch batch = db.batch();
    for (Mutation mutation : mutations) {
      try {
        addMutationToBatch(mutation, user, batch);
      } catch (DataStoreException e) {
        recordException(
            e,
            "Error adding "
                + mutation.getType()
                + " "
                + mutation.getClass().getSimpleName()
                + " for "
                + (mutation instanceof SubmissionMutation
                ? ((SubmissionMutation) mutation).getSubmissionId()
                : mutation.getFeatureId())
                + " to batch");
        Timber.e(e, "Skipping invalid mutation");
      }
    }
    return batch.commit();
  }

  private void addMutationToBatch(Mutation mutation, User user, WriteBatch batch)
      throws DataStoreException {
    if (mutation instanceof FeatureMutation) {
      addFeatureMutationToBatch((FeatureMutation) mutation, user, batch);
    } else if (mutation instanceof SubmissionMutation) {
      addSubmissionMutationToBatch((SubmissionMutation) mutation, user, batch);
    } else {
      throw new DataStoreException("Unsupported mutation " + mutation.getClass());
    }
  }

  private void addFeatureMutationToBatch(FeatureMutation mutation, User user, WriteBatch batch)
      throws DataStoreException {
    db.surveys()
        .survey(mutation.getSurveyId())
        .lois()
        .loi(mutation.getFeatureId())
        .addMutationToBatch(mutation, user, batch);
  }

  private void addSubmissionMutationToBatch(
      SubmissionMutation mutation, User user, WriteBatch batch) throws DataStoreException {
    db.surveys()
        .survey(mutation.getSurveyId())
        .submissions()
        .submission(mutation.getSubmissionId())
        .addMutationToBatch(mutation, user, batch);
  }
}
