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

package com.google.android.gnd.ui.offlinearea;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.databinding.OfflineAreasListItemBinding;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

class OfflineAreaListAdapter extends RecyclerView.Adapter<OfflineAreaListAdapter.ViewHolder> {
  private ImmutableList<OfflineArea> offlineAreas;

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public OfflineAreasListItemBinding binding;

    ViewHolder(OfflineAreasListItemBinding b) {
      super(b.getRoot());
      binding = b;
    }
  }

  OfflineAreaListAdapter() {
    offlineAreas = ImmutableList.of();
  }

  @NotNull
  @Override
  public OfflineAreaListAdapter.ViewHolder onCreateViewHolder(
      @NotNull ViewGroup parent, int viewType) {
    OfflineAreasListItemBinding o =
        OfflineAreasListItemBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);

    return new ViewHolder(o);
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position) {
    viewHolder.binding.offlineAreaName.setText(offlineAreas.get(position).getId());
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
