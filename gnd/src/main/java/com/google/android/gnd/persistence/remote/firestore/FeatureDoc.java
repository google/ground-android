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

import static com.google.android.gnd.persistence.remote.firestore.FirestoreDataStore.toTimestamps;

import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.FeatureType;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java8.util.Optional;

@IgnoreExtraProperties
public class FeatureDoc {
  public String featureTypeId;

  public String customId;

  public String caption;

  // TODO: Rename to "point".
  public GeoPoint center;

  public @ServerTimestamp Date serverTimeCreated;

  public @ServerTimestamp Date serverTimeModified;

  public Date clientTimeCreated;

  public Date clientTimeModified;

  public static Feature toObject(Project project, DocumentSnapshot doc) {
    FeatureDoc f = doc.toObject(FeatureDoc.class);
    Point point =
        Point.newBuilder()
            .setLatitude(f.center.getLatitude())
            .setLongitude(f.center.getLongitude())
            .build();
    Optional<FeatureType> featureType = project.getFeatureType(f.featureTypeId);
    if (!featureType.isPresent()) {
      throw new DataStoreException(
          "Unknown feature type " + f.featureTypeId + " in lace " + doc.getId());
    }
    return Feature.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.customId)
        .setCaption(f.caption)
        .setFeatureType(featureType.get())
        .setPoint(point)
        .setServerTimestamps(toTimestamps(f.serverTimeCreated, f.serverTimeModified))
        .setClientTimestamps(toTimestamps(f.clientTimeCreated, f.clientTimeModified))
        .build();
  }

  public static FeatureDoc fromObject(Feature feature) {
    FeatureDoc doc = new FeatureDoc();
    Point point = feature.getPoint();
    doc.featureTypeId = feature.getFeatureType().getId();
    doc.center = new GeoPoint(point.getLatitude(), point.getLongitude());
    doc.customId = feature.getCustomId();
    doc.caption = feature.getCaption();
    // TODO: Implement timestamps.
    // TODO: Don't echo server timestamp in client. When we implement a proper DAL we can
    // use FieldValue.serverTimestamp() to signal when to update the value, or not set it,
    // depending on whether the operation is a CREATE or UPDATE.
    return doc;
  }
}
