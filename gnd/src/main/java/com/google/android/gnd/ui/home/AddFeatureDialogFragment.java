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
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;
import java8.util.Optional;
import javax.inject.Inject;

@AndroidEntryPoint
public class AddFeatureDialogFragment extends AbstractDialogFragment {
  private static final String TAG = AddFeatureDialogFragment.class.getSimpleName();
  private final OfflineUuidGenerator uuidGenerator;
  private final AuthenticationManager authManager;

  private MaybeSubject<Feature> addFeatureRequestSubject;
  private HomeScreenViewModel homeScreenViewModel;
  private MapContainerViewModel mapContainerViewModel;

  @Inject
  public AddFeatureDialogFragment(
      OfflineUuidGenerator uuidGenerator, AuthenticationManager authManager) {
    this.uuidGenerator = uuidGenerator;
    this.authManager = authManager;
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
    Optional<Project> activeProject = Loadable.getValue(homeScreenViewModel.getActiveProject());
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
    builder.setNegativeButton(R.string.cancel, (dialog, id) -> onCancel());
    // TODO: Add icons.
    ImmutableList<Layer> layers =
        stream(project.getLayers())
            .sorted((pt1, pt2) -> pt1.getName().compareTo(pt2.getName()))
            .collect(toImmutableList());
    String[] items = stream(layers).map(t -> t.getName()).toArray(String[]::new);
    builder.setItems(
        items, (dialog, idx) -> onSelectLayer(project, layers.get(idx), cameraPosition));
    return builder.create();
  }

  private void onSelectLayer(Project project, Layer layer, Point cameraPosition) {
    AuditInfo auditInfo = AuditInfo.now(authManager.getCurrentUser());
    // TODO(#9): Move creating a new Feature into the ViewModel or ProjectRepository. Doing it here
    // for now to avoid conflicting with soon-to-be-merged commits for Issue #24.
    addFeatureRequestSubject.onSuccess(
        Feature.newBuilder()
            .setId(uuidGenerator.generateUuid())
            .setProject(project)
            .setLayer(layer)
            .setPoint(cameraPosition)
            .setCreated(auditInfo)
            .setLastModified(auditInfo)
            .build());
  }

  private void onCancel() {
    addFeatureRequestSubject.onComplete();
  }
}
