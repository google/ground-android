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
package org.groundplatform.android.model.mutation

import java.util.Date

/**
 * Represents a mutation that can be applied to local data and queued for sync with the remote data
 * store.
 */
sealed class Mutation {
  /** A unique identifier for this mutation. */
  abstract val id: Long?
  /** The kind of data manipulation operation this mutation represents. See [Mutation.Type]. */
  abstract val type: Type
  /**
   * Indicates whether or not this mutation has been reflected in remote storage. See
   * [Mutation.SyncStatus].
   */
  abstract val syncStatus: SyncStatus
  /** The ID of the survey containing the data this mutation alters. */
  abstract val surveyId: String
  /** The ID of the LOI containing the data this mutation alters. */
  abstract val locationOfInterestId: String
  /** The ID of the user who initiated this mutation. */
  abstract val userId: String
  /** The time at which this mutation was issued on the client. */
  abstract val clientTimestamp: Date
  /** The number of times the system has attempted to apply this mutation to remote storage. */
  abstract val retryCount: Long
  /** The error last encountered when attempting to apply this mutation to the remote storage. */
  abstract val lastError: String
  /**
   * A unique identifier tying this mutation to a singular instance of data collection. Multiple
   * mutations are associated using this identifier.
   */
  abstract val collectionId: String

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
