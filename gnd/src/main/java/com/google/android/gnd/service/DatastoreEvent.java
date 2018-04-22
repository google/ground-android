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

package com.google.android.gnd.service;

import java8.util.Optional;

public class DatastoreEvent<T> {

  public enum Type {
    ENTITY_LOADED, ENTITY_MODIFIED, ENTITY_REMOVED
  }

  public enum Source {
    LOCAL_DATASTORE, REMOTE_DATASTORE
  }

  private final String id;
  private final Optional<T> entity;
  private final Type type;
  private final Source source;

  private DatastoreEvent(String id, Type type, Source source, Optional<T> entity) {
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

  public static <T> DatastoreEvent<T> loaded(String id, Source source, T entity) {
    return new DatastoreEvent<>(id, Type.ENTITY_LOADED, source, Optional.of(entity));
  }

  public static <T> DatastoreEvent<T> modified(String id, Source source, T entity) {
    return new DatastoreEvent<>(id, Type.ENTITY_MODIFIED, source, Optional.of(entity));
  }

  public static <T> DatastoreEvent<T> removed(String id, Source source) {
    return new DatastoreEvent<>(id, Type.ENTITY_MODIFIED, source, Optional.empty());
  }
}
