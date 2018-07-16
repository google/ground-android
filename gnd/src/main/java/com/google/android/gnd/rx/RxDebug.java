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

import android.util.Log;
import com.akaita.java.rxjava2debug.RxJava2Debug;
import io.reactivex.ObservableTransformer;
import io.reactivex.SingleTransformer;

public abstract class RxDebug {
  private static final String TAG = RxDebug.class.getSimpleName();

  /** Container for static helper methods. Do not instantiate. */
  private RxDebug() {
  }

  public static void logEnhancedStackTrace(Throwable t) {
    Log.e(TAG, "Unhandled Rx error", RxJava2Debug.getEnhancedStackTrace(t));
  }

  public static <T> ObservableTransformer<T, T> logObservable(String name) {
    return single ->
      single
        .doOnSubscribe(__ -> logDebug(name, "Subscribe"))
        .doOnNext(__ -> logDebug(name, "Next"))
        .doOnDispose(() -> logDebug(name, "Disposed"))
        .doOnError(__ -> logDebug(name, "Error"));
  }

  public static <T> SingleTransformer<T, T> logSingle(String name) {
    return single ->
      single
        .doOnSubscribe(__ -> logDebug(name, "Subscribe"))
        .doOnSuccess(__ -> logDebug(name, "Success"))
        .doOnDispose(() -> logDebug(name, "Disposed"))
        .doOnError(__ -> logDebug(name, "Error"));
  }

  private static void logDebug(String name, String action) {
    Log.d(TAG, name + ": " + action);
  }
}
