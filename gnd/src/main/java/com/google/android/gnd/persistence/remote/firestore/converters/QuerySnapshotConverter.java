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

package com.google.android.gnd.persistence.remote.firestore.converters;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.util.Log;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.firestore.DataStoreException;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java8.util.function.Function;

/**
 * Converts Firestore {@link com.google.firebase.firestore.QuerySnapshot} to application-specific
 * objects.
 */
public class QuerySnapshotConverter {

  private static final String TAG = QuerySnapshotConverter.class.getSimpleName();

  /** Applies a converter function to document change events in the specified query snapshot. */
  public static <T> Iterable<RemoteDataEvent<T>> toEvents(
      QuerySnapshot snapshot, Function<DocumentSnapshot, T> converter) {
    return stream(snapshot.getDocumentChanges())
        .map(dc -> toEvent(dc, converter))
        .filter(RemoteDataEvent::isValid)
        .collect(toList());
  }

  private static <T> RemoteDataEvent<T> toEvent(
      DocumentChange dc, Function<DocumentSnapshot, T> converter) {
    Log.v(
        QuerySnapshotConverter.TAG, dc.getDocument().getReference().getPath() + " " + dc.getType());
    String id = dc.getDocument().getId();
    switch (dc.getType()) {
      case ADDED:
        return RemoteDataEvent.loaded(id, converter.apply(dc.getDocument()));
      case MODIFIED:
        return RemoteDataEvent.modified(id, converter.apply(dc.getDocument()));
      case REMOVED:
        return RemoteDataEvent.removed(id);
      default:
        return RemoteDataEvent.error(
            new DataStoreException("Unknown DocumentChange type: " + dc.getType()));
    }
  }
}
