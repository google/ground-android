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
import java.util.*
import javax.annotation.OverridingMethodsMustInvokeSuper

/** Base class for user-defined locations of interest shown on the map. */
sealed class LocationOfInterest {
    // TODO: Once all callers are converted to Kotlin, we don't need these properties.
    val isPoint: Boolean
        get() = this is PointOfInterest
    val isPolygon: Boolean
        get() = this is AreaOfInterest

    /** A system-defined ID for this LOI. */
    abstract val id: String

    /** The survey associated with this LOI. */
    abstract val survey: Survey

    /** The job associated with this LOI. */
    abstract val job: Job

    /** A user-specified ID for this location of interest. */
    abstract val customId: String?

    /** A human readable caption for this location of interest. */
    abstract val caption: String?

    /** User and time audit info pertaining to the creation of this LOI. */
    abstract val created: AuditInfo

    /** User and time audit info pertaining to the last modification of this LOI. */
    abstract val lastModified: AuditInfo

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

    // TODO: Remove once all callers are converted to Kotlin. We only retain this for Java interop.
    open class Builder {
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

        open fun setId(value: String): Builder = apply { this.id = value }
        open fun setSurvey(value: Survey): Builder = apply { this.survey = value }
        open fun setJob(value: Job): Builder = apply { this.job = value }
        open fun setCustomId(value: String?): Builder = apply { this.customId = value }
        open fun setCaption(value: String?): Builder = apply { this.caption = value }
        open fun setCreated(value: AuditInfo): Builder = apply { this.created = value }
        open fun setLastModified(value: AuditInfo): Builder = apply { this.lastModified = value }
    }

    open fun builder(init: Builder.() -> Unit): Builder {
        val builder = Builder()
        builder.init()
        return builder
    }

    open fun toBuilder() = builder {
        id = this@LocationOfInterest.id
        survey = this@LocationOfInterest.survey
        job = this@LocationOfInterest.job
        customId = this@LocationOfInterest.customId
        caption = this@LocationOfInterest.caption
        created = this@LocationOfInterest.created
        lastModified = this@LocationOfInterest.lastModified
    }

    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()
    }
}