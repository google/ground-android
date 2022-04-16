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
package com.google.android.gnd.model.mutation

import com.google.android.gnd.model.form.Form
import com.google.android.gnd.model.observation.ResponseDelta
import com.google.android.gnd.util.toImmutableList
import com.google.common.collect.ImmutableList
import java.util.*

data class ObservationMutation(
    override val id: Long? = null,
    override val type: Type = Type.UNKNOWN,
    override val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
    override val projectId: String = "",
    override val featureId: String = "",
    override val layerId: String = "",
    override val userId: String = "",
    override val clientTimestamp: Date = Date(),
    override val retryCount: Long = 0,
    override val lastError: String = "",
    var observationId: String = "",
    var form: Form? = null,
    var responseDeltas: ImmutableList<ResponseDelta> = ImmutableList.of()
) : Mutation() {

    override fun toBuilder(): Builder {
        return Builder().also {
            it.observationId = this.observationId
            it.form = this.form
            it.responseDeltas = this.responseDeltas
        }.fromMutation(this) as Builder
    }

    override fun toString(): String =
        super.toString() + "deltas= $responseDeltas"

    inner class Builder : Mutation.Builder<ObservationMutation>() {
        var observationId: String = ""
            @JvmSynthetic set
        var form: Form? = null
            @JvmSynthetic set
        var responseDeltas: ImmutableList<ResponseDelta> = ImmutableList.of()
            @JvmSynthetic set

        fun setObservationId(id: String): Builder = apply {
            this.observationId = id
        }

        fun setForm(form: Form): Builder = apply {
            this.form = form
        }

        fun setResponseDeltas(deltas: ImmutableList<ResponseDelta>): Builder = apply {
            this.responseDeltas = deltas
        }

        override fun build(): ObservationMutation = ObservationMutation(
            id,
            type,
            syncStatus,
            projectId,
            featureId,
            layerId,
            userId,
            clientTimestamp,
            retryCount,
            lastError,
            observationId,
            form,
            responseDeltas
        )
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = ObservationMutation().toBuilder()

        @JvmStatic
        fun filter(mutations: ImmutableList<out Mutation>): ImmutableList<ObservationMutation> {
            return mutations.toTypedArray().filter {
                when (it) {
                    is FeatureMutation -> false
                    is ObservationMutation -> true
                    else -> false
                }
            }.map { it as ObservationMutation }.toImmutableList()
        }
    }
}