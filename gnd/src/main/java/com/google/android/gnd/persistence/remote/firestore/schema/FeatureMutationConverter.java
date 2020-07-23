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

import android.util.Log;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.GeoPoint;

/** Converts between Firestore maps used to merge updates and {@link FeatureMutation} instances. */
class FeatureMutationConverter {
  private static final String TAG = FeatureMutationConverter.class.getSimpleName();

  private static final String LAYER_ID = "layerId";
  private static final String CENTER = "center";
  private static final String CREATED = "created";
  private static final String LAST_MODIFIED = "lastModified";

  /**
   * Returns a map containing key-value pairs usable by Firestore constructed from the provided
   * mutation.
   */
  static ImmutableMap<String, Object> toMap(FeatureMutation mutation, User user) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    map.put(LAYER_ID, mutation.getLayerId());
    mutation
        .getNewLocation()
        .map(FeatureMutationConverter::toGeoPoint)
        .ifPresent(p -> map.put(CENTER, p));
    AuditInfoNestedObject auditInfo = AuditInfoConverter.fromMutationAndUser(mutation, user);
    switch (mutation.getType()) {
      case CREATE:
        map.put(CREATED, auditInfo);
        map.put(LAST_MODIFIED, auditInfo);
        break;
      case UPDATE:
      case DELETE:
      case UNKNOWN:
        // TODO.
        throw new UnsupportedOperationException();
      default:
        Log.e(TAG, "Unhandled state: " + mutation.getType());
        break;
    }
    return map.build();
  }

  private static GeoPoint toGeoPoint(Point point) {
    return new GeoPoint(point.getLatitude(), point.getLongitude());
  }
}
