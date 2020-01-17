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
import java.util.HashMap;
import java.util.Map;
import java8.util.Optional;

public class FirestoreData {
  @NonNull private Map<String, Object> data;

  public FirestoreData() {
    this.data = new HashMap<>();
  }

  public FirestoreData(Map<String, Object> data) {
    this.data = new HashMap<>(data);
  }

  @NonNull
  public ImmutableMap<String, Object> getData() {
    return ImmutableMap.copyOf(data);
  }

  public <T> void set(FirestoreField<T> field, T value) {
    // TODO: Convert nested data back to Map.
    data.put(field.key(), value);
  }

  @NonNull
  public <T> Optional<T> get(FirestoreField<T> field) {
    return Optional.ofNullable(get(field, false));
  }

  @NonNull
  public <T> T getRequired(FirestoreField<T> field) {
    return get(field, true);
  }

  @Nullable
  private <T> T get(FirestoreField<T> field, boolean required) {
    Object value = data.get(field.key());
    if (value == null) {
      if (required) {
        throw new DataStoreException("Missing field " + field);
      } else {
        return null;
      }
    }
    Class fieldType = field.type();
    // TODO: Name: Data? Nested object? ??
    boolean isNestedData = FirestoreData.class.isAssignableFrom(field.type());
    Class expectedType = isNestedData ? Map.class : fieldType;
    if (!expectedType.isAssignableFrom(value.getClass())) {
      if (required) {
        throw new DataStoreException("Expected" + field + " to be " + expectedType);
      } else {
        return null;
      }
    }
    if (isNestedData) {
      try {
        value = fieldType.getConstructor(Map.class).newInstance(value);
      } catch (Throwable e) {
        throw new RuntimeException("Error creating " + fieldType, e);
      }
    }
    return (T) value;
  }

  protected DataStoreException error(String message) {
    return new DataStoreException(message);
  }
}
