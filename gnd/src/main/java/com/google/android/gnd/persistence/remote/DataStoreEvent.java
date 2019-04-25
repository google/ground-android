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

package com.google.android.gnd.persistence.remote;

import java8.util.Optional;

// TODO: Harmonize with com.google.android.gnd.rx.AbstractResource.
public class DataStoreEvent<T> {

  public enum Type {
    ENTITY_LOADED,
    ENTITY_MODIFIED,
    ENTITY_REMOVED,
    INVALID_RESPONSE
  }

  public enum Source {
    LOCAL_DATA_STORE,
    REMOTE_DATA_STORE
  }

  private String id;
  private Optional<T> entity;
  private Type type;
  private Source source;

  private DataStoreEvent(Type type) {
    this.type = type;
  }

  private DataStoreEvent(String id, Type type, Source source, Optional<T> entity) {
    this.id = id;
    this.type = type;
    this.source = source;
    this.entity = entity;
  }

  public String getId() {
    return id;
  }

  public Type getType() {
    return type;
  }

  public Optional<T> getEntity() {
    return entity;
  }

  public Source getSource() {
    return source;
  }

  public boolean hasPendingWrites() {
    return source == DataStoreEvent.Source.LOCAL_DATA_STORE;
  }

  public boolean isValid() {
    return !Type.INVALID_RESPONSE.equals(type);
  }

  public static <T> DataStoreEvent<T> loaded(String id, Source source, T entity) {
    return new DataStoreEvent<>(id, Type.ENTITY_LOADED, source, Optional.of(entity));
  }

  public static <T> DataStoreEvent<T> modified(String id, Source source, T entity) {
    return new DataStoreEvent<>(id, Type.ENTITY_MODIFIED, source, Optional.of(entity));
  }

  public static <T> DataStoreEvent<T> removed(String id, Source source) {
    return new DataStoreEvent<>(id, Type.ENTITY_MODIFIED, source, Optional.empty());
  }

  public static <T> DataStoreEvent<T> invalidResponse() {
    return new DataStoreEvent<>(Type.INVALID_RESPONSE);
  }
}
