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

import com.google.android.gnd.rx.annotations.Cold;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;

/** Base class for representing Firestore databases as object hierarchies. */
public abstract class FluentFirestore {
  private final FirebaseFirestore db;

  protected FluentFirestore(FirebaseFirestore db) {
    this.db = db;
  }

  protected FirebaseFirestore db() {
    return db;
  }

  /**
   * Applies the provided mapping function to each document in the specified query snapshot, if
   * present. If no results are present, completes with an empty list.
   */
  @Cold
  static <T> Single<List<T>> toSingleList(
      Maybe<QuerySnapshot> result, Function<DocumentSnapshot, T> mappingFunction) {
    return result
        .map(
            querySnapshot ->
                stream(querySnapshot.getDocuments()).map(mappingFunction).collect(toList()))
        .toSingle(Collections.emptyList());
  }

  // TODO: Wrap in fluent version of WriteBatch.
  public WriteBatch batch() {
    return db.batch();
  }
}
