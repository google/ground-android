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

package com.google.android.ground.ui.home.locationofinterestselector;

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
import com.google.android.ground.R;
import com.google.android.ground.databinding.LocationOfInterestSelectorDialogBinding;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.ui.common.AbstractDialogFragment;
import com.google.android.ground.ui.common.EphemeralPopups;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

@AndroidEntryPoint
public class LocationOfInterestSelectorFragment extends AbstractDialogFragment {
  @Inject EphemeralPopups popups;

  private LocationOfInterestSelectorViewModel viewModel;

  @SuppressWarnings("NullAway")
  private LocationOfInterestSelectorDialogBinding binding;

  @Nullable private ArrayAdapter listAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(LocationOfInterestSelectorViewModel.class);
  }

  @NotNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    AlertDialog.Builder dialog = new Builder(getContext());
    dialog.setTitle("Select LocationOfInterest");
    LayoutInflater inflater = getActivity().getLayoutInflater();
    binding = LocationOfInterestSelectorDialogBinding.inflate(inflater);
    listAdapter =
        new ArrayAdapter(
            getContext(), R.layout.location_of_interest_selector_list_item, R.id.loi_name);
    binding.locationOfInterestSelectorListView.setAdapter(listAdapter);
    binding.locationOfInterestSelectorListView.setOnItemClickListener(
        (parent, view, index, id) -> onItemSelected(index));
    dialog.setView(binding.getRoot());
    dialog.setCancelable(true);
    return dialog.create();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    showLocationOfInterestList(viewModel.getLocationsOfInterest());

    return super.onCreateView(inflater, container, savedInstanceState);
  }

  private void showLocationOfInterestList(ImmutableList<LocationOfInterest> locationsOfInterest) {
    binding.listLoadingProgressBar.setVisibility(View.GONE);
    checkNotNull(listAdapter, "listAdapter was null when attempting to show survey list");

    listAdapter.clear();
    stream(locationsOfInterest).map(viewModel::getListItemText).forEach(listAdapter::add);

    binding.locationOfInterestSelectorListView.setVisibility(View.VISIBLE);
  }

  private void onItemSelected(int index) {
    dismiss();
    viewModel.onItemClick(index);
  }
}
