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

package com.google.android.gnd.rx;

import androidx.annotation.Nullable;
import java8.util.Optional;
import java8.util.function.Consumer;

/**
 * Represents the state of an async process whose results can be represented as a single object.
 *
 * @param <S> the type to use to represent the resource's state (e.g., loading, complete, error).
 * @param <T> the type of data payload the resource contains.
 */
public abstract class AbstractResource<S, T> {
  private final OperationState<S> operationState;
  @Nullable private final T data;

  protected AbstractResource(OperationState<S> operationState, @Nullable T data) {
    this.operationState = operationState;
    this.data = data;
  }

  public OperationState<S> operationState() {
    return operationState;
  }

  public Optional<T> data() {
    return Optional.ofNullable(data);
  }

  public void ifPresent(Consumer<T> consumer) {
    if (data != null) {
      consumer.accept(data);
    }
  }
}
