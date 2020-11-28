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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.concurrent.Callable;
import java8.util.function.Consumer;
import java8.util.function.Supplier;
import timber.log.Timber;

/** Helpers for working with RxJava Completable classes. */
public abstract class RxCompletable {
  /** Do not instantiate. */
  private RxCompletable() {}

  public static Completable completeIf(Callable<Boolean> conditionFunction) {
    return Completable.create(
        em -> {
          if (conditionFunction.call()) {
            em.onComplete();
          }
        });
  }

  public static Completable completeOrError(
      Supplier<Boolean> supplier, Class<? extends Throwable> errorClass) {
    return Completable.create(
        em -> {
          if (supplier.get()) {
            em.onComplete();
          } else {
            em.onError(errorClass.newInstance());
          }
        });
  }

  /**
   * Receives a {@link Completable} and returns a {@link Single} that emits <code>true</code> if the
   * <code>Completable</code> completes successfully, <code>false</code> otherwise. In case any
   * error occurs, the exception is logged to {@link Timber}.
   */
  public static Single<Boolean> toBooleanSingle(@NonNull Completable completable) {
    return toBooleanSingle(completable, null);
  }

  /**
   * Receives a {@link Completable} and returns a {@link Single} that emits <code>true</code> if the
   * <code>Completable</code> completes successfully, <code>false</code> otherwise. In case any
   * error occurs, the exception is consumed by the argument {@param exceptionConsumer}.
   */
  public static Single<Boolean> toBooleanSingle(
      @NonNull Completable completable, @Nullable Consumer<? super Throwable> exceptionConsumer) {
    return completable
        .doOnError(exceptionConsumer::accept)
        .toSingleDefault(true)
        .onErrorReturnItem(false);
  }
}
