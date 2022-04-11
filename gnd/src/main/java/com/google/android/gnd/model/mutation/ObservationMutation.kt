package com.google.android.gnd.model.mutation

import com.google.android.gnd.model.form.Form
import com.google.android.gnd.model.observation.ResponseDelta
import com.google.android.gnd.util.toImmutableList
import com.google.common.collect.ImmutableList
import java.util.*

class ObservationMutation(
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
    override fun toBuilder(): Builder = this.Builder()

    override fun toString(): String =
        super.toString() + "deltas= $responseDeltas"

    inner class Builder : Mutation.Builder<ObservationMutation>() {
        var observationId: String = ""
            private set
        var form: Form? = null
            private set
        var responseDeltas: ImmutableList<ResponseDelta> = ImmutableList.of()
            private set

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
            super.id,
            super.type,
            super.syncStatus,
            super.projectId,
            super.featureId,
            super.layerId,
            super.userId,
            super.clientTimestamp,
            super.retryCount,
            super.lastError,
            this.observationId,
            this.form,
            this.responseDeltas
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