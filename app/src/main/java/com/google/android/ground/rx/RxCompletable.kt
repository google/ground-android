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
package com.google.android.ground.rx

import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.Callable
import java8.util.function.Consumer
import java8.util.function.Supplier

/** Helpers for working with RxJava Completable classes. */
object RxCompletable {

  fun completeIf(conditionFunction: Callable<Boolean>): Completable =
    Completable.create {
      if (conditionFunction.call()) {
        it.onComplete()
      }
    }

  fun completeOrError(supplier: Supplier<Boolean>, errorClass: Class<out Throwable>): Completable =
    Completable.create {
      if (supplier.get()) {
        it.onComplete()
      } else {
        it.onError(errorClass.newInstance())
      }
    }

  /**
   * Receives a [Completable] and returns a [Single] that emits `true` if the [Completable]
   * completes successfully, `false` otherwise. In case any error occurs, the exception is consumed
   * by the argument {@param exceptionConsumer}.
   */
  fun toBooleanSingle(
    completable: Completable,
    exceptionConsumer: Consumer<in Throwable>
  ): Single<Boolean> =
    completable
      .doOnError { exceptionConsumer.accept(it) }
      .toSingleDefault(true)
      .onErrorReturnItem(false)
}
