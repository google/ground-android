/*
 * Copyright 2021 Google LLC
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

package com.sharedtest.persistence.sync;

import android.app.PendingIntent;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.UUID;

public class FakeWorkManager extends WorkManager {

  @NonNull
  @Override
  public Operation enqueue(@NonNull List<? extends WorkRequest> requests) {
    return null;
  }

  @NonNull
  @Override
  public WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work) {
    return null;
  }

  @NonNull
  @Override
  public WorkContinuation beginUniqueWork(
      @NonNull String uniqueWorkName,
      @NonNull ExistingWorkPolicy existingWorkPolicy,
      @NonNull List<OneTimeWorkRequest> work) {
    return null;
  }

  @NonNull
  @Override
  public Operation enqueueUniqueWork(
      @NonNull String uniqueWorkName,
      @NonNull ExistingWorkPolicy existingWorkPolicy,
      @NonNull List<OneTimeWorkRequest> work) {
    return null;
  }

  @NonNull
  @Override
  public Operation enqueueUniquePeriodicWork(
      @NonNull String uniqueWorkName,
      @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
      @NonNull PeriodicWorkRequest periodicWork) {
    return null;
  }

  @NonNull
  @Override
  public Operation cancelWorkById(@NonNull UUID id) {
    return null;
  }

  @NonNull
  @Override
  public Operation cancelAllWorkByTag(@NonNull String tag) {
    return null;
  }

  @NonNull
  @Override
  public Operation cancelUniqueWork(@NonNull String uniqueWorkName) {
    return null;
  }

  @NonNull
  @Override
  public Operation cancelAllWork() {
    return null;
  }

  @NonNull
  @Override
  public PendingIntent createCancelPendingIntent(@NonNull UUID id) {
    return null;
  }

  @NonNull
  @Override
  public Operation pruneWork() {
    return null;
  }

  @NonNull
  @Override
  public LiveData<Long> getLastCancelAllTimeMillisLiveData() {
    return null;
  }

  @NonNull
  @Override
  public ListenableFuture<Long> getLastCancelAllTimeMillis() {
    return null;
  }

  @NonNull
  @Override
  public LiveData<WorkInfo> getWorkInfoByIdLiveData(@NonNull UUID id) {
    return null;
  }

  @NonNull
  @Override
  public ListenableFuture<WorkInfo> getWorkInfoById(@NonNull UUID id) {
    return null;
  }

  @NonNull
  @Override
  public LiveData<List<WorkInfo>> getWorkInfosByTagLiveData(@NonNull String tag) {
    return null;
  }

  @NonNull
  @Override
  public ListenableFuture<List<WorkInfo>> getWorkInfosByTag(@NonNull String tag) {
    return null;
  }

  @NonNull
  @Override
  public LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(
      @NonNull String uniqueWorkName) {
    return null;
  }

  @NonNull
  @Override
  public ListenableFuture<List<WorkInfo>> getWorkInfosForUniqueWork(
      @NonNull String uniqueWorkName) {
    return null;
  }

  @NonNull
  @Override
  public LiveData<List<WorkInfo>> getWorkInfosLiveData(@NonNull WorkQuery workQuery) {
    return null;
  }

  @NonNull
  @Override
  public ListenableFuture<List<WorkInfo>> getWorkInfos(@NonNull WorkQuery workQuery) {
    return null;
  }
}
