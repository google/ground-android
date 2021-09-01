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

package com.google.android.gnd.ui.home.featuredetails;

import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.util.DrawableUtil;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class FeatureDetailsViewModel extends ViewModel {

  @Hot
  private final FlowableProcessor<Optional<Feature>> selectedFeature =
      BehaviorProcessor.createDefault(Optional.empty());

  private final FeatureRepository featureRepository;
  private final ObservationRepository observationRepository;
  private final UserRepository userRepository;
  private final Bitmap markerBitmap;
  private final LiveData<String> title;
  private final LiveData<String> subtitle;
  private final LiveData<Boolean> showUploadPendingIcon;
  private final LiveData<Boolean> moveMenuOptionVisible;
  private final LiveData<Boolean> deleteMenuOptionVisible;

  @Inject
  public FeatureDetailsViewModel(
      MarkerIconFactory markerIconFactory,
      DrawableUtil drawableUtil,
      FeatureHelper featureHelper,
      FeatureRepository featureRepository,
      ObservationRepository observationRepository,
      UserRepository userRepository) {
    this.featureRepository = featureRepository;
    this.observationRepository = observationRepository;
    this.userRepository = userRepository;
    this.markerBitmap =
        markerIconFactory.getMarkerBitmap(drawableUtil.getColor(R.color.colorGrey600));
    this.title =
        LiveDataReactiveStreams.fromPublisher(selectedFeature.map(featureHelper::getLabel));
    this.subtitle =
        LiveDataReactiveStreams.fromPublisher(selectedFeature.map(featureHelper::getSubtitle));
    this.moveMenuOptionVisible =
        LiveDataReactiveStreams.fromPublisher(
            selectedFeature.map(
                feature -> feature.map(this::isMoveMenuOptionVisible).orElse(true)));
    this.deleteMenuOptionVisible =
        LiveDataReactiveStreams.fromPublisher(
            selectedFeature.map(
                feature -> feature.map(this::isDeleteMenuOptionVisible).orElse(true)));
    Flowable<ImmutableList<FeatureMutation>> featureMutations =
        selectedFeature.switchMap(this::getIncompleteFeatureMutationsOnceAndStream);
    Flowable<ImmutableList<ObservationMutation>> observationMutations =
        selectedFeature.switchMap(this::getIncompleteObservationMutationsOnceAndStream);
    this.showUploadPendingIcon =
        LiveDataReactiveStreams.fromPublisher(
            Flowable.combineLatest(
                featureMutations, observationMutations, (f, o) -> !f.isEmpty() && !o.isEmpty()));
  }

  /** Returns true if the user is {@link Role#OWNER} or {@link Role#MANAGER} of the project. */
  private boolean isUserAuthorizedToModifyFeature(Feature feature) {
    Role role = userRepository.getUserRole(feature.getProject());
    return role == Role.OWNER || role == Role.MANAGER || isFeatureCreatedByUser(feature);
  }

  /** Returns true if the {@link User} created the given {@link Feature}. */
  private boolean isFeatureCreatedByUser(Feature feature) {
    User user = userRepository.getCurrentUser();
    return feature.getCreated().getUser().getEmail().equals(user.getEmail());
  }

  /**
   * Returns true if the selected feature is of type {@link FeatureType#POINT} and the user has
   * permissions to modify the feature.
   */
  private boolean isMoveMenuOptionVisible(Feature feature) {
    return isUserAuthorizedToModifyFeature(feature) && feature.isPoint();
  }

  /** Returns true if the user has permissions to modify the feature. */
  private boolean isDeleteMenuOptionVisible(Feature feature) {
    return isUserAuthorizedToModifyFeature(feature);
  }

  private Flowable<ImmutableList<FeatureMutation>> getIncompleteFeatureMutationsOnceAndStream(
      Optional<Feature> selectedFeature) {
    return selectedFeature
        .map(
            feature ->
                featureRepository.getIncompleteFeatureMutationsOnceAndStream(feature.getId()))
        .orElse(Flowable.just(ImmutableList.of()));
  }

  private Flowable<ImmutableList<ObservationMutation>>
      getIncompleteObservationMutationsOnceAndStream(Optional<Feature> selectedFeature) {
    return selectedFeature
        .map(
            feature ->
                observationRepository.getIncompleteObservationMutationsOnceAndStream(
                    feature.getProject(), feature.getId()))
        .orElse(Flowable.just(ImmutableList.of()));
  }

  /**
   * Returns a LiveData that immediately emits the selected feature (or empty) on if none selected
   * to each new observer.
   */
  public LiveData<Optional<Feature>> getSelectedFeatureOnceAndStream() {
    return LiveDataReactiveStreams.fromPublisher(selectedFeature);
  }

  public void onFeatureSelected(Optional<Feature> feature) {
    selectedFeature.onNext(feature);
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

  public LiveData<Boolean> isDeleteMenuOptionVisible() {
    return deleteMenuOptionVisible;
  }
}
