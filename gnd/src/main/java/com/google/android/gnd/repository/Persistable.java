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

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.google.android.gnd.rx.Result;
import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Wraps the state of an entity that can be loaded or saved asynchronously. Based on Android Guide
 * to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T> the type of data payload the resource contains.
 */
public class Persistable<T> extends Result<T> {
  private final PersistenceState state;

  public enum PersistenceState {
    NOT_LOADED,
    LOADING,
    LOADED,
    // TODO: Move SAVING states out and rename this class to Loadable.
    SAVING,
    SAVED,
    NOT_FOUND,
    ERROR
  }

  private Persistable(PersistenceState state, @Nullable T data, Throwable error) {
    super(data, error);
    this.state = state;
  }

  public static <T> Persistable<T> notLoaded() {
    return new Persistable<>(PersistenceState.NOT_LOADED, null, null);
  }

  public static <T> Persistable<T> loading() {
    return new Persistable<>(PersistenceState.LOADING, null, null);
  }

  public static <T> Persistable<T> loaded(T data) {
    return new Persistable<>(PersistenceState.LOADED, data, null);
  }

  public static <T> Persistable<T> saving(T data) {
    return new Persistable<>(PersistenceState.SAVING, data, null);
  }

  public static <T> Persistable<T> saved(T data) {
    return new Persistable<>(PersistenceState.SAVED, data, null);
  }

  public static <T> Persistable<T> error(Throwable t) {
    return new Persistable<>(PersistenceState.ERROR, null, t);
  }

  public PersistenceState state() {
    return state;
  }

  public boolean isLoaded() {
    return state == PersistenceState.LOADED;
  }

  // TODO: Move this into new extended LiveData class (LiveResource?).
  @NonNull
  public static <T> Optional<T> getData(LiveData<Persistable<T>> liveData) {
    return liveData.getValue() == null ? Optional.empty() : liveData.getValue().value();
  }
}
