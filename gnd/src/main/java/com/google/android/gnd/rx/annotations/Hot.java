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

import static com.google.android.gnd.rx.annotations.ObservableProperty.INFINITE;
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
 * <p>See <a href="http://reactivex.io/documentation/observable.html">Observable</a> and <a
 * href="https://github.com/ReactiveX/RxJava/blob/2.x/DESIGN.md">RxJava v2 Design</a> for additional
 * definitions and background.
 */
@Documented
@Target({TYPE_USE})
public @interface Hot {

  /** List of non-default observable behaviors that influence observer behaviors and assumptions. */
  ObservableProperty[] value() default {INFINITE};
}
