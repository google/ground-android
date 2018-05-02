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

import android.support.annotation.Nullable;

import com.google.android.gms.tasks.Task;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

/** Static helper methods for converting Google Play Services Tasks into Rx Observables. */
public abstract class RxTask {
  /** Container for static helper methods. Do not instantiate. */
  private RxTask() {}

  /**
   * Turns a non-void {@link Task} into an Rx {@link Single}. Null values are reported as an error
   * result of {@link NullPointerException}.
   */
  public static <T> Single<T> toSingle(Task<T> task) {
    return Single.create(
        emitter ->
          task.addOnSuccessListener(v -> onNullable(v, emitter))
                .addOnFailureListener(emitter::onError));
  }

  /**
   * Turns any {@link Task} into an Rx {@link Completable}. Since the success value is ignored, this
   * can also be used to convert <code>Task&lt;Void&gt;</code>.
   */
  public static Completable toCompletable(Task<?> task) {
    return Completable.create(
        emitter ->
          task.addOnSuccessListener(__ -> emitter.onComplete())
                .addOnFailureListener(emitter::onError));
  }

  private static <T> void onNullable(@Nullable T v, SingleEmitter<T> emitter) {
    if (v == null) {
      emitter.onError(new NullPointerException());
    } else {
      emitter.onSuccess(v);
    }
  }
}
