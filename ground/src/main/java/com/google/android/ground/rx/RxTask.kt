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

import com.google.android.gms.tasks.Task
import io.reactivex.*
import java8.util.function.Supplier

/** Static helper methods for converting Google Play Services Tasks into Rx Observables. */
object RxTask {
  /**
   * Turns a non-void [Task] into an Rx [Single]. Null values are reported as an error result of
   * [NullPointerException]. The provided supplier will be invoked only onSubscribe.
   */
  @JvmStatic
  fun <T> toMaybe(task: Supplier<Task<T>>): Maybe<T> =
    Maybe.create { emitter: MaybeEmitter<T> ->
      task
        .get()
        .addOnSuccessListener { result: T -> onSuccess(result, emitter) }
        .addOnFailureListener { t: Exception -> emitter.onError(t) }
    }

  private fun <T> onSuccess(v: T?, emitter: MaybeEmitter<T>) {
    if (v == null) {
      emitter.onComplete()
    } else {
      emitter.onSuccess(v)
    }
  }

  /**
   * Turns a non-void [Task] into an Rx [Single]. Null values are reported as an error result of
   * [NullPointerException]. The provided supplier will be invoked only onSubscribe.
   */
  @JvmStatic
  fun <T> toSingle(task: Supplier<Task<T>>): Single<T> =
    Single.create { emitter: SingleEmitter<T> ->
      task
        .get()
        .addOnSuccessListener { result: T -> onNullableSuccess(result, emitter) }
        .addOnFailureListener { t: Exception -> emitter.onError(t) }
    }

  /**
   * Turns any [Task] into an Rx [Completable]. Since the success value is ignored, this can also be
   * used to convert `Task<Void>`. The provided supplier will be invoked only `onSubscribe`.
   */
  @JvmStatic
  fun toCompletable(task: Supplier<Task<*>>): Completable =
    Completable.create { emitter: CompletableEmitter ->
      task
        .get()
        .addOnSuccessListener { emitter.onComplete() }
        .addOnFailureListener { t: Exception -> emitter.onError(t) }
    }

  private fun <T> onNullableSuccess(v: T?, emitter: SingleEmitter<T>) {
    if (v == null) {
      emitter.onError(NullPointerException())
    } else {
      emitter.onSuccess(v)
    }
  }
}
