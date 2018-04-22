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

package com.google.android.gnd.service.firestore;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestorePathBuilder {
  private final FirebaseFirestore db;

  protected FirestorePathBuilder(FirebaseFirestore db) {
    this.db = db;
  }

  protected CollectionReference collection(String name) {
    return db.collection(name);
  }

  protected static class FluentCollectionReference {
    private CollectionReference ref;

    protected <T extends FluentCollectionReference> T setRef(CollectionReference ref) {
      this.ref = ref;
      return (T) this;
    }

    protected DocumentReference document(String id) {
      return ref.document(id);
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
    private DocumentReference ref;

    protected <T extends FluentDocumentReference> T setRef(DocumentReference ref) {
      this.ref = ref;
      return (T) this;
    }

    protected CollectionReference collection(String id) {
      return ref.collection(id);
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
