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
package com.google.android.gnd.persistence.remote.firestore.schema

import kotlin.Throws
import com.google.android.gnd.persistence.remote.DataStoreException
import com.google.android.gnd.model.Survey
import com.google.android.gnd.model.locationofinterest.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import timber.log.Timber
import com.google.common.collect.ImmutableList

/** Converts between Firestore documents and [LocationOfInterest] instances.  */
object FeatureConverter {
    const val LAYER_ID = "layerId"
    const val LOCATION = "location"
    const val CREATED = "created"
    const val LAST_MODIFIED = "lastModified"
    const val GEOMETRY_TYPE = "type"
    const val POLYGON_TYPE = "Polygon"
    const val GEOMETRY_COORDINATES = "coordinates"
    const val GEOMETRY = "geometry"

    @JvmStatic
    @Throws(DataStoreException::class)
    fun toFeature(survey: Survey, doc: DocumentSnapshot): LocationOfInterest<*> {
        val featureDoc = DataStoreException.checkNotNull(
            doc.toObject(FeatureDocument::class.java),
            "feature data"
        )

        if (featureDoc.geometry != null && hasNonEmptyVertices(featureDoc)) {
            return toFeatureFromGeometry(survey, doc, featureDoc)
        }

        featureDoc.geoJson?.let {
            val builder = GeoJsonLocationOfInterest.newBuilder().setGeoJsonString(it)
            fillFeature(builder, survey, doc.id, featureDoc)
            return builder.build()
        }

        featureDoc.location?.let {
            val builder = PointOfInterest.newBuilder().setPoint(toPoint(it))
            fillFeature(builder, survey, doc.id, featureDoc)
            return builder.build()
        }

        throw DataStoreException("No geometry in remote feature ${doc.id}")
    }

    private fun hasNonEmptyVertices(featureDocument: FeatureDocument): Boolean {
        val geometry = featureDocument.geometry

        if (geometry == null
            || geometry[GEOMETRY_COORDINATES] == null
            || geometry[GEOMETRY_COORDINATES] !is List<*>
        ) {
            return false
        }

        val coordinates = geometry[GEOMETRY_COORDINATES] as List<*>?
        return coordinates?.isNotEmpty() ?: false
    }

    private fun toFeatureFromGeometry(
        survey: Survey, doc: DocumentSnapshot, featureDoc: FeatureDocument
    ): PolygonOfInterest {
        val geometry = featureDoc.geometry
        val type = geometry!![GEOMETRY_TYPE]
        if (POLYGON_TYPE != type) {
            throw DataStoreException("Unknown geometry type in feature ${doc.id}: $type")
        }

        val coordinates = geometry[GEOMETRY_COORDINATES]
        if (coordinates !is List<*>) {
            throw DataStoreException("Invalid coordinates in feature ${doc.id}: $coordinates")
        }

        val vertices = ImmutableList.builder<Point>()
        for (point in coordinates) {
            if (point !is GeoPoint) {
                Timber.d("Ignoring illegal point type in feature ${doc.id}")
                break
            }
            vertices.add(
                Point.newBuilder().setLongitude(point.longitude).setLatitude(
                    point.latitude
                ).build()
            )
        }

        val builder = PolygonOfInterest.builder().setVertices(vertices.build())
        fillFeature(builder, survey, doc.id, featureDoc)
        return builder.build()
    }

    private fun fillFeature(
        builder: LocationOfInterest.Builder<*>, survey: Survey, id: String, featureDoc: FeatureDocument
    ) {
        val layerId = DataStoreException.checkNotNull(featureDoc.jobId, LAYER_ID)
        val layer =
            DataStoreException.checkNotEmpty(
                survey.getJob(layerId),
                "layer ${featureDoc.jobId}"
            )
        // Degrade gracefully when audit info missing in remote db.
        val created = featureDoc.created ?: AuditInfoNestedObject.FALLBACK_VALUE
        val lastModified = featureDoc.lastModified ?: created
        builder
            .setId(id)
            .setSurvey(survey)
            .setCustomId(featureDoc.customId)
            .setCaption(featureDoc.caption)
            .setJob(layer)
            .setCreated(AuditInfoConverter.toAuditInfo(created))
            .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
    }

    private fun toPoint(geoPoint: GeoPoint): Point =
        Point.newBuilder()
            .setLatitude(geoPoint.latitude)
            .setLongitude(geoPoint.longitude)
            .build()
}
