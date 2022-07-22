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

package com.google.android.ground.persistence.remote.firestore

import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.geojson.GeoJsonWriter

/**
 * Converts between Geometry model objects and their equivalent representation in Firestore.
 *
 * Specifically, geometries represented in Firestore as follows:
 *
 * * Geometries are persisted using a modified GeoJSON representation.
 * * The GeoJSON map hierarchy is converted to a Firestore nested map.
 * * Since Firestore does not allow nested arrays, arrays are replaced with nested maps keyed by
 *   the array index.
 * * All coordinates (two-element double arrays) are represented as GeoPoint in Firestore.
 *
 * `Point` and `MultiPolygon` are the only supported `Geometry` types. Behavior for other types is
 * undefined.
 */
class GeometryConverter {
    // Reify fromJson() to create type token from generics.
    private inline fun <reified T> Gson.fromJson(json: String) =
        fromJson<T>(json, object : TypeToken<T>() {}.type)

    /**
     * Convert a `Geometry` to a `Map` which may be used to persist
     * the provided geometry in Firestore.
     */
    fun toFirestoreMap(geometry: Geometry): Map<String, Any> {
        val writer = GeoJsonWriter()
        writer.setEncodeCRS(false)
        val jsonString = writer.write(geometry)
        return toFirestoreValue(Gson().fromJson<MutableMap<String, Any>>(jsonString))
    }

    private fun toFirestoreValue(value: Map<String, Any>): Map<String, Any> {
        return value.mapValues {
            toFirestoreValue(it.value)
        }
    }

    private fun toFirestoreValue(value: Any): Any {
        return when (value) {
            is ArrayList<*> -> {
                if (value.size == 2 && value.all { it is Double }) {
                    GeoPoint(value[0] as Double, value[1] as Double)
                } else {
                    listToMap(value)
                }
            }
            is Map<*, *> -> {
                toFirestoreValue(value)
            }
            else -> {
                value
            }
        }
    }

    private fun listToMap(list: List<Any>): Map<Int, Any> {
        return list.mapIndexedNotNull { index, value -> index to toFirestoreValue(value) }
            .toMap()
    }
}