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

import static com.google.android.gnd.rx.RxFirestoreUtil.mapSingle;
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gnd.repository.Form;
import com.google.android.gnd.repository.Place;
import com.google.android.gnd.repository.PlaceType;
import com.google.android.gnd.repository.PlaceUpdate;
import com.google.android.gnd.repository.PlaceUpdate.RecordUpdate;
import com.google.android.gnd.repository.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.repository.Project;
import com.google.android.gnd.repository.Record;
import com.google.android.gnd.repository.Timestamps;
import com.google.android.gnd.service.DataService;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.SnapshotMetadata;
import com.google.firebase.firestore.WriteBatch;
import com.google.protobuf.Timestamp;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreDataService implements DataService {

  public static final FirebaseFirestoreSettings FIRESTORE_SETTINGS =
      new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
  public static final SetOptions MERGE = SetOptions.merge();
  private static final String TAG = FirestoreDataService.class.getSimpleName();
  private FirebaseFirestore db;

  @Inject
  FirestoreDataService() {}

  // TODO: Move to shared util, since this isn't specific to Firebase.
  public static Date toDate(Timestamp timestamp) {
    return new Date(timestamp.getSeconds() * 1000);
  }

  static Timestamps toTimestamps(@Nullable Date created, @Nullable Date modified) {
    Timestamps.Builder timestamps = Timestamps.newBuilder();
    if (created != null) {
      timestamps.setCreated(toTimestamp(created));
    }
    if (modified != null) {
      timestamps.setModified(toTimestamp(modified));
    }
    return timestamps.build();
  }

  private static Timestamp.Builder toTimestamp(@NonNull Date serverTimeCreated) {
    return Timestamp.newBuilder().setSeconds(serverTimeCreated.getTime() / 1000);
  }

  @Override
  public void onCreate() {
    db = FirebaseFirestore.getInstance();
    db.setFirestoreSettings(FIRESTORE_SETTINGS);
    FirebaseFirestore.setLoggingEnabled(true);
  }

  private GndFirestorePath db() {
    return GndFirestorePath.db(db);
  }

  // TODO: Naming: fetch - get doc, load - get object/proto.
  @Override
  public Maybe<Project> loadProject(String projectId) {
    return RxFirestore.getDocument(db().project(projectId).ref())
                      .flatMap(
                        this::loadPlaceTypes,
                        (documentSnapshot, placeTypes) -> ProjectDoc.toProto(
                          documentSnapshot,
                          placeTypes));
  }

  /**
   * Loads place types and related forms.
   */
  private Maybe<List<PlaceType>> loadPlaceTypes(DocumentSnapshot projectDocSnapshot) {
    return RxFirestore.getCollection(
      GndFirestorePath.project(projectDocSnapshot).placeTypes().ref())
                      .map(QuerySnapshot::getDocuments) // Maybe<List<DocumentSnapshot>>
                      .toObservable()
                      .flatMapIterable(i -> i) // Observable<DocumentSnapshot>>
                      .flatMap(this::loadForms, PlaceTypeDoc::toProto)
                      .toList()
                      .toMaybe();
  }

  private Observable<List<Form>> loadForms(DocumentSnapshot placeTypeDocSnapshot) {
    return RxFirestore.getCollection(GndFirestorePath.placeType(placeTypeDocSnapshot).forms().ref())
                      .map(QuerySnapshot::getDocuments)
                      .map(
                        formDocSnapshots ->
                          stream(formDocSnapshots)
                            .map(FormDoc::toProto)
                            .collect(Collectors.toList()))
                      .toObservable();
  }

  // Differentiate generic "update" (CRUD operation) from database "update".
  @Override
  public Place update(String projectId, PlaceUpdate placeUpdate) {
    // NOTE: Batched writes are atomic in Firestore. We always update the timestamps on
    // Places, even when Records are added or modified, so that there will always a
    // pending write on the Place until the Record is written. We then can use hasPendingWrites
    // on the Place to guarantee all related updates have been written.
    Log.i(TAG, "Db op requested: " + placeUpdate);
    switch (placeUpdate.getOperation()) {
      case CREATE:
        return createPlace(projectId, placeUpdate);
      case UPDATE:
      case NO_CHANGE:
        return updatePlace(projectId, placeUpdate);
      case DELETE:
        // TODO: Implement delete..
      default:
        throw new IllegalArgumentException("Unknown update type: " + placeUpdate.getOperation());
    }
  }

  private Place createPlace(String projectId, PlaceUpdate placeUpdate) {
    WriteBatch batch = db.batch();
    Place.Builder place = placeUpdate.getPlace().toBuilder();
    DocumentReference fdRef = db().project(projectId).places().ref().document();
    place.setId(fdRef.getId());
    place.clearServerTimestamps();
    place.setClientTimestamps(
        Timestamps.newBuilder()
            .setCreated(placeUpdate.getClientTimestamp())
            .setModified(placeUpdate.getClientTimestamp()));
    batch.set(fdRef, PlaceDoc.fromProto(place.build()));
    updateRecords(batch, fdRef, placeUpdate);
    // We don't wait for commit() to finish because task only completes once data is stored to
    // server.
    batch.commit();
    // Pass place back with ID populated.
    return place.build();
  }

  private Place updatePlace(String projectId, PlaceUpdate placeUpdate) {
    WriteBatch batch = db.batch();
    Place.Builder place = placeUpdate.getPlace().toBuilder();
    DocumentReference fdRef = db().project(projectId).place(place.getId()).ref();
    place.setServerTimestamps(place.getServerTimestamps().toBuilder().clearModified());
    place.setClientTimestamps(
        place.getClientTimestamps().toBuilder().setModified(placeUpdate.getClientTimestamp()));
    batch.set(fdRef, PlaceDoc.fromProto(place.build()), MERGE);
    updateRecords(batch, fdRef, placeUpdate);
    batch.commit();
    return place.build();
  }

  @Override
  public Single<List<Record>> loadRecordData(String projectId, String placeId) {
    return mapSingle(
      RxFirestore.getCollection(db().project(projectId).place(placeId).records().ref()),
      doc -> RecordDoc.toProto(doc.getId(), doc));
  }

  @Override
  public Single<List<Project>> fetchProjectSummaries() {
    return mapSingle(RxFirestore.getCollection(db().projects().ref()), ProjectDoc::toProto);
  }

  @Override
  public Flowable<DatastoreEvent<Place>> observePlaces(String projectId) {
    return RxFirestore.observeQueryRef(db().project(projectId).places().ref())
                      .flatMapIterable(snapshot -> toDatastoreEvents(snapshot, PlaceDoc::toProto))
                      .doOnTerminate(
                        () -> Log.d(
                          TAG,
                          "observePlaces stream for project " + projectId + " terminated."));
  }

  private <T> Iterable<DatastoreEvent<T>> toDatastoreEvents(
      QuerySnapshot snapshot, Function<DocumentSnapshot, T> converter) {
    DatastoreEvent.Source source = getSource(snapshot.getMetadata());
    return stream(snapshot.getDocumentChanges())
      .map(dc -> toDatastoreEvent(dc, source, converter))
      .filter(DatastoreEvent::isValid)
      .collect(toList());
  }

  private <T> DatastoreEvent<T> toDatastoreEvent(
    DocumentChange dc, DatastoreEvent.Source source, Function<DocumentSnapshot, T> converter) {
    Log.v(TAG, toString(dc));
    String id = dc.getDocument().getId();
    switch (dc.getType()) {
      case ADDED:
        return DatastoreEvent.loaded(id, source, converter.apply(dc.getDocument()));
      case MODIFIED:
        return DatastoreEvent.modified(id, source, converter.apply(dc.getDocument()));
      case REMOVED:
        return DatastoreEvent.removed(id, source);
    }
    return DatastoreEvent.invalidResponse();
  }

  @NonNull
  private static String toString(DocumentChange dc) {
    return dc.getDocument().getReference().getPath() + " " + dc.getType();
  }

  private static DatastoreEvent.Source getSource(SnapshotMetadata metadata) {
    return metadata.hasPendingWrites()
        ? DatastoreEvent.Source.LOCAL_DATASTORE
        : DatastoreEvent.Source.REMOTE_DATASTORE;
  }

  private void updateRecords(
      WriteBatch batch, DocumentReference placeRef, PlaceUpdate placeUpdate) {
    CollectionReference records = GndFirestorePath.place(placeRef).records().ref();
    for (RecordUpdate recordUpdate : placeUpdate.getRecordUpdatesList()) {
      Record.Builder record = recordUpdate.getRecord().toBuilder();
      switch (recordUpdate.getOperation()) {
        case CREATE:
          record.setClientTimestamps(
              Timestamps.newBuilder()
                  .setCreated(placeUpdate.getClientTimestamp())
                  .setModified(placeUpdate.getClientTimestamp()));
          batch.set(
              records.document(), RecordDoc.fromProto(record.build(), updatedValues(recordUpdate)));
          break;
        case UPDATE:
          record.setClientTimestamps(
              record
                  .getClientTimestamps()
                  .toBuilder()
                  .setModified(placeUpdate.getClientTimestamp()));
          batch.set(
              records.document(record.getId()),
              RecordDoc.fromProto(record.build(), updatedValues(recordUpdate)),
              MERGE);
          break;
      }
    }
  }

  private Map<String, Object> updatedValues(RecordUpdate recordUpdate) {
    Map<String, Object> updatedValues = new HashMap<>();
    for (ValueUpdate valueUpdate : recordUpdate.getValueUpdatesList()) {
      switch (valueUpdate.getOperation()) {
        case CREATE:
        case UPDATE:
          updatedValues.put(valueUpdate.getElementId(), RecordDoc.toObject(valueUpdate.getValue()));
          break;
        case DELETE:
          // FieldValue.delete() is not working in nested objects; if it doesn't work in the future
          // we can remove them using dot notation ("responses.{elementId}").
          updatedValues.put(valueUpdate.getElementId(), "");
          break;
      }
    }
    return updatedValues;
  }
}
