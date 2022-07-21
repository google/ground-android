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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.geojson.GeoJsonWriter

class GeoJsonConverter {
    // Reify fromJson() to create type token from generics.
    private inline fun <reified T> Gson.fromJson(json: String) =
        fromJson<T>(json, object : TypeToken<T>() {}.type)

    fun toFirestoreMap(geometry: Geometry): Map<String, *> {
        val writer = GeoJsonWriter()
        writer.setEncodeCRS(false)
        val jsonString = writer.write(geometry)
        return Gson().fromJson<MutableMap<String, *>>(jsonString).mapValues(::toFirestoreValue)
    }

    private fun toFirestoreValue(entry: Map.Entry<String, *>): Any {
        // TODO: Convert [x,y] into GeoPoint.
        // TODO: Convert other arrays to maps.
        // TODO: Apply recursively to submaps.
        return Object()
    }
}