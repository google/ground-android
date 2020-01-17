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

package com.google.android.gnd.persistence.remote.firestore.base;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public final class FirestoreField<T> {

  private final Class<T> type;
  private final String name;

  private FirestoreField(String name, Class<T> type) {
    this.type = type;
    this.name = name;
  }

  @NonNull
  public String key() {
    return name;
  }

  @NonNull
  public Class<T> type() {
    return type;
  }

  @NonNull
  @Override
  public String toString() {
    return "'" + name + "'";
  }

  public static FirestoreField<String> string(String name) {
    return new FirestoreField<>(name, String.class);
  }

  public static FirestoreField<Integer> integer(String name) {
    return new FirestoreField<>(name, Integer.class);
  }

  public static FirestoreField<GeoPoint> geoPoint(String name) {
    return new FirestoreField<>(name, GeoPoint.class);
  }

  public static FirestoreField<Timestamp> timestamp(String name) {
    return new FirestoreField<>(name, Timestamp.class);
  }

  public static FirestoreField<FirestoreData> data(String name) {
    return new FirestoreField<>(name, FirestoreData.class);
  }
}
