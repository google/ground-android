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

import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.system.NetworkManager;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java8.util.function.Function;

public abstract class FluentCollectionReference {
  private final CollectionReference reference;

  protected FluentCollectionReference(CollectionReference reference) {
    this.reference = reference;
  }

  /**
   * Returns a Completable that completes immediately on subscribe if network is available, or fails
   * in error if not.
   */
  @Cold
  private Completable requireActiveNetwork() {
    return NetworkManager.requireActiveNetwork(
        reference.getFirestore().getApp().getApplicationContext());
  }

  /**
   * Runs the specified query, returning a Single containing a List of values created by applying
   * the mappingFunction to all results. Fails immediately with an error if an active network is not
   * available.
   */
  @Cold
  protected <T> Single<List<T>> runQuery(
      Query query, Function<DocumentSnapshot, T> mappingFunction) {
    return requireActiveNetwork()
        .andThen(FluentFirestore.toSingleList(RxFirestore.getCollection(query), mappingFunction));
  }

  protected CollectionReference reference() {
    return reference;
  }

  @Override
  public String toString() {
    return reference.getPath();
  }
}
