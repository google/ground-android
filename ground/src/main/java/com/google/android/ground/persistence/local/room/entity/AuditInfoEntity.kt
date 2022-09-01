/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.Embedded
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.persistence.local.room.models.UserDetails
import java.util.*
import java8.util.Optional

/** User details and timestamp for creation or modification of a model object. */
data class AuditInfoEntity(
  /**
   * Returns the user initiating the related action. This can never be null, since users must always
   * be logged in to make changes.
   */
  @Embedded(prefix = "user_") val user: UserDetails,

  /** Returns the time at which the user action was initiated, according to the user's device. */
  val clientTimestamp: Long,

  /**
   * Returns the time at which the server received the requested change according to the server's
   * internal clock, or null if the updated server time was not yet received.
   */
  val serverTimestamp: Long?
) {

  companion object {
    /** Converts a model object into a local db entity. */
    @JvmStatic
    fun fromObject(o: AuditInfo): AuditInfoEntity =
      AuditInfoEntity(
        user = UserDetails.fromUser(o.user),
        clientTimestamp = o.clientTimestamp.time,
        serverTimestamp = o.serverTimestamp.map { obj: Date -> obj.time }.orElse(null)
      )

    @JvmStatic
    fun toObject(entity: AuditInfoEntity): AuditInfo {
      return AuditInfo(
        UserDetails.toUser(entity.user),
        Date(entity.clientTimestamp),
        Optional.ofNullable(entity.serverTimestamp).map { Date(it!!) }
      )
    }
  }
}
