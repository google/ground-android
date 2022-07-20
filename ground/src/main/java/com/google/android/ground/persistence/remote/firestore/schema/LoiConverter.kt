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
import com.google.android.ground.model.locationofinterest.*
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.common.collect.ImmutableList
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import timber.log.Timber

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
        val loiDoc =
            DataStoreException.checkNotNull(doc.toObject(LoiDocument::class.java), "LOI data")

        if (loiDoc.geometry != null && hasNonEmptyVertices(loiDoc)) {
            return toLoiFromGeometry(survey, doc, loiDoc)
        }

        loiDoc.geoJson?.let {
            val builder = GeoJsonLocationOfInterest.newBuilder().setGeoJsonString(it)
            fillLocationOfInterest(builder, survey, doc.id, loiDoc)
            return builder.build()
        }

        loiDoc.location?.let {
            val builder = PointOfInterest.newBuilder().setPoint(toPoint(it))
            fillLocationOfInterest(builder, survey, doc.id, loiDoc)
            return builder.build()
        }

        throw DataStoreException("No geometry in remote LOI ${doc.id}")
    }

    private fun hasNonEmptyVertices(loiDocument: LoiDocument): Boolean {
        val geometry = loiDocument.geometry

        if (geometry == null
            || geometry[GEOMETRY_COORDINATES] == null
            || geometry[GEOMETRY_COORDINATES] !is List<*>
        ) {
            return false
        }

        val coordinates = geometry[GEOMETRY_COORDINATES] as List<*>?
        return coordinates?.isNotEmpty() ?: false
    }

    private fun toLoiFromGeometry(
        survey: Survey,
        doc: DocumentSnapshot,
        loiDoc: LoiDocument
    ): AreaOfInterest {
        val geometry = loiDoc.geometry
        val type = geometry!![GEOMETRY_TYPE]
        if (POLYGON_TYPE != type) {
            throw DataStoreException("Unknown geometry type in LOI ${doc.id}: $type")
        }

        val coordinates = geometry[GEOMETRY_COORDINATES]
        if (coordinates !is List<*>) {
            throw DataStoreException("Invalid coordinates in LOI ${doc.id}: $coordinates")
        }

        val vertices = ImmutableList.builder<Point>()
        for (point in coordinates) {
            if (point !is GeoPoint) {
                Timber.d("Ignoring illegal point type in LOI ${doc.id}")
                break
            }
            vertices.add(
                Point.newBuilder().setLongitude(point.longitude).setLatitude(
                    point.latitude
                ).build()
            )
        }

        val builder = AreaOfInterest.newBuilder().setVertices(vertices.build())
        fillLocationOfInterest(builder, survey, doc.id, loiDoc)
        return builder.build()
    }

    private fun fillLocationOfInterest(
        builder: LocationOfInterest.Builder,
        survey: Survey,
        id: String,
        loiDoc: LoiDocument
    ) {
        val jobId = DataStoreException.checkNotNull(loiDoc.jobId, JOB_ID)
        val job =
            DataStoreException.checkNotEmpty(
                survey.getJob(jobId),
                "job ${loiDoc.jobId}"
            )
        // Degrade gracefully when audit info missing in remote db.
        val created = loiDoc.created ?: AuditInfoNestedObject.FALLBACK_VALUE
        val lastModified = loiDoc.lastModified ?: created
        builder
            .setId(id)
            .setSurvey(survey)
            .setCustomId(loiDoc.customId)
            .setCaption(loiDoc.caption)
            .setJob(job)
            .setCreated(AuditInfoConverter.toAuditInfo(created))
            .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
    }

    private fun toPoint(geoPoint: GeoPoint): Point =
        Point.newBuilder()
            .setLatitude(geoPoint.latitude)
            .setLongitude(geoPoint.longitude)
            .build()
}
