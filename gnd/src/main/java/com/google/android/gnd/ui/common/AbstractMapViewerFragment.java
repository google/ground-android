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

package com.google.android.gnd.ui.common;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.map.MapProvider.MapType;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import javax.inject.Inject;

/** Injects a {@link MapFragment} in the container with id "map". */
public class AbstractMapViewerFragment extends AbstractFragment {

  @Inject MapProvider mapProvider;

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mapProvider
        .createFragment()
        .attachToFragment(this, R.id.map, adapter -> mapProvider.setMapAdapter(adapter));
  }

  protected Single<MapAdapter> getMapAdapter() {
    return mapProvider.getMapAdapter();
  }

  protected ImmutableList<MapType> getMapTypes() {
    return mapProvider.getMapTypes();
  }

  protected int getSelectedMapType() {
    return mapProvider.getMapType();
  }

  protected void selectMapType(int mapType) {
    mapProvider.setMapType(mapType);
  }
}
