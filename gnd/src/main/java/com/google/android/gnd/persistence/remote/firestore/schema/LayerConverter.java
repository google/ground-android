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

import com.google.android.gnd.model.layer.Layer;
import java8.util.Maps;

/** Converts between Firestore documents and {@link Layer} instances. */
class LayerConverter {

  static Layer toLayer(String id, LayerNestedObject obj) {
    Layer.Builder layer = Layer.newBuilder();
    layer
        .setId(id)
        .setListHeading(getLocalizedMessage(obj.getListHeading()))
        .setItemLabel(getLocalizedMessage(obj.getItemLabel()));
    if (obj.getDefaultStyle() != null) {
      layer.setDefaultStyle(StyleConverter.toStyle(obj.getDefaultStyle()));
    }
    if (obj.getForms() != null) {
      Maps.forEach(
          obj.getForms(),
          (formId, formDoc) -> layer.addForm(FormConverter.toForm(formId, formDoc)));
    }
    return layer.build();
  }
}
