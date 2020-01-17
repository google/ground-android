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

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.firestore.DataStoreException;
import com.google.android.gnd.rx.RxCompletable;
import com.google.android.gnd.system.NetworkManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;

public class FirestoreUtil {

  /** Returns true iff the an active network connection is available. */
  public static boolean isNetworkAvailable(FirebaseFirestore db) {
    Context context = db.getApp().getApplicationContext();
    return NetworkManager.isNetworkAvailable(context);
  }

  /**
   * Returns a Completable that completes immediately on subscribe if network is available, or fails
   * in error if not.
   */
  public static Completable requireActiveNetwork(FirebaseFirestore db) {
    return RxCompletable.completeOrError(() -> isNetworkAvailable(db), ConnectException.class);
  }

  /**
   * Applies the provided mapping function to each document in the specified query snapshot, if
   * present. If no results are present, completes with an empty list.
   */
  static <T> Single<List<T>> toSingleList(
      Maybe<QuerySnapshot> result, Function<DocumentSnapshot, T> mappingFunction) {
    return result
        .map(
            querySnapshot ->
                stream(querySnapshot.getDocuments()).map(mappingFunction).collect(toList()))
        .toSingle(Collections.emptyList());
  }

  public static <T> Iterable<RemoteDataEvent<T>> toEvents(
      QuerySnapshot snapshot, Function<DocumentSnapshot, T> converter) {
    return stream(snapshot.getDocumentChanges())
        .map(dc -> toEvent(dc, converter))
        .filter(RemoteDataEvent::isValid)
        .collect(toList());
  }

  private static <T> RemoteDataEvent<T> toEvent(
      DocumentChange dc, Function<DocumentSnapshot, T> converter) {
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
