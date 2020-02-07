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
import java.util.Map;
import java8.util.Optional;

/**
 * Wrapper for raw data being read and written by the Cloud Firestore API, including both data
 * returned from remote Firestore as a document, as well as for nested objects inside a document.
 */
public abstract class FirestoreData {

  private final ImmutableMap<String, Object> map;

  protected FirestoreData(ImmutableMap<String, Object> map) {
    this.map = map;
  }

  @NonNull
  public <T> Optional<T> get(FirestoreField<T> field) {
    return Optional.ofNullable(get(field, false));
  }

  @NonNull
  protected <T> T getRequired(FirestoreField<T> field) {
    return get(field, true);
  }

  @NonNull
  public ImmutableMap<String, Object> toMap() {
    return ImmutableMap.copyOf(map);
  }

  @Nullable
  protected <T> T get(FirestoreField<T> field, boolean required) {
    Object value = map.get(field.key());
    if (value == FieldValue.delete()) {
      value = null;
    }
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

  public abstract static class Builder<B extends Builder> {
    private final ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();

    protected <T> B set(FirestoreField<T> field, T value) {
      if (value instanceof FirestoreData) {
        map.put(field.key(), ((FirestoreData) value).toMap());
      } else {
        map.put(field.key(), value);
      }
      return (B) this;
    }

    protected <T> B delete(FirestoreField<T> field) {
      map.put(field.key(), FieldValue.delete());
      return (B) this;
    }

    protected B updateServerTimestamp(FirestoreField<Timestamp> field) {
      map.put(field.key(), FieldValue.serverTimestamp());
      return (B) this;
    }

    protected ImmutableMap<String, Object> toMap() {
      return map.build();
    }
  }
}
