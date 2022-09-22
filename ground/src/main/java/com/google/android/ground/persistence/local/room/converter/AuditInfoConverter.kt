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
package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.persistence.local.LocalDataStoreConverter
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity
import com.google.android.ground.persistence.local.room.models.UserDetails
import java.util.*
import java8.util.Optional

object AuditInfoConverter : LocalDataStoreConverter<AuditInfo, AuditInfoEntity> {
  override fun convertToDataStoreObject(o: AuditInfo): AuditInfoEntity =
    AuditInfoEntity(
      user = UserDetails.fromUser(o.user),
      clientTimestamp = o.clientTimestamp.time,
      serverTimestamp = o.serverTimestamp.map { obj: Date -> obj.time }.orElse(null)
    )

  override fun convertFromDataStoreObject(entity: AuditInfoEntity): AuditInfo {
    return AuditInfo(
      UserDetails.toUser(entity.user),
      Date(entity.clientTimestamp),
      Optional.ofNullable(entity.serverTimestamp).map { Date(it!!) }
    )
  }
}
