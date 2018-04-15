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
package com.google.gnd.service.firestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryListenOptions;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.gnd.model.Feature;
import com.google.gnd.model.FeatureType;
import com.google.gnd.model.FeatureUpdate;
import com.google.gnd.model.FeatureUpdate.RecordUpdate;
import com.google.gnd.model.FeatureUpdate.RecordUpdate.ValueUpdate;
import com.google.gnd.model.Form;
import com.google.gnd.model.Project;
import com.google.gnd.model.Record;
import com.google.gnd.model.Timestamps;
import com.google.gnd.service.DataService;
import com.google.protobuf.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import java8.util.concurrent.CompletableFuture;

import static com.google.gnd.util.Futures.allOf;
import static com.google.gnd.util.Futures.fromTask;
import static com.google.gnd.util.Streams.map;
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

@Singleton
public class FirestoreDataService implements DataService {
  public static final FirebaseFirestoreSettings FIRESTORE_SETTINGS =
      new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
  public static final SetOptions MERGE = SetOptions.merge();
  private static final String TAG = FirestoreDataService.class.getSimpleName();
  private FirebaseFirestore db;
  private ListenerRegistration featureListenerRegistration;
  private List<DataChangeListener<Feature>> featureChangeListeners;

  @Inject
  FirestoreDataService() {
    featureChangeListeners = new ArrayList<>();
  }

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

  static Timestamp.Builder toTimestamp(@NonNull Date serverTimeCreated) {
    return Timestamp.newBuilder().setSeconds(serverTimeCreated.getTime() / 1000);
  }

  public void onCreate() {
    db = FirebaseFirestore.getInstance();
    db.setFirestoreSettings(FIRESTORE_SETTINGS);
    db.setLoggingEnabled(true);
  }

  @Override
  public void addFeatureChangeListener(DataChangeListener<Feature> featureChangeListener) {
    featureChangeListeners.add(featureChangeListener);
  }

  // TODO: Naming: fetch - get doc, load - get object/proto.
  @Override
  public CompletableFuture<Project> loadProject(String projectId) {
    return fetchDocument(project(projectId))
        .thenCompose(p -> fetchFeatureTypes(p).thenApply(fts -> ProjectDoc.toProto(p, fts)));
  }

  private CompletableFuture<List<FeatureType>> fetchFeatureTypes(DocumentSnapshot project) {
    return fetchDocuments(featureTypes(project)).thenCompose(this::loadAndAssembleForms);
  }

  private CompletableFuture<List<FeatureType>> loadAndAssembleForms(
      List<DocumentSnapshot> featureTypes) {
    return allOf(map(featureTypes, d -> loadForms(d).thenApply(f -> FeatureTypeDoc.toProto(d, f))));
  }

  private CompletableFuture<List<Form>> loadForms(DocumentSnapshot featureType) {
    return fetchDocuments(forms(featureType)).thenApply(docs -> map(docs, FormDoc::toProto));
  }

  private CompletableFuture<List<DocumentSnapshot>> fetchDocuments(CollectionReference coll) {
    return fromTask(coll.get(), t -> t.getDocuments());
  }

  private CompletableFuture<DocumentSnapshot> fetchDocument(DocumentReference doc) {
    return fromTask(doc.get());
  }

  @Override
  public void removeAllListeners() {
    if (featureListenerRegistration != null) {
      featureListenerRegistration.remove();
      featureListenerRegistration = null;
    }
  }

