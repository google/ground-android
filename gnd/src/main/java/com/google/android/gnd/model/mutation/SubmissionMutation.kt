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

import com.google.android.gnd.model.task.Task
import com.google.android.gnd.model.submission.ResponseDelta
import com.google.android.gnd.util.toImmutableList
import com.google.common.collect.ImmutableList
import java.util.*

data class SubmissionMutation(
    override val id: Long? = null,
    override val type: Type = Type.UNKNOWN,
    override val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
    override val surveyId: String = "",
    override val featureId: String = "",
    override val jobId: String = "",
    override val userId: String = "",
    override val clientTimestamp: Date = Date(),
    override val retryCount: Long = 0,
    override val lastError: String = "",
    var submissionId: String = "",
    var task: Task? = null,
    var responseDeltas: ImmutableList<ResponseDelta> = ImmutableList.of()
) : Mutation() {

    override fun toBuilder(): Builder {
        return Builder().also {
            it.submissionId = this.submissionId
            it.task = this.task
            it.responseDeltas = this.responseDeltas
        }.fromMutation(this) as Builder
    }

    override fun toString(): String =
        super.toString() + "deltas= $responseDeltas"

    inner class Builder : Mutation.Builder<SubmissionMutation>() {
        var submissionId: String = ""
            @JvmSynthetic set
        var task: Task? = null
            @JvmSynthetic set
        var responseDeltas: ImmutableList<ResponseDelta> = ImmutableList.of()
            @JvmSynthetic set

        fun setSubmissionId(id: String): Builder = apply {
            this.submissionId = id
        }

        fun setTask(task: Task): Builder = apply {
            this.task = task
        }

        fun setResponseDeltas(deltas: ImmutableList<ResponseDelta>): Builder = apply {
            this.responseDeltas = deltas
        }

        override fun build(): SubmissionMutation = SubmissionMutation(
            id,
            type,
            syncStatus,
            surveyId,
            featureId,
            jobId,
            userId,
            clientTimestamp,
            retryCount,
            lastError,
            submissionId,
            task,
            responseDeltas
        )
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = SubmissionMutation().toBuilder()

        @JvmStatic
        fun filter(mutations: ImmutableList<out Mutation>): ImmutableList<SubmissionMutation> {
            return mutations.toTypedArray().filter {
                when (it) {
                    is FeatureMutation -> false
                    is SubmissionMutation -> true
                    else -> false
                }
            }.map { it as SubmissionMutation }.toImmutableList()
        }
    }
}