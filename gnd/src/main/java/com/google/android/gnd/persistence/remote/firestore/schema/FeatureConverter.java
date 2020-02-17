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

import static com.google.android.gnd.persistence.remote.firestore.DataStoreException.checkNotEmpty;
import static com.google.android.gnd.persistence.remote.firestore.DataStoreException.checkNotNull;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.remote.firestore.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

/** Converts between Firestore documents and {@link Feature} instances. */
class FeatureConverter {
  // TODO: Make @NonNull the default and add build-time nullness checking.
  static Feature toFeature(@NonNull Project project, @NonNull DocumentSnapshot doc)
      throws DataStoreException {
    FeatureDocument f = doc.toObject(FeatureDocument.class);
    String featureTypeId = checkNotNull(f.getFeatureTypeId(), "featureTypeId");
    Layer layer = checkNotEmpty(project.getLayer(featureTypeId), "layer " + f.getFeatureTypeId());
    // TODO: Rename "point" and "center" to "location" throughout for clarity.
    GeoPoint geoPoint = checkNotNull(f.getCenter(), "center");
    Point location =
        Point.newBuilder()
            .setLatitude(geoPoint.getLatitude())
            .setLongitude(geoPoint.getLongitude())
            .build();
    return Feature.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.getCustomId())
        .setCaption(f.getCaption())
        .setLayer(layer)
        .setPoint(location)
        .setCreated(AuditInfoConverter.toAuditInfo(f.getCreated()))
        .setLastModified(AuditInfoConverter.toAuditInfo(f.getModified()))
        .build();
  }
}
