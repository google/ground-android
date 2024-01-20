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

package com.google.android.ground.persistence.remote.firebase.base

import com.google.android.ground.system.NetworkManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import java8.util.function.Function
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import timber.log.Timber

open class FluentCollectionReference
protected constructor(
  private val reference: CollectionReference,
  protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  protected val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

  private val context = reference.firestore.app.applicationContext

  /**
   * Runs the specified query, returning a Single containing a List of values created by applying
   * the mappingFunction to all results. Fails immediately with an error if an active network is not
   * available.
   */
  protected suspend fun <T> runQuery(
    query: Query,
    mappingFunction: Function<DocumentSnapshot, T>,
  ): List<T> {
    NetworkManager(context).requireNetworkConnection()
    val querySnapshot = query.get().await()
    return querySnapshot.documents
      .filter { it.exists() }
      .mapNotNull { applyFunctionAndIgnoreFailures(it, mappingFunction) }
  }

  private fun <T> applyFunctionAndIgnoreFailures(
    value: DocumentSnapshot,
    mappingFunction: Function<DocumentSnapshot, T>,
  ): T? =
    try {
      mappingFunction.apply(value)
    } catch (e: Throwable) {
      Timber.e(e, "Skipping corrupt remote document: ${value.id}")
      null
    }

  protected fun reference(): CollectionReference = reference

  override fun toString(): String = reference.path
}
