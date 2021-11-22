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
import static com.google.android.gnd.util.StreamUtil.logErrorsAndSkip;
import static com.google.common.base.Preconditions.checkNotNull;
import static java8.util.stream.StreamSupport.stream;

import androidx.room.Transaction;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Mutation.SyncStatus;
import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.tile.TileSet;
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
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.ResponseMap.Builder;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.room.converter.ResponseDeltasConverter;
import com.google.android.gnd.persistence.local.room.converter.ResponseMapConverter;
import com.google.android.gnd.persistence.local.room.dao.BaseMapDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureMutationDao;
import com.google.android.gnd.persistence.local.room.dao.FieldDao;
import com.google.android.gnd.persistence.local.room.dao.FormDao;
import com.google.android.gnd.persistence.local.room.dao.LayerDao;
import com.google.android.gnd.persistence.local.room.dao.MultipleChoiceDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationMutationDao;
import com.google.android.gnd.persistence.local.room.dao.OfflineAreaDao;
import com.google.android.gnd.persistence.local.room.dao.OptionDao;
import com.google.android.gnd.persistence.local.room.dao.ProjectDao;
import com.google.android.gnd.persistence.local.room.dao.TileSetDao;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import com.google.android.gnd.persistence.local.room.entity.AuditInfoEntity;
import com.google.android.gnd.persistence.local.room.entity.BaseMapEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.FieldEntity;
import com.google.android.gnd.persistence.local.room.entity.FormEntity;
import com.google.android.gnd.persistence.local.room.entity.LayerEntity;
import com.google.android.gnd.persistence.local.room.entity.MultipleChoiceEntity;
import com.google.android.gnd.persistence.local.room.entity.ObservationEntity;
import com.google.android.gnd.persistence.local.room.entity.ObservationMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.OfflineAreaEntity;
import com.google.android.gnd.persistence.local.room.entity.OptionEntity;
import com.google.android.gnd.persistence.local.room.entity.ProjectEntity;
import com.google.android.gnd.persistence.local.room.entity.TileSetEntity;
import com.google.android.gnd.persistence.local.room.entity.UserEntity;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.persistence.local.room.models.TileSetEntityState;
import com.google.android.gnd.persistence.local.room.models.UserDetails;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.ui.util.FileUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
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
  @Inject
  TileSetDao tileSetDao;
  @Inject UserDao userDao;
  @Inject OfflineAreaDao offlineAreaDao;
  @Inject BaseMapDao baseMapDao;
  @Inject Schedulers schedulers;
  @Inject FileUtil fileUtil;

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
                    __ ->
                        insertOrUpdateMultipleChoice(
                            field.getId(), checkNotNull(field.getMultipleChoice()))))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateElements(String formId, ImmutableList<Element> elements) {
    return Observable.fromIterable(elements)
        .filter(element -> element.getType() == Element.Type.FIELD)
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

  private Completable insertOfflineBaseMapSources(Project project) {
    return Observable.fromIterable(project.getBaseMaps())
        .flatMapCompletable(
            source -> baseMapDao.insert(BaseMapEntity.fromModel(project.getId(), source)));
  }

  @Transaction
  @Override
  public Completable insertOrUpdateProject(Project project) {
    return projectDao
        .insertOrUpdate(ProjectEntity.fromProject(project))
        .andThen(layerDao.deleteByProjectId(project.getId()))
        .andThen(insertOrUpdateLayers(project.getId(), project.getLayers()))
        .andThen(baseMapDao.deleteByProjectId(project.getId()))
        .andThen(insertOfflineBaseMapSources(project))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable insertOrUpdateUser(User user) {
    return userDao.insertOrUpdate(UserEntity.fromUser(user)).subscribeOn(schedulers.io());
  }

  @Override
  public Single<User> getUser(String id) {
    return userDao
        .findById(id)
        .doOnError(e -> Timber.e(e, "Error loading user from local db: %s", id))
        // Fail with NoSuchElementException if not found.
        .toSingle()
        .map(UserEntity::toUser)
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<ImmutableList<Project>> getProjects() {
    return projectDao
        .getAllProjects()
        .map(list -> stream(list).map(ProjectEntity::toProject).collect(toImmutableList()))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Project> getProjectById(String id) {
    return projectDao.getProjectById(id).map(ProjectEntity::toProject).subscribeOn(schedulers.io());
  }

  @Override
  public Completable deleteProject(Project project) {
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

  @Override
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project) {
    return featureDao
        .findOnceAndStream(project.getId(), EntityState.DEFAULT)
        .map(featureEntities -> toFeatures(project, featureEntities))
        .subscribeOn(schedulers.io());
  }

  private ImmutableSet<Feature> toFeatures(Project project, List<FeatureEntity> featureEntities) {
    return stream(featureEntities)
        .flatMap(f -> logErrorsAndSkip(() -> FeatureEntity.toFeature(f, project)))
        .collect(toImmutableSet());
  }

  @Override
  public Maybe<Feature> getFeature(Project project, String featureId) {
    return featureDao
        .findById(featureId)
        .map(f -> FeatureEntity.toFeature(f, project))
        .doOnError(e -> Timber.e(e))
        .onErrorComplete()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Observation> getObservation(Feature feature, String observationId) {
    return observationDao
        .findById(observationId)
        .map(obs -> ObservationEntity.toObservation(feature, obs))
        .doOnError(e -> Timber.d(e))
        .onErrorComplete()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<ImmutableList<Observation>> getObservations(Feature feature, String formId) {
    return observationDao
        .findByFeatureId(feature.getId(), formId, EntityState.DEFAULT)
        .map(observationEntities -> toObservations(feature, observationEntities))
        .subscribeOn(schedulers.io());
  }

  private ImmutableList<Observation> toObservations(
      Feature feature, List<ObservationEntity> observationEntities) {
    return stream(observationEntities)
        .flatMap(obs -> logErrorsAndSkip(() -> ObservationEntity.toObservation(feature, obs)))
        .collect(toImmutableList());
  }

  @Override
  public Flowable<ImmutableSet<TileSet>> getTileSetsOnceAndStream() {
    return tileSetDao
        .findAllOnceAndStream()
        .map(list -> stream(list).map(TileSetEntity::toTileSet).collect(toImmutableSet()))
        .subscribeOn(schedulers.io());
  }

  @Cold(terminates = false)
  @Override
  public Flowable<ImmutableList<Mutation>> getMutationsOnceAndStream(Project project) {
    // TODO: Show mutations for all projects, not just current one.
    Flowable<ImmutableList<FeatureMutation>> featureMutations =
        featureMutationDao
            .loadAllOnceAndStream()
            .map(
                list ->
                    stream(list)
                        .filter(entity -> entity.getProjectId().equals(project.getId()))
                        .map(FeatureMutationEntity::toMutation)
                        .collect(toImmutableList()))
            .subscribeOn(schedulers.io());
    Flowable<ImmutableList<ObservationMutation>> observationMutations =
        observationMutationDao
            .loadAllOnceAndStream()
            .map(
                list ->
                    stream(list)
                        .filter(entity -> entity.getProjectId().equals(project.getId()))
                        .map(entity -> entity.toMutation(project))
                        .collect(toImmutableList()))
            .subscribeOn(schedulers.io());
    return Flowable.combineLatest(
        featureMutations, observationMutations, this::combineAndSortMutations);
  }

  private ImmutableList<Mutation> combineAndSortMutations(
      ImmutableList<FeatureMutation> featureMutations,
      ImmutableList<ObservationMutation> observationMutations) {
    return ImmutableList.sortedCopyOf(
        Mutation.byDescendingClientTimestamp(),
        ImmutableList.<Mutation>builder()
            .addAll(featureMutations)
            .addAll(observationMutations)
            .build());
  }

  @Override
  public Single<ImmutableList<Mutation>> getPendingMutations(String featureId) {
    return featureMutationDao
        .findByFeatureId(featureId, MutationEntitySyncStatus.PENDING)
        .flattenAsObservable(fms -> fms)
        .map(FeatureMutationEntity::toMutation)
        .cast(Mutation.class)
        .mergeWith(
            observationMutationDao
                .findByFeatureId(featureId, MutationEntitySyncStatus.PENDING)
                .flattenAsObservable(oms -> oms)
                .flatMap(
                    ome ->
                        getProjectById(ome.getProjectId())
                            .toSingle()
                            .map(project -> ome.toMutation(project))
                            .toObservable()
                            .doOnError(e -> Timber.e(e, "Observation mutation skipped"))
                            .onErrorResumeNext(Observable.empty()))
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

  @Override
  public Completable finalizePendingMutations(ImmutableList<Mutation> mutations) {
    return finalizeDeletions(mutations).andThen(markComplete(mutations));
  }

  private Completable finalizeDeletions(ImmutableList<Mutation> mutations) {
    return Observable.fromIterable(mutations)
        .filter(mutation -> mutation.getType() == Type.DELETE)
        .flatMapCompletable(
            mutation -> {
              if (mutation instanceof ObservationMutation) {
                return deleteObservation(((ObservationMutation) mutation).getObservationId());
              } else if (mutation instanceof FeatureMutation) {
                return deleteFeature(mutation.getFeatureId());
              } else {
                return Completable.error(new RuntimeException("Unknown type : " + mutation));
              }
            });
  }

  private Completable markComplete(ImmutableList<Mutation> mutations) {
    ImmutableList<FeatureMutationEntity> featureMutations =
        stream(FeatureMutation.filter(mutations))
            .map(mutation -> mutation.toBuilder().setSyncStatus(SyncStatus.COMPLETED).build())
            .map(FeatureMutationEntity::fromMutation)
            .collect(toImmutableList());
    ImmutableList<ObservationMutationEntity> observationMutations =
        stream(ObservationMutation.filter(mutations))
            .map(mutation -> mutation.toBuilder().setSyncStatus(SyncStatus.COMPLETED).build())
            .map(ObservationMutationEntity::fromMutation)
            .collect(toImmutableList());
    return featureMutationDao
        .updateAll(featureMutations)
        .andThen(
            observationMutationDao.updateAll(observationMutations).subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeFeature(Feature feature) {
    // TODO(#706): Apply pending local mutations before saving.
    return featureDao
        .insertOrUpdate(FeatureEntity.fromFeature(feature))
        .subscribeOn(schedulers.io());
  }

  @Transaction
  @Override
  public Completable mergeObservation(Observation observation) {
    ObservationEntity observationEntity = ObservationEntity.fromObservation(observation);
    return observationMutationDao
        .findByObservationId(
            observation.getId(),
            MutationEntitySyncStatus.PENDING,
            MutationEntitySyncStatus.IN_PROGRESS)
        .flatMapCompletable(
            mutations -> mergeObservation(observation.getForm(), observationEntity, mutations))
        .subscribeOn(schedulers.io());
  }

  private Completable mergeObservation(
      Form form, ObservationEntity observation, List<ObservationMutationEntity> mutations) {
    if (mutations.isEmpty()) {
      return observationDao.insertOrUpdate(observation);
    }
    ObservationMutationEntity lastMutation = mutations.get(mutations.size() - 1);
    checkNotNull(lastMutation, "Could not get last mutation");
    return getUser(lastMutation.getUserId())
        .map(user -> applyMutations(form, observation, mutations, user))
        .flatMapCompletable(obs -> observationDao.insertOrUpdate(obs));
  }

  private ObservationEntity applyMutations(
      Form form,
      ObservationEntity observation,
      List<ObservationMutationEntity> mutations,
      User user) {
    ObservationMutationEntity lastMutation = mutations.get(mutations.size() - 1);
    long clientTimestamp = lastMutation.getClientTimestamp();
    Timber.v("Merging observation " + this + " with mutations " + mutations);
    ObservationEntity.Builder builder = observation.toBuilder();
    builder.setResponses(
        ResponseMapConverter.toString(applyMutations(form, observation, mutations)));
    // Update modified user and time.
    AuditInfoEntity lastModified =
        AuditInfoEntity.builder()
            .setUser(UserDetails.fromUser(user))
            .setClientTimestamp(clientTimestamp)
            .build();
    builder.setLastModified(lastModified);
    Timber.v("Merged observation %s", builder.build());
    return builder.build();
  }

  private ResponseMap applyMutations(
      Form form, ObservationEntity observation, List<ObservationMutationEntity> mutations) {
    Builder responseMap =
        ResponseMapConverter.fromString(form, observation.getResponses()).toBuilder();
    for (ObservationMutationEntity mutation : mutations) {
      // Merge changes to responses.
      responseMap.applyDeltas(
          ResponseDeltasConverter.fromString(form, mutation.getResponseDeltas()));
    }
    return responseMap.build();
  }

  private Completable apply(FeatureMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
      case UPDATE:
        return getUser(mutation.getUserId())
            .flatMapCompletable(user -> insertOrUpdateFeature(mutation, user));
      case DELETE:
        return featureDao
            .findById(mutation.getFeatureId())
            .flatMapCompletable(entity -> markFeatureForDeletion(entity, mutation))
            .subscribeOn(schedulers.io());
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable markFeatureForDeletion(FeatureEntity entity, FeatureMutation mutation) {
    return featureDao
        .update(entity.toBuilder().setState(EntityState.DELETED).build())
        .doOnSubscribe(__ -> Timber.d("Marking feature as deleted : %s", mutation))
        .ignoreElement();
  }

  private Completable insertOrUpdateFeature(FeatureMutation mutation, User user) {
    return featureDao
        .insertOrUpdate(FeatureEntity.fromMutation(mutation, AuditInfo.now(user)))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable deleteFeature(String featureId) {
    return featureDao
        .findById(featureId)
        .toSingle()
        .doOnSubscribe(__ -> Timber.d("Deleting local feature : %s", featureId))
        .flatMapCompletable(entity -> featureDao.delete(entity))
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
   * @return A Completable that emits an error if mutation type is "UPDATE" but entity does not *
   *     exist, or if type is "CREATE" and entity already exists.
   */
  public Completable apply(ObservationMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return getUser(mutation.getUserId())
            .flatMapCompletable(user -> createObservation(mutation, user));
      case UPDATE:
        return getUser(mutation.getUserId())
            .flatMapCompletable(user -> updateObservation(mutation, user));
      case DELETE:
        return observationDao
            .findById(mutation.getObservationId())
            .flatMapCompletable(entity -> markObservationForDeletion(entity, mutation));
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
        .switchIfEmpty(fallbackObservation(mutation))
        .map(obs -> applyMutations(mutation.getForm(), obs, ImmutableList.of(mutationEntity), user))
        .flatMapCompletable(obs -> observationDao.insertOrUpdate(obs).subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
  }

  /**
   * Returns a source which creates an observation based on the provided mutation. Used in rare
   * cases when the observation is no longer in the local db, but the user is updating rather than
   * creating a new observation. In these cases creation metadata is unknown, so empty audit info is
   * used.
   */
  private SingleSource<ObservationEntity> fallbackObservation(ObservationMutation mutation) {
    return em ->
        em.onSuccess(ObservationEntity.fromMutation(mutation, AuditInfo.builder().build()));
  }

  private Completable markObservationForDeletion(
      ObservationEntity entity, ObservationMutation mutation) {
    return observationDao
        .update(entity.toBuilder().setState(EntityState.DELETED).build())
        .doOnSubscribe(__ -> Timber.d("Marking observation as deleted : %s", mutation))
        .ignoreElement()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable deleteObservation(String observationId) {
    return observationDao
        .findById(observationId)
        .toSingle()
        .doOnSubscribe(__ -> Timber.d("Deleting local observation : %s", observationId))
        .flatMapCompletable(entity -> observationDao.delete(entity))
        .subscribeOn(schedulers.io());
  }

  private Completable enqueue(ObservationMutation mutation) {
    return observationMutationDao
        .insert(ObservationMutationEntity.fromMutation(mutation))
        .doOnSubscribe(__ -> Timber.v("Enqueuing mutation: %s", mutation))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable insertOrUpdateTileSet(TileSet tileSet) {
    return tileSetDao
        .insertOrUpdate(TileSetEntity.fromTileSet(tileSet))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<TileSet> getTileSet(String tileUrl) {
    return tileSetDao
        .findByUrl(tileUrl)
        .map(TileSetEntity::toTileSet)
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<ImmutableList<TileSet>> getPendingTileSets() {
    return tileSetDao
        .findByState(TileSetEntityState.PENDING.intValue())
        .map(ts -> stream(ts).map(TileSetEntity::toTileSet).collect(toImmutableList()))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable insertOrUpdateOfflineArea(OfflineArea area) {
    return offlineAreaDao
        .insertOrUpdate(OfflineAreaEntity.fromArea(area))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Flowable<ImmutableList<OfflineArea>> getOfflineAreasOnceAndStream() {
    return offlineAreaDao
        .findAllOnceAndStream()
        .map(areas -> stream(areas).map(OfflineAreaEntity::toArea).collect(toImmutableList()))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<OfflineArea> getOfflineAreaById(String id) {
    return offlineAreaDao
        .findById(id)
        .map(OfflineAreaEntity::toArea)
        .toSingle()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable deleteOfflineArea(String id) {
    return offlineAreaDao
        .findById(id)
        .toSingle()
        .doOnSubscribe(__ -> Timber.d("Deleting offline area: %s", id))
        .flatMapCompletable(offlineAreaDao::delete)
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable updateTileSetOfflineAreaReferenceCountByUrl(int newCount, String url) {
    return Completable.fromSingle(tileSetDao.updateBasemapReferenceCount(newCount, url));
  }

  @Override
  public Completable deleteTileSetByUrl(TileSet tileSet) {
    if (tileSet.getOfflineAreaReferenceCount() < 1) {
      return Completable.fromAction(() -> fileUtil.deleteFile(tileSet.getPath()))
          .andThen(Completable.fromMaybe(tileSetDao.deleteByUrl(tileSet.getUrl())))
          .subscribeOn(schedulers.io());
    } else {
      return Completable.complete().subscribeOn(schedulers.io());
    }
  }

  @Override
  public Flowable<ImmutableList<FeatureMutation>> getFeatureMutationsByFeatureIdOnceAndStream(
      String featureId, MutationEntitySyncStatus... allowedStates) {
    return featureMutationDao
        .findByFeatureIdOnceAndStream(featureId, allowedStates)
        .map(
            list -> stream(list).map(FeatureMutationEntity::toMutation).collect(toImmutableList()));
  }

  @Override
  public Flowable<ImmutableList<ObservationMutation>>
      getObservationMutationsByFeatureIdOnceAndStream(
          Project project, String featureId, MutationEntitySyncStatus... allowedStates) {
    return observationMutationDao
        .findByFeatureIdOnceAndStream(featureId, allowedStates)
        .map(list -> stream(list).map(e -> e.toMutation(project)).collect(toImmutableList()));
  }
}
