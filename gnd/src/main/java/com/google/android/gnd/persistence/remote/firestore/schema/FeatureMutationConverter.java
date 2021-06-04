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
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.GeoPoint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

/** Converts between Firestore maps used to merge updates and {@link FeatureMutation} instances. */
class FeatureMutationConverter {

  /**
   * Returns a map containing key-value pairs usable by Firestore constructed from the provided
   * mutation.
   */
  static ImmutableMap<String, Object> toMap(FeatureMutation mutation, User user) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    map.put(FeatureConverter.LAYER_ID, mutation.getLayerId());
    mutation
        .getNewLocation()
        .map(FeatureMutationConverter::toGeoPoint)
        .ifPresent(point -> map.put(FeatureConverter.LOCATION, point));
    mutation
        .getNewPolygonVertices()
        .map(FeatureMutationConverter::toGeoPointList)
        .ifPresent(point -> {
          Map<String, Object> geometry = new HashMap<>();
          geometry.put(FeatureConverter.GEOMETRY_COORDINATES, point);
          geometry.put(FeatureConverter.GEOMETRY_TYPE, FeatureConverter.POLYGON_TYPE);
          map.put(FeatureConverter.GEOMETRY, geometry);
        });
    AuditInfoNestedObject auditInfo = AuditInfoConverter.fromMutationAndUser(mutation, user);
    switch (mutation.getType()) {
      case CREATE:
        map.put(FeatureConverter.CREATED, auditInfo);
        map.put(FeatureConverter.LAST_MODIFIED, auditInfo);
        break;
      case UPDATE:
        map.put(FeatureConverter.LAST_MODIFIED, auditInfo);
        break;
      case DELETE:
      case UNKNOWN:
        // TODO.
        throw new UnsupportedOperationException();
      default:
        Timber.e("Unhandled state: %s", mutation.getType());
        break;
    }
    return map.build();
  }

  private static GeoPoint toGeoPoint(Point point) {
    return new GeoPoint(point.getLatitude(), point.getLongitude());
  }

  private static List<GeoPoint> toGeoPointList(ImmutableList<Point> point) {
    return stream(point).map(FeatureMutationConverter::toGeoPoint).collect(toImmutableList());
  }
}
