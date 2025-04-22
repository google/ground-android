/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.map.gms.mog

import kotlin.math.abs
import kotlin.math.cos

/**
 * Returns the value shifted left [n] bits when [n] is positive, or right [-n] bits when negative.
 */
fun Int.shiftLeft(n: Int) = if (n >= 0) this shl n else this shr abs(n)

/** Returns the secant of angle `x` given in radians. */
fun sec(x: Double) = 1 / cos(x)

/** Converts degrees into radians. */
fun Double.toRadians() = this * (Math.PI / 180)
