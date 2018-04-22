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

package com.google.android.gnd.ui.map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.google.android.gms.maps.MapView;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

// TODO: Refactor view interface and adapter and allow plugging different map providers.
public class GoogleMapsView extends MapView {

  private static final String TAG = GoogleMapsView.class.getSimpleName();
  private BehaviorSubject<GoogleMapImpl> mapSubject = BehaviorSubject.create();

  public GoogleMapsView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    getMapAsync(map -> mapSubject.onNext(new GoogleMapImpl(map)));
  }

  public Observable<GoogleMapImpl> getMap() {
    return mapSubject;
  }

  private void setWatermarkPadding(int left, int top, int right, int bottom) {
    ImageView watermark = findViewWithTag("GoogleWatermark");
    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) watermark.getLayoutParams();
    params.setMargins(left, top, right, bottom);
    watermark.setLayoutParams(params);
  }

  @Override
  public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
      int insetBottom = insets.getSystemWindowInsetBottom();
      // TODO: Move extra padding to dimens.xml.
      // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
      // watermark from flying up too high due to the combination of translateY and big inset
      // size due to keyboard.
      setWatermarkPadding(20, 0, 0, Math.min(insetBottom, 250) + 8);
    }
    return super.onApplyWindowInsets(insets);
  }
}
