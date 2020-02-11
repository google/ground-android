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
import java.util.List;
import javax.annotation.Nullable;

/**
 * Defines the name of a field in a Firestore document or nested object and its expected type.
 *
 * @param <T> The data type stored in this field. Must be either one of the supported Firestore <a
 *     href="https://firebase.google.com/docs/firestore/manage-data/data-types">data types</a>, or
 *     {@link FirestoreData} to represented a nested object (map).
 */
public final class FirestoreField<T> {

  @NonNull private final String key;
  @NonNull private final Class<T> type;
  @Nullable private final Class<?> elementType;

  private FirestoreField(String key, Class<T> type) {
    this.type = type;
    this.key = key;
    this.elementType = null;
  }

  public FirestoreField(String key, Class<T> collectionType, Class<?> elementType) {
    this.type = collectionType;
    this.key = key;
    this.elementType = elementType;
  }

  @NonNull
  public String key() {
    return key;
  }

  @NonNull
  public Class<T> type() {
    return type;
  }

  @Nullable
  public Class<?> elementType() {
    return elementType;
  }

  @NonNull
  @Override
  public String toString() {
    return key;
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

  public static <T extends FirestoreData> FirestoreField<T> nestedObject(
      String name, Class<T> type) {
    return new FirestoreField<>(name, type);
  }

  public static FirestoreField<Boolean> bool(String name) {
    return new FirestoreField<>(name, Boolean.class);
  }

  public static <T> FirestoreField<List<T>> array(String name, Class<T> elementType) {
    return new FirestoreField(name, List.class, elementType);
  }
}
