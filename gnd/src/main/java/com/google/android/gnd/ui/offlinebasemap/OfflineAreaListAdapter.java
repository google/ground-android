/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.offlinebasemap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.databinding.OfflineBaseMapListItemBinding;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.collect.ImmutableList;

class OfflineAreaListAdapter extends RecyclerView.Adapter<OfflineAreaListAdapter.ViewHolder> {
  private final Navigator navigator;
  private ImmutableList<OfflineArea> offlineAreas;

  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public OfflineBaseMapListItemBinding binding;
    private ImmutableList<OfflineArea> areas;
    private final Navigator navigator;

    ViewHolder(
        OfflineBaseMapListItemBinding binding,
        ImmutableList<OfflineArea> areas,
        Navigator navigator) {
      super(binding.getRoot());
      this.binding = binding;
      this.areas = areas;
      this.navigator = navigator;
      binding.offlineAreaName.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
      if (areas.size() > 0) {
        String id = areas.get(getAdapterPosition()).getId();
        navigator.navigate(OfflineAreasFragmentDirections.viewOfflineArea(id));
      }
    }
  }

  OfflineAreaListAdapter(Navigator navigator) {
    offlineAreas = ImmutableList.of();
    this.navigator = navigator;
  }

  @NonNull
  @Override
  public OfflineAreaListAdapter.ViewHolder onCreateViewHolder(
      @NonNull ViewGroup parent, int viewType) {
    OfflineBaseMapListItemBinding offlineAreasListItemBinding =
        OfflineBaseMapListItemBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);

    return new ViewHolder(offlineAreasListItemBinding, offlineAreas, navigator);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
    viewHolder.areas = offlineAreas;
    viewHolder.binding.offlineAreaName.setText(offlineAreas.get(position).getName());
  }

  @Override
  public int getItemCount() {
    return offlineAreas.size();
  }

  void update(ImmutableList<OfflineArea> offlineAreas) {
    this.offlineAreas = offlineAreas;
    notifyDataSetChanged();
  }
}