  @Override
  public void listenForFeatureChanges(String projectId) {
    CollectionReference featuresCollection = features(projectId);
    featureListenerRegistration =
        featuresCollection.addSnapshotListener(
            new QueryListenOptions().includeDocumentMetadataChanges().includeQueryMetadataChanges(),
            (snapshots, e) -> {
              if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                return;
              }
              Log.d(TAG, "Received " + snapshots.getDocumentChanges().size() + " feature updates.");
              dispatchChanges(snapshots, FeatureDoc::toProto, featureChangeListeners);
            });
  }

  private <T> void dispatchChanges(
      QuerySnapshot snapshot,
      java8.util.function.Function<DocumentSnapshot, T> converter,
      List<DataChangeListener<T>> listeners) {
    boolean hasPendingWrites = snapshot.getMetadata().hasPendingWrites();
    for (DocumentChange dc : snapshot.getDocumentChanges()) {
      String id = dc.getDocument().getId();
      DocumentSnapshot doc = dc.getDocument();
      T obj = doc == null ? null : converter.apply(doc);
      DataChangeListener.ChangeType type = getChangeType(dc.getType());
      // TODO: Remove id, since it's already in the proto.
      stream(listeners).forEach(l -> l.onChange(id, obj, type, hasPendingWrites));
    }
  }

  @NonNull
  private DataChangeListener.ChangeType getChangeType(DocumentChange.Type type) {
    switch (type) {
      case ADDED:
        return DataChangeListener.ChangeType.ADDED;
      case MODIFIED:
        return DataChangeListener.ChangeType.MODIFIED;
      case REMOVED:
        return DataChangeListener.ChangeType.REMOVED;
      default:
        return DataChangeListener.ChangeType.UNKNOWN;
    }
  }

  // Differentiate generic "update" (CRUD operation) from database "update".
  @Override
  public Feature update(String projectId, FeatureUpdate featureUpdate) {
    // NOTE: Batched writes are atomic in Firestore. We always update the timestamps on
    // Features, even when Records are added or modified, so that there will always a
    // pending write on the Feature until the Record is written. We then can use hasPendingWrites
    // on the Feature to guarantee all related updates have been written.
    switch (featureUpdate.getOperation()) {
      case CREATE:
        return createFeature(projectId, featureUpdate);
      case UPDATE:
      case NO_CHANGE:
        return updateFeature(projectId, featureUpdate);
        // TODO: Delete..
      default:
        throw new IllegalArgumentException("Unknown update type: " + featureUpdate.getOperation());
    }
  }

  private Feature createFeature(String projectId, FeatureUpdate featureUpdate) {
    WriteBatch batch = db.batch();
    Feature.Builder feature = featureUpdate.getFeature().toBuilder();
    DocumentReference fdRef = features(projectId).document();
    feature.setId(fdRef.getId());
    feature.clearServerTimestamps();
    feature.setClientTimestamps(
        Timestamps.newBuilder()
            .setCreated(featureUpdate.getClientTimestamp())
            .setModified(featureUpdate.getClientTimestamp()));
    batch.set(fdRef, FeatureDoc.fromProto(feature.build()));
    updateRecords(batch, fdRef, featureUpdate);
    // We don't wait for commit() to finish because task only completes once data is stored to
    // server.
    batch.commit();
    // Pass feature back with ID populated.
    return feature.build();
  }

  private Feature updateFeature(String projectId, FeatureUpdate featureUpdate) {
    WriteBatch batch = db.batch();
    Feature.Builder feature = featureUpdate.getFeature().toBuilder();
    DocumentReference fdRef = features(projectId).document(feature.getId());
    feature.setServerTimestamps(feature.getServerTimestamps().toBuilder().clearModified());
    feature.setClientTimestamps(
        feature.getClientTimestamps().toBuilder().setModified(featureUpdate.getClientTimestamp()));
    batch.set(fdRef, FeatureDoc.fromProto(feature.build()), MERGE);
    updateRecords(batch, fdRef, featureUpdate);
    batch.commit();
    return feature.build();
  }

  @Override
  public CompletableFuture<List<Record>> loadRecordData(String projectId, String featureId) {
    return fetchDocuments(records(feature(projectId, featureId)))
        .thenApply(docs -> map(docs, doc -> RecordDoc.toProto(doc.getId(), doc)));
  }

  // Db paths.

  @Override
  public CompletableFuture<List<Project>> getProjectSummaries() {
    return fetchDocuments(projects())
        .thenApply(docs -> stream(docs).map(ProjectDoc::toProto).collect(toList()));
  }

  private void updateRecords(
      WriteBatch batch, DocumentReference fdRef, FeatureUpdate featureUpdate) {
    CollectionReference records = records(fdRef);
    for (RecordUpdate recordUpdate : featureUpdate.getRecordUpdatesList()) {
      Record.Builder record = recordUpdate.getRecord().toBuilder();
      switch (recordUpdate.getOperation()) {
        case CREATE:
          record.setClientTimestamps(
              Timestamps.newBuilder()
                  .setCreated(featureUpdate.getClientTimestamp())
                  .setModified(featureUpdate.getClientTimestamp()));
          batch.set(
              records.document(), RecordDoc.fromProto(record.build(), updatedValues(recordUpdate)));
          break;
        case UPDATE:
          record.setClientTimestamps(
              record
                  .getClientTimestamps()
                  .toBuilder()
                  .setModified(featureUpdate.getClientTimestamp()));
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

  @NonNull
  private CollectionReference projects() {
    return db.collection("projects");
  }

  @NonNull
  private DocumentReference project(String projectId) {
    return projects().document(projectId);
  }

  @NonNull
  private CollectionReference features(String projectId) {
    return project(projectId).collection("features");
  }

  private DocumentReference feature(String projectId, String featureId) {
    return features(projectId).document(featureId);
  }

  private CollectionReference featureTypes(DocumentReference project) {
    return project.collection("featureTypes");
  }

  private CollectionReference featureTypes(DocumentSnapshot project) {
    return featureTypes(project.getReference());
  }

  private CollectionReference forms(DocumentReference featureType) {
    return featureType.collection("forms");
  }

  private CollectionReference forms(DocumentSnapshot featureType) {
    return forms(featureType.getReference());
  }

  @NonNull
  private CollectionReference records(DocumentReference feature) {
    return feature.collection("records");
  }
}
