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

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class DrawableUtil {
  private final Resources resources;

  // TODO: Inject.
  public DrawableUtil(Resources resources) {
    this.resources = resources;
  }

  public Drawable getDrawable(int drawableId, int colorId) {
    Drawable drawable = resources.getDrawable(drawableId);
    int color = resources.getColor(colorId);
    drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    return drawable;
  }
}
