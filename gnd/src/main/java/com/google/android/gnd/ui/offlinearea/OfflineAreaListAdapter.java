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

import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.OfflineAreasListItemBinding;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java8.util.Optional;
import timber.log.Timber;

class OfflineAreaListAdapter extends RecyclerView.Adapter<OfflineAreaListAdapter.ViewHolder> {

  private final Navigator navigator;
  private ImmutableList<OfflineArea> offlineAreas;

  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public OfflineAreasListItemBinding binding;
    public int position;
    private ImmutableList<OfflineArea> areas;
    private final Navigator navigator;
    private final Geocoder geocoder;

    ViewHolder(
        OfflineAreasListItemBinding binding,
        ImmutableList<OfflineArea> areas,
        Navigator navigator) {
      super(binding.getRoot());
      this.binding = binding;
      this.areas = areas;
      this.navigator = navigator;
      this.geocoder = new Geocoder(binding.getRoot().getContext());
      binding.offlineAreaName.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
      if (areas.size() > 0) {
        String id = areas.get(position).getId();
        navigator.showOfflineAreaViewer(id);
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
    OfflineAreasListItemBinding offlineAreasListItemBinding =
        OfflineAreasListItemBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);

    return new ViewHolder(offlineAreasListItemBinding, offlineAreas, navigator);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
    String areaName =
        viewHolder.binding.getRoot().getContext().getString(R.string.offline_areas_unknown_area);

    OfflineArea area = offlineAreas.get(position);
    String id = area.getId();
    LatLng center = area.getBounds().getCenter();

    try {
      List<Address> addresses =
          viewHolder.geocoder.getFromLocation(center.latitude, center.longitude, 1);

      if (addresses.isEmpty()) {
        throw new Exception("No address found for area.");
      }

      Address address = addresses.get(0);

      areaName =
          Optional.ofNullable(address.getFeatureName())
              .or(() -> Optional.ofNullable(address.getLocality()))
              .or(() -> Optional.ofNullable(address.getSubAdminArea()))
              .or(() -> Optional.ofNullable(address.getAdminArea()))
              .or(() -> Optional.ofNullable(address.getCountryName()))
              .orElse(areaName);
    } catch (Exception e) {
      Timber.e(
          e,
          "Couldn't get address for area: %s, lat: %f, lng: %f",
          id,
          center.latitude,
          center.longitude);
    } finally {
      viewHolder.binding.offlineAreaName.setText(areaName);
      viewHolder.position = position;
    }
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
