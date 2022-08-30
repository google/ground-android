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

package com.google.android.ground.ui.home.locationofinterestdetails;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.ground.databinding.SubmissionListItemBinding;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.common.ViewModelFactory;
import java.util.Collections;
import java.util.List;

// TODO: Consider passing in ViewModel and using DataBinding like todoapp example.
class SubmissionListAdapter extends RecyclerView.Adapter<SubmissionListItemViewHolder> {

  private final ViewModelFactory viewModelFactory;
  private List<Submission> submissionList;

  @Hot(replays = true)
  private final MutableLiveData<Submission> itemClicks = new MutableLiveData<>();

  public SubmissionListAdapter(ViewModelFactory viewModelFactory) {
    this.viewModelFactory = viewModelFactory;
    submissionList = Collections.emptyList();
  }

  @NonNull
  @Override
  public SubmissionListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    SubmissionListItemBinding itemBinding =
        SubmissionListItemBinding.inflate(inflater, parent, false);
    // Note: Before using Android Data Binding for this component, holder must implement
    // LifecycleOwner and be passed to itemBinding.setLifecycleOwner().
    return new SubmissionListItemViewHolder(itemBinding);
  }

  @Override
  public void onBindViewHolder(@NonNull SubmissionListItemViewHolder holder, int position) {
    SubmissionListItemViewModel viewModel =
        viewModelFactory.create(SubmissionListItemViewModel.class);
    viewModel.setSubmission(submissionList.get(position));
    viewModel.setSubmissionCallback(submission -> itemClicks.postValue(submission));
    holder.bind(viewModel, submissionList.get(position));
  }

  @Override
  public int getItemCount() {
    return submissionList.size();
  }

  LiveData<Submission> getItemClicks() {
    return itemClicks;
  }

  void clear() {
    this.submissionList = Collections.emptyList();
    notifyDataSetChanged();
  }

  void update(List<Submission> submissionList) {
    this.submissionList = submissionList;
    notifyDataSetChanged();
  }
}
