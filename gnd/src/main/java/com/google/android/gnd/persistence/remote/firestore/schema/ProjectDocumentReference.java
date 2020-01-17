package com.google.android.gnd.persistence.remote.firestore.schema;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.persistence.remote.firestore.ProjectDoc;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreDocumentReference;
import com.google.firebase.firestore.DocumentReference;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Maybe;

public class ProjectDocumentReference extends FirestoreDocumentReference {
  protected ProjectDocumentReference(DocumentReference ref) {
    super(ref);
  }

  public FeatureCollectionReference features() {
      return new FeatureCollectionReference(ref.collection(FeatureCollectionReference.NAME));
  }

  public RecordsCollectionReference records() {
    return new RecordsCollectionReference(ref.collection(RecordsCollectionReference.NAME));
  }

  public Maybe<Project> get() {
    return RxFirestore.getDocument(ref).map(ProjectDoc::toObject);
  }
}
