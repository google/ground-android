/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.rx.annotations;

/**
 * Non-default observable behaviors that influence observer behaviors and assumptions. Unless
 * otherwise specified in the {@link Hot} or {@link Cold} annotations, observables may emit errors
 * and replay items, and they are assumed to be finite and stateless.
 */
public enum ObservableProperty {
  /**
   * Indicates an observable never emits a terminal event; the observable is considered "infinite".
   */
  INFINITE,

  /**
   * Indicates values that the producer maintains state, so observers must also hold state about
   * previous items for the sequence to be useful. Idempotent observers are not possible.
   */
  STATEFUL,

  /**
   * Indicates the observable handles errors internally or that error states are never emitted.
   * Observers of observables with this property do not need to handle error states.
   */
  ERROR_FREE,

  /**
   * Indicates an observable records one or more items and replays them on subscription. This
   * property is independent of "temperature" (hot vs cold) since it describes how items are emitted
   * by the observable itself rather than how they are produces. In other words, this property does
   * not imply work is done on each subscription, so observables with this property may be either
   * hot or cold. For example, subscribing to {@link io.reactivex.subjects.ReplaySubject} will cause
   * previously emitted items to be replayed, but it does not cause the operation that produced them
   * to be executed again.
   */
  REPLAY,
}
