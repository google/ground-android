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
import androidx.annotation.Nullable;
import com.google.android.gnd.persistence.remote.firestore.DataStoreException;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;
import java8.util.Optional;

/**
 * Wrapper for raw data maps consumed and returned by the Cloud Firestore API. This includes data
 * returned from remote Firestore, as well as updates intended to overwrite or to be merged with
 * remote data.
 */
public class FirestoreData {

  private final Map<String, Object> map;

  private FirestoreData(Map<String, Object> map) {
    this.map = map;
  }

  @NonNull
  public <T> Optional<T> get(Field<T> field) {
    return Optional.ofNullable(get(field, false));
  }

  @NonNull
  public <T> T getRequired(Field<T> field) {
    return get(field, true);
  }

  @NonNull
  public ImmutableMap<String, Object> toMap() {
    return ImmutableMap.copyOf(map);
  }

  @Nullable
  private <T> T get(Field<T> field, boolean required) {
    Object value = map.get(field.key());
    if (value == null) {
      if (required) {
        throw new DataStoreException("Missing field " + field);
      } else {
        return null;
      }
    }
    boolean isNestedObject = FirestoreData.class.isAssignableFrom(field.type());
    Class expectedType = isNestedObject ? Map.class : field.type();
    if (!expectedType.isAssignableFrom(value.getClass())) {
      if (required) {
        throw new DataStoreException("Expected" + field + " to be " + expectedType);
      } else {
        return null;
      }
    }
    if (isNestedObject) {
      try {
        value = ((Class) field.type()).getConstructor(Map.class).newInstance(value);
      } catch (Throwable e) {
        throw new DataStoreException("Error creating " + field.type(), e);
      }
    }
    return (T) value;
  }

  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  @NonNull
  public static FirestoreData fromMap(Map<String, Object> map) {
    return new FirestoreData(new HashMap<>(map));
  }

  public static final class Builder {
    private final Map<String, Object> map = new HashMap<>();

    public <T> Builder set(Field<T> field, T value) {
      if (value instanceof FirestoreData) {
        map.put(field.key(), ((FirestoreData) value).toMap());
      } else {
        map.put(field.key(), value);
      }
      return this;
    }

    public <T> Builder delete(Field<T> field) {
      map.put(field.key(), FieldValue.delete());
      return this;
    }

    public Builder updateTimestampOnServer(Field<Timestamp> field) {
      map.put(field.key(), FieldValue.serverTimestamp());
      return this;
    }

    public FirestoreData build() {
      return new FirestoreData(map);
    }
  }
}
