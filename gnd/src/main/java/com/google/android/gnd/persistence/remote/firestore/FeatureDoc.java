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

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java8.util.Optional;

@IgnoreExtraProperties
public class FeatureDoc {
  // TODO: Implement type safe field definition enums.
  private static final String FEATURE_TYPE_ID = "featureTypeId";
  private static final String CENTER = "center";
  private static final String CREATED = "created";
  private static final String LAST_MODIFIED = "lastModified";
  private static final String TAG = FeatureDoc.class.getName();

  public String featureTypeId;

  public String customId;

  public String caption;

  // TODO: Replace with consistent name throughout.
  public GeoPoint center;

  @Nullable public AuditInfoDoc created;

  @Nullable public AuditInfoDoc modified;

  public static Feature toObject(Project project, DocumentSnapshot doc) {
    FeatureDoc f = doc.toObject(FeatureDoc.class);
    Optional<Layer> layer = project.getLayer(f.featureTypeId);
    if (!layer.isPresent()) {
      throw new DataStoreException(
          "Unknown featureTypeId " + f.featureTypeId + " in feature " + doc.getId());
    }
    Point point =
        Point.newBuilder()
            .setLatitude(f.center.getLatitude())
            .setLongitude(f.center.getLongitude())
            .build();
    return Feature.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.customId)
        .setCaption(f.caption)
        .setLayer(layer.get())
        .setPoint(point)
        .setCreated(AuditInfoDoc.toObject(f.created))
        .setLastModified(AuditInfoDoc.toObject(f.modified))
        .build();
  }

  private static GeoPoint toGeoPoint(Point point) {
    return new GeoPoint(point.getLatitude(), point.getLongitude());
  }

  /**
   * Returns a map containing key-value pairs usable by Firestore constructed from the provided
   * mutation.
   */
  public static ImmutableMap<String, Object> toMap(FeatureMutation mutation, User user) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    map.put(FEATURE_TYPE_ID, mutation.getLayerId());
    mutation.getNewLocation().map(FeatureDoc::toGeoPoint).ifPresent(p -> map.put(CENTER, p));
    AuditInfoDoc auditInfo = AuditInfoDoc.fromMutationAndUser(mutation, user);
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
}
