/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.repository.local;

import androidx.room.Room;
import androidx.room.Transaction;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.repository.local.Edit.Type;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Point;
import io.reactivex.Single;
import javax.inject.Inject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Implementation of local data store using Room to map objects to relational MySQL database. */
public class RoomDataStore implements LocalDataStore {
  private static final String DB_NAME = "gnd-db";
  private static final String LOCATION_JSON_KEY = "ll";

  private final GndRoomDatabase db;

  @Inject
  public RoomDataStore(GndApplication app) {
    this.db =
        Room.databaseBuilder(app.getApplicationContext(), GndRoomDatabase.class, DB_NAME).build();
  }

  @Override
  @Transaction
  public Single<Feature> createNewFeature(Feature feature) {
    return db.featureDao()
        .insert(toFeatureEntity(feature))
        .map(newId -> feature.toBuilder().setLocalId(newId).build())
        .flatMap(f -> db.featureEditDao().insert(toFeatureEditEntity(f)).map(__ -> f));
  }

  private static FeatureEntity toFeatureEntity(Feature feature) {
    return FeatureEntity.builder()
        .setState(EntityState.DEFAULT)
        .setProjectId(feature.getProject().getId())
        .setLocation(toCoordinates(feature.getPoint()))
        .build();
  }

  private static FeatureEditEntity toFeatureEditEntity(Feature feature) throws JSONException {
    return FeatureEditEntity.builder()
        .setFeatureId(feature.getLocalId())
        .setEdit(Edit.builder().setType(Type.CREATE).setNewValues(toJSONObject(feature)).build())
        .build();
  }

  private static Coordinates toCoordinates(Point point) {
    return Coordinates.builder()
        .setLatitude(point.getLatitude())
        .setLongitude(point.getLongitude())
        .build();
  }

  private static JSONObject toJSONObject(Feature feature) throws JSONException {
    return new JSONObject().put(LOCATION_JSON_KEY, toJSONArray(feature.getPoint()));
  }

  private static JSONArray toJSONArray(Point point) throws JSONException {
    return new JSONArray().put(point.getLatitude()).put(point.getLongitude());
  }
}
