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

import io.reactivex.Single;
import java8.util.Optional;
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

  public static <T> ValueOrError<T> of(T value) {
    return new ValueOrError(value, null);
  }

  public static <T> ValueOrError<T> error(Throwable t) {
    return new ValueOrError(null, t);
  }

  @Override
  public String toString() {
    return error().map(t -> "Error: " + t).orElse("Value: " + value);
  }

  /**
   * Returns a {@link Single} that emits a {@code ValueOrError} representing either the value
   * emitted by the source on success, or the error which caused the operation to fail. The returned
   * stream itself should never fail with an error.
   *
   * @param source the stream to be modified.
   * @param <T> the type of value emitted on success.
   */
  public static <T> Single<ValueOrError<T>> wrap(Single<T> source) {
    return source.map(ValueOrError::of).onErrorReturn(ValueOrError::error);
  }

  /**
   * Returns a {@link Single} that emits a success value, or fails with an error depending on the
   * contents of the {@code ValueOrError} emitted by the source stream.
   *
   * @param source the stream to be modified.
   * @param <T> the type of value emitted on success.
   */
  public static <T> Single<T> unwrap(Single<ValueOrError<T>> source) {
    return source.flatMap(ValueOrError::onSuccessOrError);
  }

  private static <T> Single<T> onSuccessOrError(ValueOrError<T> voe) {
    return Single.create(
        em -> {
          voe.value().ifPresent(value -> em.onSuccess(value));
          voe.error().ifPresent(error -> em.onError(error));
        });
  }
}
