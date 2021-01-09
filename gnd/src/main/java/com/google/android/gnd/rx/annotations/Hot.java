/*
 * Copyright 2020 Google LLC
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

import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * Denotes a hot observable. In contrast to {@link Cold} observables, subscribing to a hot
 * observable has no side effects. As such, the following assumptions hold:
 *
 * <ul>
 *   <li>The operation producing items in the sequence (the producer) must be created and initiated
 *       outside the subscription callback.
 *   <li>Hot observables can start emitting items as soon as they're created. Observers only receive
 *       items emitted after subscribing, so items emitted before subscription may be missed.
 *   <li>Assuming the producer supports multiple observers, hot observables support
 *       <i>multicasting</i>; they allow the same sequence to be shared with multiple observers.
 * </ul>
 *
 * <p><b>Note:</b> Observables that memoize and replay values to late subscribers are still
 * considered hot, since subscribing to such observables does not create a new producer. Subscribing
 * to {@link io.reactivex.subjects.ReplaySubject}, for example, will replay previously emitted
 * items, but it will not reexecute the operation that produced them.
 *
 * <p>See <a href="http://reactivex.io/documentation/observable.html">Observable</a> and <a
 * href="https://github.com/ReactiveX/RxJava/blob/2.x/DESIGN.md">RxJava v2 Design</a> for additional
 * definitions and background.
 */
@Documented
@Target({TYPE_USE})
public @interface Hot {
  /**
   * Indicates whether this observable is expected to terminate (<code>finite=true</code>), or
   * whether it is expected to run indefinitely (<code>finite=false</code>). Hot observables are
   * considered infinite by default.
   */
  boolean finite() default false;

  /**
   * Indicates whether the whole sequence must be received for its output to be useful (stateful),
   * or whether values are independent (stateless). Stateful observables require the producer and
   * observer to maintain state, while stateless observables allow observers to use idempotent
   * <code>onNext()</code> handlers.
   */
  boolean stateful() default false;

  /**
   * Indicates whether this observable records one or more items and replays them on subscription.
   */
  boolean memoized() default false;
}
