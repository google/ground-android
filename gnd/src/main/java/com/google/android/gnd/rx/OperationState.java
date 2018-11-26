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

import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents a streamable operation state represented by an enum with an optional error state.
 * @param <S> the type used to represent the status of an operation. Usually a Boolean or Enum.
 */
public class OperationState<S> {
  private S state;
  @Nullable private Throwable error;

  protected OperationState(S state, @Nullable Throwable error) {
    this.state = state;
    this.error = error;
  }

  public S get() {
    return state;
  }

  public boolean hasError() {
    return error != null;
  }

  public Optional<Throwable> error() {
    return Optional.ofNullable(error);
  }

  public static <S> OperationState<S> of(S state) {
    return new OperationState(state, null);
  }

  public static <S> OperationState<S> error(S state, Throwable t) {
    return new OperationState(state, t);
  }
}
