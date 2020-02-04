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

package com.google.android.gnd.persistence.remote.firestore;

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;
import java.util.Map.Entry;

@IgnoreExtraProperties
public class LayerDoc {
  private static final String TAG = LayerDoc.class.getSimpleName();

  @Nullable public Map<String, String> listHeading;

  @Nullable public Map<String, String> itemLabel;

  @Nullable public StyleDoc defaultStyle;

  @Nullable public Map<String, FormDoc> forms;

  public Layer toObject(String id) {
    Layer.Builder layer = Layer.newBuilder();
    layer
        .setId(id)
        .setListHeading(getLocalizedMessage(listHeading))
        .setItemLabel(getLocalizedMessage(itemLabel));
    if (defaultStyle != null) {
      layer.setDefaultStyle(defaultStyle.toObject());
    }
    if (forms != null && !forms.isEmpty()) {
      if (forms.size() > 1) {
        Log.d(
            TAG, "Multiple forms defined in layer " + id + ". Using first form, ignoring others.");
      }
      Entry<String, FormDoc> formEntry = forms.entrySet().iterator().next();
      layer.setForm(formEntry.getValue().toObject(formEntry.getKey()));
    }
    return layer.build();
  }

  public static class StyleDoc {
    @Nullable public String color;

    public Style toObject() {
      return Style.builder().setColor(color).build();
    }
  }
}
