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

package com.google.android.gnd.persistence.sync;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.shared.Mutation;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;

/**
 * A worker that syncs local changes to the remote data store. Each instance handles mutations for a
 * specific map feature, whose id is provided in the {@link Data} object built by {@link
 * #createInputData} and provided to the worker request while being enqueued.
 */
public class LocalMutationSyncWorker extends Worker {

  private static final String TAG = LocalMutationSyncWorker.class.getSimpleName();
  public static final String FEATURE_ID_PARAM_KEY = "featureId";

  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final String featureId;

  public LocalMutationSyncWorker(
      @NonNull Context context,
      @NonNull WorkerParameters params,
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore) {
    super(context, params);
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.featureId = params.getInputData().getString(FEATURE_ID_PARAM_KEY);
  }

  /** Returns a new work {@link Data} object containing the specified feature id. */
  public static Data createInputData(String featureId) {
    return new Data.Builder().putString(FEATURE_ID_PARAM_KEY, featureId).build();
  }

  @NonNull
  @Override
  public Result doWork() {
    try {
      Log.d(TAG, "Connected. Syncing changes to feature " + featureId);
      localDataStore
          .getPendingMutations(featureId)
          .flatMapCompletable(this::processMutations)
          .blockingAwait();
      return Result.success();
    } catch (Throwable t) {
      Log.e(TAG, "Updates for feature " + featureId + " failed", t);
      // TODO: Update retry count in mutations.
      return Result.retry();
    }
  }

  private Completable processMutations(ImmutableList<Mutation> pendingMutations) {
    return remoteDataStore
        .applyMutations(pendingMutations)
        .andThen(localDataStore.removePendingMutations(pendingMutations));
  }
}
