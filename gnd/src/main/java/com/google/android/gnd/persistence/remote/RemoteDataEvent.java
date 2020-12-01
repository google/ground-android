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

import com.google.android.gnd.rx.ValueOrError;
import javax.annotation.Nullable;

/**
 * An event returned by the remote data store indicating either an entity was successfully loaded,
 * or that it was modified or removed in the remote data store.
 *
 * @param <T> the type of entity being loaded, modified, or removed.
 */
public class RemoteDataEvent<T> extends ValueOrError<T> {
  public enum EventType {
    ENTITY_LOADED,
    ENTITY_MODIFIED,
    ENTITY_REMOVED,
    ERROR
  }

  private String entityId;
  private EventType eventType;

  private RemoteDataEvent(
      String entityId, EventType eventType, @Nullable T entity, @Nullable Throwable error) {
    super(entity, error);
    this.entityId = entityId;
    this.eventType = eventType;
  }

  public String getEntityId() {
    return entityId;
  }

  public EventType getEventType() {
    return eventType;
  }

  public boolean isValid() {
    return !EventType.ERROR.equals(eventType);
  }

  public static <T> RemoteDataEvent<T> loaded(String entityId, T entity) {
    return new RemoteDataEvent<>(entityId, EventType.ENTITY_LOADED, entity, null);
  }

  public static <T> RemoteDataEvent<T> modified(String entityId, T entity) {
    return new RemoteDataEvent<>(entityId, EventType.ENTITY_MODIFIED, entity, null);
  }

  public static <T> RemoteDataEvent<T> removed(String entityId) {
    return new RemoteDataEvent<>(entityId, EventType.ENTITY_REMOVED, null, null);
  }

  // TODO (donturner): Consider moving errors into a RemoteDataException class.
  //  This avoids the need to supply an entityId. It is currently set to "ERROR" as a temporary
  //  measure.
  public static <T> RemoteDataEvent<T> error(Throwable error) {
    return new RemoteDataEvent<>("ERROR", EventType.ERROR, null, error);
  }
}
