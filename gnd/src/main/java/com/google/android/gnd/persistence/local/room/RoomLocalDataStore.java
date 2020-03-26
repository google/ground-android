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

import androidx.room.Transaction;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineArea;
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
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureMutationDao;
import com.google.android.gnd.persistence.local.room.dao.FieldDao;
import com.google.android.gnd.persistence.local.room.dao.FormDao;
import com.google.android.gnd.persistence.local.room.dao.LayerDao;
import com.google.android.gnd.persistence.local.room.dao.MultipleChoiceDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationMutationDao;
import com.google.android.gnd.persistence.local.room.dao.OptionDao;
import com.google.android.gnd.persistence.local.room.dao.ProjectDao;
import com.google.android.gnd.persistence.local.room.dao.TileDao;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import com.google.android.gnd.persistence.local.room.entity.AuditInfoEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.FieldEntity;
import com.google.android.gnd.persistence.local.room.entity.FormEntity;
import com.google.android.gnd.persistence.local.room.entity.LayerEntity;
import com.google.android.gnd.persistence.local.room.entity.MultipleChoiceEntity;
import com.google.android.gnd.persistence.local.room.entity.ObservationEntity;
import com.google.android.gnd.persistence.local.room.entity.ObservationMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.OptionEntity;
import com.google.android.gnd.persistence.local.room.entity.ProjectEntity;
import com.google.android.gnd.persistence.local.room.entity.TileEntity;
import com.google.android.gnd.persistence.local.room.entity.UserEntity;
import com.google.android.gnd.persistence.local.room.models.TileEntityState;
import com.google.android.gnd.persistence.local.room.models.UserDetails;
import com.google.android.gnd.rx.Schedulers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Implementation of local data store using Room ORM. Room abstracts persistence between a local db
 * and Java objects using a mix of inferred mappings based on Java field names and types, and custom
 * annotations. Mappings are defined through the various Entity objects in the package and related
 * embedded classes.
 */
@Singleton
public class RoomLocalDataStore implements LocalDataStore {

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
  @Inject OfflineAreaDao offlineAreaDao;
  @Inject Schedulers schedulers;

  @Inject
  RoomLocalDataStore() {}

