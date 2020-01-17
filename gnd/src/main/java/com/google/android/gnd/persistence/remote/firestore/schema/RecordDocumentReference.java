package com.google.android.gnd.persistence.remote.firestore.schema;

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.remote.firestore.ObservationDoc;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreDocumentReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Maybe;

public class RecordDocumentReference extends FirestoreDocumentReference {
  protected RecordDocumentReference(DocumentReference ref) {
    super(ref);
  }

  public Maybe<Observation> get(Feature feature) {
    return RxFirestore.getDocument(ref)
        .map(doc -> ObservationDoc.toObject(feature, doc.getId(), doc));
  }

  /** Appends the operation described by the specified mutation to the provided write batch. */
  public void addMutationToBatch(ObservationMutation mutation, User user, WriteBatch batch) {
    switch (mutation.getType()) {
      case CREATE:
      case UPDATE:
        merge(ObservationDoc.toMap(mutation, user), batch);
        break;
      case DELETE:
        // TODO: Implement me!
        break;
      default:
        throw new IllegalArgumentException("Unknown mutation type " + mutation.getType());
    }
  }
}
