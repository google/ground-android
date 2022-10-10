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

package com.google.android.ground.ui.home.locationofinterestdetails;

import static com.google.android.ground.ui.home.mapcontainer.MapContainerViewModel.DEFAULT_LOI_ZOOM_LEVEL;

import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import com.google.android.ground.R;
import com.google.android.ground.model.Role;
import com.google.android.ground.model.User;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.locationofinterest.LocationOfInterestType;
import com.google.android.ground.model.mutation.LocationOfInterestMutation;
import com.google.android.ground.model.mutation.SubmissionMutation;
import com.google.android.ground.repository.LocationOfInterestRepository;
import com.google.android.ground.repository.SubmissionRepository;
import com.google.android.ground.repository.UserRepository;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.MarkerIconFactory;
import com.google.android.ground.ui.common.LocationOfInterestHelper;
import com.google.android.ground.ui.common.SharedViewModel;
import com.google.android.ground.ui.util.DrawableUtil;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class LocationOfInterestDetailsViewModel extends ViewModel {

  @Hot
  private final FlowableProcessor<Optional<LocationOfInterest>> selectedLocationOfInterest =
      BehaviorProcessor.createDefault(Optional.empty());

  private final LocationOfInterestRepository locationOfInterestRepository;
  private final SubmissionRepository submissionRepository;
  private final UserRepository userRepository;
  private final Bitmap markerBitmap;
  private final LiveData<String> title;
  private final LiveData<String> subtitle;
  private final LiveData<Boolean> showUploadPendingIcon;
  private final LiveData<Boolean> moveMenuOptionVisible;

  @Inject
  public LocationOfInterestDetailsViewModel(
      MarkerIconFactory markerIconFactory,
      DrawableUtil drawableUtil,
      LocationOfInterestHelper locationOfInterestHelper,
      LocationOfInterestRepository locationOfInterestRepository,
      SubmissionRepository submissionRepository,
      UserRepository userRepository) {
    this.locationOfInterestRepository = locationOfInterestRepository;
    this.submissionRepository = submissionRepository;
    this.userRepository = userRepository;
    this.markerBitmap =
        markerIconFactory.getMarkerBitmap(
            drawableUtil.getColor(R.color.colorGrey600), DEFAULT_LOI_ZOOM_LEVEL, false);
    this.title =
        LiveDataReactiveStreams.fromPublisher(
            selectedLocationOfInterest.map(locationOfInterestHelper::getLabel));
    this.subtitle =
        LiveDataReactiveStreams.fromPublisher(
            selectedLocationOfInterest.map(locationOfInterestHelper::getSubtitle));
    this.moveMenuOptionVisible =
        LiveDataReactiveStreams.fromPublisher(
            selectedLocationOfInterest.map(
                locationOfInterest ->
                    locationOfInterest.map(this::isMoveMenuOptionVisible).orElse(true)));
    Flowable<ImmutableList<LocationOfInterestMutation>> locationOfInterestMutations =
        selectedLocationOfInterest.switchMap(
            this::getIncompleteLocationOfInterestMutationsOnceAndStream);
    Flowable<ImmutableList<SubmissionMutation>> submissionMutations =
        selectedLocationOfInterest.switchMap(this::getIncompleteSubmissionMutationsOnceAndStream);
    this.showUploadPendingIcon =
        LiveDataReactiveStreams.fromPublisher(
            Flowable.combineLatest(
                locationOfInterestMutations,
                submissionMutations,
                (f, o) -> !f.isEmpty() && !o.isEmpty()));
  }

  /**
   * Returns true if the user is {@link Role#OWNER} or {@link Role#SURVEY_ORGANIZER} of the project.
   */
  private boolean isUserAuthorizedToModifyLocationOfInterest(
      LocationOfInterest locationOfInterest) {
    Role role = userRepository.getUserRole(locationOfInterest.getSurveyId());
    return role == Role.OWNER
        || role == Role.SURVEY_ORGANIZER
        || isLocationOfInterestCreatedByUser(locationOfInterest);
  }

  /** Returns true if the {@link User} created the given {@link LocationOfInterest}. */
  private boolean isLocationOfInterestCreatedByUser(LocationOfInterest locationOfInterest) {
    User user = userRepository.getCurrentUser();
    return locationOfInterest.getCreated().getUser().getEmail().equals(user.getEmail());
  }

  /**
   * Returns true if the selected locationOfInterest is of type {@link LocationOfInterestType#POINT}
   * and the user has permissions to modify the locationOfInterest.
   */
  private boolean isMoveMenuOptionVisible(LocationOfInterest locationOfInterest) {
    return isUserAuthorizedToModifyLocationOfInterest(locationOfInterest)
        && locationOfInterest.getType() == LocationOfInterestType.POINT;
  }

  private Flowable<ImmutableList<LocationOfInterestMutation>>
      getIncompleteLocationOfInterestMutationsOnceAndStream(
          Optional<LocationOfInterest> selectedLocationOfInterest) {
    return selectedLocationOfInterest
        .map(
            locationOfInterest ->
                locationOfInterestRepository.getIncompleteLocationOfInterestMutationsOnceAndStream(
                    locationOfInterest.getId()))
        .orElse(Flowable.just(ImmutableList.of()));
  }

  private Flowable<ImmutableList<SubmissionMutation>> getIncompleteSubmissionMutationsOnceAndStream(
      Optional<LocationOfInterest> selectedLocationOfInterest) {
    return selectedLocationOfInterest
        .map(
            locationOfInterest ->
                submissionRepository.getIncompleteSubmissionMutationsOnceAndStream(
                    locationOfInterest.getSurveyId(), locationOfInterest.getId()))
        .orElse(Flowable.just(ImmutableList.of()));
  }

  /**
   * Returns a LiveData that immediately emits the selected LOI (or empty) on if none selected to
   * each new observer.
   */
  public LiveData<Optional<LocationOfInterest>> getSelectedLocationOfInterestOnceAndStream() {
    return LiveDataReactiveStreams.fromPublisher(selectedLocationOfInterest);
  }

  public void onLocationOfInterestSelected(Optional<LocationOfInterest> locationOfInterest) {
    selectedLocationOfInterest.onNext(locationOfInterest);
  }

  public Bitmap getMarkerBitmap() {
    return markerBitmap;
  }

  public LiveData<String> getTitle() {
    return title;
  }

  public LiveData<String> getSubtitle() {
    return subtitle;
  }

  public LiveData<Boolean> isUploadPendingIconVisible() {
    return showUploadPendingIcon;
  }

  public LiveData<Boolean> isMoveMenuOptionVisible() {
    return moveMenuOptionVisible;
  }
}
