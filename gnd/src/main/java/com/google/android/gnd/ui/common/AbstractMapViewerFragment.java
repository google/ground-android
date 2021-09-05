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

import static com.google.android.gnd.rx.RxAutoDispose.disposeOnDestroy;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.map.MapProvider.MapType;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import javax.inject.Inject;

/** Injects a {@link MapFragment} in the container with id "map". */
public class AbstractMapViewerFragment extends AbstractFragment {

  @Nullable private MapAdapter mapAdapter;

  @Inject MapProvider mapProvider;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getMapAdapter()
        .toObservable()
        .as(disposeOnDestroy(this))
        .subscribe(mapAdapter -> this.mapAdapter = mapAdapter);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mapProvider
        .createFragment()
        .attachToFragment(this, R.id.map, adapter -> mapProvider.setMapAdapter(adapter));
  }

  @Hot
  protected Flowable<MapAdapter> getMapAdapter() {
    return mapProvider.getMapAdapter();
  }

  @Nullable
  protected MapAdapter getActiveMapAdapter() {
    return mapAdapter;
  }

  protected ImmutableList<MapType> getMapTypes() {
    return mapProvider.getMapTypes();
  }
}
