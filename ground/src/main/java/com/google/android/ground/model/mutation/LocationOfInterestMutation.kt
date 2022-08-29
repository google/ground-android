/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.model.mutation

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import java8.util.Optional
import java.util.*

data class LocationOfInterestMutation(
    override val id: Long? = null,
    override val type: Type = Type.UNKNOWN,
    override val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
    override val surveyId: String = "",
    override val locationOfInterestId: String = "",
    override val userId: String = "",
    override val clientTimestamp: Date = Date(),
    override val retryCount: Long = 0,
    override val lastError: String = "",
    val jobId: String = "",
    val location: Optional<Point> = Optional.empty(),
    val polygonVertices: ImmutableList<Point> = ImmutableList.of(),
) : Mutation() {

    override fun toBuilder(): Builder {
        return Builder().also {
            it.jobId = this.jobId
            it.location = this.location
            it.polygonVertices = this.polygonVertices
        }.fromMutation(this) as Builder
    }

    // TODO: Once callers of the class are all converted to kotlin, we won't need builders.
    inner class Builder : Mutation.Builder<LocationOfInterestMutation>() {
        var jobId: String = ""
            @JvmSynthetic set
        var location: Optional<Point> = Optional.empty()
            @JvmSynthetic set
        var polygonVertices: ImmutableList<Point> = ImmutableList.of()
            @JvmSynthetic set

        fun setJobId(jobId: String): Builder = apply {
            this.jobId = jobId
        }

        fun setLocation(newLocation: Optional<Point>): Builder = apply {
            this.location = newLocation
        }

        fun setPolygonVertices(newVertices: ImmutableList<Point>): Builder = apply {
            this.polygonVertices = newVertices
        }

        override fun build() =
            LocationOfInterestMutation(
                id,
                type,
                syncStatus,
                surveyId,
                locationOfInterestId,
                userId,
                clientTimestamp,
                retryCount,
                lastError,
                jobId,
                location,
                polygonVertices
            )
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = LocationOfInterestMutation().toBuilder()

        @JvmStatic
        fun filter(mutations: ImmutableList<out Mutation>): ImmutableList<LocationOfInterestMutation> {
            return mutations.toTypedArray().filter {
                when (it) {
                    is LocationOfInterestMutation -> true
                    is SubmissionMutation -> false
                    else -> false
                }
            }.map { it as LocationOfInterestMutation }.toImmutableList()
        }
    }
}
