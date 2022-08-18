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
package com.google.android.ground.model.geometry

import com.google.common.collect.ImmutableList

/** The class of geometry errors.
 *
 * Typically thrown when a construction does not satisfy definitional constraints for a given geometry.
 */
sealed class GeometryException(override val message: String) : Throwable(message)

sealed interface Geometry {
    /** Returns a primary [Coordinate] associated with this geometry. */
    val coordinate: Coordinate

    /** Returns a list of [Coordinate]s associated with this geometry. */
    val coordinates: ImmutableList<Coordinate>
}