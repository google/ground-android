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

package com.google.android.gnd.rx;

import android.support.annotation.Nullable;
import java8.util.Optional;
import java8.util.function.Consumer;

/**
 * Represents the state of an async process whose result can be represented as an object.
 * @param <S> the type to use to represent the resource's state (e.g., loading, complete, error).
 * @param <T> the type of data payload the resource contains.
 */
public abstract class AbstractResource<S, T> {
  private final S status;
  @Nullable private final T data;
  @Nullable private final Throwable error;

  protected AbstractResource(S status, @Nullable T data, @Nullable Throwable error) {
    this.status = status;
    this.data = data;
    this.error = error;
  }

  public S getStatus() {
    return status;
  }

  public Optional<T> getData() {
    return Optional.ofNullable(data);
  }

  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  public void ifPresent(Consumer<T> consumer) {
    if (data != null) {
      consumer.accept(data);
    }
  }
}
