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

package com.google.android.gnd.ui.home.mapcontainer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.google.android.gnd.databinding.MapTypeItemBinding;
import com.google.android.gnd.ui.home.mapcontainer.MapTypeItem;
import java.util.List;

/**
 * Adapter for displaying map types.
 */
public class MapTypesAdapter extends BaseAdapter {
  private final List<MapTypeItem> mapTypes;

  MapTypesAdapter(List<MapTypeItem> mapTypes) {
    this.mapTypes = mapTypes;
  }

  @Override
  public int getCount() {
    return mapTypes.size();
  }

  @Override
  public Object getItem(int i) {
    return mapTypes.get(i).getType();
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    MapTypeItemBinding binding =
        MapTypeItemBinding.inflate(LayoutInflater.from(viewGroup.getContext()));
    MapTypeItem mapType = mapTypes.get(i);
    binding.mapTypeTextView.setChecked(mapType.isSelected());
    binding.mapTypeTextView.setText(mapType.getLabel());
    return binding.getRoot();
  }
}
