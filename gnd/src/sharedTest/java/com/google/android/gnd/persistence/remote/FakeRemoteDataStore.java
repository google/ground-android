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

package com.google.android.gnd.persistence.remote;

import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.mutation.Mutation;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FakeRemoteDataStore implements RemoteDataStore {

  private RemoteDataEvent<Feature> featureEvent;
  // TODO(#1045): Allow default survey to be initialized by tests.
  private List<Survey> testSurveys = Collections.singletonList(FakeData.SURVEY);
  // TODO(#1045): Allow default ToS to be initialized by tests.
  private Optional<TermsOfService> termsOfService = Optional.of(FakeData.TERMS_OF_SERVICE);

  @Inject
  FakeRemoteDataStore() {
  }

  /**
   * Set this before the test scenario is loaded.
   *
   * <p>In that case, launch scenario manually using ActivityScenario.launch instead of using
   * ActivityScenarioRule.
   */
  public void setTestSurvey(Survey survey) {
    this.testSurveys = Collections.singletonList(survey);
  }

  /**
   * Set this before the test scenario is loaded.
   *
   * <p>In that case, launch scenario manually using ActivityScenario.launch instead of using
   * ActivityScenarioRule.
   */
  public void setTestSurveys(List<Survey> surveys) {
    this.testSurveys = surveys;
  }

  @Override
  public Single<List<Survey>> loadSurveySummaries(User user) {
    return Single.just(testSurveys);
  }

  @Override
  public Single<Survey> loadSurvey(String surveyId) {
    return Single.just(testSurveys.get(0));
  }

  public void setTermsOfService(Optional<TermsOfService> termsOfService) {
    this.termsOfService = termsOfService;
  }

  @Override
  public @Cold Maybe<TermsOfService> loadTermsOfService() {
    return termsOfService.isEmpty() ? Maybe.empty() : Maybe.just(termsOfService.get());
  }

  @Override
  public Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Survey survey) {
    return featureEvent == null ? Flowable.empty() : Flowable.just(featureEvent);
  }

  @Override
  public Single<ImmutableList<ValueOrError<Submission>>> loadSubmissions(Feature feature) {
    return null;
  }

  @Override
  public Completable applyMutations(ImmutableCollection<Mutation> mutations, User user) {
    return null;
  }

  public void streamFeatureOnce(RemoteDataEvent<Feature> featureEvent) {
    this.featureEvent = featureEvent;
  }
}
