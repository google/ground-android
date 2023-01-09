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
import com.google.common.collect.ImmutableList
import io.reactivex.Completable

/**
 * [LocalStore]s that additionally manage data synchronization with remote storage using [Mutation]s
 * .
 */
interface LocalMutationStore<T : Mutation, M> : LocalStore<M> {
  /**
   * Commits enqueued mutations to a model object then saves it to the local database, ensuring the
   * latest version of the data is retained.
   */
  fun merge(model: M): Completable
  /** Queue a mutation for application to locally stored data. */
  fun enqueue(mutation: T): Completable
  /**
   * Applies a mutation to locally stored data. The local database will be updated according to the
   * changes in the mutation.
   */
  fun apply(mutation: T): Completable
  /** Updates all "re-queues" mutations in the list of provided list of mutations. */
  fun updateAll(mutations: ImmutableList<T>): Completable
  /**
   * Applies mutations to locally stored data, then enqueues the mutation for use when merging
   * runtime model objects.
   */
  fun applyAndEnqueue(mutation: T): Completable
}
