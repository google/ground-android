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

import androidx.annotation.NonNull;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

public class FluentDocumentReference {
  private final DocumentReference reference;

  protected FluentDocumentReference(DocumentReference reference) {
    this.reference = reference;
  }

  /**
   * Adds a request to the specified batch to merge the provided key-value pairs into the remote
   * database. If the document does not yet exist, one is created on commit.
   */
  protected void merge(@NonNull ImmutableMap<String, Object> values, @NonNull WriteBatch batch) {
    batch.set(reference, values, SetOptions.merge());
  }

  /** Adds a request to the specified batch to delete the current DocumentReference. */
  protected void delete(@NonNull WriteBatch batch) {
    batch.delete(reference);
  }

  protected DocumentReference reference() {
    return reference;
  }

  @NonNull
  @Override
  public String toString() {
    return reference.getPath();
  }
}
