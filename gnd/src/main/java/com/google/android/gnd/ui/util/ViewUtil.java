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

package com.google.android.gnd.ui.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java8.util.stream.IntStreams;
import java8.util.stream.Stream;

public class ViewUtil {
  @NonNull
  public static LinearLayout.LayoutParams createLinearLayoutParams(
      int width, int height, int marginLeft, int marginTop, int marginRight, int marginBottom) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
    params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
    return params;
  }

  public static int getScreenWidth(Activity activity) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.widthPixels;
  }

  public static int getScreenHeight(Activity activity) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.heightPixels;
  }

  public static void hideSoftInputFrom(Fragment fragment) {
    hideSoftInputFrom(fragment.getContext(), fragment.getView().getRootView());
  }

  private static void hideSoftInputFrom(Context context, View view) {
    InputMethodManager imm =
        (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  public static Stream<View> children(ViewGroup view) {
    return IntStreams.range(0, view.getChildCount()).mapToObj(view::getChildAt);
  }

  public static int getColorForStates(TextView textView, int[] stateSet) {
    return textView.getTextColors().getColorForState(stateSet, 0);
  }

  public static Bitmap tintBitmap(Bitmap bitmap, @ColorInt int color) {
    Bitmap tintedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
    Paint paint = new Paint();
    ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    paint.setColorFilter(filter);
    Canvas canvas = new Canvas(tintedBitmap);
    canvas.drawBitmap(tintedBitmap, 0, 0, paint);
    return tintedBitmap;
  }

  public static void assignGeneratedId(@Nullable View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && view != null) {
      view.setId(View.generateViewId());
    }
  }
}
