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
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import javax.annotation.Nullable;

public class Result<T> {
  public enum State {
    SUCCESS,
    ERROR
  }

  private final State state;

  @Nullable private final T value;
  @Nullable private final Throwable error;

  private Result(State state, @Nullable T value, @Nullable Throwable error) {
    this.state = state;
    this.value = value;
    this.error = error;
  }

  public static <T> Result<T> success(@NonNull T result) {
    return new Result<>(State.SUCCESS, result, null);
  }

  public static <T> Result<T> error(@NonNull Throwable t) {
    return new Result<>(State.ERROR, null, t);
  }

  @Nullable
  public State getState() {
    return state;
  }

  @Nullable
  public T get() {
    return value;
  }

  @Nullable
  public Throwable getError() {
    return error;
  }

  /**
   * Transforms a Single, returned by applying fn to an argument of type T, into a Single that emits
   * Results, returning Result::error on error.
   *
   * <p>This function is useful for handling nested RX streams, for example, in situations in which
   * a function passed to the RX {@code map} operator returns a Single. {@code mapSingle} ensures we
   * handle errors produced by such 'interior' Singles without breaking the 'exterior' stream.
   *
   * <pre>
   *     public Single<Project> getProject(String projectId) {
   *        return dataRepository.getProject(projectId);
   *     }
   *
   *     somePublishSubject.onNext("foobar");
   *
   *     somePublishSubject.map(Result.mapSingle(getSingleProject))
   *                       .subscribe(Result.unwrap(onProjectSuccess, onProjectError))
   * </pre>
   *
   * @param fn A function that takes an argument of type T and returns a Single with emissions of
   *     type R.
   * @param <T> The initial type of values passed to fn.
   * @param <R> The type of emissions produced by the Single returned by fn.
   * @return
   */
  public static <T, R> Function<T, Single<Result<R>>> mapSingle(
      @NonNull Function<T, Single<R>> fn) {
    return (T value) -> fn.apply(value).map(Result::success).onErrorReturn(Result::error);
  }

  /**
   * Wraps single emissions in a Result without applying any other transformations. Use this
   * function to wrap and handle potential errors at a given point in a chain of RX transformations.
   *
   * @param single
   * @param <T> The type of the Single's emissions.
   * @return A Single that emits Results.
   */
  public static <T> Single<Result<T>> wrapSingle(Single<T> single) {
    return single.map(Result::success).onErrorReturn(Result::error);
  }

  /**
   * Transforms an Observable, returned by applying fn to an argument of type T, into an Observable
   * that emits Results, returning Result::error on error.
   *
   *<p>This function is useful for handling nested RX streams, for example, in situations in which
   * a function passed to the RX {@code map} operator returns a Observable. {@code mapObservable} ensures we
   * handle errors produced by such 'interior' Observables without breaking the 'exterior' stream.
   *
   * <pre>
   *     public Observable<Project> getProject(String projectId) {
   *        return dataRepository.getProject(projectId);
   *     }
   *
   *     somePublishSubject.onNext("foobar");
   *
   *     somePublishSubject.map(Result.mapObservable(getProject))
   *                       .subscribe(Result.unwrap(onProjectSuccess, onProjectError))
   * </pre>
   *
   *
   * @param fn A function that takes an argument of type T and returns an Observable with emissions
   *     of type R.
   * @param <T> The initial type of values passed to fn.
   * @param <R> The type of emissions produced by the Observable returned by fn.
   * @return
   */
  public static <T, R> Function<T, Observable<Result<R>>> mapObservable(
      @NonNull Function<T, Observable<R>> fn) {
    return (T value) -> fn.apply(value).map(Result::success).onErrorReturn(Result::error);
  }

  /**
   * Wraps observable emissions in a Result without applying any other transformations. Use this
   * function to wrap and handle potential errors at a given point in a chain of RX transformations.
   *
   * @param observable
   * @param <T> The type of the observable's emissions.
   * @return An Observable that emits Results.
   */
  public static <T> Observable<Result<T>> wrapObservable(Observable<T> observable) {
    return observable.map(Result::success).onErrorReturn(Result::error);
  }

  public static <T> Consumer<? super Result<T>> unwrap(
      Consumer<T> onSuccess, Consumer<Throwable> onError) {
    return result -> {
      switch (result.getState()) {
        case SUCCESS:
          onSuccess.accept(result.get());
          break;
        case ERROR:
          onError.accept(result.getError());
          break;
      }
    };
  }
}
