/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.GeoJsonLocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.Point
import com.google.android.ground.model.locationofinterest.PointOfInterest
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.DataStoreException.checkNotNull
import com.google.android.ground.persistence.remote.firestore.GeometryConverter
import com.google.firebase.firestore.DocumentSnapshot
import org.locationtech.jts.io.geojson.GeoJsonWriter

// TODO: Add tests.
/** Converts between Firestore documents and [LocationOfInterest] instances.  */
object LoiConverter {
    const val JOB_ID = "jobId"
    const val LOCATION = "location"
    const val CREATED = "created"
    const val LAST_MODIFIED = "lastModified"
    const val GEOMETRY_TYPE = "type"
    const val POLYGON_TYPE = "Polygon"
    const val GEOMETRY_COORDINATES = "coordinates"
    const val GEOMETRY = "geometry"

    @JvmStatic
    @Throws(DataStoreException::class)
    fun toLoi(survey: Survey, doc: DocumentSnapshot): LocationOfInterest {
        val loiId = doc.id
        val loiDoc = checkNotNull(doc.toObject(LoiDocument::class.java), "LOI data")
        val geometryMap = checkNotNull(loiDoc.geometry, "geometry")
        // TODO: Return `Result` instead of throwing exception.
        val geometry = GeometryConverter.fromFirestoreMap(geometryMap).getOrThrow()

        // As an interim solution, we map geometries to existing LOI types.
        // TODO: Get rid of LOI subclasses and just use Geometry on LOI class.
        when (geometry.geometryType) {
            "Point" -> {
                val builder = PointOfInterest.newBuilder()
                builder.setPoint(
                    Point.newBuilder().setLatitude(geometry.coordinate.x)
                        .setLongitude(geometry.coordinate.y).build()
                )
                fillLocationOfInterest(builder, survey, loiId, loiDoc)
                return builder.build()
            }
            "Polygon", "MultiPolygon" -> {
                val builder = GeoJsonLocationOfInterest.newBuilder()
                builder.setGeoJsonString(GeoJsonWriter().write(geometry))
                fillLocationOfInterest(builder, survey, loiId, loiDoc)
                return builder.build()
            }
            else -> {
                throw DataStoreException("Unsupported geometry $geometry")
            }
        }
    }

    private fun fillLocationOfInterest(
        builder: LocationOfInterest.Builder,
        survey: Survey,
        loiId: String,
        loiDoc: LoiDocument
    ) {
        val jobId = checkNotNull(loiDoc.jobId, JOB_ID)
        val job =
            DataStoreException.checkNotEmpty(
                survey.getJob(jobId),
                "job ${loiDoc.jobId}"
            )
        // Degrade gracefully when audit info missing in remote db.
        val created = loiDoc.created ?: AuditInfoNestedObject.FALLBACK_VALUE
        val lastModified = loiDoc.lastModified ?: created
        builder
            .setId(loiId)
            .setSurvey(survey)
            .setCustomId(loiDoc.customId)
            .setCaption(loiDoc.caption)
            .setJob(job)
            .setCreated(AuditInfoConverter.toAuditInfo(created))
            .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
    }
}
