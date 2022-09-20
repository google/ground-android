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
package com.google.android.ground.persistence.remote

import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

/**
 * An event returned by the remote data store indicating either an entity was successfully loaded,
 * or that it was modified or removed in the remote data store.
 *
 * @param <T> the type of entity being loaded, modified, or removed.
 */
class RemoteDataEvent<T>
private constructor(val eventType: EventType, val result: Result<Pair<String, T?>>) {

  enum class EventType {
    ENTITY_LOADED,
    ENTITY_MODIFIED,
    ENTITY_REMOVED,
    ERROR
  }

  companion object {
    @JvmStatic
    fun <T> loaded(entityId: String, entity: T): RemoteDataEvent<T> =
      RemoteDataEvent(EventType.ENTITY_LOADED, success(Pair(entityId, entity)))

    @JvmStatic
    fun <T> modified(entityId: String, entity: T): RemoteDataEvent<T> =
      RemoteDataEvent(EventType.ENTITY_MODIFIED, success(Pair(entityId, entity)))

    @JvmStatic
    fun <T> removed(entityId: String): RemoteDataEvent<T> =
      RemoteDataEvent(EventType.ENTITY_REMOVED, success(Pair(entityId, null)))

    @JvmStatic
    fun <T> error(error: Throwable): RemoteDataEvent<T> =
      RemoteDataEvent(EventType.ERROR, failure(error))
  }
}
