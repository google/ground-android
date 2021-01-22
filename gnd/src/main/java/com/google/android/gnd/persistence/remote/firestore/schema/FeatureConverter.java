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
import androidx.annotation.Nullable;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.common.base.Strings;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import java8.util.Objects;

/** Converts between Firestore documents and {@link Feature} instances. */
public class FeatureConverter {

  protected static final String LAYER_ID = "layerId";
  protected static final String LOCATION = "location";
  protected static final String CREATED = "created";
  protected static final String LAST_MODIFIED = "lastModified";

  // TODO: Make @NonNull the default and add build-time nullness checking.
  static Feature toFeature(@NonNull Project project, @NonNull DocumentSnapshot doc)
      throws DataStoreException {
    FeatureDocument f = checkNotNull(doc.toObject(FeatureDocument.class), "feature data");
    String layerId = checkNotNull(f.getLayerId(), LAYER_ID);
    Layer layer = checkNotEmpty(project.getLayer(layerId), "layer " + f.getLayerId());
    Point location = checkNotNull(toPoint(f.getLocation()), LOCATION);
    String geoJsonString = Strings.isNullOrEmpty(f.getGeoJson()) ? null : f.getGeoJson();
    // Degrade gracefully when audit info missing in remote db.
    AuditInfoNestedObject created =
        Objects.requireNonNullElse(f.getCreated(), AuditInfoNestedObject.FALLBACK_VALUE);
    AuditInfoNestedObject lastModified = Objects.requireNonNullElse(f.getLastModified(), created);
    return Feature.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.getCustomId())
        .setCaption(f.getCaption())
        .setLayer(layer)
        .setPoint(location)
        .setGeoJsonString(geoJsonString)
        .setCreated(AuditInfoConverter.toAuditInfo(created))
        .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
        .build();
  }

  @Nullable
  private static Point toPoint(@Nullable GeoPoint geoPoint) {
    if (geoPoint == null) {
      return null;
    }
    return Point.newBuilder()
        .setLatitude(geoPoint.getLatitude())
        .setLongitude(geoPoint.getLongitude())
        .build();
  }
}
