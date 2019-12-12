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
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import androidx.room.Room;
import androidx.room.Transaction;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
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

  @Override
  public Completable insertOrUpdateLayer(String projectId, Layer layer) {
    return db.layerDao()
        .insertOrUpdate(LayerEntity.fromLayer(projectId, layer))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Single<List<Project>> getProjects() {
    return db.projectDao()
        .findAll()
        .map(list -> stream(list).map(ProjectEntity::toProject).collect(toList()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable insertOrUpdateProject(Project project) {
    return db.projectDao()
        .insertOrUpdate(ProjectEntity.fromProject(project))
        .andThen(
            Observable.fromIterable(project.getLayers())
                .flatMapCompletable(layer -> insertOrUpdateLayer(project.getId(), layer)))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Maybe<Project> getProjectById(String id) {
    return db.projectDao()
        .getProjectById(id)
        .map(ProjectEntity::toProject)
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable removeProject(Project project) {
    return db.projectDao()
        .deleteProject(ProjectEntity.fromProject(project))
        .subscribeOn(Schedulers.io());
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
                    .collect(toImmutableSet()))
        .subscribeOn(Schedulers.io());
  }

  // TODO(#127): Decouple from Project and remove project from args.
  @Override
  public Maybe<Feature> getFeature(Project project, String featureId) {
    return db.featureDao()
        .findById(featureId)
        .map(f -> FeatureEntity.toFeature(f, project))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Maybe<Observation> getRecord(Feature feature, String recordId) {
    return db.recordDao()
        .findById(recordId)
        .map(record -> RecordEntity.toRecord(feature, record))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Single<ImmutableList<Observation>> getRecords(Feature feature, String formId) {
    return db.recordDao()
        .findByFeatureId(feature.getId(), formId)
        .map(
            list ->
                stream(list)
                    .map(record -> RecordEntity.toRecord(feature, record))
                    .collect(toImmutableList()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Flowable<ImmutableSet<Tile>> getTilesOnceAndStream() {
    return db.tileDao()
        .findAll()
        .map(list -> stream(list).map(TileEntity::toTile).collect(toImmutableSet()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Single<ImmutableList<Mutation>> getPendingMutations(String featureId) {
    return db.featureMutationDao()
        .findByFeatureId(featureId)
        .zipWith(db.recordMutationDao().findByFeatureId(featureId), this::mergeMutations)
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable updateMutations(ImmutableList<Mutation> mutations) {
    return db.featureMutationDao()
        .updateAll(toFeatureMutationEntities(mutations))
        .andThen(
            db.recordMutationDao()
                .updateAll(toRecordMutationEntities(mutations))
                .subscribeOn(Schedulers.io()))
        .subscribeOn(Schedulers.io());
  }

  private ImmutableList<RecordMutationEntity> toRecordMutationEntities(
      ImmutableList<Mutation> mutations) {
    return stream(ObservationMutation.filter(mutations))
        .map(RecordMutationEntity::fromMutation)
        .collect(toImmutableList());
  }

  private ImmutableList<FeatureMutationEntity> toFeatureMutationEntities(
      ImmutableList<Mutation> mutations) {
    return stream(FeatureMutation.filter(mutations))
        .map(FeatureMutationEntity::fromMutation)
        .collect(toImmutableList());
  }

  @Transaction
  @Override
  public Completable removePendingMutations(ImmutableList<Mutation> mutations) {
    return db.featureMutationDao()
        .deleteAll(FeatureMutation.ids(mutations))
        .andThen(
            db.recordMutationDao()
                .deleteAll(ObservationMutation.ids(mutations))
                .subscribeOn(Schedulers.io()))
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeFeature(Feature feature) {
    // TODO(#109): Once we user can edit feature locally, apply pending mutations before saving.
    return db.featureDao()
        .insertOrUpdate(FeatureEntity.fromFeature(feature))
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeRecord(Observation observation) {
    RecordEntity recordEntity = RecordEntity.fromRecord(observation);
    return db.recordMutationDao()
        .findByRecordId(observation.getId())
        .map(recordEntity::applyMutations)
        .flatMapCompletable(db.recordDao()::insertOrUpdate)
        .subscribeOn(Schedulers.io());
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
        return db.featureDao()
            .insertOrUpdate(FeatureEntity.fromMutation(mutation))
            .subscribeOn(Schedulers.io());
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(FeatureMutation mutation) {
    return db.featureMutationDao()
        .insert(FeatureMutationEntity.fromMutation(mutation))
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable applyAndEnqueue(ObservationMutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      return Completable.error(e);
    }
  }

  private Completable apply(ObservationMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
      case UPDATE:
        return db.recordDao()
            .insertOrUpdate(RecordEntity.fromMutation(mutation))
            .subscribeOn(Schedulers.io());
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(ObservationMutation mutation) {
    return db.recordMutationDao()
        .insert(RecordMutationEntity.fromMutation(mutation))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable insertOrUpdateTile(Tile tile) {
    return db.tileDao().insertOrUpdate(TileEntity.fromTile(tile)).subscribeOn(Schedulers.io());
  }

  @Override
  public Maybe<Tile> getTile(String tileId) {
    return db.tileDao().findById(tileId).map(TileEntity::toTile).subscribeOn(Schedulers.io());
  }
}
