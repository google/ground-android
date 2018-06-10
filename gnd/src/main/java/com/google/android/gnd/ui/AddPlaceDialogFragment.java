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

package com.google.android.gnd.ui;

import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.ProjectState;
import com.google.android.gnd.ui.common.GndDialogFragment;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import com.google.common.collect.ImmutableList;
import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;
import java8.util.Optional;
import javax.inject.Inject;

public class AddPlaceDialogFragment extends GndDialogFragment {
  private static final String TAG = AddPlaceDialogFragment.class.getSimpleName();

  private MaybeSubject<AddPlaceRequest> addPlaceRequestSubject;

  @Inject
  public AddPlaceDialogFragment() {
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  public Maybe<AddPlaceRequest> show(FragmentManager fragmentManager) {
    addPlaceRequestSubject = MaybeSubject.create();
    show(fragmentManager, TAG);
    return addPlaceRequestSubject;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MainViewModel mainViewModel =
      ViewModelProviders.of(getParentFragment()).get(MainViewModel.class);
    ProjectState projectState = mainViewModel.getProjectState().getValue();
    Point location = mainViewModel.getShowAddPlaceDialogRequests().getValue();
    Optional<Project> activeProject = projectState.getActiveProject();
    if (activeProject.isPresent()) {
      return createDialog(activeProject.get(), location);
    } else {
      // TODO: Handle this error upstream.
      addPlaceRequestSubject.onError(new IllegalStateException("No project loaded"));
      return null;
    }
  }

  private Dialog createDialog(Project project, Point location) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle(R.string.add_place_select_type_dialog_title);
    builder.setNegativeButton(R.string.add_place_cancel, (dialog, id) -> onCancel());
    // TODO: Add icons.
    ImmutableList<PlaceType> placeTypes = project.getPlaceTypes();
    String[] items = stream(placeTypes).map(t -> t.getListHeading()).toArray(String[]::new);
    builder.setItems(
      items, (dialog, idx) -> onSelectPlaceType(project, placeTypes.get(idx), location));
    return builder.create();
  }

  private void onSelectPlaceType(Project project, PlaceType placeType, Point location) {
    addPlaceRequestSubject.onSuccess(new AddPlaceRequest(project, location, placeType));
  }

  private void onCancel() {
    addPlaceRequestSubject.onComplete();
  }

  public static class AddPlaceRequest {
    private final Project project;
    private final Point location;
    private final PlaceType placeType;

    public AddPlaceRequest(Project project, Point location, PlaceType placeType) {
      this.project = project;
      this.location = location;
      this.placeType = placeType;
    }

    public Project getProject() {
      return project;
    }

    public Point getLocation() {
      return location;
    }

    public PlaceType getPlaceType() {
      return placeType;
    }
  }
}
