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

import static com.google.android.gnd.persistence.remote.DataStoreException.checkNotEmpty;
import static com.google.android.gnd.persistence.remote.DataStoreException.checkNotNull;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import java8.util.Optional;

/** Converts between Firestore documents and {@link Feature} instances. */
class FeatureConverter {
  // TODO: Make @NonNull the default and add build-time nullness checking.
  static Feature toFeature(@NonNull Project project, @NonNull DocumentSnapshot doc)
      throws DataStoreException {
    FeatureDocument f = checkNotNull(doc.toObject(FeatureDocument.class), "feature data");
    String layerId = checkNotNull(f.getlayerId(), "layerId");
    Layer layer = checkNotEmpty(project.getLayer(layerId), "layer " + f.getlayerId());
    // TODO: Rename "point" and "center" to "location" throughout for clarity.
    GeoPoint geoPoint = checkNotNull(f.getLocation(), "location");
    Point location =
        Point.newBuilder()
            .setLatitude(geoPoint.getLatitude())
            .setLongitude(geoPoint.getLongitude())
            .build();
    // Degrade gracefully when audit info missing in remote db.
    AuditInfoNestedObject created =
        Optional.ofNullable(f.getCreated()).orElse(AuditInfoNestedObject.FALLBACK_VALUE);
    AuditInfoNestedObject lastModified = Optional.ofNullable(f.getLastModified()).orElse(created);
    return Feature.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.getCustomId())
        .setCaption(f.getCaption())
        .setLayer(layer)
        .setPoint(location)
        .setCreated(AuditInfoConverter.toAuditInfo(created))
        .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
        .build();
  }
}
