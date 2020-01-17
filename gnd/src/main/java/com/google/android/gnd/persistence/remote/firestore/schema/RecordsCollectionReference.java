package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.persistence.remote.firestore.ObservationDoc;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreCollectionReference;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Query;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Single;

public class RecordsCollectionReference extends FirestoreCollectionReference {

  public static final String NAME = "observations";

  protected RecordsCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public RecordDocumentReference record(String id) {
    return new RecordDocumentReference(ref.document(id));
  }

  public Single<ImmutableList<Observation>> recordsByFeatureId(Feature feature) {
    return RxFirestore.getCollection(byFeatureId(feature.getId()))
        .map(
            querySnapshot ->
                stream(querySnapshot.getDocuments())
                    .map(
                        recordDoc ->
                            ObservationDoc.toObject(feature, recordDoc.getId(), recordDoc))
                    .collect(toImmutableList()))
        .toSingle(ImmutableList.of());
  }

  private Query byFeatureId(String featureId) {
    return ref().whereEqualTo(FieldPath.of(ObservationDoc.FEATURE_ID), featureId);
  }
}
