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

package com.google.android.ground.ui.syncstatus;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.ground.databinding.SyncStatusListItemBinding;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.mutation.LocationOfInterestMutation;
import com.google.android.ground.model.mutation.Mutation;
import com.google.android.ground.ui.common.LocationOfInterestHelper;
import com.google.common.collect.ImmutableList;
import java.text.DateFormat;
import java8.util.Optional;

class SyncStatusListAdapter extends RecyclerView.Adapter<SyncStatusViewHolder> {

  private final LocationOfInterestHelper locationOfInterestHelper;
  private ImmutableList<Pair<LocationOfInterest, Mutation>> mutations;
  private final DateFormat dateFormat;
  private final DateFormat timeFormat;

  SyncStatusListAdapter(
      @Nullable Context context, LocationOfInterestHelper locationOfInterestHelper) {
    this.mutations = ImmutableList.of();
    this.dateFormat = android.text.format.DateFormat.getDateFormat(context);
    this.timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    this.locationOfInterestHelper = locationOfInterestHelper;
  }

  @NonNull
  @Override
  public SyncStatusViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new SyncStatusViewHolder(
        SyncStatusListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(SyncStatusViewHolder viewHolder, int position) {
    // TODO: i18n; add user friendly names.
    // TODO: Use data binding.
    // TODO(#876): Improve L&F and layout.

    Pair<LocationOfInterest, Mutation> pair = mutations.get(position);
    LocationOfInterest locationOfInterest = pair.first;
    Mutation mutation = pair.second;
    String text =
        new StringBuilder()
            .append(mutation.getType())
            .append(' ')
            .append(
                mutation instanceof LocationOfInterestMutation
                    ? "LocationOfInterest"
                    : "Submission")
            .append(' ')
            .append(dateFormat.format(mutation.getClientTimestamp()))
            .append(' ')
            .append(timeFormat.format(mutation.getClientTimestamp()))
            .append('\n')
            .append(locationOfInterestHelper.getLabel(Optional.of(locationOfInterest)))
            .append('\n')
            .append(locationOfInterestHelper.getSubtitle(Optional.of(locationOfInterest)))
            .append('\n')
            .append("Sync ")
            .append(mutation.getSyncStatus())
            .toString();
    viewHolder.binding.syncStatusText.setText(text);
  }

  @Override
  public int getItemCount() {
    return mutations.size();
  }

  void update(ImmutableList<Pair<LocationOfInterest, Mutation>> mutations) {
    this.mutations = mutations;
    notifyDataSetChanged();
  }
}
