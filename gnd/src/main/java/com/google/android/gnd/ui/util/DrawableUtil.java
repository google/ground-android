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

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import javax.inject.Inject;

public class DrawableUtil {

  private final Context context;

  @Inject
  public DrawableUtil(Application context) {
    this.context = context;
  }

  private Resources getResources() {
    return context.getResources();
  }

  @ColorInt
  public int getColor(@ColorRes int colorId) {
    return getResources().getColor(colorId);
  }

  public Drawable getDrawable(@DrawableRes int drawableId) {
    return ResourcesCompat.getDrawable(getResources(), drawableId, null);
  }

  public Drawable getDrawable(@DrawableRes int drawableId, @ColorRes int colorId) {
    Drawable drawable = getDrawable(drawableId);
    drawable.setColorFilter(getColor(colorId), PorterDuff.Mode.SRC_ATOP);
    return drawable;
  }
}
