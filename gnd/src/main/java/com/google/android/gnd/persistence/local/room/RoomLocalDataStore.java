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
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.util.Log;
import androidx.room.Transaction;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.system.AuthenticationManager.User;
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
  private static final String TAG = RoomLocalDataStore.class.getSimpleName();

  @Inject OptionDao optionDao;
  @Inject MultipleChoiceDao multipleChoiceDao;
  @Inject FieldDao fieldDao;
  @Inject FormDao formDao;
  @Inject LayerDao layerDao;
  @Inject ProjectDao projectDao;
  @Inject FeatureDao featureDao;
  @Inject FeatureMutationDao featureMutationDao;
  @Inject ObservationDao observationDao;
  @Inject ObservationMutationDao observationMutationDao;
  @Inject TileDao tileDao;
  @Inject UserDao userDao;

  @Inject
  public RoomLocalDataStore() {}

  private Completable insertOrUpdateOption(String fieldId, Option option) {
    return optionDao
        .insertOrUpdate(OptionEntity.fromOption(fieldId, option))
        .subscribeOn(Schedulers.io());
  }

  private Completable insertOrUpdateOptions(String fieldId, ImmutableList<Option> options) {
    return Observable.fromIterable(options)
        .flatMapCompletable(option -> insertOrUpdateOption(fieldId, option))
        .subscribeOn(Schedulers.io());
  }

  private Completable insertOrUpdateMultipleChoice(String fieldId, MultipleChoice multipleChoice) {
    return multipleChoiceDao
        .insertOrUpdate(MultipleChoiceEntity.fromMultipleChoice(fieldId, multipleChoice))
        .andThen(insertOrUpdateOptions(fieldId, multipleChoice.getOptions()))
        .subscribeOn(Schedulers.io());
  }

  private Completable insertOrUpdateField(String formId, Element.Type elementType, Field field) {
    return fieldDao
        .insertOrUpdate(FieldEntity.fromField(formId, elementType, field))
        .andThen(
            Observable.just(field)
                .filter(__ -> field.getMultipleChoice() != null)
                .flatMapCompletable(
                    __ -> insertOrUpdateMultipleChoice(field.getId(), field.getMultipleChoice())))
        .subscribeOn(Schedulers.io());
  }

  private Completable insertOrUpdateElements(String formId, ImmutableList<Element> elements) {
    return Observable.fromIterable(elements)
        .flatMapCompletable(
            element -> insertOrUpdateField(formId, element.getType(), element.getField()));
  }

  private Completable insertOrUpdateForm(String layerId, Form form) {
    return formDao
        .insertOrUpdate(FormEntity.fromForm(layerId, form))
        .andThen(insertOrUpdateElements(form.getId(), form.getElements()))
        .subscribeOn(Schedulers.io());
  }

  private Completable insertOrUpdateForms(String layerId, List<Form> forms) {
    return Observable.fromIterable(forms)
        .flatMapCompletable(form -> insertOrUpdateForm(layerId, form));
  }

  private Completable insertOrUpdateLayer(String projectId, Layer layer) {
    return layerDao
        .insertOrUpdate(LayerEntity.fromLayer(projectId, layer))
        .andThen(insertOrUpdateForms(layer.getId(), layer.getForms()))
        .subscribeOn(Schedulers.io());
  }

  private Completable insertOrUpdateLayers(String projectId, List<Layer> layers) {
    return Observable.fromIterable(layers)
        .flatMapCompletable(layer -> insertOrUpdateLayer(projectId, layer));
  }

  @Override
  public Completable insertOrUpdateProject(Project project) {
    return projectDao
        .insertOrUpdate(ProjectEntity.fromProject(project))
        .andThen(insertOrUpdateLayers(project.getId(), project.getLayers()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable insertOrUpdateUser(User user) {
    return userDao.insertOrUpdate(UserEntity.fromUser(user)).subscribeOn(Schedulers.io());
  }

  @Override
  public Single<List<Project>> getProjects() {
    return projectDao
        .getAllProjects()
        .map(list -> stream(list).map(ProjectEntity::toProject).collect(toList()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Maybe<Project> getProjectById(String id) {
    return projectDao.getProjectById(id).map(ProjectEntity::toProject).subscribeOn(Schedulers.io());
  }

  @Override
  public Completable removeProject(Project project) {
    return projectDao.delete(ProjectEntity.fromProject(project)).subscribeOn(Schedulers.io());
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
    return featureDao
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
    return featureDao
        .findById(featureId)
        .map(f -> FeatureEntity.toFeature(f, project))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Maybe<Observation> getObservation(Feature feature, String observationId) {
    return observationDao
        .findById(observationId)
        .map(obs -> ObservationEntity.toObservation(feature, obs))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Single<ImmutableList<Observation>> getObservations(Feature feature, String formId) {
    return observationDao
        .findByFeatureId(feature.getId(), formId)
        .map(
            list ->
                stream(list)
                    .map(obs -> ObservationEntity.toObservation(feature, obs))
                    .collect(toImmutableList()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Flowable<ImmutableSet<Tile>> getTilesOnceAndStream() {
    return tileDao
        .findAll()
        .map(list -> stream(list).map(TileEntity::toTile).collect(toImmutableSet()))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Single<ImmutableList<Mutation>> getPendingMutations(String featureId) {
    return featureMutationDao
        .findByFeatureId(featureId)
        .flattenAsObservable(fms -> fms)
        .map(FeatureMutationEntity::toMutation)
        .cast(Mutation.class)
        .mergeWith(
            observationMutationDao
                .findByFeatureId(featureId)
                .flattenAsObservable(oms -> oms)
                .map(ObservationMutationEntity::toMutation)
                .cast(Mutation.class))
        .toList()
        .map(ImmutableList::copyOf)
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable updateMutations(ImmutableList<Mutation> mutations) {
    return featureMutationDao
        .updateAll(toFeatureMutationEntities(mutations))
        .andThen(
            observationMutationDao
                .updateAll(toObservationMutationEntities(mutations))
                .subscribeOn(Schedulers.io()))
        .subscribeOn(Schedulers.io());
  }

  private ImmutableList<ObservationMutationEntity> toObservationMutationEntities(
      ImmutableList<Mutation> mutations) {
    return stream(ObservationMutation.filter(mutations))
        .map(ObservationMutationEntity::fromMutation)
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
    return featureMutationDao
        .deleteAll(FeatureMutation.ids(mutations))
        .andThen(
            observationMutationDao
                .deleteAll(ObservationMutation.ids(mutations))
                .subscribeOn(Schedulers.io()))
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeFeature(Feature feature) {
    // TODO(#109): Once we user can edit feature locally, apply pending mutations before saving.
    return featureDao
        .insertOrUpdate(FeatureEntity.fromFeature(feature))
        .subscribeOn(Schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeObservation(Observation observation) {
    ObservationEntity observationEntity = ObservationEntity.fromObservation(observation);
    return observationMutationDao
        .findByObservationId(observation.getId())
        .map(observationEntity::applyMutations)
        .flatMapCompletable(observationDao::insertOrUpdate)
        .subscribeOn(Schedulers.io());
  }

  private Completable apply(FeatureMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return featureDao
            .insertOrUpdate(FeatureEntity.fromMutation(mutation))
            .subscribeOn(Schedulers.io());
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(FeatureMutation mutation) {
    return featureMutationDao
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

  /**
   * Applies mutation to observation in database or creates a new one.
   *
   * @return A Completable that emits an error if mutation type is "UPDATE" but entity does not
   *     exist, or if type is "CREATE" and entity already exists.
   */
  private Completable apply(ObservationMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return observationDao
            .insert(ObservationEntity.fromMutation(mutation))
            .doOnSubscribe(__ -> Log.v(TAG, "Inserting observation: " + mutation))
            .subscribeOn(Schedulers.io());
      case UPDATE:
        return observationDao
            .findById(mutation.getObservationId())
            .doOnSubscribe(__ -> Log.v(TAG, "Applying mutation: " + mutation))
            // Emit NoSuchElementException if not found.
            .toSingle()
            .map(obs -> obs.applyMutation(mutation))
            .flatMapCompletable(
                obs -> observationDao.insertOrUpdate(obs).subscribeOn(Schedulers.io()))
            .subscribeOn(Schedulers.io());
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(ObservationMutation mutation) {
    return observationMutationDao
        .insert(ObservationMutationEntity.fromMutation(mutation))
        .doOnSubscribe(__ -> Log.v(TAG, "Enqueuing mutation: " + mutation))
        .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable insertOrUpdateTile(Tile tile) {
    return tileDao.insertOrUpdate(TileEntity.fromTile(tile)).subscribeOn(Schedulers.io());
  }

  @Override
  public Maybe<Tile> getTile(String tileId) {
    return tileDao.findById(tileId).map(TileEntity::toTile).subscribeOn(Schedulers.io());
  }
}
