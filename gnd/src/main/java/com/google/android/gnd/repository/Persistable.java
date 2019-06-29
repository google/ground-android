/*
 * Copyright 2018 Google LLC
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
import com.google.android.gnd.repository.Persistable.PersistenceState;
import com.google.android.gnd.rx.AbstractResource;
import com.google.android.gnd.rx.OperationState;
import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Wraps the state of an entity that can be loaded or saved asynchronously. Based on Android Guide
 * to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T> the type of data payload the resource contains.
 */
public class Persistable<T> extends AbstractResource<PersistenceState, T> {
  public enum PersistenceState {
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

  private Persistable(
      OperationState<PersistenceState> operationState, @Nullable T data, SyncStatus syncStatus) {
    super(operationState, data);
    this.syncStatus = syncStatus;
  }

  public static <T> Persistable<T> notLoaded() {
    return new Persistable<>(OperationState.of(PersistenceState.NOT_LOADED), null, SyncStatus.UNKNOWN);
  }

  public static <T> Persistable<T> loading() {
    return new Persistable<>(OperationState.of(PersistenceState.LOADING), null, SyncStatus.UNKNOWN);
  }

  public static <T> Persistable<T> loaded(T data) {
    return new Persistable<>(OperationState.of(PersistenceState.LOADED), data, SyncStatus.NO_CHANGES_PENDING);
  }

  public static <T> Persistable<T> loaded(T data, SyncStatus syncStatus) {
    return new Persistable<>(OperationState.of(PersistenceState.LOADED), data, syncStatus);
  }

  public static <T> Persistable<T> saving(T data) {
    return new Persistable<>(OperationState.of(PersistenceState.SAVING), data, SyncStatus.LOCAL_CHANGES_PENDING);
  }

  public static <T> Persistable<T> saved(T data) {
    return new Persistable<>(OperationState.of(PersistenceState.SAVED), data, SyncStatus.LOCAL_CHANGES_PENDING);
  }

  public static <T> Persistable<T> saved(T data, SyncStatus syncStatus) {
    return new Persistable<>(OperationState.of(PersistenceState.SAVED), data, syncStatus);
  }

  public static <T> Persistable<T> error(Throwable t) {
    return new Persistable<>(OperationState.error(PersistenceState.ERROR, t), null, SyncStatus.UNKNOWN);
  }

  public SyncStatus getSyncStatus() {
    return syncStatus;
  }

  public boolean isLoaded() {
    return operationState().get() == PersistenceState.LOADED;
  }

  @NonNull
  public static <T> Persistable<T> getValue(LiveData<Persistable<T>> liveData) {
    return Optional.ofNullable(liveData.getValue()).orElse(notLoaded());
  }

  // TODO: Move this into new extended LiveData class (LiveResource?).
  @NonNull
  public static <T> Optional<T> getData(LiveData<Persistable<T>> liveData) {
    return getValue(liveData).data();
  }
}
