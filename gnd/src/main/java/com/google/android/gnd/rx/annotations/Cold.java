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
 * Denotes a cold observable. Unlike {@link Hot} observables, cold observables may have side effects
 * when subscribed to. This interpretation has several consequences:
 *
 * <ul>
 *   <li>Cold observables are <i>lazy</i>: the operation responsible for producing the sequence of
 *       items is created and executed each time an observer subscribes. In other words, a new
 *       producer is created on each subscription.
 *   <li>Since a new producer is created and executed on each subscription, the sequence emitted may
 *       for any two subscriptions may differ; observers are not guaranteed to receive the same
 *       sequence of items. As such, cold observables are not suitable for sharing the same sequence
 *       with multiple observers (<i>multicasting</i>).
 *   <li>Since items are only emitted when observers subscribe, no items can be missed as result of
 *       an observer subscribing "too late".
 * </ul>
 *
 * <p>Unless otherwise specified, an observable annotated with {@link Cold} may emit {@link
 * #errors()}, never {@link #replays()} items, is not {@link #stateful()}, and eventually {@link
 * #terminates()}.
 *
 * <p>See <a href="http://reactivex.io/documentation/observable.html">Observable</a> and <a
 * href="https://github.com/ReactiveX/RxJava/blob/2.x/DESIGN.md">RxJava v2 Design</a> for additional
 * definitions and background.
 */
@Documented
@Target({TYPE_USE})
public @interface Cold {
  /**
   * When true, indicates this observable may emit an error. When false, error states are handled
   * upstream, so downstream observers don't not need to.
   */
  boolean errors() default true;

  /**
   * When true, indicates this observable records one or more items and replays them on
   * subscription. This property is orthogonal to temperature ({@link Hot} vs {@link Cold}), since
   * sequences emitted by an observer are considered the output or result of the observer, not a
   * side effect.
   */
  boolean replays() default false;

  /**
   * When true, indicates items in this sequence are not independent, so the producer and observer
   * must maintain state between emissions. When false, the observable is stateless, allowing
   * idempotent observers to be applied.
   */
  boolean stateful() default false;

  /**
   * When true, indicates this observable is finite; it is expected to terminate. When false, the
   * observable is considered infinite.
   */
  boolean terminates() default true;
}
