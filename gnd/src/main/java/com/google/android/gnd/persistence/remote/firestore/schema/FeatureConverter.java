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

import androidx.annotation.Nullable;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;
import java.util.Map;
import java8.util.Objects;
import timber.log.Timber;

/** Converts between Firestore documents and {@link Feature} instances. */
public class FeatureConverter {

  protected static final String LAYER_ID = "layerId";
  protected static final String LOCATION = "location";
  protected static final String CREATED = "created";
  protected static final String LAST_MODIFIED = "lastModified";
  protected static final String GEOMETRY_TYPE = "type";
  protected static final String POLYGON_TYPE = "Polygon";
  protected static final String GEOMETRY_COORDINATES = "coordinates";
  protected static final String GEOMETRY = "geometry";

  static Feature toFeature(Project project, DocumentSnapshot doc) throws DataStoreException {
    FeatureDocument f = checkNotNull(doc.toObject(FeatureDocument.class), "feature data");
    if (f.getGeometry() != null && hasNonEmptyVertices(f)) {
      return toFeatureFromGeometry(project, doc, f);
    }

    if (f.getGeoJson() != null) {
      GeoJsonFeature.Builder builder = GeoJsonFeature.newBuilder().setGeoJsonString(f.getGeoJson());
      fillFeature(builder, project, doc.getId(), f);
      return builder.build();
    }

    if (f.getLocation() != null) {
      PointFeature.Builder builder = PointFeature.newBuilder().setPoint(toPoint(f.getLocation()));
      fillFeature(builder, project, doc.getId(), f);
      return builder.build();
    }

    throw new DataStoreException("No geometry in remote feature " + doc.getId());
  }

  private static boolean hasNonEmptyVertices(FeatureDocument featureDocument) {
    Map<String, Object> geometry = featureDocument.getGeometry();

    if (geometry == null
        || geometry.get(GEOMETRY_COORDINATES) == null
        || !(geometry.get(GEOMETRY_COORDINATES) instanceof List)) {
      return false;
    }

    List<?> coordinates = (List<?>) geometry.get(GEOMETRY_COORDINATES);
    return !coordinates.isEmpty();
  }

  private static PolygonFeature toFeatureFromGeometry(
      Project project, DocumentSnapshot doc, FeatureDocument f) {
    Map<String, Object> geometry = f.getGeometry();
    Object type = geometry.get(GEOMETRY_TYPE);
    if (!POLYGON_TYPE.equals(type)) {
      throw new DataStoreException("Unknown geometry type in feature " + doc.getId() + ": " + type);
    }
    Object coordinates = geometry.get(GEOMETRY_COORDINATES);
    if (!(coordinates instanceof List)) {
      throw new DataStoreException(
          "Invalid coordinates in feature " + doc.getId() + ": " + coordinates);
    }
    ImmutableList.Builder<Point> vertices = ImmutableList.builder();
    for (Object point : (List<?>) coordinates) {
      if (!(point instanceof GeoPoint)) {
        Timber.d("Ignoring illegal point type in feature %s", doc.getId());
        break;
      }
      vertices.add(Point.newBuilder().setLongitude(((GeoPoint) point).getLongitude()).setLatitude(
          ((GeoPoint) point).getLatitude()).build());

    }
    PolygonFeature.Builder builder = PolygonFeature.builder().setVertices(vertices.build());
    fillFeature(builder, project, doc.getId(), f);
    return builder.build();
  }

  private static void fillFeature(
      Feature.Builder builder, Project project, String id, FeatureDocument f) {
    String layerId = checkNotNull(f.getLayerId(), LAYER_ID);
    Layer layer = checkNotEmpty(project.getLayer(layerId), "layer " + f.getLayerId());
    // Degrade gracefully when audit info missing in remote db.
    AuditInfoNestedObject created =
        Objects.requireNonNullElse(f.getCreated(), AuditInfoNestedObject.FALLBACK_VALUE);
    AuditInfoNestedObject lastModified = Objects.requireNonNullElse(f.getLastModified(), created);
    builder
        .setId(id)
        .setProject(project)
        .setCustomId(f.getCustomId())
        .setCaption(f.getCaption())
        .setLayer(layer)
        .setCreated(AuditInfoConverter.toAuditInfo(created))
        .setLastModified(AuditInfoConverter.toAuditInfo(lastModified));
  }

  @Nullable
  private static Point toPoint(GeoPoint geoPoint) {
    return Point.newBuilder()
        .setLatitude(geoPoint.getLatitude())
        .setLongitude(geoPoint.getLongitude())
        .build();
  }
}
