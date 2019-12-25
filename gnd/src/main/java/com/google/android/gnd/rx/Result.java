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
import io.reactivex.SingleTransformer;
import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents the result of an operation that either succeeds with a value, or fails with an
 * exception.
 *
 * @param <T> the type of value held by the {@link Result}.
 */
public class Result<T> {
  @Nullable private T value;
  @Nullable private Throwable error;

  protected Result(@Nullable T value, @Nullable Throwable error) {
    this.value = value;
    this.error = error;
  }

  public Optional<T> value() {
    return Optional.ofNullable(value);
  }

  public Optional<Throwable> error() {
    return Optional.ofNullable(error);
  }

  public static <T> Result<T> of(T value) {
    return new Result(value, null);
  }

  public static <T> Result<T> error(Throwable t) {
    return new Result(null, t);
  }

  @Override
  public String toString() {
    return error().map(t -> "Error: " + t).orElse("Value: " + value);
  }

  /**
   * Returns a {@link Single} that emits a Result representing either the value emitted by the
   * source on success, or the error which caused the operation to fail. The returned stream itself
   * should never fail with an error.
   *
   * @param source the stream to be modified.
   * @param <T> the type of value emitted on success.
   */
  public static <T> Single<Result<T>> wrap(Single<T> source) {
    return source.map(Result::of).onErrorReturn(Result::error);
  }

  /**
   * Returns a {@link Single} that emits a success value, or fails with an error depending on the
   * contents of the Result emitted by the source stream.
   *
   * @param source the stream to be modified.
   * @param <T> the type of value emitted on success.
   */
  public static <T> Single<T> unwrap(Single<Result<T>> source) {
    return source.flatMap(Result::onSuccessOrError);
  }

  private static <T> Single<T> onSuccessOrError(Result<T> result) {
    return Single.create(
        em -> {
          result.value().ifPresent(value -> em.onSuccess(value));
          result.error().ifPresent(error -> em.onError(error));
        });
  }

  /**
   * Returns a composable transformer that replaces the source {@link Single} with the specified
   * one when the source fails with and error.
   *
   * @param onError the Single to fall back to on error.
   * @param <T> the type of value emitted on success.
   */
  public static <T> SingleTransformer<Result<T>, Result<T>> onErrorResumeNext(
      Single<Result<T>> onError) {
    return source ->
        source.flatMap(result -> result.error().map(r -> onError).orElse(Single.just(result)));
  }
}
