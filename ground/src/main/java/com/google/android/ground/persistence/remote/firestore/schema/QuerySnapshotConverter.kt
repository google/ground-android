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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.error
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.loaded
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.modified
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.removed
import com.google.android.ground.util.toImmutableList
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import java8.util.function.Function
import timber.log.Timber

/**
 * Converts Firestore [com.google.firebase.firestore.QuerySnapshot] to application-specific objects.
 */
internal object QuerySnapshotConverter {

  /** Applies a converter function to document change events in the specified query snapshot. */
  fun <T> toEvents(
    snapshot: QuerySnapshot,
    converter: Function<DocumentSnapshot, Result<T>>
  ): Iterable<RemoteDataEvent<T>> =
    snapshot.documentChanges.map { dc: DocumentChange -> toEvent(dc, converter) }.toImmutableList()

  private fun <T> toEvent(
    dc: DocumentChange,
    converter: Function<DocumentSnapshot, Result<T>>
  ): RemoteDataEvent<T> =
    try {
      Timber.v("${dc.document.reference.path}  ${dc.type}")
      val id = dc.document.id
      when (dc.type) {
        DocumentChange.Type.ADDED -> loaded(id, converter.apply(dc.document).getOrThrow())
        DocumentChange.Type.MODIFIED -> modified(id, converter.apply(dc.document).getOrThrow())
        DocumentChange.Type.REMOVED -> removed(id)
        else -> throw DataStoreException("Unknown DocumentChange type: ${dc.type}")
      }
    } catch (t: Throwable) {
      error(t)
    }
}
