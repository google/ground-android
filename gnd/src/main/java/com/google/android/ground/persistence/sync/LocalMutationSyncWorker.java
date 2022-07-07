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

package com.google.android.ground.persistence.sync;

import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.android.ground.R;
import com.google.android.ground.model.User;
import com.google.android.ground.model.mutation.Mutation;
import com.google.android.ground.model.mutation.SubmissionMutation;
import com.google.android.ground.model.task.Field.Type;
import com.google.android.ground.persistence.local.LocalDataStore;
import com.google.android.ground.persistence.remote.RemoteDataStore;
import com.google.android.ground.system.NotificationManager;
import com.google.common.collect.ImmutableList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.Map;
import java.util.Set;
import java8.util.stream.Collectors;
import timber.log.Timber;

/**
 * A worker that syncs local changes to the remote data store. Each instance handles mutations for a
 * specific map location of interest, whose id is provided in the {@link Data} object built by
 * {@link #createInputData} and provided to the worker request while being enqueued.
 */
@HiltWorker
public class LocalMutationSyncWorker extends BaseWorker {

  private static final String LOCATION_OF_INTEREST_ID_PARAM_KEY = "locationOfInterestId";

  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final String locationOfInterestId;
  private final PhotoSyncWorkManager photoSyncWorkManager;

  @AssistedInject
  public LocalMutationSyncWorker(
      @Assisted @NonNull Context context,
      @Assisted @NonNull WorkerParameters params,
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      NotificationManager notificationManager,
      PhotoSyncWorkManager photoSyncWorkManager) {
    super(context, params, notificationManager, LocalMutationSyncWorker.class.hashCode());
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.locationOfInterestId = params.getInputData().getString(LOCATION_OF_INTEREST_ID_PARAM_KEY);
    this.photoSyncWorkManager = photoSyncWorkManager;
  }

  /** Returns a new work {@link Data} object containing the specified location of interest id. */
  public static Data createInputData(String locationOfInterestId) {
    return new Data.Builder()
        .putString(LOCATION_OF_INTEREST_ID_PARAM_KEY, locationOfInterestId)
        .build();
  }

  @NonNull
  @Override
  public Result doWork() {
    Timber.d("Connected. Syncing changes to location of interest %s", locationOfInterestId);
    ImmutableList<Mutation> mutations =
        localDataStore.getPendingMutations(locationOfInterestId).blockingGet();
    try {
      Timber.v("Mutations: %s", mutations);
      processMutations(mutations).compose(this::notifyTransferState).blockingAwait();
      return Result.success();
    } catch (Throwable t) {
      FirebaseCrashlytics.getInstance()
          .log("Error applying remote updates to location of interest " + locationOfInterestId);
      FirebaseCrashlytics.getInstance().recordException(t);
      Timber.e(t, "Remote updates for location of interest %s failed", locationOfInterestId);
      localDataStore.updateMutations(incrementRetryCounts(mutations, t)).blockingAwait();
      return Result.retry();
    }
  }

  /**
   * Groups mutations by user id, loads each user, applies mutations, and removes processed
   * mutations.
   */
  private Completable processMutations(ImmutableList<Mutation> pendingMutations) {
    Map<String, ImmutableList<Mutation>> mutationsByUserId = groupByUserId(pendingMutations);
    Set<String> userIds = mutationsByUserId.keySet();
    return Observable.fromIterable(userIds)
        .flatMapCompletable(
            userId -> {
              ImmutableList<Mutation> mutations = mutationsByUserId.get(userId);
              if (mutations == null) {
                mutations = ImmutableList.of();
              }
              return processMutations(mutations, userId);
            });
  }

  /** Loads each user with specified id, applies mutations, and removes processed mutations. */
  private Completable processMutations(ImmutableList<Mutation> mutations, String userId) {
    return localDataStore
        .getUser(userId)
        .flatMapCompletable(user -> processMutations(mutations, user))
        .doOnError(__ -> Timber.d("User account removed before mutation processed"))
        .onErrorComplete();
  }

  /** Applies mutations to remote data store. Once successful, removes them from the local db. */
  private Completable processMutations(ImmutableList<Mutation> mutations, User user) {
    return remoteDataStore
        .applyMutations(mutations, user)
        .andThen(processPhotoFieldMutations(mutations))
        // TODO: If the remote sync fails, reset the state to DEFAULT.
        .andThen(localDataStore.finalizePendingMutations(mutations));
  }

  /**
   * Filters all mutations containing submission mutations with changes to photo fields and uploads
   * to remote storage.
   */
  private Completable processPhotoFieldMutations(@Nullable ImmutableList<Mutation> mutations) {
    return Observable.fromIterable(mutations)
        .filter(mutation -> mutation instanceof SubmissionMutation)
        .flatMapIterable(mutation -> ((SubmissionMutation) mutation).getResponseDeltas())
        .filter(delta -> delta.getFieldType() == Type.PHOTO && delta.getNewResponse().isPresent())
        .map(delta -> delta.getNewResponse().get().toString())
        .flatMapCompletable(
            remotePath ->
                Completable.fromRunnable(() -> photoSyncWorkManager.enqueueSyncWorker(remotePath)));
  }

  private Map<String, ImmutableList<Mutation>> groupByUserId(
      ImmutableList<Mutation> pendingMutations) {
    return stream(pendingMutations)
        .collect(Collectors.groupingBy(Mutation::getUserId, toImmutableList()));
  }

  private ImmutableList<Mutation> incrementRetryCounts(
      ImmutableList<Mutation> mutations, Throwable error) {
    return stream(mutations).map(m -> incrementRetryCount(m, error)).collect(toImmutableList());
  }

  private Mutation incrementRetryCount(Mutation mutation, Throwable error) {
    return mutation.toBuilder()
        .setRetryCount(mutation.getRetryCount() + 1)
        .setLastError(error.toString())
        .build();
  }

  @Override
  public String getNotificationTitle() {
    return getApplicationContext().getString(R.string.uploading_data);
  }
}
