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
import com.google.android.gnd.rx.RxTransformers;
import io.reactivex.FlowableTransformer;
import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents a resource that can be loaded from local or remote data stores. Based on Android Guide
 * to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T>
 */
public class Resource<T> {
  /**
   * Note that "loading" state is intentionally not represented here. This is to prevent streams and
   * LiveData from getting stuck in the loading state if a fetch is interrupted while in progress.
   * Rather than manage these cases explicitly, we leave it up to the triggering UIs to display
   * loading affordances upon requesting new data. Those states can then be cleared once the
   * Resource request is complete. TODO: Is this actually a good idea?
   */
  public enum State {
    NOT_LOADED,
    LOADED,
    NOT_FOUND,
    // REMOVED? ID?
    ERROR
  }

  public enum SyncStatus {
    UNKNOWN,
    NO_PENDING_CHANGES,
    LOCAL_CHANGES_PENDING
  }

  // TODO: Enable Nullness checking, NonNull by default.
  private final State state;

  @Nullable private final T data;

  @Nullable private final Throwable error;

  private final SyncStatus syncStatus;

  private Resource(
      State state, @Nullable T data, @Nullable Throwable error, SyncStatus syncStatus) {
    this.state = state;
    this.data = data;
    this.error = error;
    this.syncStatus = syncStatus;
  }

  public static <T> Resource<T> notLoaded() {
    return new Resource<>(State.NOT_LOADED, null, null, SyncStatus.UNKNOWN);
  }

  public static <T> Resource<T> loaded(T data) {
    return new Resource<>(State.LOADED, data, null, SyncStatus.NO_PENDING_CHANGES);
  }

  public static <T> Resource<T> loaded(T data, SyncStatus syncStatus) {
    return new Resource<>(State.LOADED, data, null, syncStatus);
  }

  public static <T> Resource<T> error(Throwable t) {
    return new Resource<>(State.ERROR, null, t, SyncStatus.UNKNOWN);
  }

  public State getState() {
    return state;
  }

  public Optional<T> get() {
    return Optional.ofNullable(data);
  }

  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  public SyncStatus getSyncStatus() {
    return syncStatus;
  }

  public boolean isLoaded() {
    return state.equals(State.LOADED);
  }

  public boolean isSynced() {
    return syncStatus.equals(SyncStatus.NO_PENDING_CHANGES);
  }

  /**
   * Get value from LiveData in a null-safe way.
   *
   * @param liveData
   * @param <T>
   * @return
   */
  public static <T> Resource<T> getValue(LiveData<Resource<T>> liveData) {
    return Optional.ofNullable(liveData.getValue()).orElse(notLoaded());
  }

  public static <T> FlowableTransformer<Resource<T>, T> ifPresentGet() {
    return upstream -> upstream.map(Resource::get).compose(RxTransformers.ifPresentGet());
  }
}
