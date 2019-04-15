/*
 * Copyright 2019 Google LLC
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

import androidx.annotation.Nullable;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.service.DataStoreEvent;
import com.google.android.gnd.service.RemoteDataStore;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.FeatureUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Timestamps;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreDataStore implements RemoteDataStore {

  private static final FirebaseFirestoreSettings FIRESTORE_SETTINGS =
      new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
  private static final SetOptions MERGE = SetOptions.merge();
  private static final String TAG = FirestoreDataStore.class.getSimpleName();
  private final GndFirestore db;

  @Inject
  FirestoreDataStore() {
    // TODO: Run on I/O thread, return asynchronously.
    final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    firestore.setFirestoreSettings(FIRESTORE_SETTINGS);
    FirebaseFirestore.setLoggingEnabled(true);
    db = new GndFirestore(firestore);
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

  @Override
  public Single<Project> loadProject(String projectId) {
    return db.projects()
        .project(projectId)
        .get()
        .switchIfEmpty(Single.error(new DocumentNotFoundException()));
  }

  @Override
  public Single<List<Record>> loadRecordSummaries(Feature feature) {
    return db.projects().project(feature.getProject().getId()).records().getByFeature(feature);
  }

  @Override
  public Single<Record> loadRecordDetails(Feature feature, String recordId) {
    // TODO: Replace Singles with Maybes?
    return db.projects()
        .project(feature.getProject().getId())
        .records()
        .record(recordId)
        .get(feature)
        .toSingle();
  }

  @Override
  public Single<List<Project>> loadProjectSummaries(User user) {
    return db.projects().getReadable(user);
  }

  @Override
  public Flowable<DataStoreEvent<Feature>> getFeatureVectorStream(Project project) {
    return db.projects().project(project.getId()).features().observe(project);
  }

  // TODO: Move relevant Record fields and updates into "RecordUpdate" object.
  @Override
  public Single<Record> saveChanges(Record record, ImmutableList<ValueUpdate> updates) {
    GndFirestore.RecordsCollectionReference records =
        db.projects().project(record.getProject().getId()).records();

    if (record.getId() == null) {
      DocumentReference recordDocRef = records.ref().document();
      record = record.toBuilder().setId(recordDocRef.getId()).build();
      return saveChanges(recordDocRef, record, updates);
    } else {
      return saveChanges(records.record(record.getId()).ref(), record, updates);
    }
  }

  private Single<Record> saveChanges(
      DocumentReference recordDocRef, Record record, ImmutableList<ValueUpdate> updates) {
    return RxTask.toCompletable(
            () ->
                db.batch()
                    .set(recordDocRef, RecordDoc.forUpdates(record, updatedValues(updates)), MERGE)
                    .commit())
        .andThen(Single.just(record));
  }

  @Override
  public Single<Feature> addFeature(Feature feature) {
    String projectId = feature.getProject().getId();
    return db.projects().project(projectId).features().add(feature);
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
