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

/**
 * Defines the name of a field in a Firestore document and its expected type.
 *
 * @param <T> The data type stored in this field. Must be either one of the supported Firestore <a
 *     href="https://firebase.google.com/docs/firestore/manage-data/data-types">data types</a>, or
 *     {@link FirestoreData} to represented a nested object (map).
 */
public final class Field<T> {

  private final String key;
  private final Class<T> type;

  private Field(String key, Class<T> type) {
    this.type = type;
    this.key = key;
  }

  @NonNull
  public String key() {
    return key;
  }

  @NonNull
  public Class<T> type() {
    return type;
  }

  @NonNull
  @Override
  public String toString() {
    return key;
  }

  public static Field<String> string(String name) {
    return new Field<>(name, String.class);
  }

  public static Field<Integer> integer(String name) {
    return new Field<>(name, Integer.class);
  }

  public static Field<GeoPoint> geoPoint(String name) {
    return new Field<>(name, GeoPoint.class);
  }

  public static Field<Timestamp> timestamp(String name) {
    return new Field<>(name, Timestamp.class);
  }

  public static Field<FirestoreData> nestedObject(String name) {
    return new Field<>(name, FirestoreData.class);
  }
}
