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

import java.util.Date

/**
 * Represents a mutation that can be applied to local data and queued for sync with the remote data
 * store.
 */
sealed class Mutation {
  abstract val id: Long?
  abstract val type: Type
  abstract val syncStatus: SyncStatus
  abstract val surveyId: String
  abstract val locationOfInterestId: String
  abstract val userId: String
  abstract val clientTimestamp: Date
  abstract val retryCount: Long
  abstract val lastError: String

  enum class Type {
    /** Indicates a new entity should be created. */
    CREATE,

    /** Indicates an existing entity should be updated. */
    UPDATE,

    /** Indicates an existing entity should be marked for deletion. */
    DELETE,

    /** Indicates database skew or an implementation bug. */
    UNKNOWN,
  }

  /** Status of mutation being applied to remote data store. */
  enum class SyncStatus {
    /** Invalid or unrecognized state. */
    UNKNOWN,

    /** Sync pending. Pending includes failed sync attempts pending retry. */
    PENDING,

    /** Sync currently in progress. */
    IN_PROGRESS,

    /**
     * Upload of media associated with a given mutation is pending. Pending includes failed sync
     * attempts pending retry.
     */
    MEDIA_UPLOAD_PENDING,

    /** Upload of media associated with a given mutation is in progress. */
    MEDIA_UPLOAD_IN_PROGRESS,

    /** Upload of media associated with a given mutation is awaiting another attempt. */
    MEDIA_UPLOAD_AWAITING_RETRY,

    /** Sync complete. */
    COMPLETED,

    /** Failed indicates all retries have failed. */
    FAILED,
  }

  override fun toString(): String = "$syncStatus $type $clientTimestamp"

  companion object {
    @JvmStatic
    fun byDescendingClientTimestamp(): Comparator<Mutation> =
      Comparator { m1: Mutation, m2: Mutation ->
        m2.clientTimestamp.compareTo(m1.clientTimestamp)
      }
  }
}
