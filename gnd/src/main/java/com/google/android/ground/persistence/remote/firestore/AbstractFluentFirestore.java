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

package com.google.android.ground.persistence.remote.firestore;

import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

/** Base class for representing Firestore databases as object hierarchies. */
public abstract class AbstractFluentFirestore {
  protected final FirebaseFirestore db;

  protected AbstractFluentFirestore(FirebaseFirestore db) {
    this.db = db;
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
