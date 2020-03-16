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
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.ObservationDetailsFieldBinding;
import com.google.android.gnd.databinding.ObservationDetailsFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.squareup.picasso.Picasso;
import javax.inject.Inject;
import timber.log.Timber;

@ActivityScoped
public class ObservationDetailsFragment extends AbstractFragment {

  @Inject Navigator navigator;
  @Inject StorageManager storageManager;
  @Inject Schedulers schedulers;

  @BindView(R.id.observation_details_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.observation_details_layout)
  LinearLayout observationDetailsLayout;

  private ObservationDetailsViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ObservationDetailsFragmentArgs args = getObservationDetailFragmentArgs();
    viewModel = getViewModel(ObservationDetailsViewModel.class);
    // TODO: Move toolbar setting logic into the ViewModel once we have
    // determined the fate of the toolbar.
    viewModel.toolbarTitle.observe(this, this::setToolbarTitle);
    viewModel.toolbarSubtitle.observe(this, this::setToolbarSubtitle);
    viewModel.observations.observe(this, this::onUpdate);
    viewModel.loadObservationDetails(args);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    ObservationDetailsFragBinding binding =
        ObservationDetailsFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(toolbar, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.observation_details_menu, menu);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
  }

  private void setToolbarTitle(String title) {
    if (toolbar != null) {
      toolbar.setTitle(title);
    }
  }

  private void setToolbarSubtitle(String subtitle) {
    if (toolbar != null) {
      toolbar.setSubtitle(subtitle);
    }
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
        EphemeralPopups.showError(getContext());
        break;
      default:
        Timber.e("Unhandled state: %s", observation.getState());
        break;
    }
  }

  private void showObservation(Observation observation) {
    observationDetailsLayout.removeAllViews();
    for (Element element : observation.getForm().getElements()) {
      switch (element.getType()) {
        case FIELD:
          addField(element.getField(), observation);
          break;
        default:
      }
    }
  }

  private void addField(Field field, Observation observation) {
    ObservationDetailsFieldBinding binding =
        ObservationDetailsFieldBinding.inflate(getLayoutInflater());
    binding.setField(field);
    binding.setLifecycleOwner(this);
    observationDetailsLayout.addView(binding.getRoot());

    observation
        .getResponses()
        .getResponse(field.getId())
        .map(r -> r.getDetailsText(field))
        .ifPresent(
            value -> {
              if (field.getType().equals(Type.PHOTO)) {
                binding.fieldValue.setVisibility(View.GONE);
                binding.imagePreview.setVisibility(View.VISIBLE);

                storageManager
                    .loadUriFromDestinationPath(value)
                    .subscribeOn(schedulers.io())
                    .observeOn(schedulers.ui())
                    .as(autoDisposable(this))
                    .subscribe(
                        uri ->
                            Picasso.get()
                                .load(uri)
                                .placeholder(R.drawable.ic_photo_grey_600_24dp)
                                .into(binding.imagePreview));

              } else {
                binding.fieldValue.setText(value);
              }
            });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.edit_observation_menu_item:
        // This is required to prevent menu from reappearing on back.
        getActivity().closeOptionsMenu();
        ObservationDetailsFragmentArgs args = getObservationDetailFragmentArgs();
        navigator.editObservation(
            args.getProjectId(), args.getFeatureId(), args.getObservationId());
        return true;
      case R.id.delete_observation_menu_item:
        // TODO: Implement delete observation.
        return true;
      default:
        return false;
    }
  }

  private ObservationDetailsFragmentArgs getObservationDetailFragmentArgs() {
    return ObservationDetailsFragmentArgs.fromBundle(getArguments());
  }
}
