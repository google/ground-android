/*
 * Copyright 2023 Google LLC
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

import com.google.android.ground.model.AuditInfo as AuditInfoModel
import com.google.android.ground.proto.auditInfo
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.protobuf.timestamp
import com.sharedtest.FakeData.USER
import java.util.Date
import org.junit.Test

class AuditInfoConverterTest {

  @Test
  fun `Converts to AuditInfo from AuditInfo proto`() {
    val taskProto = auditInfo {
      userId = USER.id
      displayName = USER.displayName
      photoUrl = USER.photoUrl.orEmpty()
      clientTimestamp = timestamp { seconds = 987654321 }
      serverTimestamp = timestamp { seconds = 9876543210 }
    }
    assertThat(AuditInfoConverter.toAuditInfo(taskProto))
      .isEqualTo(AuditInfoModel(user = USER, Date(987654321L * 1000), Date(9876543210L * 1000)))
  }

  @Test
  fun `Sets serverTimestamp to null if missing from AuditInfo proto`() {
    val taskProto = auditInfo {
      userId = USER.id
      displayName = USER.displayName
      photoUrl = USER.photoUrl.orEmpty()
      clientTimestamp = timestamp { seconds = 987654321 }
    }
    assertThat(AuditInfoConverter.toAuditInfo(taskProto).serverTimestamp).isNull()
  }

  @Test
  fun `Converts to AuditInfo from AuditInfoNestedObject`() {
    val taskNestedObject =
      AuditInfoNestedObject(
        user = UserNestedObject(USER.id, USER.email, USER.displayName),
        clientTimestamp = Timestamp(Date(987654321L * 1000)),
        serverTimestamp = Timestamp(Date(9876543210L * 1000)),
      )
    assertThat(AuditInfoConverter.toAuditInfo(taskNestedObject))
      .isEqualTo(AuditInfoModel(user = USER, Date(987654321L * 1000), Date(9876543210L * 1000)))
  }
}
