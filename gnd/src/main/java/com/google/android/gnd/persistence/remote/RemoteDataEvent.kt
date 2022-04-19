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
package com.google.android.gnd.persistence.remote

import com.google.android.gnd.rx.ValueOrError

/**
 * An event returned by the remote data store indicating either an entity was successfully loaded,
 * or that it was modified or removed in the remote data store.
 *
 * @param <T> the type of entity being loaded, modified, or removed.
 *
 * TODO: Replace with kotlin.Result once migration to Kotlin is complete. For more info, see this
 *  https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/
 */
class RemoteDataEvent<T> private constructor(
    val entityId: String, val eventType: EventType, entity: T?, error: Throwable?
) : ValueOrError<T>(entity, error) {

    enum class EventType {
        ENTITY_LOADED, ENTITY_MODIFIED, ENTITY_REMOVED, ERROR
    }

    companion object {
        @JvmStatic
        fun <T> loaded(entityId: String, entity: T): RemoteDataEvent<T> =
            RemoteDataEvent(entityId, EventType.ENTITY_LOADED, entity, null)

        @JvmStatic
        fun <T> modified(entityId: String, entity: T): RemoteDataEvent<T> =
            RemoteDataEvent(entityId, EventType.ENTITY_MODIFIED, entity, null)

        @JvmStatic
        fun <T> removed(entityId: String): RemoteDataEvent<T?> =
            RemoteDataEvent(entityId, EventType.ENTITY_REMOVED, null, null)

        // TODO (donturner): Consider moving errors into a RemoteDataException class.
        //  This avoids the need to supply an entityId. It is currently set to "ERROR" as a temporary
        //  measure.
        @JvmStatic
        fun <T> error(error: Throwable?): RemoteDataEvent<T?> =
            RemoteDataEvent("ERROR", EventType.ERROR, null, error)
    }
}