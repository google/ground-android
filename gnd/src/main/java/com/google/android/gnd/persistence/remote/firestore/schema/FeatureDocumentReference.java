package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.data;
import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.geoPoint;
import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.string;

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreData;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreDocumentReference;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreField;
import com.google.android.gnd.persistence.remote.firestore.converters.FeatureMutationConverter;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.WriteBatch;

public class FeatureDocumentReference extends FirestoreDocumentReference {
  // TODO: Rename db field name.
  public static final FirestoreField<String> LAYER_ID = string("featureTypeId");
  // TODO: Rename db field name.
  public static final FirestoreField<GeoPoint> LOCATION = geoPoint("center");
  public static final FirestoreField<FirestoreData> CREATED = data("created");
  public static final FirestoreField<FirestoreData> LAST_MODIFIED = data("lastModified");

  protected FeatureDocumentReference(DocumentReference ref) {
    super(ref);
  }

  /** Appends the operation described by the specified mutation to the provided write batch. */
  public void addMutationToBatch(FeatureMutation mutation, User user, WriteBatch batch) {
    switch (mutation.getType()) {
      case CREATE:
      case UPDATE:
        merge(FeatureMutationConverter.toFirestoreData(mutation, user), batch);
        break;
      case DELETE:
        // TODO: Implement me!
        break;
      default:
        throw new IllegalArgumentException("Unknown mutation type " + mutation.getType());
    }
  }

  public RecordsCollectionReference records() {
    return new RecordsCollectionReference(ref.collection(RecordsCollectionReference.NAME));
  }
}
