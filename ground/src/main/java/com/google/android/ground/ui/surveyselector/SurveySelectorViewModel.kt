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

package com.google.android.ground.ui.surveyselector;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.ground.model.Survey;
import com.google.android.ground.repository.SurveyRepository;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.system.auth.AuthenticationManager;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

/** Represents view state and behaviors of the survey selector dialog. */
public class SurveySelectorViewModel extends AbstractViewModel {
  private final SurveyRepository surveyRepository;
  private final LiveData<Loadable<List<Survey>>> surveySummaries;

  @Inject
  SurveySelectorViewModel(SurveyRepository surveyRepository, AuthenticationManager authManager) {
    this.surveyRepository = surveyRepository;
    this.surveySummaries =
        LiveDataReactiveStreams.fromPublisher(
            surveyRepository.getSurveySummaries(authManager.getCurrentUser()));
  }

  public LiveData<Loadable<List<Survey>>> getSurveySummaries() {
    return surveySummaries;
  }

  public Single<ImmutableList<Survey>> getOfflineSurveys() {
    return surveyRepository.getOfflineSurveys();
  }

  /**
   * Triggers the specified survey to be loaded and activated.
   *
   * @param idx the index in the survey summary list.
   */
  public void activateSurvey(int idx) {
    Optional<List<Survey>> surveys = Loadable.getValue(this.surveySummaries);
    if (surveys.isEmpty()) {
      Timber.e("Can't activate survey before list is loaded");
      return;
    }
    if (idx >= surveys.get().size()) {
      Timber.e(
          "Can't activate survey at index %d, only %d surveys in list", idx, surveys.get().size());
      return;
    }
    Survey survey = surveys.get().get(idx);
    surveyRepository.activateSurvey(survey.getId());
  }

  public void activateOfflineSurvey(String surveyId) {
    surveyRepository.activateSurvey(surveyId);
  }
}
