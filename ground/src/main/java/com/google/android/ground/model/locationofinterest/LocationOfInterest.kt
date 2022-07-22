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
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.GeometryType
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.LocationOfInterestMutation.Companion.builder
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import java.util.*
import javax.annotation.OverridingMethodsMustInvokeSuper

/** Base class for user-defined locations of interest (LOI) shown on the map. */
data class LocationOfInterest<T : Geometry>(
    /** A system-defined ID for this LOI. */
    val id: String = "",
    /** The survey associated with this LOI. */
    val survey: Survey,
    /** The job associated with this LOI. */
    val job: Job,
    /** A user-specified ID for this LOI. */
    val customId: String? = null,
    /** A human readable caption for this LOI. */
    val caption: String? = null,
    /** User and time audit info pertaining to the last modification of this LOI. */
    val lastModified: AuditInfo,
    /** User and time audit info pertaining to the creation of this LOI. */
    val created: AuditInfo,
    /** Geometry associated with this LOI. */
    val geometry: T,
) {
    val isPoint: Boolean = geometry.type == GeometryType.POINT
    val isPolygon: Boolean = geometry.type == GeometryType.POLYGON
    val isMultipolygon: Boolean = geometry.type == GeometryType.MULTIPOLYGON

    /**
     * Converts this LOI to a mutation that can be used to update this LOI in the remote and local database.
     */
    @OverridingMethodsMustInvokeSuper
    fun toMutation(type: Mutation.Type, userId: String): LocationOfInterestMutation {
        return builder()
            .setType(type)
            .setSyncStatus(SyncStatus.PENDING)
            .setSurveyId(survey.id)
            .setLocationOfInterestId(id)
            .setJobId(job.id)
            .setUserId(userId)
            .setClientTimestamp(Date())
            .build()
    }

    // TODO: Remove once all callers are converted to Kotlin. We only retain this for Java interop.
    class Builder<T : Geometry> {
        var id: String = ""
            @JvmSynthetic set
        var survey: Survey? = null
            @JvmSynthetic set
        var job: Job? = null
            @JvmSynthetic set
        var customId: String? = null
            @JvmSynthetic set
        var caption: String? = null
            @JvmSynthetic set
        var created: AuditInfo? = null
            @JvmSynthetic set
        var lastModified: AuditInfo? = null
            @JvmSynthetic set
        var geometry: T? = null
            @JvmSynthetic set

        fun setId(value: String): Builder<T> = apply { this.id = value }
        fun setSurvey(value: Survey): Builder<T> = apply { this.survey = value }
        fun setJob(value: Job): Builder<T> = apply { this.job = value }
        fun setCustomId(value: String?): Builder<T> = apply { this.customId = value }
        fun setCaption(value: String?): Builder<T> = apply { this.caption = value }
        fun setCreated(value: AuditInfo): Builder<T> = apply { this.created = value }
        fun setLastModified(value: AuditInfo): Builder<T> = apply { this.lastModified = value }
        fun setGeometry(value: T): Builder<T> = apply { this.geometry = value }

        fun build(): LocationOfInterest<T> {
            val survey = survey ?: throw Exception("Expected a survey")
            val job = job ?: throw Exception("Expected a job")
            val created = created ?: throw Exception("Expected a creation timestamp")
            val lastModified = lastModified ?: throw Exception("Expected a last modified timestamp")
            val geometry = geometry ?: throw Exception("Expected a geometry")

            return LocationOfInterest(
                id,
                survey,
                job,
                customId,
                caption,
                lastModified,
                created,
                geometry,
            )
        }
    }

    fun builder(init: Builder<T>.() -> Unit): Builder<T> {
        val builder = Builder<T>()
        builder.init()
        return builder
    }

    fun toBuilder() = builder {
        id = this@LocationOfInterest.id
        survey = this@LocationOfInterest.survey
        job = this@LocationOfInterest.job
        customId = this@LocationOfInterest.customId
        caption = this@LocationOfInterest.caption
        created = this@LocationOfInterest.created
        lastModified = this@LocationOfInterest.lastModified
        geometry = this@LocationOfInterest.geometry
    }

    companion object {
        @JvmStatic
        fun <T : Geometry> newBuilder(): Builder<T> = Builder<T>()
    }
}