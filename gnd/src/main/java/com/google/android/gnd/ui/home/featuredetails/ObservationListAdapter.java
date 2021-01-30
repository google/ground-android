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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.databinding.ObservationListItemBinding;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.ViewModelFactory;
import java.util.Collections;
import java.util.List;

// TODO: Consider passing in ViewModel and using DataBinding like todoapp example.
class ObservationListAdapter extends RecyclerView.Adapter<ObservationListItemViewHolder> {

  private final ViewModelFactory viewModelFactory;
  private List<Observation> observationList;

  @Hot(replays = true)
  private MutableLiveData<Observation> itemClicks = new MutableLiveData<>();

  public ObservationListAdapter(ViewModelFactory viewModelFactory) {
    this.viewModelFactory = viewModelFactory;
    observationList = Collections.emptyList();
  }

  @NonNull
  @Override
  public ObservationListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    ObservationListItemBinding itemBinding =
        ObservationListItemBinding.inflate(inflater, parent, false);
    // Note: Before using Android Data Binding for this component, holder must implement
    // LifecycleOwner and be passed to itemBinding.setLifecycleOwner().
    return new ObservationListItemViewHolder(itemBinding);
  }

  @Override
  public void onBindViewHolder(@NonNull ObservationListItemViewHolder holder, int position) {
    ObservationViewModel viewModel = viewModelFactory.create(ObservationViewModel.class);
    viewModel.setObservation(observationList.get(position));
    viewModel.setObservationCallback(observation -> itemClicks.postValue(observation));
    holder.bind(viewModel, observationList.get(position));
  }

  @Override
  public int getItemCount() {
    return observationList.size();
  }

  LiveData<Observation> getItemClicks() {
    return itemClicks;
  }

  void clear() {
    this.observationList = Collections.emptyList();
    notifyDataSetChanged();
  }

  void update(List<Observation> observationList) {
    this.observationList = observationList;
    notifyDataSetChanged();
  }
}
