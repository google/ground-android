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

package com.google.android.gnd.service.firestore;

import static com.google.android.gnd.service.firestore.FirestoreDataService.toDate;
import static com.google.android.gnd.service.firestore.FirestoreDataService.toTimestamps;

import com.google.android.gnd.model.Place;
import com.google.android.gnd.model.Point;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class PlaceDoc {
  public String featureTypeId;

  public String customId;

  public String caption;

  // TODO: Rename to "point".
  public GeoPoint center;

  public @ServerTimestamp Date serverTimeCreated;

  public @ServerTimestamp Date serverTimeModified;

  public Date clientTimeCreated;

  public Date clientTimeModified;

  public static Place toProto(DocumentSnapshot doc) {
    PlaceDoc f = doc.toObject(PlaceDoc.class);
    Point point =
        Point.newBuilder()
            .setLatitude(f.center.getLatitude())
            .setLongitude(f.center.getLongitude())
            .build();
    return Place.newBuilder()
        .setId(doc.getId())
        .setCustomId(f.customId)
        .setCaption(f.caption)
        .setPlaceTypeId(f.featureTypeId)
        .setPoint(point)
        .setServerTimestamps(toTimestamps(f.serverTimeCreated, f.serverTimeModified))
        .setClientTimestamps(toTimestamps(f.clientTimeCreated, f.clientTimeModified))
        .build();
  }

  public static PlaceDoc fromProto(Place place) {
    PlaceDoc doc = new PlaceDoc();
    Point point = place.getPoint();
    doc.featureTypeId = place.getPlaceTypeId();
    doc.center = new GeoPoint(point.getLatitude(), point.getLongitude());
    doc.customId = place.getCustomId();
    doc.caption = place.getCaption();
    // TODO: Don't echo server timestamp in client. When we implement a proper DAL we can
    // use FieldValue.serverTimestamp() to signal when to update the value, or not set it,
    // depending on whether the operation is a CREATE or UPDATE.
    if (place.getServerTimestamps().hasCreated()) {
      doc.serverTimeCreated = toDate(place.getServerTimestamps().getCreated());
    }
    if (place.getServerTimestamps().hasModified()) {
      doc.serverTimeModified = toDate(place.getServerTimestamps().getModified());
    }
    if (place.getClientTimestamps().hasCreated()) {
      doc.clientTimeCreated = toDate(place.getClientTimestamps().getCreated());
    }
    if (place.getClientTimestamps().hasModified()) {
      doc.clientTimeModified = toDate(place.getClientTimestamps().getModified());
    }
    return doc;
  }
}