  private Completable insertOrUpdateOption(String fieldId, Option option) {
    return optionDao
        .insertOrUpdate(OptionEntity.fromOption(fieldId, option))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateOptions(String fieldId, ImmutableList<Option> options) {
    return Observable.fromIterable(options)
        .flatMapCompletable(option -> insertOrUpdateOption(fieldId, option))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateMultipleChoice(String fieldId, MultipleChoice multipleChoice) {
    return multipleChoiceDao
        .insertOrUpdate(MultipleChoiceEntity.fromMultipleChoice(fieldId, multipleChoice))
        .andThen(insertOrUpdateOptions(fieldId, multipleChoice.getOptions()))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateField(String formId, Element.Type elementType, Field field) {
    return fieldDao
        .insertOrUpdate(FieldEntity.fromField(formId, elementType, field))
        .andThen(
            Observable.just(field)
                .filter(__ -> field.getMultipleChoice() != null)
                .flatMapCompletable(
                    __ -> insertOrUpdateMultipleChoice(field.getId(), field.getMultipleChoice())))
        .subscribeOn(schedulers.io());
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
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateForms(String layerId, List<Form> forms) {
    return Observable.fromIterable(forms)
        .flatMapCompletable(form -> insertOrUpdateForm(layerId, form));
  }

  private Completable insertOrUpdateLayer(String projectId, Layer layer) {
    return layerDao
        .insertOrUpdate(LayerEntity.fromLayer(projectId, layer))
        .andThen(
            insertOrUpdateForms(
                layer.getId(), layer.getForm().map(Arrays::asList).orElseGet(ArrayList::new)))
        .subscribeOn(schedulers.io());
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
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable insertOrUpdateUser(User user) {
    return userDao.insertOrUpdate(UserEntity.fromUser(user)).subscribeOn(schedulers.io());
  }

  @Override
  public Single<List<Project>> getProjects() {
    return projectDao
        .getAllProjects()
        .map(list -> stream(list).map(ProjectEntity::toProject).collect(toList()))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Project> getProjectById(String id) {
    return projectDao.getProjectById(id).map(ProjectEntity::toProject).subscribeOn(schedulers.io());
  }

  @Override
  public Completable removeProject(Project project) {
    return projectDao.delete(ProjectEntity.fromProject(project)).subscribeOn(schedulers.io());
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
        .subscribeOn(schedulers.io());
  }

  // TODO(#127): Decouple from Project and remove project from args.
  @Override
  public Maybe<Feature> getFeature(Project project, String featureId) {
    return featureDao
        .findById(featureId)
        .map(f -> FeatureEntity.toFeature(f, project))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Observation> getObservation(Feature feature, String observationId) {
    return observationDao
        .findById(observationId)
        .map(obs -> ObservationEntity.toObservation(feature, obs))
        .subscribeOn(schedulers.io());
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
        .subscribeOn(schedulers.io());
  }

  @Override
  public Flowable<ImmutableSet<Tile>> getTilesOnceAndStream() {
    return tileDao
        .findAll()
        .map(list -> stream(list).map(TileEntity::toTile).collect(toImmutableSet()))
        .toFlowable()
        .subscribeOn(schedulers.io());
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
        .subscribeOn(schedulers.io());
  }

  @Transaction
  @Override
  public Completable updateMutations(ImmutableList<Mutation> mutations) {
    return featureMutationDao
        .updateAll(toFeatureMutationEntities(mutations))
        .andThen(
            observationMutationDao
                .updateAll(toObservationMutationEntities(mutations))
                .subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
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
                .subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeFeature(Feature feature) {
    // TODO(#109): Once we user can edit feature locally, apply pending mutations before saving.
    return featureDao
        .insertOrUpdate(FeatureEntity.fromFeature(feature))
        .subscribeOn(schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeObservation(Observation observation) {
    ObservationEntity observationEntity = ObservationEntity.fromObservation(observation);
    return observationMutationDao
        .findByObservationId(observation.getId())
        .flatMapCompletable(mutations -> mergeObservation(observationEntity, mutations));
  }

  private Completable mergeObservation(
      ObservationEntity observation, List<ObservationMutationEntity> mutations) {
    if (mutations.isEmpty()) {
      return Completable.complete();
    }
    ObservationMutationEntity lastMutation = mutations.get(mutations.size() - 1);
    return loadUser(lastMutation.getUserId())
        .map(user -> applyMutations(observation, mutations, user))
        .flatMapCompletable(obs -> observationDao.insertOrUpdate(obs).subscribeOn(schedulers.io()));
  }

  private ObservationEntity applyMutations(
      ObservationEntity observation, List<ObservationMutationEntity> mutations, User user) {
    ObservationMutationEntity lastMutation = mutations.get(mutations.size() - 1);
    long clientTimestamp = lastMutation.getClientTimestamp();
    Timber.v("Merging observation " + this + " with mutations " + mutations);
    ObservationEntity.Builder builder = observation.toBuilder();
    // Merge changes to responses.
    for (ObservationMutationEntity mutation : mutations) {
      builder.applyMutation(mutation);
    }
    // Update modified user and time.
    AuditInfoEntity lastModified =
        AuditInfoEntity.builder()
            .setUser(UserDetails.fromUser(user))
            .setClientTimeMillis(clientTimestamp)
            .build();
    builder.setLastModified(lastModified);
    Timber.v("Merged observation %s", builder.build());
    return builder.build();
  }

  @Override
  public Single<User> loadUser(String id) {
    return userDao
        .findById(id)
        .doOnError(e -> Timber.e(e, "Error loading user from local db: %s", id))
        // Fail with NoSuchElementException if not found.
        .toSingle()
        .map(UserEntity::toUser)
        .subscribeOn(schedulers.io());
  }

  private Completable apply(FeatureMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return loadUser(mutation.getUserId())
            .flatMapCompletable(user -> insertOrUpdateFeature(mutation, user));
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable insertOrUpdateFeature(FeatureMutation mutation, User user) {
    return featureDao
        .insertOrUpdate(FeatureEntity.fromMutation(mutation, AuditInfo.now(user)))
        .subscribeOn(schedulers.io());
  }

  private Completable enqueue(FeatureMutation mutation) {
    return featureMutationDao
        .insert(FeatureMutationEntity.fromMutation(mutation))
        .subscribeOn(schedulers.io());
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
        return loadUser(mutation.getUserId())
            .flatMapCompletable(user -> createObservation(mutation, user));
      case UPDATE:
        return loadUser(mutation.getUserId())
            .flatMapCompletable(user -> updateObservation(mutation, user));
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable createObservation(ObservationMutation mutation, User user) {
    return observationDao
        .insert(ObservationEntity.fromMutation(mutation, AuditInfo.now(user)))
        .doOnSubscribe(__ -> Timber.v("Inserting observation: %s", mutation))
        .subscribeOn(schedulers.io());
  }

  private Completable updateObservation(ObservationMutation mutation, User user) {
    ObservationMutationEntity mutationEntity = ObservationMutationEntity.fromMutation(mutation);
    return observationDao
        .findById(mutation.getObservationId())
        .doOnSubscribe(__ -> Timber.v("Applying mutation: %s", mutation))
        // Emit NoSuchElementException if not found.
        .toSingle()
        .map(obs -> applyMutations(obs, ImmutableList.of(mutationEntity), user))
        .flatMapCompletable(obs -> observationDao.insertOrUpdate(obs).subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
  }

  private Completable enqueue(ObservationMutation mutation) {
    return observationMutationDao
        .insert(ObservationMutationEntity.fromMutation(mutation))
        .doOnSubscribe(__ -> Timber.v("Enqueuing mutation: %s", mutation))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable insertOrUpdateTile(Tile tile) {
    return tileDao.insertOrUpdate(TileEntity.fromTile(tile)).subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Tile> getTile(String tileId) {
    return tileDao.findById(tileId).map(TileEntity::toTile).subscribeOn(schedulers.io());
  }

  @Override
  public Single<ImmutableList<Tile>> getPendingTiles() {
    return tileDao
        .findByState(TileEntityState.PENDING.intValue())
        .map(ts -> stream(ts).map(TileEntity::toTile).collect(toImmutableList()))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable insertOrUpdateOfflineArea(OfflineArea area) {
    return offlineAreaDao
        .insertOrUpdate(OfflineAreaEntity.fromArea(area))
        .subscribeOn(schedulers.io());
  }
}
