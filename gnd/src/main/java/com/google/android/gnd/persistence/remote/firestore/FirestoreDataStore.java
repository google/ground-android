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

package com.google.android.gnd.persistence.remote.firestore;

import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import com.google.android.gnd.persistence.shared.Mutation;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.FeatureUpdate.RecordUpdate.ResponseUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Timestamps;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO(#92): Break implementation of OfflineUuidGenerator into a separate class.
@Singleton
public class FirestoreDataStore implements RemoteDataStore, OfflineUuidGenerator {

  private static final FirebaseFirestoreSettings FIRESTORE_SETTINGS =
      new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build();
  private static final SetOptions MERGE = SetOptions.merge();
  private static final String TAG = FirestoreDataStore.class.getSimpleName();
  private static final String ID_COLLECTION = "/ids";
  private final GndFirestore db;
  private final FirebaseFirestore firestore;

  @Inject
  FirestoreDataStore() {
    // TODO: Run on I/O thread, return asynchronously.
    // TODO: Bind the Firestore instance in a module and inject it here.
    this.firestore = FirebaseFirestore.getInstance();
    firestore.setFirestoreSettings(FIRESTORE_SETTINGS);
    FirebaseFirestore.setLoggingEnabled(true);
    this.db = new GndFirestore(firestore);
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
  public Flowable<RemoteDataEvent<Record>> loadRecordSummariesOnceAndStreamChanges(
      Feature feature) {
    return db.projects()
        .project(feature.getProject().getId())
        .records()
        .getRecordsByFeatureOnceAndStreamChanges(feature);
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
  public Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Project project) {
    return db.projects().project(project.getId()).features().observe(project);
  }

  // TODO: Move relevant Record fields and updates into "RecordUpdate" object.
  @Override
  public Single<Record> saveChanges(Record record, ImmutableList<ResponseUpdate> updates) {
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
      DocumentReference recordDocRef, Record record, ImmutableList<ResponseUpdate> updates) {
    return RxTask.toCompletable(
            () ->
                db.batch()
                    .set(
                        recordDocRef,
                        RecordDoc.forUpdates(record, updatedResponses(updates)),
                        MERGE)
                    .commit())
        .andThen(Single.just(record));
  }

  @Override
  public Completable applyMutations(ImmutableCollection<Mutation> mutations) {
    return RxTask.toCompletable(() -> applyMutationsInternal(mutations));
  }

  private Task<?> applyMutationsInternal(ImmutableCollection<Mutation> mutations) {
    WriteBatch batch = db.batch();
    for (Mutation mutation : mutations) {
      addMutationToBatch(mutation, batch);
    }
    return batch.commit();
  }

  private void addMutationToBatch(Mutation mutation, WriteBatch batch) {
    if (mutation instanceof FeatureMutation) {
      addFeatureMutationToBatch((FeatureMutation) mutation, batch);
    } else if (mutation instanceof RecordMutation) {
      addRecordMutationToBatch((RecordMutation) mutation, batch);
    } else {
      throw new IllegalArgumentException("Unsupported mutation " + mutation.getClass());
    }
  }

  private void addFeatureMutationToBatch(FeatureMutation mutation, WriteBatch batch) {
    db.projects()
        .project(mutation.getProjectId())
        .features()
        .feature(mutation.getFeatureId())
        .addMutationToBatch(mutation, batch);
  }

  private void addRecordMutationToBatch(RecordMutation mutation, WriteBatch batch) {
    db.projects()
        .project(mutation.getProjectId())
        .records()
        .record(mutation.getRecordId())
        .addMutationToBatch(mutation, batch);
  }

  private Map<String, Object> updatedResponses(ImmutableList<ResponseUpdate> updates) {
    Map<String, Object> updatedResponses = new HashMap<>();
    for (ResponseUpdate responseUpdate : updates) {
      switch (responseUpdate.getOperation()) {
        case CREATE:
        case UPDATE:
          responseUpdate
              .getResponse()
              .ifPresent(
                  r -> updatedResponses.put(responseUpdate.getElementId(), RecordDoc.toObject(r)));
          break;
        case DELETE:
          // FieldValue.delete() is not working in nested objects; if it doesn't work in the future
          // we can remove them using dot notation ("responses.{elementId}").
          updatedResponses.put(responseUpdate.getElementId(), "");
          break;
      }
    }
    return updatedResponses;
  }

  @Override
  public String generateUuid() {
    return firestore.collection(ID_COLLECTION).document().getId();
  }
}
