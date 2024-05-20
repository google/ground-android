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
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.firebase.Timestamp

/** Converts between Firestore nested objects and [AuditInfo] instances. */
internal object AuditInfoConverter {

  @Throws(DataStoreException::class)
  fun toAuditInfo(doc: AuditInfoNestedObject): AuditInfo {
    DataStoreException.checkNotNull(doc.clientTimestamp, "clientTimestamp")
    return AuditInfo(
      UserConverter.toUser(doc.user),
      doc.clientTimestamp!!.toDate(),
      doc.serverTimestamp?.toDate(),
    )
  }

  @JvmStatic
  fun fromMutationAndUser(mutation: Mutation, user: User): AuditInfoNestedObject =
    AuditInfoNestedObject(
      UserConverter.toNestedObject(user),
      Timestamp(mutation.clientTimestamp),
      null,
    )
}
