/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.ui.home.featureselector;

import static com.google.common.base.Preconditions.checkNotNull;
import static java8.util.stream.StreamSupport.stream;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.FeatureSelectorDialogBinding;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

@AndroidEntryPoint
public class FeatureSelectorFragment extends AbstractDialogFragment {
  @Inject EphemeralPopups popups;

  private FeatureSelectorViewModel viewModel;

  @SuppressWarnings("NullAway")
  private FeatureSelectorDialogBinding binding;

  @Nullable private ArrayAdapter listAdapter;

  @Inject
  public FeatureSelectorFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(FeatureSelectorViewModel.class);
  }

  @NotNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    AlertDialog.Builder dialog = new Builder(getContext());
    dialog.setTitle("Select Feature");
    LayoutInflater inflater = getActivity().getLayoutInflater();
    binding = FeatureSelectorDialogBinding.inflate(inflater);
    listAdapter =
        new ArrayAdapter(getContext(), R.layout.feature_selector_list_item, R.id.feature_name);
    binding.featureSelectorListView.setAdapter(listAdapter);
    binding.featureSelectorListView.setOnItemClickListener(
        (parent, view, index, id) -> onItemSelected(index));
    dialog.setView(binding.getRoot());
    dialog.setCancelable(true);
    return dialog.create();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    showFeatureList(viewModel.getFeatures());

    return super.onCreateView(inflater, container, savedInstanceState);
  }

  private void showFeatureList(ImmutableList<Feature> features) {
    binding.listLoadingProgressBar.setVisibility(View.GONE);
    checkNotNull(listAdapter, "listAdapter was null when attempting to show project list");

    listAdapter.clear();
    stream(features).map(viewModel::getListItemText).forEach(listAdapter::add);

    binding.featureSelectorListView.setVisibility(View.VISIBLE);
  }

  private void onItemSelected(int index) {
    dismiss();
    viewModel.onItemClick(index);
  }
}
