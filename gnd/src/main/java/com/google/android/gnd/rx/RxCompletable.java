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

import java.util.concurrent.Callable;

import io.reactivex.Completable;

/** Helpers for working with RxJava Completable classes. */
public abstract class RxCompletable {
  /** Do not instantiate */
  private RxCompletable() {}

  public static Completable completeIf(Callable<Boolean> conditionFunction) {
    return Completable.create(
        emitter -> {
          if (conditionFunction.call()) {
            emitter.onComplete();
          }
        });
  }
}
