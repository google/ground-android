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

package com.google.android.gnd.persistence.remote.firestore.fluent;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import com.google.android.gnd.rx.RxCompletable;
import com.google.android.gnd.system.NetworkManager;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;

/** Base class for representing Firestore databases as object hierarchies. */
public abstract class AbstractFluentFirestore {
  protected final FirebaseFirestore db;

  protected AbstractFluentFirestore(FirebaseFirestore db) {
    this.db = db;
  }

  /** Returns true iff the an active network connection is available. */
  private static boolean isNetworkAvailable(FirebaseFirestore db) {
    Context context = db.getApp().getApplicationContext();
    return NetworkManager.isNetworkAvailable(context);
  }

  /**
   * Returns a Completable that completes immediately on subscribe if network is available, or fails
   * in error if not.
   */
  private static Completable requireActiveNetwork(FirebaseFirestore db) {
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

  // TOOD: Wrap in fluent version of WriteBatch.
  public WriteBatch batch() {
    return db.batch();
  }

  protected abstract static class FluentCollectionReference {
    protected final CollectionReference ref;

    protected FluentCollectionReference(CollectionReference ref) {
      this.ref = ref;
    }

    /**
     * Returns a Completable that completes immediately on subscribe if network is available, or
     * fails in error if not.
     */
    protected Completable requireActiveNetwork() {
      return AbstractFluentFirestore.requireActiveNetwork(ref.getFirestore());
    }

    /**
     * Runs the specified query, returning a Single containing a List of values created by applying
     * the mappingFunction to all results. Fails immediately with an error if an active network is
     * not available.
     */
    protected <T> Single<List<T>> runQuery(
        Query query, Function<DocumentSnapshot, T> mappingFunction) {
      return requireActiveNetwork()
          .andThen(toSingleList(RxFirestore.getCollection(query), mappingFunction));
    }

    public CollectionReference ref() {
      return ref;
    }

    @Override
    public String toString() {
      return ref.getPath();
    }
  }

  protected static class FluentDocumentReference {
    protected final DocumentReference ref;

    protected FluentDocumentReference(DocumentReference ref) {
      this.ref = ref;
    }

    /**
     * Adds a request to the specified batch to merge the provided key-value pairs into the remote
     * database. If the document does not yet exist, one is created on commit.
     */
    public void merge(ImmutableMap<String, Object> values, WriteBatch batch) {
      batch.set(ref, values, SetOptions.merge());
    }

    public DocumentReference ref() {
      return ref;
    }

    @Override
    public String toString() {
      return ref.getPath();
    }
  }
}
