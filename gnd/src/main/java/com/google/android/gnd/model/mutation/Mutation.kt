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
package com.google.android.gnd.model.mutation

import java.util.*

/**
 * Represents a mutation that can be applied to local data and queued for sync with the remote data
 * store.
 */
sealed class Mutation {
    abstract val id: Long?
    abstract val type: Type
    abstract val syncStatus: SyncStatus
    abstract val projectId: String
    abstract val featureId: String
    abstract val layerId: String
    abstract val userId: String
    abstract val clientTimestamp: Date
    abstract val retryCount: Long
    abstract val lastError: String

    enum class Type {
        /** Indicates a new entity should be created.  */
        CREATE,

        /** Indicates an existing entity should be updated.  */
        UPDATE,

        /** Indicates an existing entity should be marked for deletion.  */
        DELETE,

        /** Indicates database skew or an implementation bug.  */
        UNKNOWN
    }

    /** Status of mutation being applied to remote data store.  */
    enum class SyncStatus {
        /** Invalid or unrecognized state.  */
        UNKNOWN,

        /** Sync pending. Pending includes failed sync attempts pending retry.  */
        PENDING,

        /** Sync currently in progress.  */
        IN_PROGRESS,

        /** Sync complete.  */
        COMPLETED,

        /** Failed indicates all retries have failed.  */
        FAILED
    }

    abstract fun toBuilder(): Builder<*>

    override fun toString(): String {
        return "$syncStatus $type $clientTimestamp"
    }

    // TODO: Once callers of the class are all converted to kotlin, we won't need builders.
    abstract inner class Builder<T : Mutation> {
        var id: Long? = null
            private set
        var type: Type = Type.UNKNOWN
            private set
        var syncStatus: SyncStatus = SyncStatus.UNKNOWN
            private set
        var projectId: String = ""
            private set
        var featureId: String = ""
            private set
        var layerId: String = ""
            private set
        var userId: String = ""
            private set
        var clientTimestamp: Date = Date()
            private set
        var retryCount: Long = 0
            private set
        var lastError: String = ""
            private set

        fun setId(id: Long): Builder<T> = apply { this.id = id }
        fun setType(type: Type): Builder<T> = apply { this.type = type }
        fun setSyncStatus(syncStatus: SyncStatus): Builder<T> =
            apply { this.syncStatus = syncStatus }

        fun setProjectId(projectId: String): Builder<T> = apply { this.projectId = projectId }
        fun setFeatureId(featureId: String): Builder<T> = apply { this.featureId = featureId }
        fun setLayerId(layerId: String): Builder<T> = apply { this.layerId = layerId }
        fun setUserId(userId: String): Builder<T> = apply { this.userId = userId }
        fun setClientTimestamp(timestamp: Date): Builder<T> =
            apply { this.clientTimestamp = timestamp }

        fun setRetryCount(count: Long): Builder<T> = apply { this.retryCount = count }
        fun setLastError(error: String): Builder<T> = apply { this.lastError = error }

        abstract fun build(): T
    }

    companion object {
        @JvmStatic
        fun byDescendingClientTimestamp(): Comparator<Mutation> {
            return Comparator { m1: Mutation, m2: Mutation -> m2.clientTimestamp.compareTo(m1.clientTimestamp) }
        }
    }
}