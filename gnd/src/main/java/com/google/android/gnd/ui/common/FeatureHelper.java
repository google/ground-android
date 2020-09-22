/*
 * Copyright 2020 Google LLC
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

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.gnd.model.feature.Feature;
import java8.util.Optional;

public class FeatureHelper {

  public static String getTitle(@Nullable Optional<Feature> featureOptional) {
    if (featureOptional == null || !featureOptional.isPresent()) {
      return "";
    }
    String caption = featureOptional.get().getCaption();
    String layerName = featureOptional.get().getLayer().getName();
    return caption == null || caption.isEmpty() ? layerName : caption;
  }

  public static String getCreatedBy(Context context, @Nullable Optional<Feature> featureOptional) {
    if (featureOptional == null || !featureOptional.isPresent()) {
      return "";
    }
    String username = featureOptional.get().getCreated().getUser().getDisplayName();
    return String.format(context.getResources().getString(R.string.added_by), username);
  }
}
