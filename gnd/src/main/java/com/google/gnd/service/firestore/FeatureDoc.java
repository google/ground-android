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

package com.google.gnd.service.firestore;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.gnd.model.Feature;
import com.google.gnd.model.Point;

import java.util.Date;

import static com.google.gnd.service.firestore.FirestoreDataService.toDate;
import static com.google.gnd.service.firestore.FirestoreDataService.toTimestamps;

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

  public static Feature toProto(DocumentSnapshot doc) {
    FeatureDoc f = doc.toObject(FeatureDoc.class);
    Point point =
        Point.newBuilder()
            .setLatitude(f.center.getLatitude())
            .setLongitude(f.center.getLongitude())
            .build();
    return Feature.newBuilder()
        .setId(doc.getId())
        .setCustomId(f.customId)
        .setCaption(f.caption)
        .setFeatureTypeId(f.featureTypeId)
        .setPoint(point)
        .setServerTimestamps(toTimestamps(f.serverTimeCreated, f.serverTimeModified))
        .setClientTimestamps(toTimestamps(f.clientTimeCreated, f.clientTimeModified))
        .build();
  }

  public static FeatureDoc fromProto(Feature feature) {
    FeatureDoc doc = new FeatureDoc();
    Point point = feature.getPoint();
    doc.featureTypeId = feature.getFeatureTypeId();
    doc.center = new GeoPoint(point.getLatitude(), point.getLongitude());
    doc.customId = feature.getCustomId();
    doc.caption = feature.getCaption();
    // TODO: Don't echo server timestamp in client. When we implement a proper DAL we can
    // use FieldValue.serverTimestamp() to signal when to update the value, or not set it,
    // depending on whether the operation is a CREATE or UPDATE.
    if (feature.getServerTimestamps().hasCreated()) {
      doc.serverTimeCreated = toDate(feature.getServerTimestamps().getCreated());
    }
    if (feature.getServerTimestamps().hasModified()) {
      doc.serverTimeModified = toDate(feature.getServerTimestamps().getModified());
    }
    if (feature.getClientTimestamps().hasCreated()) {
      doc.clientTimeCreated = toDate(feature.getClientTimestamps().getCreated());
    }
    if (feature.getClientTimestamps().hasModified()) {
      doc.clientTimeModified = toDate(feature.getClientTimestamps().getModified());
    }
    return doc;
  }
}
