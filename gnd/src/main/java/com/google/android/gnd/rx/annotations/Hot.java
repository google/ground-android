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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * Denotes a hot observable. In contrast to {@link Cold} observables, subscribing to a hot
 * observable has no side effects. As such, the producer pushing items to the stream must be created
 * outside the subscription callback. As a result, the following conclusions also hold:
 *
 * <ul>
 *   <li>Hot observables may start emitting items as soon as they're created. Observers only receive
 *       items emitted after subscribing, so items emitted before subscription might be missed.
 *   <li>Assuming the producer supports multiple observers, hot observables support
 *       <i>multicasting</i>; they allow the same sequence to be shared with multiple observers.
 * </ul>
 *
 * <p>Note: Observables that replay values to late subscribers are considered hot because
 * subscribing to a these observables does not create a new producer. For example, subscribing to
 * {@link io.reactivex.subjects.ReplaySubject} will not cause upstream producers to be recreated;
 * upstream operations will not be reexecuted. Similarly, {@link
 * io.reactivex.subjects.BehaviorSubject} are not derived from other observables, and their
 * producers are independent of their existence.
 *
 * <p>Some operations on hot observables will result in cold ones. <code>switchMap</code> or <code>
 * flatMap</code> with a cold observable, for example, will result in a new cold observable, since
 * the act of subscribing causes to source stream to start emitting values to an observer which
 * creates new producers.
 *
 * <p>See ReactiveX's <a href="https://github.com/ReactiveX/RxJava/blob/2.x/DESIGN.md">RxJava v2
 * Design</a> for more definitions.
 */
@Documented
@Target({ANNOTATION_TYPE, TYPE_USE})
public @interface Hot {}
