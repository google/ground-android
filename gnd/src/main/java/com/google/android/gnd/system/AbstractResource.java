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

package com.google.android.gnd.system;

import android.support.annotation.Nullable;
import java8.util.Optional;

/**
 * Represents the state of an async process whose result can be represented as an object.
 */
// TODO: Refactor common logic in {@link com.google.android.gnd.repository.Resource}.
public abstract class AbstractResource<S, T> {
  private final S state;
  @Nullable
  private final T data;
  @Nullable
  private final Throwable error;

  protected AbstractResource(S state, T data, Throwable error) {
    this.state = state;
    this.data = data;
    this.error = error;
  }

  public S getState() {
    return state;
  }

  @Nullable
  public Throwable getError() {
    return error;
  }

  public Optional<T> getData() {
    return Optional.ofNullable(data);
  }
}
