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

import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Target;

/**
 * Denotes an observable that emits deltas between the application's presumed internal snapshot of a
 * set of entities and the entities' actual state. On subscription, Observers first receive the
 * initial state of entities, followed by events indicating changes to that base state. Such
 * observables are typically used to represent the output of "load once and stream changes"
 * operations.
 */
@Cold
@Infinite
@Target({TYPE_USE})
public @interface Deltas {}
