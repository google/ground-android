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

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.android.gnd.rx.RxTransformers;

import javax.annotation.Nullable;

import io.reactivex.FlowableTransformer;
import java8.util.Optional;
import java8.util.function.Consumer;

/**
 * Represents a resource that can be loaded from local or remote data stores. Based on Android Guide
 * to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T>
 */
public class Resource<T> {
  public enum Status {
    NOT_LOADED,
    LOADING,
    LOADED,
    SAVING,
    SAVED,
    NOT_FOUND,
    // REMOVED? ID?
    ERROR
  }

  public enum SyncStatus {
    UNKNOWN,
    NO_CHANGES_PENDING,
    LOCAL_CHANGES_PENDING
  }

  // TODO: Enable Nullness checking, NonNull by default.
  private final Status status;

  @Nullable private final T data;

  @Nullable private final Throwable error;

  private final SyncStatus syncStatus;

  private Resource(
      Status status, @Nullable T data, @Nullable Throwable error, SyncStatus syncStatus) {
    this.status = status;
    this.data = data;
    this.error = error;
    this.syncStatus = syncStatus;
  }

  public static <T> Resource<T> notLoaded() {
    return new Resource<>(Status.NOT_LOADED, null, null, SyncStatus.UNKNOWN);
  }

  public static <T> Resource<T> loading() {
    return new Resource<>(Status.LOADING, null, null, SyncStatus.UNKNOWN);
  }

  public static <T> Resource<T> loaded(T data) {
    return new Resource<>(Status.LOADED, data, null, SyncStatus.NO_CHANGES_PENDING);
  }

  public static <T> Resource<T> loaded(T data, SyncStatus syncStatus) {
    return new Resource<>(Status.LOADED, data, null, syncStatus);
  }

  public static <T> Resource<T> saving(T data) {
    return new Resource<>(Status.SAVING, data, null, SyncStatus.LOCAL_CHANGES_PENDING);
  }

  public static <T> Resource<T> saved(T data) {
    return new Resource<>(Status.SAVED, data, null, SyncStatus.LOCAL_CHANGES_PENDING);
  }

  public static <T> Resource<T> saved(T data, SyncStatus syncStatus) {
    return new Resource<>(Status.SAVED, data, null, syncStatus);
  }

  public static <T> Resource<T> error(Throwable t) {
    return new Resource<>(Status.ERROR, null, t, SyncStatus.UNKNOWN);
  }

  public Status getStatus() {
    return status;
  }

  public Optional<T> getData() {
    return Optional.ofNullable(data);
  }

  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  public SyncStatus getSyncStatus() {
    return syncStatus;
  }

  public boolean isLoaded() {
    return status.equals(Status.LOADED);
  }

  public boolean isSynced() {
    return syncStatus.equals(SyncStatus.NO_CHANGES_PENDING);
  }

  public void ifPresent(Consumer<T> consumer) {
    if (data != null) {
      consumer.accept(data);
    }
  }

  @NonNull
  public static <T> Resource<T> getValue(LiveData<Resource<T>> liveData) {
    return Optional.ofNullable(liveData.getValue()).orElse(notLoaded());
  }

  // TODO: Move this into new extended LiveData class (LiveResource?).
  @NonNull
  public static <T> Optional<T> getData(LiveData<Resource<T>> liveData) {
    return getValue(liveData).getData();
  }

  public static <T> FlowableTransformer<Resource<T>, T> filterAndGetData() {
    return upstream ->
        upstream.map(Resource::getData).compose(RxTransformers.filterAndGetOptional());
  }
}
