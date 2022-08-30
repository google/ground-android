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
import com.google.android.ground.system.NetworkManager.requireActiveNetwork
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Completable
import io.reactivex.Single
import java8.util.function.Function

abstract class FluentCollectionReference
protected constructor(private val reference: CollectionReference) {

  /**
   * Returns a Completable that completes immediately on subscribe if network is available, or fails
   * in error if not.
   */
  private fun requireActiveNetwork(): @Cold Completable {
    return requireActiveNetwork(reference.firestore.app.applicationContext)
  }

  /**
   * Runs the specified query, returning a Single containing a List of values created by applying
   * the mappingFunction to all results. Fails immediately with an error if an active network is not
   * available.
   */
  protected fun <T> runQuery(
    query: Query,
    mappingFunction: Function<DocumentSnapshot, T>
  ): @Cold Single<List<T>> {
    return requireActiveNetwork()
      .andThen(FluentFirestore.toSingleList(RxFirestore.getCollection(query), mappingFunction))
  }

  protected fun reference(): CollectionReference = reference

  override fun toString(): String = reference.path
}
