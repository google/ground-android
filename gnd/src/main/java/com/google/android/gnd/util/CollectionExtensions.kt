package com.google.android.gnd.util

import com.google.common.collect.ImmutableList

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