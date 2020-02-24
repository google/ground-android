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

import io.reactivex.Observable;
import java8.util.Optional;
import java8.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Represents the outcome of an operation that either succeeds with a value, or fails with an
 * exception.
 *
 * @param <T> the type of value held by instances of this {@code ValueOrError}.
 */
public class ValueOrError<T> {
  @Nullable private T value;
  @Nullable private Throwable error;

  protected ValueOrError(@Nullable T value, @Nullable Throwable error) {
    this.value = value;
    this.error = error;
  }

  public Optional<T> value() {
    return Optional.ofNullable(value);
  }

  public Optional<Throwable> error() {
    return Optional.ofNullable(error);
  }

  @Override
  public String toString() {
    return error().map(t -> "Error: " + t).orElse("Value: " + value);
  }

  /** Returns the value returned by the specified supplier, or an error if the supplier fails. */
  public static <T> ValueOrError<T> create(Supplier<T> supplier) {
    try {
      return ValueOrError.newValue(supplier.get());
    } catch (Throwable e) {
      return ValueOrError.newError(e);
    }
  }

  /** Returns a new instance with the specified value. */
  public static <T> ValueOrError<T> newValue(T value) {
    return new ValueOrError(value, null);
  }

  /** Returns a new instance with the specified error. */
  public static <T> ValueOrError<T> newError(Throwable t) {
    return new ValueOrError(null, t);
  }

  /** Modifies the specified stream to ignore errors, returning wrapped values. */
  public static <T> Observable<T> ignoreErrors(Observable<ValueOrError<T>> observable) {
    return observable.filter(voe -> voe.value().isPresent()).map(voe -> voe.value().get());
  }
}
