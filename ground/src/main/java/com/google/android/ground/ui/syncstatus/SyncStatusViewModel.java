/*
 * Copyright 2021 Google LLC
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

package com.google.android.ground.ui.syncstatus;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.mutation.Mutation;
import com.google.android.ground.repository.LocationOfInterestRepository;
import com.google.android.ground.repository.SurveyRepository;
import com.google.android.ground.rx.annotations.Cold;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.common.Navigator;
import com.google.android.ground.ui.offlinebasemap.OfflineAreasFragmentDirections;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.Single;
import javax.inject.Inject;

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
public class SyncStatusViewModel extends AbstractViewModel {

  private final LiveData<ImmutableList<Pair<LocationOfInterest, Mutation>>> mutations;
  private final Navigator navigator;
  private final SurveyRepository surveyRepository;
  private final LocationOfInterestRepository locationOfInterestRepository;

  @Inject
  SyncStatusViewModel(
      SurveyRepository surveyRepository,
      LocationOfInterestRepository locationOfInterestRepository,
      Navigator navigator) {
    this.navigator = navigator;
    this.surveyRepository = surveyRepository;
    this.locationOfInterestRepository = locationOfInterestRepository;

    this.mutations =
        LiveDataReactiveStreams.fromPublisher(
            getMutationsOnceAndStream().switchMap(this::loadLocationsOfInterestAndPair));
  }

  private Flowable<ImmutableList<Pair<LocationOfInterest, Mutation>>>
      loadLocationsOfInterestAndPair(ImmutableList<Mutation> mutations) {
    return Single.merge(
            stream(mutations).map(this::loadLocationOfInterestAndPair).collect(toList()))
        .toList()
        .map(ImmutableList::copyOf)
        .toFlowable();
  }

  private Single<Pair<LocationOfInterest, Mutation>> loadLocationOfInterestAndPair(
      Mutation mutation) {
    return locationOfInterestRepository
        .getLocationOfInterest(mutation.getSurveyId(), mutation.getLocationOfInterestId())
        .map(locationOfInterest -> Pair.create(locationOfInterest, mutation));
  }

  private Flowable<ImmutableList<Mutation>> getMutationsOnceAndStream() {
    return surveyRepository
        .getActiveSurvey()
        .switchMap(
            survey ->
                survey
                    .map(surveyRepository::getMutationsOnceAndStream)
                    .orElse(Flowable.just(ImmutableList.of())));
  }

  public void showOfflineAreaSelector() {
    navigator.navigate(OfflineAreasFragmentDirections.showOfflineAreaSelector());
  }

  @Cold(replays = true, terminates = false)
  LiveData<ImmutableList<Pair<LocationOfInterest, Mutation>>> getMutations() {
    return mutations;
  }
}
