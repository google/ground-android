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

package com.google.android.gnd.ui.browse;

import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.browse.mapcontainer.MapContainerViewModel;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import com.google.common.collect.ImmutableList;
import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;
import java8.util.Optional;
import javax.inject.Inject;

public class AddPlaceDialogFragment extends AbstractDialogFragment {
  private static final String TAG = AddPlaceDialogFragment.class.getSimpleName();

  @Inject
  ViewModelFactory viewModelFactory;

  private MaybeSubject<Place> addPlaceRequestSubject;
  private BrowseViewModel browseViewModel;
  private MapContainerViewModel mapContainerViewModel;

  @Inject
  public AddPlaceDialogFragment() {}

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  protected void obtainViewModels() {
    // TODO: Move into new AddPlaceDialogViewModel?
    this.browseViewModel =
      ViewModelProviders.of(getActivity(), viewModelFactory).get(BrowseViewModel.class);
    this.mapContainerViewModel =
      ViewModelProviders.of(getActivity(), viewModelFactory).get(MapContainerViewModel.class);
  }

  public Maybe<Place> show(FragmentManager fragmentManager) {
    addPlaceRequestSubject = MaybeSubject.create();
    show(fragmentManager, TAG);
    return addPlaceRequestSubject;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // TODO: Inject and use custom factory.
    Optional<Project> activeProject = Resource.getData(browseViewModel.getActiveProject());
    Optional<Point> cameraPosition =
      Optional.ofNullable(mapContainerViewModel.getCameraPosition().getValue());
    if (!activeProject.isPresent()) {
      addPlaceRequestSubject.onError(new IllegalStateException("No active project"));
      return fail("Could not get active project");
    }
    if (!cameraPosition.isPresent()) {
      addPlaceRequestSubject.onError(new IllegalStateException("No camera position"));
      return fail("Could not get camera position");
    }
    return createDialog(activeProject.get(), cameraPosition.get());
  }

  private Dialog createDialog(Project project, Point cameraPosition) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle(R.string.add_place_select_type_dialog_title);
    builder.setNegativeButton(R.string.add_place_cancel, (dialog, id) -> onCancel());
    // TODO: Add icons.
    ImmutableList<PlaceType> placeTypes = project.getPlaceTypes();
    String[] items =
      stream(placeTypes).map(t -> t.getItemLabel()).sorted().toArray(String[]::new);
    builder.setItems(
      items, (dialog, idx) -> onSelectPlaceType(project, placeTypes.get(idx), cameraPosition));
    return builder.create();
  }

  private void onSelectPlaceType(Project project, PlaceType placeType, Point cameraPosition) {
    addPlaceRequestSubject.onSuccess(
      Place.newBuilder()
           .setProject(project)
           .setPlaceType(placeType)
           .setPoint(cameraPosition)
           .build());
  }

  private void onCancel() {
    addPlaceRequestSubject.onComplete();
  }
}
