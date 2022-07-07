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

package com.google.android.gnd.ui.home;

import static com.google.android.gnd.rx.Nil.NIL;
import static com.google.android.gnd.rx.RxCompletable.toBooleanSingle;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.job.Job;
import com.google.android.gnd.model.locationofinterest.LocationOfInterest;
import com.google.android.gnd.model.locationofinterest.Point;
import com.google.android.gnd.model.locationofinterest.PolygonOfInterest;
import com.google.android.gnd.model.mutation.LocationOfInterestMutation;
import com.google.android.gnd.model.mutation.Mutation.Type;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.repository.LocationOfInterestRepository;
import com.google.android.gnd.repository.SurveyRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapLocationOfInterest;
import com.google.android.gnd.ui.map.MapPin;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.Date;
import java8.util.Objects;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class HomeScreenViewModel extends AbstractViewModel {

  @Hot(replays = true)
  public final MutableLiveData<Boolean> isSubmissionButtonVisible = new MutableLiveData<>(false);

  private final SurveyRepository surveyRepository;
  private final Navigator navigator;
  private final LocationOfInterestRepository locationOfInterestRepository;
  private final UserRepository userRepository;

  /** The state and value of the currently active survey (loading, loaded, etc.). */
  private final LiveData<Loadable<Survey>> surveyLoadingState;

  // TODO(#719): Move into LocationOfInterestDetailsViewModel.
  @Hot private final FlowableProcessor<Nil> openDrawerRequests = PublishProcessor.create();

  @Hot(replays = true)
  private final MutableLiveData<BottomSheetState> bottomSheetState = new MutableLiveData<>();

  @Hot
  private final FlowableProcessor<LocationOfInterestMutation> addLocationOfInterestRequests =
      PublishProcessor.create();

  @Hot
  private final FlowableProcessor<LocationOfInterestMutation> updateLocationOfInterestRequests =
      PublishProcessor.create();

  @Hot
  private final FlowableProcessor<LocationOfInterestMutation> deleteLocationOfInterestRequests =
      PublishProcessor.create();

  @Hot private final Flowable<LocationOfInterest> addLocationOfInterestResults;
  @Hot private final Flowable<Boolean> updateLocationOfInterestResults;
  @Hot private final Flowable<Boolean> deleteLocationOfInterestResults;

  @Hot private final FlowableProcessor<Throwable> errors = PublishProcessor.create();

  @Hot
  private final Subject<ImmutableList<LocationOfInterest>> showLocationOfInterestSelectorRequests =
      PublishSubject.create();

  @Inject
  HomeScreenViewModel(
      SurveyRepository surveyRepository,
      LocationOfInterestRepository locationOfInterestRepository,
      Navigator navigator,
      UserRepository userRepository) {
    this.surveyRepository = surveyRepository;
    this.locationOfInterestRepository = locationOfInterestRepository;
    this.navigator = navigator;
    this.userRepository = userRepository;

    surveyLoadingState =
        LiveDataReactiveStreams.fromPublisher(surveyRepository.getSurveyLoadingState());
    addLocationOfInterestResults =
        addLocationOfInterestRequests.switchMapSingle(
            mutation ->
                locationOfInterestRepository
                    .applyAndEnqueue(mutation)
                    .andThen(locationOfInterestRepository.getLocationOfInterest(mutation))
                    .doOnError(errors::onNext)
                    .onErrorResumeNext(Single.never())); // Prevent from breaking upstream.
    deleteLocationOfInterestResults =
        deleteLocationOfInterestRequests.switchMapSingle(
            mutation ->
                toBooleanSingle(
                    locationOfInterestRepository.applyAndEnqueue(mutation), errors::onNext));
    updateLocationOfInterestResults =
        updateLocationOfInterestRequests.switchMapSingle(
            mutation ->
                toBooleanSingle(
                    locationOfInterestRepository.applyAndEnqueue(mutation), errors::onNext));
  }

  @Hot
  public Observable<ImmutableList<LocationOfInterest>> getShowLocationOfInterestSelectorRequests() {
    return showLocationOfInterestSelectorRequests;
  }

  public Flowable<LocationOfInterest> getAddLocationOfInterestResults() {
    return addLocationOfInterestResults;
  }

  public Flowable<Boolean> getUpdateLocationOfInterestResults() {
    return updateLocationOfInterestResults;
  }

  public Flowable<Boolean> getDeleteLocationOfInterestResults() {
    return deleteLocationOfInterestResults;
  }

  public Flowable<Throwable> getErrors() {
    return errors;
  }

  public void addLocationofInterest(Job job, Point point) {
    getActiveSurvey()
        .map(Survey::getId)
        .ifPresentOrElse(
            surveyId ->
                addLocationOfInterestRequests.onNext(
                    locationOfInterestRepository.newMutation(
                        surveyId, job.getId(), point, new Date())),
            () -> {
              throw new IllegalStateException("Empty survey");
            });
  }

  public void addPolygonOfInterest(PolygonOfInterest polygonOfInterest) {
    getActiveSurvey()
        .map(Survey::getId)
        .ifPresentOrElse(
            surveyId ->
                addLocationOfInterestRequests.onNext(
                    locationOfInterestRepository.newPolygonOfInterestMutation(
                        surveyId,
                        polygonOfInterest.getJob().getId(),
                        polygonOfInterest.getVertices(),
                        new Date())),
            () -> {
              throw new IllegalStateException("Empty survey");
            });
  }

  public void updateLocationOfInterest(LocationOfInterest locationOfInterest) {
    updateLocationOfInterestRequests.onNext(
        locationOfInterest.toMutation(Type.UPDATE, userRepository.getCurrentUser().getId()));
  }

  public void deleteLocationOfInterest(LocationOfInterest locationOfInterest) {
    deleteLocationOfInterestRequests.onNext(
        locationOfInterest.toMutation(Type.DELETE, userRepository.getCurrentUser().getId()));
  }

  public boolean shouldShowSurveySelectorOnStart() {
    return surveyRepository.getLastActiveSurveyId().isEmpty();
  }

  public Flowable<Nil> getOpenDrawerRequests() {
    return openDrawerRequests;
  }

  public void openNavDrawer() {
    openDrawerRequests.onNext(NIL);
  }

  public LiveData<Loadable<Survey>> getSurveyLoadingState() {
    return surveyLoadingState;
  }

  public LiveData<BottomSheetState> getBottomSheetState() {
    return bottomSheetState;
  }

  public void onMarkerClick(MapPin marker) {
    if (marker.getLocationOfInterest() != null) {
      showBottomSheet(marker.getLocationOfInterest());
    }
  }

  public void onLocationOfInterestSelected(LocationOfInterest locationOfInterest) {
    showBottomSheet(locationOfInterest);
  }

  private void showBottomSheet(LocationOfInterest locationOfInterest) {
    Timber.d("showing bottom sheet");
    isSubmissionButtonVisible.setValue(true);
    bottomSheetState.setValue(BottomSheetState.visible(locationOfInterest));
  }

  public void onBottomSheetHidden() {
    bottomSheetState.setValue(BottomSheetState.hidden());
    isSubmissionButtonVisible.setValue(false);
  }

  public void addSubmission() {
    BottomSheetState state = bottomSheetState.getValue();
    if (state == null) {
      Timber.e("Missing bottomSheetState");
      return;
    }

    Optional<LocationOfInterest> optionalLocationOfInterest = state.getLocationOfInterest();
    if (optionalLocationOfInterest.isEmpty()) {
      Timber.e("Missing locationOfInterest");
      return;
    }
    LocationOfInterest locationOfInterest = optionalLocationOfInterest.get();
    Optional<Task> form = locationOfInterest.getJob().getTask();
    if (form.isEmpty()) {
      // .TODO: Hide Add Submission button if no forms defined.
      Timber.e("No tasks in job");
      return;
    }
    Survey survey = locationOfInterest.getSurvey();
    if (survey == null) {
      Timber.e("Missing survey");
      return;
    }
    navigator.navigate(
        HomeScreenFragmentDirections.addSubmission(
            survey.getId(), locationOfInterest.getId(), form.get().getId()));
  }

  public void init() {
    // Last active survey will be loaded once view subscribes to activeProject.
    surveyRepository.loadLastActiveSurvey();
  }

  public void showOfflineAreas() {
    navigator.navigate(HomeScreenFragmentDirections.showOfflineAreas());
  }

  public void showSettings() {
    navigator.navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity());
  }

  public void onLocationOfInterestClick(
      ImmutableList<MapLocationOfInterest> mapLocationsOfInterest) {
    ImmutableList<LocationOfInterest> locationsOfInterest =
        stream(mapLocationsOfInterest)
            .map(MapLocationOfInterest::getLocationOfInterest)
            .filter(Objects::nonNull)
            .collect(toImmutableList());

    if (locationsOfInterest.isEmpty()) {
      Timber.e("onLocationOfInterestClick called with empty or null map locationsOfInterest");
      return;
    }

    if (locationsOfInterest.size() == 1) {
      onLocationOfInterestSelected(locationsOfInterest.get(0));
      return;
    }

    showLocationOfInterestSelectorRequests.onNext(locationsOfInterest);
  }

  public Optional<Survey> getActiveSurvey() {
    return Loadable.getValue(getSurveyLoadingState());
  }

  public void showSyncStatus() {
    navigator.navigate(HomeScreenFragmentDirections.showSyncStatus());
  }
}
