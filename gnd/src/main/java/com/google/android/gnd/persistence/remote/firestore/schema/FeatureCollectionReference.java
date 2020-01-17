package com.google.android.gnd.persistence.remote.firestore.schema;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreCollectionReference;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreUtil;
import com.google.android.gnd.persistence.remote.firestore.converters.FeatureDataConverter;
import com.google.firebase.firestore.CollectionReference;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;

public class FeatureCollectionReference extends FirestoreCollectionReference {

  public static final String NAME = "features";

  protected FeatureCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public FeatureDocumentReference feature(String id) {
    return new FeatureDocumentReference(ref.document(id));
  }

  // TODO: Rename to streamChanges()?
  public Flowable<RemoteDataEvent<Feature>> observe(Project project) {
    return RxFirestore.observeQueryRef(ref)
        .flatMapIterable(
            featureQuerySnapshot ->
                FirestoreUtil.toEvents(
                    featureQuerySnapshot,
                    featureDocSnapshot ->
                        FeatureDataConverter.toFeature(featureDocSnapshot, project)));
  }
}
