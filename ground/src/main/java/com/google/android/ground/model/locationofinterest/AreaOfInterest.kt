/*
 * Copyright 2021 Google LLC
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
import com.google.android.ground.model.mutation.Mutation
import com.google.common.collect.ImmutableList

/** User-defined LOI consisting of a polygon or multi-polygon. */
data class AreaOfInterest(
    override val id: String,
    override val survey: Survey,
    override val job: Job,
    override val customId: String?,
    override val caption: String?,
    override val created: AuditInfo,
    override val lastModified: AuditInfo,
    val vertices: ImmutableList<Point>,
) : LocationOfInterest() {
    override fun toMutation(type: Mutation.Type, userId: String): LocationOfInterestMutation {
        return super.toMutation(type, userId).toBuilder().setPolygonVertices(vertices).build()
    }

    // TODO: Remove once all callers are converted to Kotlin. We only retain this for Java interop.
    class Builder : LocationOfInterest.Builder() {
        var vertices: ImmutableList<Point> = ImmutableList.of()
            @JvmSynthetic set

        // Preserve typing.
        override fun setId(value: String): Builder = apply { super.setId(value) }
        override fun setCaption(value: String?): Builder = apply { super.setCaption(value) }
        override fun setCreated(value: AuditInfo): Builder = apply { super.setCreated(value) }
        override fun setCustomId(value: String?): Builder = apply { super.setCustomId(value) }
        override fun setJob(value: Job): Builder = apply { super.setJob(value) }
        override fun setSurvey(value: Survey): Builder = apply { super.setSurvey(value) }
        override fun setLastModified(value: AuditInfo): Builder =
            apply { super.setLastModified(value) }

        fun setVertices(value: ImmutableList<Point>) = apply { vertices = value }

        fun build(): AreaOfInterest {
            val survey = survey ?: throw Exception("Expected a survey")
            val job = job ?: throw Exception("Expected a job")
            val created = created ?: throw Exception("Expected a creation timestamp")
            val lastModified = lastModified ?: throw Exception("Expected a last modified timestamp")

            return AreaOfInterest(
                id,
                survey,
                job,
                customId,
                caption,
                created,
                lastModified,
                vertices
            )
        }
    }

    override fun toBuilder(): Builder =
        Builder().setVertices(vertices).setCaption(caption).setJob(job)
            .setCreated(created).setId(id)
            .setCustomId(customId).setSurvey(survey).setLastModified(lastModified)

    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()
    }
}