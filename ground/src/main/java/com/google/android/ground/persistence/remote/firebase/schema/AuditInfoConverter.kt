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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.User
import com.google.android.ground.proto.AuditInfo as AuditInfoProto
import java.util.Date

/** Converts between Firestore nested objects and [AuditInfo] instances. */
internal object AuditInfoConverter {

  private fun com.google.protobuf.Timestamp.isZero() = this.seconds == 0L

  private fun com.google.protobuf.Timestamp.toDate() = Date(this.seconds * 1000)

  fun toAuditInfo(info: AuditInfoProto): AuditInfo =
    AuditInfo(
      User(info.userId, "", info.displayName, info.photoUrl.ifEmpty { null }),
      info.clientTimestamp.toDate(),
      if (info.serverTimestamp.isZero()) {
        null
      } else {
        info.serverTimestamp.toDate()
      },
    )
}
