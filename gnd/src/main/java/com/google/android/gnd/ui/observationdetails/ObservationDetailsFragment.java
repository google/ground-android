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

package com.google.android.gnd.ui.observationdetails;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.ObservationDetailsFieldBinding;
import com.google.android.gnd.databinding.ObservationDetailsFragBinding;
import com.google.android.gnd.databinding.PhotoFieldBinding;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.editobservation.PhotoFieldViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@AndroidEntryPoint
public class ObservationDetailsFragment extends AbstractFragment {

  @Inject FeatureHelper featureHelper;
  @Inject Navigator navigator;
  @Inject EphemeralPopups popups;

  private ObservationDetailsViewModel viewModel;
  private ObservationDetailsFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ObservationDetailsFragmentArgs args = getObservationDetailFragmentArgs();
    viewModel = getViewModel(ObservationDetailsViewModel.class);
    viewModel.observations.observe(this, this::onUpdate);
    viewModel.loadObservationDetails(args);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    binding = ObservationDetailsFragBinding.inflate(inflater, container, false);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(binding.observationDetailsToolbar, false);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.observation_details_menu, menu);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
  }

  private void onUpdate(Loadable<Observation> observation) {
    switch (observation.getState()) {
      case LOADED:
        observation.value().ifPresent(this::showObservation);
        break;
      case NOT_FOUND:
      case ERROR:
        // TODO: Replace w/error view?
        Timber.e("Failed to load observation");
        popups.showError();
        break;
      default:
        Timber.e("Unhandled state: %s", observation.getState());
        break;
    }
  }

  private void showObservation(Observation observation) {
    binding.observationDetailsLayout.removeAllViews();
    for (Element element : observation.getForm().getElementsSorted()) {
      if (element.getType() == Element.Type.FIELD) {
        addField(element.getField(), observation);
      }
    }
  }

  private void addField(Field field, Observation observation) {
    ObservationDetailsFieldBinding fieldBinding =
        ObservationDetailsFieldBinding.inflate(getLayoutInflater());
    fieldBinding.setField(field);
    fieldBinding.setLifecycleOwner(this);
    binding.observationDetailsLayout.addView(fieldBinding.getRoot());

    observation
        .getResponses()
        .getResponse(field.getId())
        .ifPresent(
            response -> {
              if (field.getType() == Type.PHOTO) {
                addPhotoField((ViewGroup) fieldBinding.getRoot(), field, response);
              } else {
                fieldBinding.fieldValue.setText(response.getDetailsText(field));
              }
            });
  }

  private void addPhotoField(ViewGroup container, Field field, Response response) {
    PhotoFieldBinding photoFieldBinding = PhotoFieldBinding.inflate(getLayoutInflater());
    PhotoFieldViewModel photoFieldViewModel = viewModelFactory.create(PhotoFieldViewModel.class);
    photoFieldBinding.setLifecycleOwner(this);
    photoFieldBinding.setViewModel(photoFieldViewModel);
    photoFieldViewModel.updateField(response, field);
    container.addView(photoFieldBinding.getRoot());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ObservationDetailsFragmentArgs args = getObservationDetailFragmentArgs();
    String projectId = args.getProjectId();
    String featureId = args.getFeatureId();
    String observationId = args.getObservationId();

    switch (item.getItemId()) {
      case R.id.edit_observation_menu_item:
        // This is required to prevent menu from reappearing on back.
        getActivity().closeOptionsMenu();
        navigator.navigate(
            ObservationDetailsFragmentDirections.editObservation(
                projectId, featureId, observationId));
        return true;
      case R.id.delete_observation_menu_item:
        viewModel
            .deleteCurrentObservation(projectId, featureId, observationId)
            .as(autoDisposable(this))
            .subscribe(() -> navigator.navigateUp());
        return true;
      default:
        return false;
    }
  }

  public String getFeatureTitle(@Nullable Optional<Feature> feature) {
    return feature == null ? "" : featureHelper.getTitle(feature);
  }

  public String getFeatureSubtitle(@Nullable Optional<Feature> feature) {
    return feature == null ? "" : featureHelper.getCreatedBy(feature);
  }

  private ObservationDetailsFragmentArgs getObservationDetailFragmentArgs() {
    return ObservationDetailsFragmentArgs.fromBundle(getArguments());
  }
}
