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

package com.google.android.gnd.ui.map.gms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.util.BitmapUtil;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.function.Consumer;
import javax.inject.Inject;

/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint
public class GoogleMapsFragment extends SupportMapFragment
    implements MapFragment, OnMapReadyCallback {

  @Inject BitmapUtil bitmapUtil;
  @Inject MarkerIconFactory markerIconFactory;

  private Consumer<MapAdapter> adapterConsumer;

  @NonNull
  @Override
  public View onCreateView(
      @NonNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
    View view = super.onCreateView(layoutInflater, viewGroup, bundle);
    ViewCompat.setOnApplyWindowInsetsListener(view, this::onApplyWindowInsets);
    return view;
  }

  private WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
    int insetBottom = insets.getSystemWindowInsetBottom();
    // TODO: Move extra padding to dimens.xml.
    // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
    // watermark from flying up too high due to the combination of translateY and big inset
    // size due to keyboard.
    setWatermarkPadding(view, 20, 0, 0, Math.min(insetBottom, 250) + 8);
    return insets;
  }

  private void setWatermarkPadding(View view, int left, int top, int right, int bottom) {
    ImageView watermark = view.findViewWithTag("GoogleWatermark");
    // Watermark may be null if Maps failed to load.
    if (watermark == null) {
      return;
    }
    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) watermark.getLayoutParams();
    params.setMargins(left, top, right, bottom);
    watermark.setLayoutParams(params);
  }

  @Override
  public void attachToFragment(
      @NonNull AbstractFragment containerFragment,
      @IdRes int containerId,
      @NonNull Consumer<MapAdapter> adapterConsumer) {
    this.adapterConsumer = adapterConsumer;
    containerFragment.replaceFragment(containerId, this);
    getMapAsync(this);
  }

  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    adapterConsumer.accept(
        new GoogleMapsMapAdapter(googleMap, getContext(), markerIconFactory, bitmapUtil));
  }
}
