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

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon

/** A grouping of one or more [Polygon]s. */
data class MultiPolygon(val polygons: Collection<Polygon>) : Geometry {
    private val factory: GeometryFactory = GeometryFactory()
    private val geometries = polygons.map { it.geometry }

    override val geometry: MultiPolygon = factory.createMultiPolygon(geometries.toTypedArray())
    override val type: GeometryType = GeometryType.MULTIPOLYGON
    override val isClosed: Boolean = true
}