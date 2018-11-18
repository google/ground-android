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

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.util.Log;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SnapshotMetadata;
import com.google.firebase.firestore.WriteBatch;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java8.util.function.Function;

public abstract class AbstractFluentFirestore {
  private static final String TAG = AbstractFluentFirestore.class.getSimpleName();
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

    protected Flowable<QuerySnapshot> observe() {
      return RxFirestore.observeQueryRef(ref);
    }

    protected Single<DocumentReference> add(Object object) {
      return RxFirestore.addDocument(ref, object);
    }
  }

  protected static class FluentDocumentReference {
    protected final DocumentReference ref;

    protected FluentDocumentReference(DocumentReference ref) {
      this.ref = ref;
    }

    public DocumentReference ref() {
      return ref;
    }

    @Override
    public String toString() {
      return ref.getPath();
    }
  }

  protected static <T> Iterable<DatastoreEvent<T>> toDatastoreEvents(
      QuerySnapshot snapshot, Function<DocumentSnapshot, T> converter) {
    DatastoreEvent.Source source = getSource(snapshot.getMetadata());
    return stream(snapshot.getDocumentChanges())
        .map(dc -> toDatastoreEvent(dc, source, converter))
        .filter(DatastoreEvent::isValid)
        .collect(toList());
  }

  private static <T> DatastoreEvent<T> toDatastoreEvent(
      DocumentChange dc, DatastoreEvent.Source source, Function<DocumentSnapshot, T> converter) {
    Log.v(TAG, dc.getDocument().getReference().getPath() + " " + dc.getType());
    try {
      String id = dc.getDocument().getId();
      switch (dc.getType()) {
        case ADDED:
          return DatastoreEvent.loaded(id, source, converter.apply(dc.getDocument()));
        case MODIFIED:
          return DatastoreEvent.modified(id, source, converter.apply(dc.getDocument()));
        case REMOVED:
          return DatastoreEvent.removed(id, source);
      }
    } catch (DatastoreException e) {
      Log.d(TAG, "Datastore error:", e);
    }
    return DatastoreEvent.invalidResponse();
  }

  private static DatastoreEvent.Source getSource(SnapshotMetadata metadata) {
    return metadata.hasPendingWrites()
        ? DatastoreEvent.Source.LOCAL_DATASTORE
        : DatastoreEvent.Source.REMOTE_DATASTORE;
  }
}
