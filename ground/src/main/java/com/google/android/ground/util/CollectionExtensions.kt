/*
 * Copyright 2022 Google LLC
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

package com.google.android.ground.util

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet

/**
 * Converts a Kotlin array to a new [ImmutableList]. Needed for interoperability with existing java
 * code. Remove once all dependencies use Kotlin list which is already immutable.
 */
@JvmSynthetic
fun <E : Any> Array<out E>.toImmutableList(): ImmutableList<E> = ImmutableList.copyOf(this)

/**
 * Converts a Kotlin list to a new [ImmutableList]. Needed for interoperability with existing java
 * code. Remove once all dependencies use Kotlin list which is already immutable.
 */
@JvmSynthetic
fun <E : Any?> List<E>.toImmutableList(): ImmutableList<E> = ImmutableList.copyOf(this)

/** Converts a Kotlin set to a new [ImmutableSet]. */
@JvmSynthetic fun <E : Any?> List<E>.toImmutableSet(): ImmutableSet<E> = ImmutableSet.copyOf(this)
