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
import static com.google.android.gnd.util.Streams.toImmutableList;
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.service.RemoteDataService;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Timestamps;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.SnapshotMetadata;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreDataService implements RemoteDataService {

  private static final FirebaseFirestoreSettings FIRESTORE_SETTINGS =
      new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
  private static final SetOptions MERGE = SetOptions.merge();
  private static final String TAG = FirestoreDataService.class.getSimpleName();
  private final FirebaseFirestore db;

  @Inject
  FirestoreDataService() {
    this.db = FirebaseFirestore.getInstance();
    db.setFirestoreSettings(FIRESTORE_SETTINGS);
    FirebaseFirestore.setLoggingEnabled(true);
  }

  static Timestamps toTimestamps(@Nullable Date created, @Nullable Date modified) {
    Timestamps.Builder timestamps = Timestamps.newBuilder();
    if (created != null) {
      timestamps.setCreated(created);
    }
    if (modified != null) {
      timestamps.setModified(modified);
    }
    return timestamps.build();
  }

  private GndFirestorePath db() {
    return GndFirestorePath.db(db);
  }

  @Override
  public Single<Project> loadProject(String projectId) {
    return RxFirestore.getDocument(db().project(projectId).ref())
        .flatMap(this::loadPlaceTypes, ProjectDoc::toProto)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()));
  }

  /** Loads place types and related forms. */
  private Maybe<List<PlaceType>> loadPlaceTypes(DocumentSnapshot projectDocSnapshot) {
    // TODO: Return Single.
    return RxFirestore.getCollection(
            GndFirestorePath.project(projectDocSnapshot).placeTypes().ref())
        .map(QuerySnapshot::getDocuments) // Maybe<List<DocumentSnapshot>>
        .toObservable()
        .flatMapIterable(i -> i) // Observable<DocumentSnapshot>>
        .flatMap(this::loadForms, PlaceTypeDoc::toProto)
        .toList()
        .toMaybe();
  }

  private Observable<ImmutableList<Form>> loadForms(DocumentSnapshot placeTypeDocSnapshot) {
    // TODO: Return Single.
    return RxFirestore.getCollection(GndFirestorePath.placeType(placeTypeDocSnapshot).forms().ref())
        .map(QuerySnapshot::getDocuments)
        .map(
            formDocSnapshots ->
                stream(formDocSnapshots).map(FormDoc::toProto).collect(toImmutableList()))
        .defaultIfEmpty(ImmutableList.of())
        .toObservable();
  }

  @Override
  public Single<List<Record>> loadRecordSummaries(Place place) {
    return mapSingle(
        RxFirestore.getCollection(
            db().project(place.getProject().getId()).place(place.getId()).records().ref()),
        doc -> RecordDoc.toProto(place, doc.getId(), doc));
  }

  @Override
  public Single<Record> loadRecordDetails(Place place, String recordId) {
    return RxFirestore.getDocument(
            db().project(place.getProject().getId())
                .place(place.getId())
                .records()
                .record(recordId)
                .ref())
        .map(doc -> RecordDoc.toProto(place, doc.getId(), doc))
        .toSingle();
  }

  @Override
  public Single<List<Project>> loadProjectSummaries() {
    return mapSingle(RxFirestore.getCollection(db().projects().ref()), ProjectDoc::toProto);
  }

  @Override
  public Flowable<DatastoreEvent<Place>> getPlaceVectorStream(Project project) {
    return RxFirestore.observeQueryRef(db().project(project.getId()).places().ref())
        .flatMapIterable(
            placeQuerySnapshot ->
                toDatastoreEvents(
                    placeQuerySnapshot,
                    placeDocSnapshot -> PlaceDoc.toProto(project, placeDocSnapshot)))
        .doOnTerminate(
            () ->
                Log.d(
                    TAG,
                    "getPlaceVectorStream stream for project " + project.getId() + " terminated."));
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

  @NonNull
  private static String toString(DocumentChange dc) {
    return dc.getDocument().getReference().getPath() + " " + dc.getType();
  }

  private static DatastoreEvent.Source getSource(SnapshotMetadata metadata) {
    return metadata.hasPendingWrites()
        ? DatastoreEvent.Source.LOCAL_DATASTORE
        : DatastoreEvent.Source.REMOTE_DATASTORE;
  }

  // TODO: Move relevant Record fields and updates into "RecordUpdate" object.
  @Override
  public Completable saveChanges(Record record, ImmutableList<ValueUpdate> updates) {
    return RxTask.toCompletable(
      () ->
        db.batch()
          .set(
            db().projects()
                .project(record.getProject().getId())
                .places()
                .place(record.getPlace().getId())
                .records()
                .record(record.getId())
                .ref(),
            RecordDoc.forUpdates(record, updatedValues(updates)),
            MERGE)
          .commit());
  }

  private Map<String, Object> updatedValues(ImmutableList<ValueUpdate> updates) {
    Map<String, Object> updatedValues = new HashMap<>();
    for (ValueUpdate valueUpdate : updates) {
      switch (valueUpdate.getOperation()) {
        case CREATE:
        case UPDATE:
          valueUpdate
            .getValue()
            .ifPresent(
              value ->
                updatedValues.put(valueUpdate.getElementId(), RecordDoc.toObject(value)));
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
