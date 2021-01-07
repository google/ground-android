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
 * Denotes an observable that emits the latest state of an entity. The observer emits the latest
 * state on subscribe, then continues emitting the latest state each time it changes.
 *
 * <p>Operations on items emitted by this observable should be idempotent; the same item being
 * emitted multiple times should have the same result. Observers shouldn't depend on values being
 * emitted in any particular order.
 *
 * <p>Recommended base types:
 *
 * <ul>
 *   <li>{@link io.reactivex.Flowable} with {@link io.reactivex.BackpressureStrategy#LATEST}
 *   <li>{@link androidx.lifecycle.LiveData} (for view state only)
 * </ul>
 */
@Documented
@Hot
@Infinite
@Target({TYPE_USE})
public @interface States {}
