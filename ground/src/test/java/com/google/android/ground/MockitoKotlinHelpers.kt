package com.google.android.ground

import org.mockito.ArgumentCaptor
import org.mockito.kotlin.eq

/**
 * Returns ArgumentCaptor.capture() as nullable type to avoid java.lang.IllegalStateException
 * when null is returned.
 */
fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

fun <T : Any> safeEq(value: T): T = eq(value)