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
import org.locationtech.jts.io.geojson.GeoJsonReader
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
        val jsonMap = Gson().fromJson<MutableMap<String, Any>>(jsonString)
        return toFirestoreValue(jsonMap)
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
                    indexedMap(value)
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

    private fun indexedMap(list: List<Any>): Map<Int, Any> =
        list.mapIndexed { index, value -> index to toFirestoreValue(value) }.toMap()

    /**
     * Converts a `Map` deserialized from Firestore into a `Geometry` instance.
     */
    fun fromFirestoreMap(map: Map<String, Any>): Geometry? {
        val jsonMap = fromFirestoreValue(map)
        val jsonString = Gson().toJson(jsonMap)
        val reader = GeoJsonReader()
        return reader.read(jsonString)
    }

    private fun fromFirestoreValue(value: Any): Any {
        return when (value) {
            is Map<*, *> -> {
                fromFirestoreValue(value)
            }
            is GeoPoint -> {
                arrayOf(value.latitude, value.longitude)
            }
            else -> {
                value
            }
        }
    }

    private fun fromFirestoreValue(map: Map<*, *>): Any {
        // If all keys are non-null Ints, assume it refers to an indexed map.
        // If heuristic breaks, we may also want to check keys are in order starting at 0.
        return if (map.entries.all { it.key is Int && it.value != null }) {
            indexedMapToList(map as Map<Int, Any>).map(::fromFirestoreValue)
        } else {
            map.mapValues { it.value?.let(::fromFirestoreValue) }
        }
    }

    /**
     * Converts map representation used to store nested arrays in Firestore into a List. Assumes
     * keys are consecutive ints starting from 0.
     */
    private fun indexedMapToList(map: Map<Int, Any>): List<Any> {
        return map.entries.sortedBy { it.key }.map { it.value }
    }
}