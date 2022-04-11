package com.google.android.gnd.util

import com.google.common.collect.ImmutableList

@JvmSynthetic
fun <E : Any> Array<out E>.toImmutableList() = ImmutableList.copyOf(this)

@JvmSynthetic
fun <E : Any?> List<E>.toImmutableList() = ImmutableList.copyOf(this)
