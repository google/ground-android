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

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.FeatureType;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;
import java8.util.Optional;
import javax.inject.Inject;

@ActivityScoped
public class AddFeatureDialogFragment extends AbstractDialogFragment {
  private static final String TAG = AddFeatureDialogFragment.class.getSimpleName();
  private final OfflineUuidGenerator uuidGenerator;

  private MaybeSubject<Feature> addFeatureRequestSubject;
  private HomeScreenViewModel homeScreenViewModel;
  private MapContainerViewModel mapContainerViewModel;

  @Inject
  public AddFeatureDialogFragment(OfflineUuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // TODO: Move into new AddFeatureDialogViewModel?
    this.homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
    this.mapContainerViewModel = getViewModel(MapContainerViewModel.class);
  }

  public Maybe<Feature> show(FragmentManager fragmentManager) {
    addFeatureRequestSubject = MaybeSubject.create();
    show(fragmentManager, TAG);
    return addFeatureRequestSubject;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    // TODO: Inject and use custom factory.
    Optional<Project> activeProject = Persistable.getData(homeScreenViewModel.getActiveProject());
    if (!activeProject.isPresent()) {
      addFeatureRequestSubject.onError(new IllegalStateException("No active project"));
      return fail("Could not get active project");
    }
    Optional<Point> cameraPosition =
        Optional.ofNullable(mapContainerViewModel.getCameraPosition().getValue());
    if (!cameraPosition.isPresent()) {
      addFeatureRequestSubject.onError(new IllegalStateException("No camera position"));
      return fail("Could not get camera position");
    }
    return createDialog(activeProject.get(), cameraPosition.get());
  }

  private Dialog createDialog(Project project, Point cameraPosition) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle(R.string.add_feature_select_type_dialog_title);
    builder.setNegativeButton(R.string.add_feature_cancel, (dialog, id) -> onCancel());
    // TODO: Add icons.
    ImmutableList<FeatureType> featureTypes =
        stream(project.getFeatureTypes())
            .sorted((pt1, pt2) -> pt1.getItemLabel().compareTo(pt2.getItemLabel()))
            .collect(toImmutableList());
    String[] items = stream(featureTypes).map(t -> t.getItemLabel()).toArray(String[]::new);
    builder.setItems(
        items,
        (dialog, idx) -> onSelectFeatureType(project, featureTypes.get(idx), cameraPosition));
    return builder.create();
  }

  private void onSelectFeatureType(Project project, FeatureType featureType, Point cameraPosition) {
    // TODO(#9): Move creating a new Feature into the ViewModel or DataRepository. Doing it here
    // for now to avoid conflicting with soon-to-be-merged commits for Issue #24.
    addFeatureRequestSubject.onSuccess(
        Feature.newBuilder()
            .setId(uuidGenerator.generateUuid())
            .setProject(project)
            .setFeatureType(featureType)
            .setPoint(cameraPosition)
            .build());
  }

  private void onCancel() {
    addFeatureRequestSubject.onComplete();
  }
}
