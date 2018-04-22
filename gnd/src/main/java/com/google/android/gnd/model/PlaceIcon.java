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

package com.google.android.gnd.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.util.ViewUtil;

public class PlaceIcon {

  private static final String TAG = PlaceIcon.class.getSimpleName();
  private final Context context;
  private BitmapDrawable drawable;
  private int color;

  public PlaceIcon(Context context, String iconId, int color) {
    this.context = context;
    String resourceName = "ic_marker_" + iconId.replace("-", "_");
    this.color = color;
    try {
      int resourceId =
          context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
      drawable = (BitmapDrawable) ContextCompat.getDrawable(context, resourceId);
    } catch (Resources.NotFoundException e) {
      // TODO: Fall back to default marker. 
      Log.e(TAG, e + toString());
    }
  }

  // TODO: Cache tinted bitmaps.
  public BitmapDescriptor getBitmap() {
    Bitmap tintedBitmap = ViewUtil.tintBitmap(drawable.getBitmap(), color);
    return BitmapDescriptorFactory.fromBitmap(tintedBitmap);
  }

  // TODO: Cache tinted bitmaps.
  public BitmapDescriptor getGreyBitmap() {
    Bitmap tintedBitmap =
        ViewUtil.tintBitmap(
            drawable.getBitmap(), context.getResources().getColor(R.color.colorGrey300));
    return BitmapDescriptorFactory.fromBitmap(tintedBitmap);
  }


  // TODO: Cache tinted bitmaps.
  public BitmapDescriptor getWhiteBitmap() {
    Bitmap tintedBitmap =
        ViewUtil.tintBitmap(
            drawable.getBitmap(), context.getResources().getColor(R.color.colorWhite));
    return BitmapDescriptorFactory.fromBitmap(tintedBitmap);
  }
}
