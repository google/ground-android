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

import static com.google.android.gnd.service.firestore.FirestoreDataService.toTimestamps;

import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java8.util.Optional;

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

  public static Place toProto(Project project, DocumentSnapshot doc) {
    PlaceDoc f = doc.toObject(PlaceDoc.class);
    Point point =
        Point.newBuilder()
            .setLatitude(f.center.getLatitude())
            .setLongitude(f.center.getLongitude())
            .build();
    Optional<PlaceType> placeType = project.getPlaceType(f.featureTypeId);
    if (!placeType.isPresent()) {
      throw new DatastoreException(
          "Unknown place type " + f.featureTypeId + " in lace " + doc.getId());
    }
    return Place.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.customId)
        .setCaption(f.caption)
        .setPlaceType(placeType.get())
        .setPoint(point)
        .setServerTimestamps(toTimestamps(f.serverTimeCreated, f.serverTimeModified))
        .setClientTimestamps(toTimestamps(f.clientTimeCreated, f.clientTimeModified))
        .build();
  }

  public static PlaceDoc fromProto(Place place) {
    PlaceDoc doc = new PlaceDoc();
    Point point = place.getPoint();
    doc.featureTypeId = place.getPlaceType().getId();
    doc.center = new GeoPoint(point.getLatitude(), point.getLongitude());
    doc.customId = place.getCustomId();
    doc.caption = place.getCaption();
    // TODO: Implement timestamps.
    // TODO: Don't echo server timestamp in client. When we implement a proper DAL we can
    // use FieldValue.serverTimestamp() to signal when to update the value, or not set it,
    // depending on whether the operation is a CREATE or UPDATE.
    return doc;
  }
}
