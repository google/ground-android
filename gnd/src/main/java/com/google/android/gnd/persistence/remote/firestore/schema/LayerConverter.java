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

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import android.util.Log;
import com.google.android.gnd.model.layer.Layer;
import java8.util.Optional;

/** Converts between Firestore documents and {@link Layer} instances. */
class LayerConverter {
  private static final String TAG = LayerConverter.class.getSimpleName();

  /** Black marker used when default remote data is corrupt or missing. */
  private static final String FALLBACK_COLOR = "#000000";

  static Layer toLayer(String id, LayerNestedObject obj) {
    Layer.Builder layer = Layer.newBuilder();
    // Use "color" field until web UI is updated:
    // TODO(https://github.com/google/ground-platform/issues/402): Remove fallback once updated in
    // web client.
    StyleNestedObject style =
        Optional.ofNullable(obj.getDefaultStyle())
            .orElse(
                new StyleNestedObject(Optional.ofNullable(obj.getColor()).orElse(FALLBACK_COLOR)));
    layer
        .setId(id)
        .setName(getLocalizedMessage(obj.getName()))
        .setItemLabel(getLocalizedMessage(obj.getItemLabel()))
        .setDefaultStyle(StyleConverter.toStyle(style));
    if (obj.getForms() != null && !obj.getForms().isEmpty()) {
      if (obj.getForms().size() > 1) {
        Log.w(TAG, "Multiple forms not supported");
      }
      String formId = obj.getForms().keySet().iterator().next();
      layer.setForm(FormConverter.toForm(formId, obj.getForms().get(formId)));
    }
    return layer.build();
  }
}
