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
import com.google.android.gnd.ui.map.MapFragment;
import javax.inject.Inject;

/** Injects a {@link MapFragment} in the container with id "map". */
public abstract class AbstractMapViewerFragment extends AbstractFragment {

  @Inject MapFragment mapFragment;

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    getMapFragment().attachToFragment(this, R.id.map, this::onMapReady);
  }

  protected MapFragment getMapFragment() {
    return mapFragment;
  }

  protected abstract void onMapReady(MapFragment mapFragment);
}
