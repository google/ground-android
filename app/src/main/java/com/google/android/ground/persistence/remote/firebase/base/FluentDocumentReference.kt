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

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch

open class FluentDocumentReference protected constructor(private val reference: DocumentReference) {

  /**
   * Adds a request to the specified batch to merge the provided key-value pairs into the remote
   * database. If the document does not yet exist, one is created on commit.
   */
  protected fun merge(values: Map<String, Any>, batch: WriteBatch) {
    batch[reference, values] = SetOptions.merge()
  }

  /** Adds a request to the specified batch to delete the current DocumentReference. */
  protected fun delete(batch: WriteBatch) {
    batch.delete(reference)
  }

  protected fun reference(): DocumentReference = reference

  override fun toString(): String = reference.path
}
