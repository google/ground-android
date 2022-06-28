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
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.tile.TileSet;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.job.Job;
import com.google.android.gnd.model.mutation.FeatureMutation;
import com.google.android.gnd.model.mutation.Mutation;
import com.google.android.gnd.model.mutation.Mutation.SyncStatus;
import com.google.android.gnd.model.mutation.Mutation.Type;
import com.google.android.gnd.model.mutation.SubmissionMutation;
import com.google.android.gnd.model.submission.ResponseMap;
import com.google.android.gnd.model.submission.ResponseMap.Builder;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.model.task.Field;
import com.google.android.gnd.model.task.MultipleChoice;
import com.google.android.gnd.model.task.Option;
import com.google.android.gnd.model.task.Step;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.room.converter.ResponseDeltasConverter;
import com.google.android.gnd.persistence.local.room.converter.ResponseMapConverter;
import com.google.android.gnd.persistence.local.room.dao.BaseMapDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureMutationDao;
import com.google.android.gnd.persistence.local.room.dao.FieldDao;
import com.google.android.gnd.persistence.local.room.dao.JobDao;
import com.google.android.gnd.persistence.local.room.dao.MultipleChoiceDao;
import com.google.android.gnd.persistence.local.room.dao.OfflineAreaDao;
import com.google.android.gnd.persistence.local.room.dao.OptionDao;
import com.google.android.gnd.persistence.local.room.dao.SubmissionDao;
import com.google.android.gnd.persistence.local.room.dao.SubmissionMutationDao;
import com.google.android.gnd.persistence.local.room.dao.SurveyDao;
import com.google.android.gnd.persistence.local.room.dao.TaskDao;
import com.google.android.gnd.persistence.local.room.dao.TileSetDao;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import com.google.android.gnd.persistence.local.room.entity.AuditInfoEntity;
import com.google.android.gnd.persistence.local.room.entity.BaseMapEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.FieldEntity;
import com.google.android.gnd.persistence.local.room.entity.JobEntity;
import com.google.android.gnd.persistence.local.room.entity.MultipleChoiceEntity;
import com.google.android.gnd.persistence.local.room.entity.OfflineAreaEntity;
import com.google.android.gnd.persistence.local.room.entity.OptionEntity;
import com.google.android.gnd.persistence.local.room.entity.SubmissionEntity;
import com.google.android.gnd.persistence.local.room.entity.SubmissionMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.SurveyEntity;
import com.google.android.gnd.persistence.local.room.entity.TaskEntity;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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
  @Inject TaskDao taskDao;
  @Inject
  JobDao jobDao;
  @Inject SurveyDao surveyDao;
  @Inject FeatureDao featureDao;
  @Inject FeatureMutationDao featureMutationDao;
  @Inject SubmissionDao submissionDao;
  @Inject SubmissionMutationDao submissionMutationDao;
  @Inject TileSetDao tileSetDao;
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

  private Completable insertOrUpdateField(String taskId, Step.Type stepType, Field field) {
    return fieldDao
        .insertOrUpdate(FieldEntity.fromField(taskId, stepType, field))
        .andThen(
            Observable.just(field)
                .filter(__ -> field.getMultipleChoice() != null)
                .flatMapCompletable(
                    __ ->
                        insertOrUpdateMultipleChoice(
                            field.getId(), checkNotNull(field.getMultipleChoice()))))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateSteps(String taskId, ImmutableList<Step> steps) {
    return Observable.fromIterable(steps)
        .filter(step -> step.getType() == Step.Type.FIELD)
        .flatMapCompletable(
            step -> insertOrUpdateField(taskId, step.getType(), step.getField()));
  }

  private Completable insertOrUpdateTask(String jobId, Task task) {
    return taskDao
        .insertOrUpdate(TaskEntity.fromTask(jobId, task))
        .andThen(insertOrUpdateSteps(task.getId(), task.getSteps()))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateTasks(String jobId, List<Task> tasks) {
    return Observable.fromIterable(tasks)
        .flatMapCompletable(task -> insertOrUpdateTask(jobId, task));
  }

  private Completable insertOrUpdateJob(String surveyId, Job job) {
    return jobDao
        .insertOrUpdate(JobEntity.fomJob(surveyId, job))
        .andThen(
            insertOrUpdateTasks(
                job.getId(), job.getTask().map(Arrays::asList).orElseGet(ArrayList::new)))
        .subscribeOn(schedulers.io());
  }

  private Completable insertOrUpdateJobs(String surveyId, List<Job> jobs) {
    return Observable.fromIterable(jobs)
        .flatMapCompletable(job -> insertOrUpdateJob(surveyId, job));
  }

  private Completable insertOfflineBaseMapSources(Survey survey) {
    return Observable.fromIterable(survey.getBaseMaps())
        .flatMapCompletable(
            source -> baseMapDao.insert(BaseMapEntity.fromModel(survey.getId(), source)));
  }

  @Transaction
  @Override
  public Completable insertOrUpdateSurvey(Survey survey) {
    return surveyDao
        .insertOrUpdate(SurveyEntity.fromSurvey(survey))
        .andThen(jobDao.deleteBySurveyId(survey.getId()))
        .andThen(insertOrUpdateJobs(survey.getId(), survey.getJobs()))
        .andThen(baseMapDao.deleteBySurveyId(survey.getId()))
        .andThen(insertOfflineBaseMapSources(survey))
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
  public Single<ImmutableList<Survey>> getSurveys() {
    return surveyDao
        .getAllSurveys()
        .map(list -> stream(list).map(SurveyEntity::toSurvey).collect(toImmutableList()))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Survey> getSurveyById(String id) {
    return surveyDao.getSurveyById(id).map(SurveyEntity::toSurvey).subscribeOn(schedulers.io());
  }

  @Override
  public Completable deleteSurvey(Survey survey) {
    return surveyDao.delete(SurveyEntity.fromSurvey(survey)).subscribeOn(schedulers.io());
  }

  @Transaction
  @Override
  public Completable applyAndEnqueue(FeatureMutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      FirebaseCrashlytics.getInstance()
          .log(
              "Error enqueueing "
                  + mutation.getType()
                  + "mutation for feature "
                  + mutation.getFeatureId());
      FirebaseCrashlytics.getInstance().recordException(e);
      return Completable.error(e);
    }
  }

  @Override
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Survey survey) {
    return featureDao
        .findOnceAndStream(survey.getId(), EntityState.DEFAULT)
        .map(featureEntities -> toFeatures(survey, featureEntities))
        .subscribeOn(schedulers.io());
  }

  private ImmutableSet<Feature> toFeatures(Survey survey, List<FeatureEntity> featureEntities) {
    return stream(featureEntities)
        .flatMap(f -> logErrorsAndSkip(() -> FeatureEntity.toFeature(f, survey)))
        .collect(toImmutableSet());
  }

  @Override
  public Maybe<Feature> getFeature(Survey survey, String featureId) {
    return featureDao
        .findById(featureId)
        .map(f -> FeatureEntity.toFeature(f, survey))
        .doOnError(e -> Timber.e(e))
        .onErrorComplete()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Maybe<Submission> getSubmission(Feature feature, String submissionId) {
    return submissionDao
        .findById(submissionId)
        .map(obs -> SubmissionEntity.toSubmission(feature, obs))
        .doOnError(e -> Timber.d(e))
        .onErrorComplete()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<ImmutableList<Submission>> getSubmissions(Feature feature, String taskId) {
    return submissionDao
        .findByFeatureId(feature.getId(), taskId, EntityState.DEFAULT)
        .map(submissionEntities -> toSubmissions(feature, submissionEntities))
        .subscribeOn(schedulers.io());
  }

  private ImmutableList<Submission> toSubmissions(
      Feature feature, List<SubmissionEntity> submissionEntities) {
    return stream(submissionEntities)
        .flatMap(obs -> logErrorsAndSkip(() -> SubmissionEntity.toSubmission(feature, obs)))
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
  public Flowable<ImmutableList<Mutation>> getMutationsOnceAndStream(Survey survey) {
    // TODO: Show mutations for all surveys, not just current one.
    Flowable<ImmutableList<FeatureMutation>> featureMutations =
        featureMutationDao
            .loadAllOnceAndStream()
            .map(
                list ->
                    stream(list)
                        .filter(entity -> entity.getSurveyId().equals(survey.getId()))
                        .map(FeatureMutationEntity::toMutation)
                        .collect(toImmutableList()))
            .subscribeOn(schedulers.io());
    Flowable<ImmutableList<SubmissionMutation>> submissionMutations =
        submissionMutationDao
            .loadAllOnceAndStream()
            .map(
                list ->
                    stream(list)
                        .filter(entity -> entity.getSurveyId().equals(survey.getId()))
                        .map(entity -> entity.toMutation(survey))
                        .collect(toImmutableList()))
            .subscribeOn(schedulers.io());
    return Flowable.combineLatest(
        featureMutations, submissionMutations, this::combineAndSortMutations);
  }

  private ImmutableList<Mutation> combineAndSortMutations(
      ImmutableList<FeatureMutation> featureMutations,
      ImmutableList<SubmissionMutation> submissionMutations) {
    return ImmutableList.sortedCopyOf(
        Mutation.byDescendingClientTimestamp(),
        ImmutableList.<Mutation>builder()
            .addAll(featureMutations)
            .addAll(submissionMutations)
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
            submissionMutationDao
                .findByFeatureId(featureId, MutationEntitySyncStatus.PENDING)
                .flattenAsObservable(oms -> oms)
                .flatMap(
                    ome ->
                        getSurveyById(ome.getSurveyId())
                            .toSingle()
                            .map(ome::toMutation)
                            .toObservable()
                            .doOnError(e -> Timber.e(e, "Submission mutation skipped"))
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
            submissionMutationDao
                .updateAll(toSubmissionMutationEntities(mutations))
                .subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
  }

  private ImmutableList<SubmissionMutationEntity> toSubmissionMutationEntities(
      ImmutableList<Mutation> mutations) {
    return stream(SubmissionMutation.filter(mutations))
        .map(SubmissionMutationEntity::fromMutation)
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
              if (mutation instanceof SubmissionMutation) {
                return deleteSubmission(((SubmissionMutation) mutation).getSubmissionId());
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
    ImmutableList<SubmissionMutationEntity> submissionMutations =
        stream(SubmissionMutation.filter(mutations))
            .map(mutation -> mutation.toBuilder().setSyncStatus(SyncStatus.COMPLETED).build())
            .map(SubmissionMutationEntity::fromMutation)
            .collect(toImmutableList());
    return featureMutationDao
        .updateAll(featureMutations)
        .andThen(submissionMutationDao.updateAll(submissionMutations).subscribeOn(schedulers.io()))
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
  public Completable mergeSubmission(Submission submission) {
    SubmissionEntity submissionEntity = SubmissionEntity.fromSubmission(submission);
    return submissionMutationDao
        .findBySubmissionId(
            submission.getId(),
            MutationEntitySyncStatus.PENDING,
            MutationEntitySyncStatus.IN_PROGRESS)
        .flatMapCompletable(
            mutations -> mergeSubmission(submission.getTask(), submissionEntity, mutations))
        .subscribeOn(schedulers.io());
  }

  private Completable mergeSubmission(
      Task task, SubmissionEntity submission, List<SubmissionMutationEntity> mutations) {
    if (mutations.isEmpty()) {
      return submissionDao.insertOrUpdate(submission);
    }
    SubmissionMutationEntity lastMutation = mutations.get(mutations.size() - 1);
    checkNotNull(lastMutation, "Could not get last mutation");
    return getUser(lastMutation.getUserId())
        .map(user -> applyMutations(task, submission, mutations, user))
        .flatMapCompletable(obs -> submissionDao.insertOrUpdate(obs));
  }

  private SubmissionEntity applyMutations(
      Task task, SubmissionEntity submission, List<SubmissionMutationEntity> mutations, User user) {
    SubmissionMutationEntity lastMutation = mutations.get(mutations.size() - 1);
    long clientTimestamp = lastMutation.getClientTimestamp();
    Timber.v("Merging submission " + this + " with mutations " + mutations);
    SubmissionEntity.Builder builder = submission.toBuilder();
    builder.setResponses(
        ResponseMapConverter.toString(applyMutations(task, submission, mutations)));
    // Update modified user and time.
    AuditInfoEntity lastModified =
        AuditInfoEntity.builder()
            .setUser(UserDetails.fromUser(user))
            .setClientTimestamp(clientTimestamp)
            .build();
    builder.setLastModified(lastModified);
    Timber.v("Merged submission %s", builder.build());
    return builder.build();
  }

  private ResponseMap applyMutations(
      Task task, SubmissionEntity submission, List<SubmissionMutationEntity> mutations) {
    Builder responseMap =
        ResponseMapConverter.fromString(task, submission.getResponses()).toBuilder();
    for (SubmissionMutationEntity mutation : mutations) {
      // Merge changes to responses.
      responseMap.applyDeltas(
          ResponseDeltasConverter.fromString(task, mutation.getResponseDeltas()));
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
  public Completable applyAndEnqueue(SubmissionMutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      FirebaseCrashlytics.getInstance()
          .log(
              "Error enqueueing "
                  + mutation.getType()
                  + "mutation for submission "
                  + mutation.getSubmissionId());
      FirebaseCrashlytics.getInstance().recordException(e);
      return Completable.error(e);
    }
  }

  /**
   * Applies mutation to submission in database or creates a new one.
   *
   * @return A Completable that emits an error if mutation type is "UPDATE" but entity does not
   *     exist, or if type is "CREATE" and entity already exists.
   */
  public Completable apply(SubmissionMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return getUser(mutation.getUserId())
            .flatMapCompletable(user -> createSubmission(mutation, user));
      case UPDATE:
        return getUser(mutation.getUserId())
            .flatMapCompletable(user -> updateSubmission(mutation, user));
      case DELETE:
        return submissionDao
            .findById(mutation.getSubmissionId())
            .flatMapCompletable(entity -> markSubmissionForDeletion(entity, mutation));
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable createSubmission(SubmissionMutation mutation, User user) {
    return submissionDao
        .insert(SubmissionEntity.fromMutation(mutation, AuditInfo.now(user)))
        .doOnSubscribe(__ -> Timber.v("Inserting submission: %s", mutation))
        .subscribeOn(schedulers.io());
  }

  private Completable updateSubmission(SubmissionMutation mutation, User user) {
    SubmissionMutationEntity mutationEntity = SubmissionMutationEntity.fromMutation(mutation);
    return submissionDao
        .findById(mutation.getSubmissionId())
        .doOnSubscribe(__ -> Timber.v("Applying mutation: %s", mutation))
        .switchIfEmpty(fallbackSubmission(mutation))
        .map(obs -> applyMutations(mutation.getTask(), obs, ImmutableList.of(mutationEntity), user))
        .flatMapCompletable(obs -> submissionDao.insertOrUpdate(obs).subscribeOn(schedulers.io()))
        .subscribeOn(schedulers.io());
  }

  /**
   * Returns a source which creates an submission based on the provided mutation. Used in rare cases
   * when the submission is no longer in the local db, but the user is updating rather than creating
   * a new submission. In these cases creation metadata is unknown, so empty audit info is used.
   */
  private SingleSource<SubmissionEntity> fallbackSubmission(SubmissionMutation mutation) {
    return em -> em.onSuccess(SubmissionEntity.fromMutation(mutation, AuditInfo.builder().build()));
  }

  private Completable markSubmissionForDeletion(
      SubmissionEntity entity, SubmissionMutation mutation) {
    return submissionDao
        .update(entity.toBuilder().setState(EntityState.DELETED).build())
        .doOnSubscribe(__ -> Timber.d("Marking submission as deleted : %s", mutation))
        .ignoreElement()
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable deleteSubmission(String submissionId) {
    return submissionDao
        .findById(submissionId)
        .toSingle()
        .doOnSubscribe(__ -> Timber.d("Deleting local submission : %s", submissionId))
        .flatMapCompletable(entity -> submissionDao.delete(entity))
        .subscribeOn(schedulers.io());
  }

  private Completable enqueue(SubmissionMutation mutation) {
    return submissionMutationDao
        .insert(SubmissionMutationEntity.fromMutation(mutation))
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
    return tileSetDao.findByUrl(tileUrl).map(TileSetEntity::toTileSet).subscribeOn(schedulers.io());
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
  public Flowable<ImmutableList<SubmissionMutation>> getSubmissionMutationsByFeatureIdOnceAndStream(
      Survey survey, String featureId, MutationEntitySyncStatus... allowedStates) {
    return submissionMutationDao
        .findByFeatureIdOnceAndStream(featureId, allowedStates)
        .map(list -> stream(list).map(e -> e.toMutation(survey)).collect(toImmutableList()));
  }
}
