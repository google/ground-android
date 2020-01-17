package com.google.android.gnd.persistence.remote.firestore.base;

import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

public abstract class FirestoreDocumentReference {
  protected final DocumentReference ref;

  protected FirestoreDocumentReference(DocumentReference ref) {
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
