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

package com.google.android.ground.persistence.remote.firestore.base

import com.google.android.ground.rx.annotations.Cold
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.reactivex.Maybe
import io.reactivex.Single
import java8.util.function.Function

/** Base class for representing Firestore databases as object hierarchies. */
abstract class FluentFirestore protected constructor(private val db: FirebaseFirestore) {

  protected fun db(): FirebaseFirestore = db

  // TODO: Wrap in fluent version of WriteBatch.
  fun batch(): WriteBatch {
    return db.batch()
  }

  companion object {
    /**
     * Applies the provided mapping function to each document in the specified query snapshot, if
     * present. If no results are present, completes with an empty list.
     */
    fun <T> toSingleList(
      result: Maybe<QuerySnapshot>,
      mappingFunction: Function<DocumentSnapshot, T>
    ): @Cold Single<List<T>> {
      return result
        .map { querySnapshot: QuerySnapshot ->
          querySnapshot.documents.map { mappingFunction.apply(it) }
        }
        .toSingle(emptyList())
    }
  }
}
