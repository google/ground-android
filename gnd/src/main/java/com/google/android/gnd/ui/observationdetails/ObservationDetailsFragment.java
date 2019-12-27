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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.ObservationDetailsFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.repository.Loadable;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import javax.inject.Inject;

@ActivityScoped
public class ObservationDetailsFragment extends AbstractFragment {
  private static final String TAG = ObservationDetailsFragment.class.getSimpleName();

  @Inject Navigator navigator;

  @BindView(R.id.record_details_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.record_details_layout)
  LinearLayout recordDetailsLayout;

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
    viewModel.records.observe(this, this::onUpdate);
    viewModel.loadRecordDetails(args);
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

  private void onUpdate(Loadable<Observation> record) {
    switch (record.getState()) {
      case LOADED:
        record.value().ifPresent(this::showRecord);
        break;
      case NOT_FOUND:
      case ERROR:
        // TODO: Replace w/error view?
        Log.e(TAG, "Failed to load observation");
        EphemeralPopups.showError(getContext());
        break;
    }
  }

  private void showRecord(Observation observation) {
    recordDetailsLayout.removeAllViews();
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
    FieldViewHolder fieldViewHolder = FieldViewHolder.newInstance(getLayoutInflater());
    fieldViewHolder.setLabel(field.getLabel());
    observation
        .getResponses()
        .getResponse(field.getId())
        .map(r -> r.getDetailsText(field))
        .ifPresent(fieldViewHolder::setValue);
    recordDetailsLayout.addView(fieldViewHolder.getRoot());
  }

  // TODO: Extract into outer class.
  static class FieldViewHolder {
    private ViewGroup root;

    @BindView(R.id.field_label)
    TextView labelView;

    @BindView(R.id.field_value)
    TextView valueView;

    FieldViewHolder(ViewGroup root) {
      this.root = root;
    }

    static FieldViewHolder newInstance(LayoutInflater inflater) {
      ViewGroup root = (ViewGroup) inflater.inflate(R.layout.observation_details_field, null);
      FieldViewHolder holder = new FieldViewHolder(root);
      ButterKnife.bind(holder, root);
      return holder;
    }

    void setLabel(String label) {
      labelView.setText(label);
    }

    void setValue(String value) {
      valueView.setText(value);
    }

    public ViewGroup getRoot() {
      return root;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.edit_record_menu_item:
        // This is required to prevent menu from reappearing on back.
        getActivity().closeOptionsMenu();
        ObservationDetailsFragmentArgs args = getObservationDetailFragmentArgs();
        navigator.editObservation(args.getProjectId(), args.getFeatureId(), args.getRecordId());
        return true;
      case R.id.delete_record_menu_item:
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
