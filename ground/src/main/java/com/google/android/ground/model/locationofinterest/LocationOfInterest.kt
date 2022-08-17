/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.model.locationofinterest

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.LocationOfInterestMutation.Companion.builder
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Polygon
import java.util.*
import javax.annotation.OverridingMethodsMustInvokeSuper

/** User-defined locations of interest (LOI) shown on the map. */
data class LocationOfInterest(
    /** A system-defined ID for this LOI. */
    val id: String,
    /** The survey associated with this LOI. */
    val survey: Survey,
    /** The job associated with this LOI. */
    val job: Job,
    /** A user-specified ID for this location of interest. */
    val customId: String? = null,
    /** A human readable caption for this location of interest. */
    val caption: String? = null,
    /** User and time audit info pertaining to the creation of this LOI. */
    val created: AuditInfo,
    /** User and time audit info pertaining to the last modification of this LOI. */
    val lastModified: AuditInfo,
    /** Geometry associated with this LOI. */
    val geometry: Geometry,
) {
    /** Returns the type of this LOI based on its Geometry. */
    val type: LocationOfInterestType =
        when (geometry) {
            is org.locationtech.jts.geom.Point -> LocationOfInterestType.POINT
            is Polygon -> LocationOfInterestType.POLYGON
            else -> LocationOfInterestType.UNKNOWN
        }

    /**
     * Converts this LOI to a mutation that can be used to update this LOI in the remote and local database.
     */
    @OverridingMethodsMustInvokeSuper
    open fun toMutation(type: Mutation.Type, userId: String): LocationOfInterestMutation {
        return builder()
            .setJobId(job.id)
            .setType(type)
            .setSyncStatus(SyncStatus.PENDING)
            .setSurveyId(survey.id)
            .setLocationOfInterestId(id)
            .setUserId(userId)
            .setClientTimestamp(Date())
            .build()
    }

    val coordinatesAsPoints: ImmutableList<Point> =
        geometry.coordinates.map(Point::fromCoordinate).toImmutableList()

    val coordinatesAsPoint: Point =
        Point.fromCoordinate(geometry.coordinate)
}