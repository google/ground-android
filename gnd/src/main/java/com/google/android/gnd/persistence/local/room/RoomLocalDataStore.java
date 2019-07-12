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

package com.google.android.gnd.persistence.local.room;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static com.google.android.gnd.util.ImmutableSetCollector.toImmutableSet;
import static java8.lang.Iterables.forEach;
import static java8.util.stream.StreamSupport.stream;

import androidx.room.Room;
import androidx.room.Transaction;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import com.google.android.gnd.persistence.shared.Mutation;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of local data store using Room ORM. Room abstracts persistence between a local db
 * and Java objects using a mix of inferred mappings based on Java field names and types, and custom
 * annotations. Mappings are defined through the various Entity objects in the package and related
 * embedded classes.
 */
@Singleton
public class RoomLocalDataStore implements LocalDataStore {
  private static final String DB_NAME = "gnd.db";

  private final LocalDatabase db;

  @Inject
  public RoomLocalDataStore(GndApplication app) {
    // TODO: Create db in module and inject DAOs directly.
    this.db =
        Room.databaseBuilder(app.getApplicationContext(), LocalDatabase.class, DB_NAME)
            // TODO(#128): Disable before official release.
            .fallbackToDestructiveMigration()
            .build();
  }

  @Transaction
  @Override
  public Completable applyAndEnqueue(FeatureMutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      return Completable.error(e);
    }
  }

  // TODO(#127): Decouple from Project and pass in project id instead.
  @Override
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project) {
    return db.featureDao()
        .findByProjectIdStream(project.getId())
        .map(
            list ->
                stream(list)
                    .map(f -> FeatureEntity.toFeature(f, project))
                    .collect(toImmutableSet()));
  }

  // TODO(#127): Decouple from Project and remove project from args.
  @Override
  public Maybe<Feature> getFeature(Project project, String featureId) {
    return db.featureDao().findById(featureId).map(f -> FeatureEntity.toFeature(f, project));
  }

  @Override
  public Maybe<Record> getRecord(Feature feature, String recordId) {
    return db.recordDao().findById(recordId).map(record -> RecordEntity.toRecord(feature, record));
  }

  @Override
  public Flowable<ImmutableList<Record>> getRecordsOnceAndStream(Feature feature, String formId) {
    return db.recordDao()
        .findByFeatureIdOnceAndStream(feature.getId(), formId)
        .map(
            list ->
                stream(list)
                    .map(record -> RecordEntity.toRecord(feature, record))
                    .collect(toImmutableList()));
  }

  @Override
  public Single<ImmutableList<Mutation>> getPendingMutations(String featureId) {
    return db.featureMutationDao()
        .findByFeatureId(featureId)
        .zipWith(db.recordMutationDao().findByFeatureId(featureId), this::mergeMutations);
  }

  @Transaction
  @Override
  public Completable removePendingMutations(ImmutableList<Mutation> mutations) {
    return db.featureMutationDao()
        .deleteAll(FeatureMutation.ids(mutations))
        .andThen(db.recordMutationDao().deleteAll(RecordMutation.ids(mutations)));
  }

  @Transaction
  @Override
  public Completable mergeFeature(Feature feature) {
    // TODO(#109): Once we user can edit feature locally, apply pending mutations before saving.
    return db.featureDao().insertOrUpdate(FeatureEntity.fromFeature(feature));
  }

  @Transaction
  @Override
  public Completable mergeRecord(Record record) {
    RecordEntity recordEntity = RecordEntity.fromRecord(record);
    return db.recordMutationDao()
        .findByRecordId(record.getId())
        .map(mutations -> recordEntity.applyMutations(mutations))
        .flatMapCompletable(db.recordDao()::insertOrUpdate);
  }

  // TODO: Can this be simplified and inlined?
  private ImmutableList<Mutation> mergeMutations(
      List<FeatureMutationEntity> featureMutationEntities,
      List<RecordMutationEntity> recordMutationEntities) {
    ImmutableList.Builder<Mutation> mutations = ImmutableList.builder();
    forEach(featureMutationEntities, fm -> mutations.add(fm.toMutation()));
    forEach(recordMutationEntities, rm -> mutations.add(rm.toMutation()));
    return mutations.build();
  }

  private Completable apply(FeatureMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return db.featureDao().insertOrUpdate(FeatureEntity.fromMutation(mutation));
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(FeatureMutation mutation) {
    return db.featureMutationDao().insert(FeatureMutationEntity.fromMutation(mutation));
  }

  @Transaction
  @Override
  public Completable applyAndEnqueue(RecordMutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      return Completable.error(e);
    }
  }

  private Completable apply(RecordMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
      case UPDATE:
        return db.recordDao().insertOrUpdate(RecordEntity.fromMutation(mutation));
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(RecordMutation mutation) {
    return db.recordMutationDao().insert(RecordMutationEntity.fromMutation(mutation));
  }
}
