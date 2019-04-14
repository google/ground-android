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

package com.google.android.gnd.repository;

import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;
import com.google.android.gnd.rx.AbstractResource;
import com.google.android.gnd.rx.OperationState;
import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents a resource that can be loaded from local or remote data stores. Based on Android Guide
 * to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T> the type of data payload the resource contains.
 */
public class Resource<T> extends AbstractResource<Resource.Status, T> {
  public enum Status {
    NOT_LOADED,
    LOADING,
    LOADED,
    SAVING,
    SAVED,
    NOT_FOUND,
    ERROR
  }

  public enum SyncStatus {
    UNKNOWN,
    NO_CHANGES_PENDING,
    LOCAL_CHANGES_PENDING
  }

  private final SyncStatus syncStatus;

  private Resource(
      OperationState<Status> operationState, @Nullable T data, SyncStatus syncStatus) {
    super(operationState, data);
    this.syncStatus = syncStatus;
  }

  public static <T> Resource<T> notLoaded() {
    return new Resource<>(OperationState.of(Status.NOT_LOADED), null, SyncStatus.UNKNOWN);
  }

  public static <T> Resource<T> loading() {
    return new Resource<>(OperationState.of(Status.LOADING), null, SyncStatus.UNKNOWN);
  }

  public static <T> Resource<T> loaded(T data) {
    return new Resource<>(OperationState.of(Status.LOADED), data, SyncStatus.NO_CHANGES_PENDING);
  }

  public static <T> Resource<T> loaded(T data, SyncStatus syncStatus) {
    return new Resource<>(OperationState.of(Status.LOADED), data, syncStatus);
  }

  public static <T> Resource<T> saving(T data) {
    return new Resource<>(OperationState.of(Status.SAVING), data, SyncStatus.LOCAL_CHANGES_PENDING);
  }

  public static <T> Resource<T> saved(T data) {
    return new Resource<>(OperationState.of(Status.SAVED), data, SyncStatus.LOCAL_CHANGES_PENDING);
  }

  public static <T> Resource<T> saved(T data, SyncStatus syncStatus) {
    return new Resource<>(OperationState.of(Status.SAVED), data, syncStatus);
  }

  public static <T> Resource<T> error(Throwable t) {
    return new Resource<>(OperationState.error(Status.ERROR, t), null, SyncStatus.UNKNOWN);
  }

  public SyncStatus getSyncStatus() {
    return syncStatus;
  }

  public boolean isLoaded() {
    return operationState().get() == Status.LOADED;
  }

  @NonNull
  public static <T> Resource<T> getValue(LiveData<Resource<T>> liveData) {
    return Optional.ofNullable(liveData.getValue()).orElse(notLoaded());
  }

  // TODO: Move this into new extended LiveData class (LiveResource?).
  @NonNull
  public static <T> Optional<T> getData(LiveData<Resource<T>> liveData) {
    return getValue(liveData).data();
  }
}
