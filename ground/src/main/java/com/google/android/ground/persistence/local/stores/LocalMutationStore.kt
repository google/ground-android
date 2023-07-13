/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.persistence.local.stores

import com.google.android.ground.model.mutation.Mutation
import io.reactivex.Completable

/**
 * A data store capable of applying changes in the form of [Mutation]s to existing data and managing
 * a queue of [Mutations]s for synchronization with other store(s).
 */
interface LocalMutationStore<T : Mutation, M> {
  /**
   * Applies enqueued mutations to an entity then saves it to the local database, ensuring the
   * latest version of the data is retained.
   */
  fun merge(model: M): Completable

  /** Enqueue a mutation to be applied to the remote data store. */
  fun enqueue(mutation: T): Completable

  /** Applies a mutation to the local data store. */
  fun apply(mutation: T): Completable

  /** Updates specified mutations in the local queue. */
  @Deprecated("Use updateAllSuspend instead") fun updateAll(mutations: List<T>): Completable

  /** Updates specified mutations in the local queue. */
  suspend fun updateAllSuspend(mutations: List<T>)

  /**
   * Applies mutations to locally stored data, then enqueues the mutation for use when merging
   * runtime model objects.
   */
  fun applyAndEnqueue(mutation: T): Completable
}
