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

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static com.google.android.gnd.util.Localization.getLocalizedMessage;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.common.collect.ImmutableList;
import java8.util.Objects;
import java8.util.Optional;
import timber.log.Timber;

/** Converts between Firestore documents and {@link Layer} instances. */
class LayerConverter {
  /** Black marker used when default remote data is corrupt or missing. */
  private static final String FALLBACK_COLOR = "#000000";

  static Layer toLayer(String id, LayerNestedObject obj) {
    Layer.Builder layer = Layer.newBuilder();
    layer.setId(id).setName(getLocalizedMessage(obj.getName())).setDefaultStyle(toStyle(obj));
    if (obj.getForms() != null && !obj.getForms().isEmpty()) {
      if (obj.getForms().size() > 1) {
        Timber.e("Multiple forms not supported");
      }
      String formId = obj.getForms().keySet().iterator().next();
      layer.setForm(FormConverter.toForm(formId, obj.getForms().get(formId)));
    }
    if (obj.getContributorsCanAdd() != null) {
      ImmutableList<FeatureType> featureTypes =
          stream(obj.getContributorsCanAdd())
              .map(LayerConverter::toFeatureType)
              .collect(toImmutableList());
      layer.setContributorsCanAdd(featureTypes);
    }
    return layer.build();
  }

  private static FeatureType toFeatureType(String stringValue) {
    switch (stringValue) {
      case "points":
        return FeatureType.POINT;
      case "polygons":
        return FeatureType.POLYGON;
      default:
        return FeatureType.UNKNOWN;
    }
  }

  private static Style toStyle(LayerNestedObject obj) {
    return Optional.ofNullable(obj.getDefaultStyle())
        .map(StyleConverter::toStyle)
        .orElseGet(() -> LayerConverter.toStyleFromLegacyColorField(obj));
  }

  private static Style toStyleFromLegacyColorField(LayerNestedObject obj) {
    // Use "color" field until web UI is updated:
    // TODO(https://github.com/google/ground-platform/issues/402): Remove fallback once updated in
    // web client.
    return Style.builder()
        .setColor(Objects.requireNonNullElse(obj.getColor(), FALLBACK_COLOR))
        .build();
  }
}
